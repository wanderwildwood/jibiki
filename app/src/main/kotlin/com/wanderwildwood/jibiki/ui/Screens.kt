package com.wanderwildwood.jibiki.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.divider.HorizontalDividerMMD
import com.mudita.mmd.components.lazy.LazyColumnMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.jibiki.R
import com.wanderwildwood.jibiki.words.Entry
import com.wanderwildwood.jibiki.words.Found
import com.wanderwildwood.jibiki.words.Sense

/** The bar every screen has: a title, Back where there is somewhere to go back to, and About. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Bar(title: String, onBack: (() -> Unit)?, onAbout: () -> Unit) {
    TopAppBarMMD(
        title = { TextMMD(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = { if (onBack != null) BarButton(Icons.Back, stringResource(R.string.cd_back), onBack) },
        actions = { BarButton(Icons.Info, stringResource(R.string.cd_about), onAbout) },
    )
}

/** A screen with nothing to show yet but a sentence: unpacking, or a selection too long to look up. */
@Composable
fun MessageScreen(title: String, message: String, onBack: (() -> Unit)?, onAbout: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { Bar(title, onBack, onAbout) },
    ) { padding ->
        TextMMD(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(padding).padding(20.dp),
        )
    }
}

/**
 * The home screen: a field to type into, and under it either the words that begin with what
 * has been typed, or, while it is empty, the words looked up lately.
 */
@Composable
fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    suggestions: List<String>,
    recent: List<String>,
    onLookUp: (String) -> Unit,
    onAbout: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { Bar(stringResource(R.string.app_name), null, onAbout) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp)) {
                TextFieldMMD(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    placeholder = { TextMMD(text = stringResource(R.string.search_hint), style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onLookUp(query) }),
                    modifier = Modifier.weight(1f).focusRequester(focus).textActions(),
                )
                if (query.isNotEmpty()) BarButton(Icons.Close, stringResource(R.string.cd_clear)) { onQuery("") }
            }
            val words = if (query.isBlank()) recent else suggestions
            LazyColumnMMD(modifier = Modifier.fillMaxSize()) {
                if (query.isBlank() && recent.isNotEmpty()) {
                    item(key = "recent") {
                        TextMMD(
                            text = stringResource(R.string.search_recent),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                }
                for (word in words) {
                    item(key = "w:$word") { WordRow(word) { onLookUp(word) } }
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: String, onClick: () -> Unit) {
    TextMMD(
        text = word,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/**
 * Everything the word list has for one selection. Usually one word; "better" brings "good"
 * and "well" with it, each under its own heading.
 *
 * Every word on the page that is not the one being read about can be pressed, to look it up
 * in turn or, when [onWord] says so, to put it in the other app in place of the selection.
 */
@Composable
fun WordScreen(
    title: String,
    found: List<Found>,
    near: List<String>,
    onWord: (String) -> Unit,
    onBack: () -> Unit,
    onAbout: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { Bar(title, onBack, onAbout) },
    ) { padding ->
        val list = rememberLazyListState()
        val scope = rememberCoroutineScope()
        // Where each word's heading falls in the list, for the line that jumps between them.
        val headings = remember(found) {
            var index = if (found.size > 1) 1 else 0
            found.mapIndexed { i, f ->
                if (i > 0) index++
                val at = index
                index += 1 + f.entries.sumOf { 1 + it.senses.size }
                at
            }
        }
        LazyColumnMMD(state = list, modifier = Modifier.padding(padding).fillMaxSize()) {
            if (found.size > 1) {
                // "running" is also a form of "run", and "run" is usually what was meant; it
                // should not sit forty meanings down with nothing to say it is there.
                item(key = "jump") {
                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)) {
                        WordList(stringResource(R.string.on_this_page), found.map { it.word }) { word ->
                            val i = found.indexOfFirst { it.word == word }
                            scope.launch { list.scrollToItem(headings[i]) }
                        }
                    }
                }
            }
            if (found.isEmpty()) {
                item(key = "none") {
                    Column(Modifier.padding(20.dp)) {
                        TextMMD(text = stringResource(R.string.not_found, title), style = MaterialTheme.typography.bodyMedium)
                        if (near.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            TextMMD(text = stringResource(R.string.not_found_near), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                for (word in near) item(key = "near:$word") { WordRow(word) { onWord(word) } }
            }
            found.forEachIndexed { i, f ->
                if (i > 0) item(key = "rule:$i") { HorizontalDividerMMD(modifier = Modifier.padding(vertical = 8.dp)) }
                item(key = "head:$i") { Heading(f) }
                f.entries.forEachIndexed { j, entry ->
                    item(key = "pos:$i:$j") { PartOfSpeech(entry) }
                    entry.senses.forEachIndexed { k, sense ->
                        item(key = "sense:$i:$j:$k") { SenseBlock(k + 1, entry.pos, sense, onWord) }
                    }
                }
            }
            item(key = "foot") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Heading(found: Found) {
    // One spoken form for the heading: the first entry that has one. "Bank" the noun and the
    // verb sound the same; where parts of speech differ ("record"), each says its own below.
    val ipa = found.entries.firstNotNullOfOrNull { it.ipa }
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) {
        TextMMD(text = found.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (ipa != null) TextMMD(text = "/$ipa/", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PartOfSpeech(entry: Entry) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
    ) {
        TextMMD(
            text = stringResource(posName(entry.pos)),
            style = MaterialTheme.typography.titleSmall,
            fontStyle = FontStyle.Italic,
        )
    }
}

private fun posName(pos: String) = when (pos) {
    "n" -> R.string.pos_n
    "v" -> R.string.pos_v
    "a" -> R.string.pos_a
    else -> R.string.pos_r
}

@Composable
private fun SenseBlock(number: Int, pos: String, sense: Sense, onWord: (String) -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp)) {
        TextMMD(
            text = "$number.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(28.dp),
        )
        Column {
            TextMMD(text = sense.definition, style = MaterialTheme.typography.bodyMedium)
            for (example in sense.examples.take(2)) {
                TextMMD(
                    text = "“$example”",
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            WordList(stringResource(R.string.same), sense.synonyms, onWord)
            WordList(stringResource(R.string.opposite), sense.antonyms, onWord)
            WordList(
                stringResource(if (pos == "a") R.string.near else R.string.kind_of),
                sense.related.take(6),
                onWord,
            )
        }
    }
}

/**
 * A label and a run of words that wrap like a sentence, each one big enough to press on the
 * panel: a link drawn inline at text size is a target a finger cannot find.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordList(label: String, words: List<String>, onWord: (String) -> Unit) {
    if (words.isEmpty()) return
    FlowRow(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Box(Modifier.padding(end = 6.dp, top = 8.dp, bottom = 8.dp)) {
            TextMMD(text = "$label:", style = MaterialTheme.typography.labelSmall)
        }
        for (word in words) {
            TextMMD(
                text = word,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onWord(word) }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Pressing a word when the selection came from somewhere it can be written back to: put it
 * there, or read about it first.
 */
@Composable
fun ReplaceDialog(word: String, selection: String, onUse: () -> Unit, onLookUp: () -> Unit, onDismiss: () -> Unit) {
    EInkDialog(onDismiss = onDismiss) {
        OutlinedButtonMMD(onClick = onUse, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            TextMMD(text = stringResource(R.string.replace_use, word, selection), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButtonMMD(onClick = onLookUp, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            TextMMD(text = stringResource(R.string.replace_look_up, word), style = MaterialTheme.typography.bodySmall)
        }
    }
}

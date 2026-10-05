package com.wanderwildwood.jibiki.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.wanderwildwood.jibiki.R
import com.wanderwildwood.jibiki.words.Dictionary
import com.wanderwildwood.jibiki.words.Forms
import com.wanderwildwood.jibiki.words.Found
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The whole app: a stack of words read, over the search screen.
 *
 * Reached from the launcher, the stack starts empty and Back from the first word returns to
 * the search field. Reached from Define in another app, it starts with the selection, and
 * Back from that first page goes back to the app it came from ([onExit]).
 *
 * [selection] and [onReplace] are set when the selection came from a field that can be
 * written in: pressing a word then offers to put it there instead.
 */
@Composable
fun DictionaryApp(
    start: String?,
    selection: String? = null,
    onReplace: ((String) -> Unit)? = null,
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    val dictionary by produceState<Dictionary?>(null) {
        value = withContext(Dispatchers.IO) { Dictionary.open(context) }
    }
    var about by rememberSaveable { mutableStateOf(false) }
    val stack = rememberSaveable(saver = listSaver()) { mutableStateListOf<String>().apply { start?.let(::add) } }
    var offered by remember { mutableStateOf<String?>(null) }
    val onAbout = { about = true }

    fun back() {
        if (stack.isEmpty() || (start != null && stack.size == 1)) onExit() else stack.removeAt(stack.lastIndex)
    }
    BackHandler(enabled = stack.isNotEmpty()) { back() }

    val dict = dictionary
    if (dict == null) {
        MessageScreen(
            title = stringResource(R.string.app_name),
            message = if (Dictionary.needsUnpacking(context)) stringResource(R.string.unpacking) else "",
            onBack = start?.let { { onExit() } },
            onAbout = onAbout,
        )
    } else if (stack.isEmpty()) {
        var query by rememberSaveable { mutableStateOf("") }
        val suggestions by produceState(emptyList<String>(), query) {
            value = withContext(Dispatchers.IO) { dict.startingWith(query) }
        }
        SearchScreen(
            query = query,
            onQuery = { query = it },
            suggestions = suggestions,
            recent = Recent.list(context),
            onLookUp = { stack.add(it) },
            onAbout = onAbout,
        )
    } else {
        val word = stack.last()
        if (Forms.clean(word).isEmpty()) {
            MessageScreen(stringResource(R.string.app_name), stringResource(R.string.too_long), ::back, onAbout)
        } else {
            val page by produceState<Page?>(null, word) {
                value = withContext(Dispatchers.IO) {
                    val found = dict.lookUp(word)
                    Page(found, if (found.isEmpty()) nearby(dict, word) else emptyList())
                }
            }
            LaunchedEffect(page) { page?.found?.firstOrNull()?.let { Recent.add(context, it.word) } }
            val p = page
            if (p == null) {
                // Not "not found" while it is still looking: an empty page would say that.
                MessageScreen(Forms.tidy(word), "", ::back, onAbout)
            } else {
                WordScreen(
                    title = Forms.tidy(word),
                    found = p.found,
                    near = p.near,
                    onWord = { w -> if (onReplace != null) offered = w else stack.add(w) },
                    onBack = ::back,
                    onAbout = onAbout,
                )
            }
        }
    }

    offered?.let { w ->
        ReplaceDialog(
            word = w,
            selection = selection.orEmpty().trim(),
            onUse = { offered = null; onReplace?.invoke(w) },
            onLookUp = { offered = null; stack.add(w) },
            onDismiss = { offered = null },
        )
    }
    if (about) AboutDialog(onDismiss = { about = false })
}

private data class Page(val found: List<Found>, val near: List<String>)

/** For a word not in the list: the words sharing the longest beginning with it. */
private fun nearby(dict: Dictionary, word: String): List<String> {
    val key = Forms.clean(word)
    for (n in key.length - 1 downTo 3) {
        val words = dict.startingWith(key.take(n), limit = 12)
        if (words.isNotEmpty()) return words
    }
    return emptyList()
}

private fun listSaver() = androidx.compose.runtime.saveable.listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { mutableStateListOf<String>().apply { addAll(it) } },
)

/** The last words looked up, newest first, kept on this phone only. */
object Recent {
    private const val MAX = 30

    fun list(context: Context): List<String> =
        prefs(context).getString("words", "").orEmpty().split('\n').filter { it.isNotEmpty() }

    fun add(context: Context, word: String) {
        val words = (listOf(word) + list(context).filter { it != word }).take(MAX)
        prefs(context).edit().putString("words", words.joinToString("\n")).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("recent", Context.MODE_PRIVATE)
}

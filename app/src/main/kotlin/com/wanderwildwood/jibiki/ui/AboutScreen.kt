package com.wanderwildwood.jibiki.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.top_app_bar.TopAppBarMMD
import com.wanderwildwood.jibiki.BuildConfig
import com.wanderwildwood.jibiki.R

private const val MAIN = 0
private const val PRIVACY = 1
private const val TERMS = 2
private const val LICENCES = 3

/**
 * What this is, how it is meant to be reached, and rows for Privacy, Terms and Licences, each a
 * page of its own. The list pages by whole rows like every list here; nothing scrolls.
 *
 * The first line says how to use it from other apps, because that is how it is mostly used
 * and nothing on its own screen would tell a stranger so. The WordNet attribution its licence
 * asks for is on the Licences page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    var page by rememberSaveable { mutableIntStateOf(MAIN) }
    BackHandler { if (page == MAIN) onBack() else page = MAIN }
    if (page == LICENCES) {
        LicencesScreen(onBack = { page = MAIN })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBarMMD(
                title = {
                    TextMMD(
                        text = when (page) {
                            PRIVACY -> stringResource(R.string.about_privacy_title)
                            TERMS -> stringResource(R.string.about_terms_title)
                            else -> stringResource(R.string.about_title, BuildConfig.VERSION_NAME)
                        },
                    )
                },
                navigationIcon = {
                    BarButton(
                        Icons.Close,
                        stringResource(R.string.about_close),
                        { if (page == MAIN) onBack() else page = MAIN },
                    )
                },
            )
        },
    ) { contentPadding ->
        PagedColumn(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (page) {
                PRIVACY -> item { Body(stringResource(R.string.about_privacy)) }
                TERMS -> item { Body(stringResource(R.string.about_terms)) }
                else -> {
                    item { Body(stringResource(R.string.about_what)) }
                    item { NavRow(stringResource(R.string.about_privacy_title)) { page = PRIVACY } }
                    item { NavRow(stringResource(R.string.about_terms_title)) { page = TERMS } }
                    item { NavRow(stringResource(R.string.licences_title)) { page = LICENCES } }
                    item { Llama() }
                }
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    TextMMD(text = text, style = MaterialTheme.typography.bodyMedium)
}

/** A row that opens a page of its own: 48 dp tall to press, with an arrow at the end. */
@Composable
private fun NavRow(text: String, onPress: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onPress),
    ) {
        TextMMD(text = text, style = MaterialTheme.typography.bodyLarge)
        TextMMD(text = "\u203A", style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A llama at the foot of the About, which opens the page a donation goes to. The site's
 * address sits at the start of the same line and opens the site; the llama and its words
 * open the page.
 *
 * Straight to the checkout: the Donate button on the site only leads there anyway. The short
 * square.link form, which is what the site itself links to, so a regenerated checkout follows
 * it. A phone with nothing that opens a web address says so rather than doing nothing.
 */
@Composable
private fun Llama() {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextMMD(
            text = "wanderthe.dev",
            style = MaterialTheme.typography.bodyMedium,
            // The site's address opens the site, the way the llama beside it opens its page.
            modifier = Modifier
                .clickable {
                    openLink(context, "https://wanderthe.dev", context.getString(R.string.about_no_browser_address, "wanderthe.dev"))
                }
                .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically),
        )
        Spacer(Modifier.width(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    openLink(context, "https://square.link/u/AGu8oT10", context.getString(R.string.about_no_browser_address, "square.link/u/AGu8oT10"))
                }
                .heightIn(min = 48.dp).wrapContentHeight(Alignment.CenterVertically),
        ) {
            Image(
                painter = painterResource(R.drawable.llama),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(6.dp))
            TextMMD(text = stringResource(R.string.about_feed_the_llamas), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

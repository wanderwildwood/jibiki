package com.wanderwildwood.jibiki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.jibiki.ui.DictionaryApp
import com.wanderwildwood.jibiki.ui.houseTypography
import com.wanderwildwood.jibiki.ui.monochrome
import com.wanderwildwood.jibiki.ui.DarkSelection
import com.wanderwildwood.jibiki.ui.ArmedScope

/**
 * "Define" in the menu other apps show over selected text.
 *
 * It opens in the task of the app that asked, and Back from the word goes straight back
 * there. When that app says the selection can be written to — a text field, not a page —
 * a word pressed on the page can be put back in its place: that is the thesaurus.
 */
class DefineActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val whole = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (whole.isNullOrBlank()) {
            finish()
            return
        }
        // Anything this long reads as too long to look up; only its start is kept, so a whole
        // document selected is not tidied on the main thread or carried in saved state. A cut
        // selection is never written back over the whole.
        val selection = whole.take(MAX_SELECTION)
        val writable = selection.length == whole.length &&
            !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) && callingActivity != null
        setContent {
            // A steady cursor: a blinking one repaints the panel twice a second for as long
            // as the search field has focus.
            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                ThemeMMD(colorScheme = monochrome, typography = houseTypography) { DarkSelection {
                    ArmedScope {
                        DictionaryApp(
                            start = selection,
                            selection = selection,
                            onReplace = if (writable) { word -> replaceWith(selection, word) } else null,
                            onExit = ::finish,
                        )
                    }
                } }
            }
        }
    }

    private companion object {
        const val MAX_SELECTION = 1000
    }

    private fun replaceWith(selection: String, word: String) {
        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, Casing.match(selection, word)))
        finish()
    }
}

package com.wanderwildwood.jibiki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.jibiki.ui.DictionaryApp
import com.wanderwildwood.jibiki.ui.monochrome

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
        val selection = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        if (selection.isNullOrBlank()) {
            finish()
            return
        }
        val writable = !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) && callingActivity != null
        setContent {
            ThemeMMD(colorScheme = monochrome) {
                DictionaryApp(
                    start = selection,
                    selection = selection,
                    onReplace = if (writable) { word -> replaceWith(selection, word) } else null,
                    onExit = ::finish,
                )
            }
        }
    }

    private fun replaceWith(selection: String, word: String) {
        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, Casing.match(selection, word)))
        finish()
    }
}

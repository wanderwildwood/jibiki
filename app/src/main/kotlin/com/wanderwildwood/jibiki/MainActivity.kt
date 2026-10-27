package com.wanderwildwood.jibiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalCursorBlinkEnabled
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.jibiki.ui.DictionaryApp
import com.wanderwildwood.jibiki.ui.monochrome

/** The home-screen entry: a field to type a word into. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // A steady cursor: a blinking one repaints the panel twice a second for as long
            // as the search field has focus.
            CompositionLocalProvider(LocalCursorBlinkEnabled provides false) {
                ThemeMMD(colorScheme = monochrome) {
                    DictionaryApp(start = null)
                }
            }
        }
    }
}

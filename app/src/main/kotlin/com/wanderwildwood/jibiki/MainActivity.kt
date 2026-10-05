package com.wanderwildwood.jibiki

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mudita.mmd.ThemeMMD
import com.wanderwildwood.jibiki.ui.DictionaryApp
import com.wanderwildwood.jibiki.ui.monochrome

/** The home-screen entry: a field to type a word into. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThemeMMD(colorScheme = monochrome) {
                DictionaryApp(start = null)
            }
        }
    }
}

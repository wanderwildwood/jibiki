package com.wanderwildwood.jibiki.ui

import android.content.Context
import android.content.Intent

/**
 * Translate (tsuyaku), offered on the page for a word this dictionary does not have, when it
 * is on the phone: a word missing from an English word list is often not English.
 *
 * It opens Translate's own panel, the one it shows over selected text in any app, on the word
 * that was looked up; nothing comes back. Where Translate is not installed there is no such row.
 * The same request Messaging makes.
 */
object TranslateApp {
    const val PACKAGE = "com.wanderwildwood.tsuyaku"

    /** The request for [text], or null when Translate is not here to take it. */
    fun intent(context: Context, text: String): Intent? {
        if (text.isBlank()) return null
        val intent = Intent(Intent.ACTION_PROCESS_TEXT)
            .setType("text/plain")
            .setPackage(PACKAGE)
            .putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        return intent.takeIf { it.resolveActivity(context.packageManager) != null }
    }
}

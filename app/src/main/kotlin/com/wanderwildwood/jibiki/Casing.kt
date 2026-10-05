package com.wanderwildwood.jibiki

/** A replacement written the way the word it replaces was, so a sentence stays a sentence. */
object Casing {
    fun match(selection: String, word: String): String {
        val core = selection.trim()
        val lead = selection.substring(0, selection.indexOf(core.firstOrNull() ?: return word))
        val trail = selection.substring(lead.length + core.length)
        val letters = core.filter { it.isLetter() }
        val cased = when {
            letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase()
            letters.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
            else -> word
        }
        // The spaces a selection took with it go back, so "the cat " does not lose its space.
        return lead + cased + trail
    }
}

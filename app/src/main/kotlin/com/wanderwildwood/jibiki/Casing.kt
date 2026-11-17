package com.wanderwildwood.jibiki

/** A replacement written the way the word it replaces was, so a sentence stays a sentence. */
object Casing {
    fun match(selection: String, word: String): String {
        // Everything before the first letter or digit and after the last one: the spaces,
        // quotes and commas the selection took with it, which the lookup left out.
        val first = selection.indexOfFirst { it.isLetterOrDigit() }
        if (first < 0) return word
        val last = selection.indexOfLast { it.isLetterOrDigit() }
        val lead = selection.substring(0, first)
        val core = selection.substring(first, last + 1)
        val trail = selection.substring(last + 1)
        val letters = core.filter { it.isLetter() }
        val cased = when {
            letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase()
            letters.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
            else -> word
        }
        // What the selection took with it goes back, so "the cat, " keeps its comma and space.
        return lead + cased + trail
    }
}

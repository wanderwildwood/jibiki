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
        return lead + possessive(core, cased, lead, trail) + trail
    }

    private fun isApostrophe(c: Char) = c == '\'' || c == '\u2019'

    /**
     * The possessive the lookup left out to find the word ("dog's" is looked up as "dog") put
     * back on the replacement: "dog's" becomes "hound's" and "dogs'" becomes "hounds'", with
     * the apostrophe the text used. A word in single quotes ("'dogs'") is not a possessive.
     */
    private fun possessive(core: String, word: String, lead: String, trail: String): String {
        if (word.isEmpty()) return word
        val shout = word.any { it.isLetter() } && word.filter { it.isLetter() }.all { it.isUpperCase() }
        if (core.length > 2 && isApostrophe(core[core.length - 2]) && core.last().lowercaseChar() == 's') {
            if (word.length > 1 && isApostrophe(word[word.length - 2]) && word.last().lowercaseChar() == 's') return word
            return word + core.takeLast(2)
        }
        val pluralPossessive = trail.firstOrNull()?.let(::isApostrophe) == true &&
            lead.none(::isApostrophe) && core.length > 1 && core.last().lowercaseChar() == 's'
        if (!pluralPossessive || word.last().lowercaseChar() == 's') return word
        return plural(word).let { if (shout) it.uppercase() else it }
    }

    /** The plain English plural, for the "s'" put back: "fox" → "foxes", "city" → "cities". */
    private fun plural(word: String): String {
        val lower = word.lowercase()
        return when {
            lower.endsWith("x") || lower.endsWith("z") || lower.endsWith("ch") || lower.endsWith("sh") -> word + "es"
            lower.length > 1 && lower.endsWith("y") && lower[lower.length - 2] !in "aeiou" -> word.dropLast(1) + "ies"
            else -> word + "s"
        }
    }
}

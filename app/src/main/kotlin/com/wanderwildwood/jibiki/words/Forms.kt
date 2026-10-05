package com.wanderwildwood.jibiki.words

/**
 * Turning what someone selected into something the word list can be asked about.
 *
 * Nothing here touches the database, so all of it is tested on the JVM.
 */
object Forms {

    /** Longer than this, a selection is a passage rather than a word, and is not looked up. */
    const val MAX_WORDS = 5

    private val edges = Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}.]+$""")
    private val space = Regex("""\s+""")

    /**
     * A selection as the dictionary keys it: trimmed of the punctuation a finger takes with a
     * word ("“Hello,”" → "hello"), curly apostrophes made straight, runs of space made one, and
     * lower case. A trailing full stop is kept only where it belongs to the word ("etc."), which
     * is decided later by trying both.
     *
     * Empty when nothing word-like is left, or the selection is longer than [MAX_WORDS].
     */
    fun clean(selection: String): String {
        val text = tidy(selection)
        if (text.isEmpty() || text.count { it == ' ' } >= MAX_WORDS) return ""
        return text.lowercase()
    }

    /** A selection as it should be shown: [clean] without the lower-casing or the length limit. */
    fun tidy(selection: String): String =
        selection
            .replace('\u2019', '\'')
            .replace('\u2018', '\'')
            .replace('\u00A0', ' ')
            .replace(space, " ")
            .replace(edges, "")
            .trim()

    /**
     * The ways a cleaned selection may be written in the word list, most likely first: as
     * given, without a trailing full stop or possessive, and a phrase with hyphens or spaces
     * swapped the way WordNet sometimes keys it ("well known" is listed as "well-known").
     */
    fun spellings(key: String): List<String> {
        if (key.isEmpty()) return emptyList()
        val out = LinkedHashSet<String>()
        val bases = buildList {
            add(key)
            if (key.endsWith('.')) add(key.dropLast(1))
            if (key.endsWith("'s")) add(key.dropLast(2))
            if (key.endsWith("s'")) add(key.dropLast(1))
        }
        for (base in bases) {
            out += base
            if (' ' in base) out += base.replace(' ', '-')
            if ('-' in base) out += base.replace('-', ' ')
        }
        return out.filter { it.isNotEmpty() }
    }

    /** A base form a regular English ending points back to, and the part of speech it must be. */
    data class Base(val word: String, val pos: String)

    /**
     * WordNet's own detachment rules (morphy) for each part of speech, plus undoing the
     * doubled consonant English adds before -ing, -ed, -er and -est ("stopped" → "stop").
     *
     * The part of speech is what keeps this honest: "better" read as a comparative points to
     * "bet", but "bet" is no adjective, so the lookup drops it. Irregular forms ("geese",
     * "ran") come from the word list, not from here.
     *
     * A phrase bends on its first word ("running into" → "run into") or its last.
     */
    fun bases(spelling: String): List<Base> {
        val words = spelling.split(' ')
        if (words.size == 1) return baseForms(spelling)
        val out = LinkedHashSet<Base>()
        baseForms(words.first()).forEach { out += Base((listOf(it.word) + words.drop(1)).joinToString(" "), it.pos) }
        baseForms(words.last()).forEach { out += Base((words.dropLast(1) + it.word).joinToString(" "), it.pos) }
        return out.toList()
    }

    fun baseForms(word: String): List<Base> {
        val out = LinkedHashSet<Base>()
        for ((pos, rules) in RULES) {
            for ((ending, replacement) in rules) {
                if (word.length > ending.length + 1 && word.endsWith(ending)) {
                    val stem = word.dropLast(ending.length)
                    out += Base(stem + replacement, pos)
                    if (replacement.isEmpty() && ending in DOUBLING && stem.length >= 3 &&
                        stem[stem.length - 1] == stem[stem.length - 2] && stem.last() !in "aeiouyw"
                    ) {
                        out += Base(stem.dropLast(1), pos)
                    }
                }
            }
        }
        return out.filter { it.word != word }
    }

    private val DOUBLING = setOf("ing", "ed", "er", "est")

    private val RULES = mapOf(
        "n" to listOf(
            "s" to "", "ses" to "s", "xes" to "x", "zes" to "z", "ches" to "ch", "shes" to "sh",
            "men" to "man", "ies" to "y",
        ),
        "v" to listOf(
            "s" to "", "ies" to "y", "es" to "e", "es" to "", "ed" to "e", "ed" to "",
            "ing" to "e", "ing" to "", "ied" to "y",
        ),
        "a" to listOf("er" to "", "est" to "", "er" to "e", "est" to "e", "ier" to "y", "iest" to "y"),
    )
}

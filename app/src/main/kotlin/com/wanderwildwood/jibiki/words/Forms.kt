package com.wanderwildwood.jibiki.words

/**
 * Turning what someone selected into something the word list can be asked about.
 *
 * Nothing here touches the database, so all of it is tested on the JVM.
 */
object Forms {

    /** More words than this, a selection is a passage rather than a word, and is not looked up. */
    const val MAX_WORDS = 5

    /** Longer than this, one string is no word either (a web address, a pasted line). */
    const val MAX_CHARS = 80

    /** What a selection amounts to for a lookup. */
    enum class Reading {
        /** A word or short phrase to look up. */
        Word,
        /** Nothing word-like in it: spaces, punctuation. */
        Nothing,
        /** A passage, or one string too long to be a word. */
        TooLong,
    }

    /** Why [clean] came back empty, or [Reading.Word] when it did not. */
    fun reading(selection: String): Reading {
        val text = tidy(selection)
        return when {
            text.isEmpty() -> Reading.Nothing
            text.count { it == ' ' } >= MAX_WORDS || text.length > MAX_CHARS -> Reading.TooLong
            else -> Reading.Word
        }
    }

    private val marks = Regex("""\p{Mn}+""")

    /**
     * Letters with their accents taken off, for searching: "café" → "cafe", "Zoë" → "zoe".
     * The dictionary keys its words this way (`data/build-db.py` does the same) and the
     * headword on the page keeps its own spelling. Lower case in, lower case out.
     */
    fun fold(text: String): String {
        val plain = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD).replace(marks, "")
        val out = StringBuilder(plain.length)
        for (c in plain) {
            when (c) {
                'ß' -> out.append("ss")
                'æ' -> out.append("ae")
                'œ' -> out.append("oe")
                'ø' -> out.append('o')
                'đ' -> out.append('d')
                'ł' -> out.append('l')
                'ı' -> out.append('i')
                else -> out.append(c)
            }
        }
        return java.text.Normalizer.normalize(out, java.text.Normalizer.Form.NFC)
    }

    private val edges = Regex("""^[^\p{L}\p{N}]+|[^\p{L}\p{N}.]+$""")
    private val space = Regex("""\s+""")

    /**
     * A selection as the dictionary keys it: trimmed of the punctuation a finger takes with a
     * word ("“Hello,”" → "hello"), curly apostrophes made straight, runs of space made one, and
     * lower case. A trailing full stop is kept only where it belongs to the word ("etc."), which
     * is decided later by trying both. Accents are folded off ([fold]).
     *
     * Empty when nothing word-like is left, or the selection is longer than [MAX_WORDS] words
     * or [MAX_CHARS] characters ([reading] says which).
     */
    fun clean(selection: String): String {
        if (reading(selection) != Reading.Word) return ""
        return fold(tidy(selection).lowercase())
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

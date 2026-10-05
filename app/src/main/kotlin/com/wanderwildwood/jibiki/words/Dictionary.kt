package com.wanderwildwood.jibiki.words

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** One meaning of a word, with the words that share it. */
data class Sense(
    val definition: String,
    val examples: List<String>,
    /** Other words for this same meaning, in WordNet's own order. */
    val synonyms: List<String>,
    /** Words for the opposite meaning. */
    val antonyms: List<String>,
    /** For an adjective, words close to this meaning; for a noun or verb, what it is a kind of. */
    val related: List<String>,
)

/** A word as one part of speech: "bank" the noun and "bank" the verb are two of these. */
data class Entry(
    val lemma: String,
    /** n, v, a or r. */
    val pos: String,
    val ipa: String?,
    val senses: List<Sense>,
)

/** What a lookup found: the word it settled on, and every part of speech it is. */
data class Found(val word: String, val entries: List<Entry>)

/**
 * The word list: Open English WordNet, built into SQLite by `data/build-db.py` and shipped
 * compressed in the APK. It is unpacked once into the app's own storage, because SQLite cannot
 * open a file inside an APK, and again only when a new edition ships.
 */
class Dictionary private constructor(private val db: SQLiteDatabase) {

    /**
     * Everything the word list has for [selection]: the word itself, the irregular forms the
     * list knows ("geese" → goose), and the bases [Forms.bases] gives. Several words can come
     * back: "better" is itself an adjective and a verb, and also a form of "good" and "well".
     * The word as selected comes first.
     */
    fun lookUp(selection: String): List<Found> {
        val found = LinkedHashMap<String, MutableList<Long>>()
        fun add(key: String, ids: List<Long>) {
            if (ids.isEmpty()) return
            val list = found.getOrPut(key) { mutableListOf() }
            ids.forEach { if (it !in list) list += it }
        }
        for (spelling in Forms.spellings(Forms.clean(selection))) {
            add(spelling, entryIds(spelling))
            // The entry that owns the form first: "ran" is "run" the verb before "run" the noun.
            for ((base, owner) in irregular(spelling)) add(base, listOf(owner) + entryIds(base))
            for (base in Forms.bases(spelling)) add(base.word, entryIds(base.word, base.pos))
            // "etc." is a word; only when the selection as given is not does "end." become "end".
            if (found.isNotEmpty()) break
        }
        return found.map { (key, ids) ->
            val entries = ids.map(::entry)
            Found(entries.first().lemma, entries)
        }
    }

    /** Words beginning with [prefix], for the list under the search field. */
    fun startingWith(prefix: String, limit: Int = 40): List<String> {
        val key = Forms.clean(prefix)
        if (key.isEmpty()) return emptyList()
        return db.rawQuery(
            "SELECT DISTINCT lemma FROM entry WHERE key >= ? AND key < ? ORDER BY key, lemma LIMIT ?",
            arrayOf(key, key + "\uFFFF", limit.toString()),
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
    }

    private fun entryIds(key: String, pos: String? = null): List<Long> =
        (
            if (pos == null) db.rawQuery("SELECT id FROM entry WHERE key = ? ORDER BY id", arrayOf(key))
            else db.rawQuery("SELECT id FROM entry WHERE key = ? AND pos = ? ORDER BY id", arrayOf(key, pos))
        ).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }

    /** The words [key] is an irregular form of, each with the entry that lists it. */
    private fun irregular(key: String): List<Pair<String, Long>> =
        db.rawQuery(
            "SELECT e.key, e.id FROM form f JOIN entry e ON e.id = f.entry WHERE f.key = ? ORDER BY e.id",
            arrayOf(key),
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0) to c.getLong(1)) } }

    private fun entry(id: Long): Entry {
        val (lemma, pos, ipa) = db.rawQuery("SELECT lemma, pos, ipa FROM entry WHERE id = ?", arrayOf(id.toString())).use { c ->
            c.moveToFirst()
            Triple(c.getString(0), c.getString(1), if (c.isNull(2)) null else c.getString(2))
        }
        val senses = db.rawQuery(
            """SELECT s.id, s.synset, y.definition, y.examples FROM sense s
               JOIN synset y ON y.id = s.synset WHERE s.entry = ? ORDER BY s.rank""",
            arrayOf(id.toString()),
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val sense = c.getLong(0)
                    val synset = c.getLong(1)
                    add(
                        Sense(
                            definition = c.getString(2),
                            examples = c.getString(3).split('\n').filter { it.isNotBlank() },
                            synonyms = words(
                                "SELECT e.lemma FROM sense s JOIN entry e ON e.id = s.entry WHERE s.synset = ? AND s.entry != ? ORDER BY s.member",
                                synset, id,
                            ),
                            antonyms = words(
                                "SELECT e.lemma FROM antonym a JOIN sense s ON s.id = a.target JOIN entry e ON e.id = s.entry WHERE a.sense = ?",
                                sense,
                            ),
                            related = words(
                                // The first-listed word of each related meaning stands for it.
                                """SELECT e.lemma FROM relation r JOIN sense s ON s.synset = r.target AND s.member = 0
                                   JOIN entry e ON e.id = s.entry WHERE r.synset = ? AND r.kind IN (1, 2) ORDER BY r.rowid""",
                                synset,
                            ),
                        ),
                    )
                }
            }
        }
        return Entry(lemma, pos, ipa, senses)
    }

    private fun words(sql: String, vararg args: Long): List<String> =
        db.rawQuery(sql, args.map { it.toString() }.toTypedArray()).use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }.distinct()

    companion object {
        /** Bumped with each edition `data/build-db.py` is run on; it is the database's user_version. */
        private const val EDITION = 2025
        /**
         * ⚠ The source tree holds `assets/wordnet.db.gz`, to keep 16 MB out of git, but the
         * Android asset packer un-gzips any `.gz` asset and stores it under the name without
         * the extension, deflated like everything else in the APK. So it is read by this name,
         * and with no GZIPInputStream.
         */
        private const val ASSET = "wordnet.db"

        @Volatile private var opened: Dictionary? = null

        /**
         * The dictionary, unpacking it first if this is the first run or a new edition. Slow
         * the first time (tens of megabytes), so never call it on the main thread.
         */
        fun open(context: Context): Dictionary {
            opened?.let { return it }
            synchronized(this) {
                opened?.let { return it }
                val file = File(context.noBackupFilesDir, "wordnet.db")
                if (edition(file) != EDITION) unpack(context, file)
                val db = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
                return Dictionary(db).also { opened = it }
            }
        }

        /** Whether [open] will have to unpack first, so the screen can say why it is slow. */
        fun needsUnpacking(context: Context): Boolean =
            opened == null && edition(File(context.noBackupFilesDir, "wordnet.db")) != EDITION

        private fun edition(file: File): Int {
            if (!file.isFile) return -1
            return runCatching {
                SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
            }.getOrDefault(-1)
        }

        /**
         * Into a temporary file and then renamed, so a run killed halfway leaves no
         * half-written database that would pass for a whole one.
         */
        private fun unpack(context: Context, file: File) {
            val partial = File(file.parentFile, "wordnet.db.partial")
            context.assets.open(ASSET).use { input ->
                partial.outputStream().use { input.copyTo(it, 1 shl 16) }
            }
            check(partial.renameTo(file)) { "could not put the word list in place" }
        }
    }
}

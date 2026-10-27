package com.wanderwildwood.jibiki.words

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormsTest {

    @Test
    fun `a selection loses the punctuation a finger takes with it`() {
        assertEquals("hello", Forms.clean("“Hello,”"))
        assertEquals("don't", Forms.clean("Don’t"))
        assertEquals("well known.", Forms.clean("  well\n known. "))
        assertEquals("etc.", Forms.clean("etc."))
        assertEquals("", Forms.clean("("))
        assertEquals("", Forms.clean("—"))
    }

    @Test
    fun `a passage is not looked up`() {
        assertEquals("", Forms.clean("one two three four five six"))
        assertEquals("one two three four five", Forms.clean("one two three four five"))
    }

    @Test
    fun `accents are folded for searching`() {
        assertEquals("cafe", Forms.fold("café"))
        assertEquals("senor", Forms.fold("señor"))
        assertEquals("naive", Forms.fold("naïve"))
        assertEquals("strasse", Forms.fold("straße"))
        assertEquals("encyclopaedia", Forms.fold("encyclopædia"))
        assertEquals("plain", Forms.fold("plain"))
        // Typed with a combining accent, as some keyboards do
        assertEquals("cafe", Forms.fold("cafe\u0301"))
    }

    @Test
    fun `a selection is looked up by its folded lower-case key`() {
        assertEquals("cafe", Forms.clean("Café"))
        assertEquals("cafe", Forms.clean("“CAFÉ,”"))
        assertEquals("senor", Forms.clean("Señor"))
        // The page title keeps the spelling it was given
        assertEquals("Café", Forms.tidy("Café"))
    }

    @Test
    fun `nothing word-like is told apart from too long`() {
        assertEquals(Forms.Reading.Nothing, Forms.reading("("))
        assertEquals(Forms.Reading.Nothing, Forms.reading("  — "))
        assertEquals(Forms.Reading.Word, Forms.reading("ice cream"))
        assertEquals(Forms.Reading.Word, Forms.reading("one two three four five"))
        assertEquals(Forms.Reading.TooLong, Forms.reading("one two three four five six"))
        // One string, no spaces, but no word
        assertEquals(Forms.Reading.TooLong, Forms.reading("a".repeat(Forms.MAX_CHARS + 1)))
        assertEquals(Forms.Reading.Word, Forms.reading("a".repeat(Forms.MAX_CHARS)))
        assertEquals("", Forms.clean("("))
    }

    private fun bases(word: String) = Forms.bases(word).map { it.word }
    private fun base(word: String, pos: String) = Forms.bases(word).filter { it.pos == pos }.map { it.word }

    @Test
    fun `regular endings point back to the base`() {
        assertTrue("walk" in base("walked", "v"))
        assertTrue("bake" in base("baking", "v"))
        assertTrue("city" in base("cities", "n"))
        assertTrue("box" in base("boxes", "n"))
        assertTrue("happy" in base("happier", "a"))
        assertTrue("fireman" in base("firemen", "n"))
    }

    @Test
    fun `a doubled consonant is undone`() {
        assertTrue("stop" in base("stopped", "v"))
        assertTrue("big" in base("biggest", "a"))
        // and a pair the word always had survives
        assertTrue("kiss" in base("kissed", "v"))
    }

    @Test
    fun `an ending only points to the part of speech that takes it`() {
        // "better" as a comparative would be "bet", which is a verb and so never matches
        assertFalse("bet" in base("better", "v"))
        assertFalse("walk" in base("walked", "n"))
    }

    @Test
    fun `short words are not stripped to nothing`() {
        assertEquals(emptyList<String>(), bases("is"))
        assertFalse("" in bases("ed"))
    }

    @Test
    fun `the word as given is tried first`() {
        assertEquals("geese", Forms.spellings("geese").first())
    }

    @Test
    fun `a possessive and a sentence's full stop are tried off`() {
        assertTrue("river" in Forms.spellings("river's"))
        assertTrue("end" in Forms.spellings("end."))
    }

    @Test
    fun `a phrase is tried hyphenated and bent on either end`() {
        assertTrue("well-known" in Forms.spellings("well known"))
        assertTrue("run into" in base("running into", "v"))
        assertTrue("look up" in base("look ups", "n"))
    }
}

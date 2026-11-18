package com.wanderwildwood.jibiki

import org.junit.Assert.assertEquals
import org.junit.Test

class CasingTest {
    @Test
    fun `a replacement takes the selection's case`() {
        assertEquals("glad", Casing.match("happy", "glad"))
        assertEquals("Glad", Casing.match("Happy", "glad"))
        assertEquals("GLAD", Casing.match("HAPPY", "glad"))
    }

    @Test
    fun `the spaces around a selection are kept`() {
        assertEquals(" glad ", Casing.match(" happy ", "glad"))
    }

    @Test
    fun `punctuation the selection took with it stays in the text`() {
        assertEquals("glad, ", Casing.match("happy, ", "glad"))
        assertEquals("“Glad”", Casing.match("“Happy”", "glad"))
        assertEquals("(glad).", Casing.match("(happy).", "glad"))
    }

    @Test
    fun `a possessive the lookup left out goes back on the replacement`() {
        assertEquals("hound's", Casing.match("dog's", "hound"))
        assertEquals("Hound’s ", Casing.match("Dog’s ", "hound"))
        assertEquals("HOUND'S", Casing.match("DOG'S", "hound"))
        assertEquals("hounds'", Casing.match("dogs'", "hound"))
        assertEquals("foxes'", Casing.match("dogs'", "fox"))
        assertEquals("'hound'", Casing.match("'dogs'", "hound"))
        assertEquals("hound", Casing.match("dogs", "hound"))
    }

    @Test
    fun `a one-letter capital is a capital, not shouting`() {
        assertEquals("One", Casing.match("A", "one"))
    }
}

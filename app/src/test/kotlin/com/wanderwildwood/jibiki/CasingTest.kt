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
    fun `a one-letter capital is a capital, not shouting`() {
        assertEquals("One", Casing.match("A", "one"))
    }
}

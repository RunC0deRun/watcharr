package io.github.RunC0deRun.shared.data.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class EpgMatcherTest {

    @Test
    fun testNormalize() {
        assertEquals("hbo", EpgMatcher.normalize("HBO HD"))
        assertEquals("hbo", EpgMatcher.normalize("hbo hd (us)"))
        assertEquals("espn", EpgMatcher.normalize("ESPN SD [UK]"))
        assertEquals("skysportspremierleague", EpgMatcher.normalize("Sky Sports - Premier League HD"))
        // Edge cases
        assertEquals("", EpgMatcher.normalize(""))
        assertEquals("", EpgMatcher.normalize("   "))
        assertEquals("123", EpgMatcher.normalize("123"))
    }

    @Test
    fun testIsMatch() {
        assertTrue(EpgMatcher.isMatch("hbo.us", "HBO", "hbo.us"))
        assertTrue(EpgMatcher.isMatch(null, "HBO HD", "hbo"))
        assertTrue(EpgMatcher.isMatch("", "ESPN SD [UK]", "espn"))
        assertTrue(EpgMatcher.isMatch("some.other.id", "Sky Sports - Premier League HD", "skysportspremierleague"))
        
        assertFalse(EpgMatcher.isMatch("hbo.us", "ESPN", "hbo"))
        assertFalse(EpgMatcher.isMatch(null, "HBO HD", "espn"))
        assertFalse(EpgMatcher.isMatch("hbo", "Showtime", "starz"))
        
        // Blank names or ids
        assertFalse(EpgMatcher.isMatch("", "", ""))
        assertFalse(EpgMatcher.isMatch(null, "  ", ""))
    }
}

package com.iptv.shared.data.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EpgMatcherTest {

    @Test
    fun testNormalize() {
        assertEquals("hbo", EpgMatcher.normalize("HBO HD"))
        assertEquals("hbo", EpgMatcher.normalize("hbo hd (us)"))
        assertEquals("espn", EpgMatcher.normalize("ESPN SD [UK]"))
        assertEquals("skysportspremierleague", EpgMatcher.normalize("Sky Sports - Premier League HD"))
    }

    @Test
    fun testIsMatch() {
        assertTrue(EpgMatcher.isMatch("hbo.us", "HBO", "hbo.us"))
        assertTrue(EpgMatcher.isMatch(null, "HBO HD", "hbo"))
        assertTrue(EpgMatcher.isMatch("", "ESPN SD [UK]", "espn"))
        assertTrue(EpgMatcher.isMatch("some.other.id", "Sky Sports - Premier League HD", "skysportspremierleague"))
    }
}

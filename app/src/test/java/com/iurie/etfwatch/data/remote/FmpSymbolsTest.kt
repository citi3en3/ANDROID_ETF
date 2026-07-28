package com.iurie.etfwatch.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the symbol spelling verified against the live FMP API: `/quote/HYLD.U.TO` returns an
 * empty array while `/quote/HYLD-U.TO` returns real data.
 */
class FmpSymbolsTest {

    @Test
    fun `bare TSX ticker gets the TO suffix`() {
        assertEquals("HDIV.TO", FmpSymbols.toTsxSymbol("HDIV"))
    }

    @Test
    fun `bare USD unit becomes dash-U dot TO`() {
        assertEquals("HYLD-U.TO", FmpSymbols.toTsxSymbol("HYLD.U"))
        assertEquals("HBIL-U.TO", FmpSymbols.toTsxSymbol("HBIL.U"))
    }

    @Test
    fun `already-suffixed ticker is left alone`() {
        assertEquals("HDIV.TO", FmpSymbols.toTsxSymbol("HDIV.TO"))
    }

    @Test
    fun `legacy dotted USD unit is rewritten`() {
        assertEquals("HYLD-U.TO", FmpSymbols.normalize("HYLD.U.TO"))
    }

    @Test
    fun `normalize leaves ordinary symbols untouched`() {
        assertEquals("TQQQ", FmpSymbols.normalize("TQQQ"))
        assertEquals("HDIV.TO", FmpSymbols.normalize("HDIV.TO"))
        assertEquals("HYLD-U.TO", FmpSymbols.normalize("HYLD-U.TO"))
    }

    @Test
    fun `normalize is idempotent`() {
        val once = FmpSymbols.normalize("HYLD.U.TO")
        assertEquals(once, FmpSymbols.normalize(once))
    }

    @Test
    fun `legacy detection only matches the dotted form`() {
        assertTrue(FmpSymbols.isLegacyUsdUnit("HYLD.U.TO"))
        assertFalse(FmpSymbols.isLegacyUsdUnit("HYLD-U.TO"))
        assertFalse(FmpSymbols.isLegacyUsdUnit("HDIV.TO"))
        assertFalse(FmpSymbols.isLegacyUsdUnit("TQQQ"))
    }

    @Test
    fun `input is case and whitespace insensitive`() {
        assertEquals("HYLD-U.TO", FmpSymbols.normalize("  hyld.u.to  "))
        assertEquals("HDIV.TO", FmpSymbols.toTsxSymbol(" hdiv "))
    }
}

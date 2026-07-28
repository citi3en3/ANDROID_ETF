package com.iurie.etfwatch.ui.common

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class FormatTest {

    private val original: Locale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(original)

    @Test
    fun `prices use a dot even on a comma-decimal device`() {
        Locale.setDefault(Locale.GERMANY)
        assertEquals("12.30", Format.price(12.3))
        assertEquals("$12.30", Format.money(12.3))
        assertEquals("+1.25%", Format.signedPct(1.25))
    }

    @Test
    fun `missing values render as an em dash`() {
        assertEquals(Format.EM_DASH, Format.price(null))
        assertEquals(Format.EM_DASH, Format.signedPct(null))
        assertEquals(Format.EM_DASH, Format.pct(null))
    }

    @Test
    fun `signed percentages always carry a sign`() {
        assertEquals("+0.00%", Format.signedPct(0.0))
        assertEquals("-3.40%", Format.signedPct(-3.4))
    }

    @Test
    fun `plain percentages omit the plus`() {
        assertEquals("9.84%", Format.pct(9.84))
    }

    @Test
    fun `parsing accepts both decimal separators`() {
        assertEquals(12.34, Format.parseDecimal("12.34")!!, 1e-9)
        assertEquals(12.34, Format.parseDecimal("12,34")!!, 1e-9)
        assertEquals(12.0, Format.parseDecimal("  12  ")!!, 1e-9)
    }

    @Test
    fun `parsing rejects junk`() {
        assertNull(Format.parseDecimal(""))
        assertNull(Format.parseDecimal("abc"))
        assertNull(Format.parseDecimal("1.2.3"))
    }

    /** What the app prints must be re-enterable in the alert dialog on any locale. */
    @Test
    fun `formatted output round-trips through the parser`() {
        Locale.setDefault(Locale.GERMANY)
        val formatted = Format.price(123.45)
        assertEquals(123.45, Format.parseDecimal(formatted)!!, 1e-9)
    }
}

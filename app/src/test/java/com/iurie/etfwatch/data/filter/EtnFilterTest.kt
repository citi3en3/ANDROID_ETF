package com.iurie.etfwatch.data.filter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EtnFilterTest {

    @Test
    fun `denylisted symbols are ETNs regardless of name`() {
        assertTrue(EtnFilter.isEtn("FNGU", null))
        assertTrue(EtnFilter.isEtn("VXX", "iPath Series B S&P 500 VIX Short-Term Futures"))
    }

    @Test
    fun `listing suffixes are stripped before the denylist lookup`() {
        assertTrue(EtnFilter.isEtn("FNGU.TO", null))
        assertTrue(EtnFilter.isEtn("fngu", null))
    }

    @Test
    fun `the word ETN in a name is enough`() {
        assertTrue(EtnFilter.isEtn("ZZZZ", "Some Bank 3X Leveraged ETN"))
        assertTrue(EtnFilter.isEtn("ZZZZ", "Exchange Traded Note on Something"))
    }

    @Test
    fun `ETN-only issuer brands are caught`() {
        assertTrue(EtnFilter.isEtn("ZZZZ", "MicroSectors U.S. Big Oil Index"))
        assertTrue(EtnFilter.isEtn("ZZZZ", "ETRACS Alerian MLP"))
    }

    @Test
    fun `ordinary leveraged ETFs are not ETNs`() {
        assertFalse(EtnFilter.isEtn("TQQQ", "ProShares UltraPro QQQ"))
        assertFalse(EtnFilter.isEtn("HDIV.TO", "Hamilton Enhanced Canadian Covered Call ETF"))
        assertFalse(EtnFilter.isEtn("SOXL", "Direxion Daily Semiconductor Bull 3X Shares"))
    }

    @Test
    fun `ETN matching is word-bounded so it cannot fire mid-word`() {
        assertFalse(EtnFilter.isEtn("VNM", "VanEck Vietnam ETF"))
        assertFalse(EtnFilter.isEtn("ZZZZ", "Strengthen Growth Fund"))
    }

    /**
     * The ETN filter deletes matching rows from the DB on every seed pass, so a seeded ETF that
     * trips it would silently vanish from the app on each launch.
     */
    @Test
    fun `no seeded ticker is misclassified as an ETN`() {
        listOf("seed_hamilton.json", "seed_leveraged.json").forEach { asset ->
            val file = File("src/main/assets/$asset")
            assertTrue("missing seed asset $asset", file.exists())
            val text = file.readText()
            val entries = ENTRY.findAll(text).map { it.groupValues[1] to it.groupValues[2] }.toList()
            assertTrue("no entries parsed from $asset", entries.isNotEmpty())
            entries.forEach { (ticker, name) ->
                assertFalse(
                    "$asset: seeded ETF '$ticker' ($name) is treated as an ETN and would be purged",
                    EtnFilter.isEtn(ticker, name),
                )
            }
        }
    }

    private companion object {
        val ENTRY = Regex("\"ticker\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"name\"\\s*:\\s*\"([^\"]+)\"")
    }
}

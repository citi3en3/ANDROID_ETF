package com.iurie.etfwatch.ui.leveraged

import com.iurie.etfwatch.data.db.EtfEntity
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.ui.common.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeveragedFilterTest {

    private fun item(ticker: String, leverage: Int?) = EtfWithQuote(
        etf = EtfEntity(
            ticker = ticker,
            name = ticker,
            exchange = "NASDAQ",
            isLeveraged = true,
            leverageFactor = leverage,
        ),
        quote = null,
    )

    private val universe = listOf(
        item("TQQQ", 3),
        item("SQQQ", -3),
        item("QLD", 2),
        item("QID", -2),
        item("PSQ", -1),
        item("PLAIN", null),
    )

    private fun tickers(items: List<EtfWithQuote>) = items.map { it.etf.ticker }.toSet()

    @Test
    fun `the default filter set shows every fund with a declared factor`() {
        val result = applyFilters(universe, DEFAULT_LEV_FILTERS)
        assertEquals(setOf("TQQQ", "SQQQ", "QLD", "QID", "PSQ"), tickers(result))
    }

    @Test
    fun `a fund with no declared leverage is never counted as 1x`() {
        val result = applyFilters(universe, setOf(SortMode.NonInverse, SortMode.Lev1x))
        assertTrue("PLAIN has unknown leverage and must not appear under 1x", "PLAIN" !in tickers(result))
    }

    @Test
    fun `inverse only`() {
        val result = applyFilters(
            universe,
            setOf(SortMode.Inverse, SortMode.Lev1x, SortMode.Lev2x, SortMode.Lev3x),
        )
        assertEquals(setOf("SQQQ", "QID", "PSQ"), tickers(result))
    }

    @Test
    fun `magnitude and direction compose`() {
        val result = applyFilters(universe, setOf(SortMode.Inverse, SortMode.Lev3x))
        assertEquals(setOf("SQQQ"), tickers(result))
    }

    @Test
    fun `clearing every magnitude yields nothing`() {
        val result = applyFilters(universe, setOf(SortMode.Inverse, SortMode.NonInverse))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `clearing every direction yields nothing`() {
        val result = applyFilters(universe, setOf(SortMode.Lev1x, SortMode.Lev2x, SortMode.Lev3x))
        assertTrue(result.isEmpty())
    }
}

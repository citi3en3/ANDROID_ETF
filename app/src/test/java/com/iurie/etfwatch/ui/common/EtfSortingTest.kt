package com.iurie.etfwatch.ui.common

import com.iurie.etfwatch.data.db.EtfEntity
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.db.QuoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EtfSortingTest {

    private fun item(
        ticker: String,
        sector: String? = null,
        leverage: Int? = null,
        price: Double? = null,
        changePct: Double? = null,
        monthReturn: Double? = null,
        withQuote: Boolean = true,
    ) = EtfWithQuote(
        etf = EtfEntity(ticker = ticker, name = ticker, exchange = "NASDAQ", sector = sector, leverageFactor = leverage),
        quote = if (withQuote) {
            QuoteEntity(
                ticker = ticker,
                price = price,
                changePct = changePct,
                dividendYield = null,
                marketCap = null,
                updatedAt = 0L,
                monthReturnPct = monthReturn,
            )
        } else null,
    )

    private fun tickers(list: List<EtfWithQuote>) = list.map { it.etf.ticker }

    @Test
    fun `descending sort puts missing values last`() {
        val items = listOf(
            item("AAA", monthReturn = null),
            item("BBB", monthReturn = 5.0),
            item("CCC", monthReturn = -2.0),
        )
        assertEquals(
            listOf("BBB", "CCC", "AAA"),
            tickers(EtfSorting.sort(items, SortMode.MonthReturn)),
        )
    }

    @Test
    fun `rows without a quote sort last rather than crashing`() {
        val items = listOf(item("AAA", withQuote = false), item("BBB", price = 10.0))
        assertEquals(listOf("BBB", "AAA"), tickers(EtfSorting.sort(items, SortMode.Price)))
    }

    @Test
    fun `ties break on ticker so ordering is stable`() {
        val items = listOf(
            item("ZZZ", changePct = 1.0),
            item("AAA", changePct = 1.0),
            item("MMM", changePct = 1.0),
        )
        assertEquals(
            listOf("AAA", "MMM", "ZZZ"),
            tickers(EtfSorting.sort(items, SortMode.ChangePct)),
        )
    }

    @Test
    fun `unclassified sectors sort after named ones`() {
        val items = listOf(
            item("AAA", sector = null),
            item("BBB", sector = "Technology"),
            item("CCC", sector = "Banks"),
        )
        assertEquals(
            listOf("CCC", "BBB", "AAA"),
            tickers(EtfSorting.sort(items, SortMode.Sector)),
        )
    }

    @Test
    fun `magnitude is null when no leverage factor is declared`() {
        assertNull(EtfSorting.magnitude(item("AAA", leverage = null)))
        assertEquals(3, EtfSorting.magnitude(item("BBB", leverage = -3)))
        assertEquals(2, EtfSorting.magnitude(item("CCC", leverage = 2)))
    }

    @Test
    fun `inverse is determined by a negative factor`() {
        assertEquals(true, EtfSorting.isInverse(item("AAA", leverage = -1)))
        assertEquals(false, EtfSorting.isInverse(item("BBB", leverage = 3)))
        assertEquals(false, EtfSorting.isInverse(item("CCC", leverage = null)))
    }

    @Test
    fun `returnFor only answers for return periods`() {
        val row = item("AAA", monthReturn = 7.5)
        assertEquals(7.5, EtfSorting.returnFor(row, SortMode.MonthReturn)!!, 1e-9)
        assertNull(EtfSorting.returnFor(row, SortMode.Price))
        assertNull(EtfSorting.returnFor(row, null))
    }
}

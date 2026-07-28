package com.iurie.etfwatch.data.scrape

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture markup mirrors the live hamiltonetfs.com/performance/ table, including the quirk that
 * matters: the fund name and its attribute tags share one `<td>`.
 */
class HamiltonParserTest {

    private fun html(rows: String) = """
        <table class="tablesaw">
          <thead><tr><th>Ticker</th><th>Yield</th><th>Fund</th></tr></thead>
          <tbody>$rows</tbody>
        </table>
    """.trimIndent()

    private val hdivRow = """
        <tr>
          <td><span class="ticker-block">HDIV</span></td>
          <td><span class="ticker-block">9.84%</span></td>
          <td>
            <a href="#" class="fund-link">Hamilton Enhanced Canadian<br>Covered Call ETF</a>
            <br>
            <span class="etf-attribute">Covered Call</span><span class="etf-attribute">Modest Leverage</span><span class="etf-attribute">Canada</span>
          </td>
          <td>2.4%</td>
        </tr>
    """.trimIndent()

    private val usdUnitRow = """
        <tr>
          <td><span class="ticker-block">HYLD.U</span></td>
          <td><span class="ticker-block">11.13%</span></td>
          <td>
            <a href="#" class="fund-link">Hamilton Enhanced U.S. Covered Call ETF</a>
            <span class="etf-attribute">Covered Call</span><span class="etf-attribute">U.S.</span>
          </td>
          <td>1.1%</td>
        </tr>
    """.trimIndent()

    @Test
    fun `ticker gets the TSX suffix`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(hdivRow)))
        assertEquals(1, result.size)
        assertEquals("HDIV.TO", result[0].ticker)
    }

    @Test
    fun `USD unit class uses the dash form FMP accepts`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(usdUnitRow)))
        assertEquals("HYLD-U.TO", result[0].ticker)
    }

    @Test
    fun `yield is parsed as a percentage`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(hdivRow)))
        assertEquals(9.84, result[0].yieldPct!!, 1e-9)
    }

    @Test
    fun `name excludes the attribute tags that share the cell`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(hdivRow)))
        val name = result[0].name!!
        assertEquals("Hamilton Enhanced Canadian Covered Call ETF", name)
        assertTrue("tag text leaked into the name: $name", !name.contains("Modest Leverage"))
    }

    @Test
    fun `sector is composed from the strategy and region tags`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(hdivRow)))
        assertEquals("Covered Call / Canada", result[0].sector)
    }

    @Test
    fun `region tags are normalised to the seed files spelling`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(usdUnitRow)))
        assertEquals("Covered Call / US", result[0].sector)
    }

    @Test
    fun `header rows and malformed rows are skipped`() {
        val junk = "<tr><td>not a ticker at all</td><td>x</td><td>y</td></tr>"
        val result = HamiltonParser.parse(Jsoup.parse(html(junk + hdivRow)))
        assertEquals(1, result.size)
    }

    @Test
    fun `duplicate tickers collapse to one row`() {
        val result = HamiltonParser.parse(Jsoup.parse(html(hdivRow + hdivRow)))
        assertEquals(1, result.size)
    }

    @Test
    fun `a missing yield does not drop the row`() {
        val noYield = hdivRow.replace("9.84%", "—")
        val result = HamiltonParser.parse(Jsoup.parse(html(noYield)))
        assertEquals(1, result.size)
        assertNull(result[0].yieldPct)
        assertNotNull(result[0].name)
    }

    @Test
    fun `falls back to keyword matching when the markup has no tags`() {
        val plain = """
            <tr>
              <td>HUTS</td><td>5.00%</td>
              <td>Hamilton Utilities Yield Maximizer ETF</td>
            </tr>
        """.trimIndent()
        val result = HamiltonParser.parse(Jsoup.parse(html(plain)))
        assertEquals("HUTS.TO", result[0].ticker)
        assertEquals("Yield Maximizer", result[0].sector)
    }

    @Test
    fun `an empty document yields no rows`() {
        assertTrue(HamiltonParser.parse(Jsoup.parse("<html><body>nothing</body></html>")).isEmpty())
    }
}

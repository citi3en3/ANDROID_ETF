package com.iurie.etfwatch.data.scrape

import org.jsoup.Jsoup
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ScrapedHamilton(
    val ticker: String,
    val yieldPct: Double?,
    val name: String?,
    val sector: String?,
)

/**
 * Scrapes https://hamiltonetfs.com/performance/.
 * Row layout: <td>TICKER</td><td>YIELD%</td><td>Long name + tags…</td><td>perf cols…</td>
 */
@Singleton
class HamiltonScraper @Inject constructor() {

    suspend fun fetch(): List<ScrapedHamilton> = runCatching {
        val doc = Jsoup.connect(URL)
            .userAgent(UA)
            .timeout(20_000)
            .get()
        val out = mutableListOf<ScrapedHamilton>()
        doc.select("table tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 3) return@forEach
            val rawTicker = cells[0].text().trim().uppercase(Locale.ROOT)
            if (!TICKER_REGEX.matches(rawTicker)) return@forEach
            val ticker = if (rawTicker.endsWith(".TO")) rawTicker else "$rawTicker.TO"
            val yieldPct = YIELD_REGEX.find(cells[1].text())
                ?.groupValues?.getOrNull(1)
                ?.replace(",", ".")?.toDoubleOrNull()
            val nameCell = cells[2].text().trim()
            val name = nameCell.takeIf { it.isNotEmpty() }
            val sector = nameCell.extractSector()
            out += ScrapedHamilton(ticker, yieldPct, name, sector)
        }
        out.distinctBy { it.ticker }.also { Timber.d("Hamilton scrape: ${it.size} rows") }
    }.onFailure { Timber.w(it, "Hamilton scrape failed") }.getOrDefault(emptyList())

    private fun String.extractSector(): String? {
        val lower = this.lowercase(Locale.ROOT)
        return SECTOR_RULES.firstNotNullOfOrNull { (kw, label) -> if (lower.contains(kw)) label else null }
    }

    companion object {
        private const val URL = "https://hamiltonetfs.com/performance/"
        private const val UA = "Mozilla/5.0 (Android) EtfWatch/0.1"
        private val TICKER_REGEX = Regex("^[A-Z]{2,5}(\\.U)?$")
        private val YIELD_REGEX = Regex("(\\d{1,2}(?:[.,]\\d{1,2})?)\\s*%")
        private val SECTOR_RULES = listOf(
            "covered call" to "Covered Call",
            "yield maximizer" to "Yield Maximizer",
            "bank" to "Banks",
            "financial" to "Financials",
            "utilit" to "Utilities",
            "technology" to "Technology",
            "energy" to "Energy",
            "gold" to "Gold",
            "healthcare" to "Healthcare",
            "reit" to "REITs",
            "bond" to "Bonds",
            "t-bill" to "T-Bills",
            "dividend" to "Dividend",
            "multi-asset" to "Multi-Asset",
        )
    }
}

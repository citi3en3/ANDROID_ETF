package com.iurie.etfwatch.data.scrape

import com.iurie.etfwatch.data.remote.FmpSymbols
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
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
 *
 * Row layout (verified against the live page):
 * ```
 * <td><span class="ticker-block">HDIV</span></td>
 * <td><span class="ticker-block">9.84%</span></td>
 * <td>
 *   <a class="fund-link">Hamilton Enhanced Canadian<br>Covered Call ETF</a>
 *   <span class="etf-attribute">Covered Call</span>
 *   <span class="etf-attribute">Modest Leverage</span>
 *   <span class="etf-attribute">Canada</span>
 * </td>
 * <td>…perf columns…</td>
 * ```
 * The name and the attribute tags share one cell, so taking the whole cell's text yields
 * "Hamilton Enhanced Canadian Covered Call ETF Covered Call Modest Leverage Canada". Read the
 * anchor and the tag spans separately instead, and fall back to whole-cell keyword matching if
 * the markup ever changes.
 */
@Singleton
class HamiltonScraper @Inject constructor() {

    suspend fun fetch(): List<ScrapedHamilton> = runCatching {
        // Jsoup.connect().get() blocks; the callers run on Dispatchers.Default.
        withContext(Dispatchers.IO) {
            val doc = Jsoup.connect(URL).userAgent(UA).timeout(TIMEOUT_MS).get()
            HamiltonParser.parse(doc)
        }
    }.onFailure { Timber.w(it, "Hamilton scrape failed") }.getOrDefault(emptyList())

    private companion object {
        const val URL = "https://hamiltonetfs.com/performance/"
        const val UA = "Mozilla/5.0 (Android) EtfWatch/0.1"
        const val TIMEOUT_MS = 20_000
    }
}

/** Pure parsing half of the scraper, split out so it can be unit-tested against saved HTML. */
internal object HamiltonParser {

    fun parse(doc: Document): List<ScrapedHamilton> {
        val out = mutableListOf<ScrapedHamilton>()
        doc.select("table tr").forEach { row ->
            val cells = row.select("td")
            if (cells.size < 3) return@forEach
            val rawTicker = cells[0].text().trim().uppercase(Locale.ROOT)
            if (!TICKER_REGEX.matches(rawTicker)) return@forEach

            val yieldPct = YIELD_REGEX.find(cells[1].text())
                ?.groupValues?.getOrNull(1)
                ?.replace(",", ".")
                ?.toDoubleOrNull()

            val nameCell = cells[2]
            val name = nameCell.selectFirst("a.fund-link")?.text()?.trim()
                ?: nameCell.text().trim().takeIf { it.isNotEmpty() }
            val tags = nameCell.select("span.etf-attribute").map { it.text().trim() }.filter { it.isNotEmpty() }

            out += ScrapedHamilton(
                ticker = FmpSymbols.toTsxSymbol(rawTicker),
                yieldPct = yieldPct,
                name = name,
                sector = sectorFrom(tags, nameCell),
            )
        }
        return out.distinctBy { it.ticker }.also { Timber.d("Hamilton scrape: ${it.size} rows") }
    }

    /**
     * Composes "Strategy / Region" from the attribute tags to match the seed files' convention
     * (e.g. "Covered Call / Canada"), falling back to keyword matching on the raw cell text.
     */
    private fun sectorFrom(tags: List<String>, nameCell: Element): String? {
        if (tags.isEmpty()) return keywordSector(nameCell.text())
        val region = tags.firstOrNull { it.normalizeRegion() != null }?.normalizeRegion()
        val strategy = tags.firstOrNull { tag -> STRATEGY_TAGS.any { tag.equals(it, ignoreCase = true) } }
            ?: tags.firstOrNull { it.normalizeRegion() == null }
        return when {
            strategy != null && region != null -> "$strategy / $region"
            strategy != null -> strategy
            region != null -> region
            else -> keywordSector(nameCell.text())
        }
    }

    /** Maps the site's region tags onto the shorter spellings the seed files use. */
    private fun String.normalizeRegion(): String? = when (lowercase(Locale.ROOT).trim()) {
        "canada", "canadian" -> "Canada"
        "u.s.", "us", "u.s", "united states" -> "US"
        "global" -> "Global"
        "international" -> "International"
        else -> null
    }

    private fun keywordSector(text: String): String? {
        val lower = text.lowercase(Locale.ROOT)
        return SECTOR_RULES.firstNotNullOfOrNull { (kw, label) -> if (lower.contains(kw)) label else null }
    }

    private val TICKER_REGEX = Regex("^[A-Z]{2,5}(\\.U)?$")
    private val YIELD_REGEX = Regex("(\\d{1,2}(?:[.,]\\d{1,2})?)\\s*%")

    private val STRATEGY_TAGS = listOf(
        "Covered Call", "Yield Maximizer", "Modest Leverage", "Enhanced", "Multi-Sector", "Mutli-Sector",
    )

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

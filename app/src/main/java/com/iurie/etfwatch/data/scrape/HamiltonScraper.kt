package com.iurie.etfwatch.data.scrape

import org.jsoup.Jsoup
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ScrapedHamilton(val ticker: String, val yieldPct: Double?)

/**
 * Best-effort scrape of Hamilton ETFs' product list.
 * Their site can change anytime — always fall back to the bundled seed list on any failure.
 */
@Singleton
class HamiltonScraper @Inject constructor() {

    suspend fun fetch(): List<ScrapedHamilton> = runCatching {
        val doc = Jsoup.connect(URL)
            .userAgent(UA)
            .timeout(15_000)
            .get()
        val rows = doc.select("table tr, .etf-card, [data-ticker]")
        val out = mutableListOf<ScrapedHamilton>()
        rows.forEach { row ->
            val text = row.text()
            val tickerMatch = TICKER_REGEX.find(text)?.value
            val yieldMatch = YIELD_REGEX.find(text)?.groupValues?.getOrNull(1)
                ?.replace(",", ".")?.toDoubleOrNull()
            if (tickerMatch != null) {
                val ticker = tickerMatch.uppercase(Locale.ROOT).let { if (it.endsWith(".TO")) it else "$it.TO" }
                out += ScrapedHamilton(ticker, yieldMatch)
            }
        }
        out.distinctBy { it.ticker }
    }.onFailure { Timber.w(it, "Hamilton scrape failed") }.getOrDefault(emptyList())

    companion object {
        private const val URL = "https://hamiltonetfs.com/etfs/"
        private const val UA = "Mozilla/5.0 (Android) EtfWatch/0.1"
        private val TICKER_REGEX = Regex("\\b[A-Z]{3,5}(?:\\.TO)?\\b")
        private val YIELD_REGEX = Regex("(\\d{1,2}(?:[.,]\\d{1,2})?)\\s*%")
    }
}

package com.iurie.etfwatch.data.remote

import java.util.Locale

/**
 * Symbol shapes FMP actually accepts.
 *
 * TSX-listed tickers take a `.TO` suffix (`HDIV` → `HDIV.TO`). The USD-denominated unit classes
 * are the trap: FMP spells them `BASE-U.TO` (`HYLD-U.TO`), **not** `BASE.U.TO`. The dotted form
 * is not an error — `/quote/HYLD.U.TO` returns `[]`, so those rows just sit priceless forever.
 * Verified against the live API.
 */
object FmpSymbols {

    /** The wrong-but-previously-shipped spelling, e.g. `HYLD.U.TO`. */
    private val LEGACY_USD_UNIT = Regex("^([A-Z0-9]+)\\.U\\.TO$")

    /** A bare TSX ticker as scraped from Hamilton, e.g. `HYLD` or `HYLD.U`. */
    private val BARE_USD_UNIT = Regex("^([A-Z0-9]+)\\.U$")

    /** Turns a bare scraped TSX ticker into the symbol FMP expects. */
    fun toTsxSymbol(rawTicker: String): String {
        val t = rawTicker.trim().uppercase(Locale.ROOT)
        BARE_USD_UNIT.find(t)?.let { return "${it.groupValues[1]}-U.TO" }
        return if (t.endsWith(".TO")) normalize(t) else "$t.TO"
    }

    /** Rewrites a legacy `BASE.U.TO` symbol to the working `BASE-U.TO`; other symbols pass through. */
    fun normalize(ticker: String): String {
        val t = ticker.trim().uppercase(Locale.ROOT)
        LEGACY_USD_UNIT.find(t)?.let { return "${it.groupValues[1]}-U.TO" }
        return t
    }

    fun isLegacyUsdUnit(ticker: String): Boolean =
        LEGACY_USD_UNIT.matches(ticker.trim().uppercase(Locale.ROOT))
}

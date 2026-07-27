package com.iurie.etfwatch.data.filter

import java.util.Locale

/**
 * ETNs (exchange-traded notes) are unsecured issuer debt rather than funds — this app tracks ETFs
 * only, so nothing that looks like an ETN is ever seeded, searched, added, or refreshed.
 *
 * Detection is name-first, since FMP and the seed files spell the suffix out ("… 3X Leveraged ETN")
 * or carry an ETN-only issuer brand. [DENYLIST] backstops the actively-traded US notes whose
 * vendor-supplied names don't always say "ETN".
 */
object EtnFilter {

    fun isEtn(ticker: String, name: String?): Boolean =
        baseSymbol(ticker) in DENYLIST || nameLooksLikeEtn(name)

    private fun nameLooksLikeEtn(name: String?): Boolean {
        val lower = name?.lowercase(Locale.ROOT) ?: return false
        return ETN_WORD.containsMatchIn(lower) || NAME_KEYWORDS.any { lower.contains(it) }
    }

    /** Strips the `.TO` / `.U.TO` listing suffixes so denylist lookups see the bare symbol. */
    private fun baseSymbol(ticker: String): String =
        ticker.uppercase(Locale.ROOT).substringBefore('.')

    /** Word-bounded so tickers/names like "Vietnam" can't trip it. */
    private val ETN_WORD = Regex("\\betns?\\b")

    private val NAME_KEYWORDS = listOf(
        "exchange traded note",
        "exchange-traded note",
        "index-linked note",
        "linked notes",
        // ETN-only issuer brands.
        "microsectors",
        "ipath",
        "etracs",
        "velocityshares",
    )

    /**
     * Base symbols of US-listed ETNs. Includes delisted/closed notes so a stale watchlist entry or
     * an odd search hit can't reintroduce one.
     */
    private val DENYLIST = setOf(
        // MicroSectors (BMO)
        "FNGU", "FNGD", "FNGO", "FNGZ", "BNKU", "BNKD", "NRGU", "NRGD", "WTIU", "WTID",
        "GDXU", "GDXD", "SHNY", "DULL", "FLYU", "FLYD", "MJJ", "MJO",
        // iPath (Barclays)
        "VXX", "VXZ", "DJP", "BAL", "COW", "GRN", "NIB", "SGG", "PGM", "LD", "OIL", "GAZ",
        "JJC", "JJE", "JJG", "JJM", "JJN", "JJP", "JJS", "JJT", "JJU",
        // ETRACS (UBS)
        "BDCZ", "BDCL", "SMHB", "PFFL", "MLPB", "ATMP", "MVRL", "MORL", "MRRL", "LMLP",
        "IWDL", "IWFL", "IWML", "HDLB", "DVYL", "CEFD", "USML",
        // ELEMENTS (Swedish Export Credit)
        "RJI", "RJA", "RJN", "RJZ",
        // JPMorgan, Deutsche Bank, Credit Suisse, VelocityShares
        "AMJ", "AMJB", "DGP", "DGZ", "DZZ", "DTO", "TVIX", "ZIV", "UGAZ", "DGAZ", "UWT", "DWT",
    )
}

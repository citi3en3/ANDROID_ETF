package com.iurie.etfwatch.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class QuoteDto(
    val symbol: String,
    val name: String?,
    val price: Double?,
    @Json(name = "changesPercentage") val changesPercentage: Double?,
    val change: Double?,
    val marketCap: Double?,
    val exchange: String?,
)

/**
 * Subset of FMP's `/profile/{symbol}` payload. `isEtf` is the authoritative fund-type flag —
 * `/search` returns stocks, mutual funds and trusts alongside ETFs with nothing to tell them
 * apart, so this is what keeps non-ETFs out of the watchlist.
 */
@JsonClass(generateAdapter = true)
data class ProfileDto(
    val symbol: String,
    val companyName: String? = null,
    val exchangeShortName: String? = null,
    val isEtf: Boolean? = null,
    val isFund: Boolean? = null,
    val isActivelyTrading: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class HistoricalResponse(
    val symbol: String?,
    val historical: List<HistoricalPoint>?,
)

@JsonClass(generateAdapter = true)
data class HistoricalPoint(
    val date: String,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val close: Double,
)

@JsonClass(generateAdapter = true)
data class SearchHit(
    val symbol: String,
    val name: String?,
    val stockExchange: String?,
    val exchangeShortName: String?,
)

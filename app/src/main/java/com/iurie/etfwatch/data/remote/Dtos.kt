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

@JsonClass(generateAdapter = true)
data class EtfInfoDto(
    val symbol: String,
    val name: String?,
    val expenseRatio: Double?,
    val assetsUnderManagement: Double?,
    val sector: String?,
    @Json(name = "dividendYield") val dividendYield: Double?,
)

@JsonClass(generateAdapter = true)
data class HistoricalResponse(
    val symbol: String?,
    val historical: List<HistoricalPoint>?,
)

@JsonClass(generateAdapter = true)
data class HistoricalPoint(
    val date: String,
    val close: Double,
)

@JsonClass(generateAdapter = true)
data class SearchHit(
    val symbol: String,
    val name: String?,
    val stockExchange: String?,
    val exchangeShortName: String?,
)

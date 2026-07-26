package com.iurie.etfwatch.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Unauthenticated Yahoo Finance chart endpoint.
 * Base: https://query1.finance.yahoo.com/
 * Used as a price source for tickers FMP doesn't cover (e.g., TSX .TO listings on the free tier).
 */
interface YahooService {
    @GET("v8/finance/chart/{symbol}")
    suspend fun chart(
        @Path("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("range") range: String = "1mo",
    ): YahooChartResponse
}

@JsonClass(generateAdapter = true)
data class YahooChartResponse(val chart: YahooChart?)

@JsonClass(generateAdapter = true)
data class YahooChart(val result: List<YahooChartResult>?)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
    val meta: YahooChartMeta?,
    val indicators: YahooIndicators?,
)

@JsonClass(generateAdapter = true)
data class YahooIndicators(val quote: List<YahooQuoteSeries>?)

@JsonClass(generateAdapter = true)
data class YahooQuoteSeries(val close: List<Double?>?)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
    val symbol: String?,
    val regularMarketPrice: Double?,
    val previousClose: Double?,
    val chartPreviousClose: Double?,
    val currency: String?,
)

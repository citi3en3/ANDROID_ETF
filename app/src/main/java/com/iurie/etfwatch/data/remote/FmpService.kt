package com.iurie.etfwatch.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Financial Modeling Prep — https://site.financialmodelingprep.com/
 * Base URL: https://financialmodelingprep.com/api/v3/
 * The API key is appended via OkHttp interceptor as `?apikey=...`.
 */
interface FmpService {

    @GET("quote/{symbols}")
    suspend fun quotes(@Path("symbols") commaSeparated: String): List<QuoteDto>

    @GET("etf-info")
    suspend fun etfInfo(@Query("symbol") symbol: String): List<EtfInfoDto>

    @GET("historical-price-full/{symbol}")
    suspend fun historical(
        @Path("symbol") symbol: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("serietype") serieType: String = "line",
    ): HistoricalResponse

    @GET("search")
    suspend fun search(
        @Query("query") q: String,
        @Query("limit") limit: Int = 20,
        @Query("exchange") exchange: String? = null,
    ): List<SearchHit>
}

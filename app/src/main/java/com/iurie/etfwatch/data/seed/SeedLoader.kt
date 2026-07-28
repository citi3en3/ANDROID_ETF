package com.iurie.etfwatch.data.seed

import android.content.Context
import com.iurie.etfwatch.data.db.EtfDao
import com.iurie.etfwatch.data.db.EtfEntity
import com.iurie.etfwatch.data.filter.EtnFilter
import com.iurie.etfwatch.data.remote.FmpSymbols
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class SeedEntry(
    val ticker: String,
    val name: String,
    val exchange: String,
    val sector: String? = null,
    val leverageFactor: Int? = null,
)

@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val etfDao: EtfDao,
    moshi: Moshi,
) {
    private val listType = Types.newParameterizedType(List::class.java, SeedEntry::class.java)
    private val adapter = moshi.adapter<List<SeedEntry>>(listType)

    suspend fun seedIfNeeded() {
        // insertAllIgnore is idempotent on the primary key, so running every launch
        // safely adds any newly shipped tickers without overwriting user changes.
        load("seed_hamilton.json", isHamilton = true, isLeveraged = false)
        load("seed_leveraged.json", isHamilton = false, isLeveraged = true)
    }

    private suspend fun load(asset: String, isHamilton: Boolean, isLeveraged: Boolean) {
        val entries: List<SeedEntry> = withContext(Dispatchers.IO) {
            val json = ctx.assets.open(asset).bufferedReader().use { it.readText() }
            adapter.fromJson(json).orEmpty()
        }
        val rows = entries
            .filterNot { EtnFilter.isEtn(it.ticker, it.name) }
            .map {
                EtfEntity(
                    ticker = FmpSymbols.normalize(it.ticker),
                    name = it.name,
                    exchange = it.exchange,
                    sector = it.sector,
                    isLeveraged = isLeveraged,
                    leverageFactor = it.leverageFactor,
                    isHamilton = isHamilton,
                )
            }
        etfDao.insertAllIgnore(rows)
    }
}

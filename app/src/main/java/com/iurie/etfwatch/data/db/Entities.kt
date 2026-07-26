package com.iurie.etfwatch.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "etfs")
data class EtfEntity(
    @PrimaryKey val ticker: String,
    val name: String,
    val exchange: String,
    val sector: String? = null,
    val isLeveraged: Boolean = false,
    val leverageFactor: Int? = null,
    val isHamilton: Boolean = false,
    val isUserAdded: Boolean = false,
    val isWatchlist: Boolean = false,
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val ticker: String,
    val price: Double?,
    @ColumnInfo(name = "change_pct") val changePct: Double?,
    @ColumnInfo(name = "dividend_yield") val dividendYield: Double?,
    @ColumnInfo(name = "market_cap") val marketCap: Double?,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "month_return_pct") val monthReturnPct: Double? = null,
)

@Entity(tableName = "alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val threshold: Double,
    /** "above" or "below" */
    val direction: String,
    val enabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
)

data class EtfWithQuote(
    @Embedded val etf: EtfEntity,
    @Embedded(prefix = "q_") val quote: QuoteEntity?,
)

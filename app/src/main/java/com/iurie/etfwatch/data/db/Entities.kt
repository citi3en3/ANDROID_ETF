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
    @ColumnInfo(name = "two_month_return_pct") val twoMonthReturnPct: Double? = null,
    @ColumnInfo(name = "week1_return_pct") val week1ReturnPct: Double? = null,
    @ColumnInfo(name = "week2_return_pct") val week2ReturnPct: Double? = null,
    @ColumnInfo(name = "week3_return_pct") val week3ReturnPct: Double? = null,
    @ColumnInfo(name = "week5_return_pct") val week5ReturnPct: Double? = null,
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
    /**
     * Hysteresis latch. An alert fires only while armed, then disarms; it re-arms once the price
     * moves back to the safe side of the threshold. Without this an "above" alert on a rising ETF
     * re-notifies on every refresh for as long as the condition holds.
     */
    val armed: Boolean = true,
)

data class EtfWithQuote(
    @Embedded val etf: EtfEntity,
    @Embedded(prefix = "q_") val quote: QuoteEntity?,
)

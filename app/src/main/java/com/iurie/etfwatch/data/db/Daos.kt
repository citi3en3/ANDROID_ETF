package com.iurie.etfwatch.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val JOIN_COLS = """
    e.*,
    q.ticker AS q_ticker,
    q.price AS q_price,
    q.change_pct AS q_change_pct,
    q.dividend_yield AS q_dividend_yield,
    q.market_cap AS q_market_cap,
    q.updated_at AS q_updated_at,
    q.month_return_pct AS q_month_return_pct
"""

@Dao
interface EtfDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(items: List<EtfEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EtfEntity)

    @Update
    suspend fun update(item: EtfEntity)

    @Query("SELECT * FROM etfs WHERE ticker = :ticker")
    suspend fun byTicker(ticker: String): EtfEntity?

    @Query("SELECT * FROM etfs")
    suspend fun all(): List<EtfEntity>

    @Query("UPDATE etfs SET isWatchlist = :inList WHERE ticker = :ticker")
    suspend fun setWatchlist(ticker: String, inList: Boolean)

    @Query("DELETE FROM etfs WHERE ticker = :ticker AND isUserAdded = 1")
    suspend fun deleteUserAdded(ticker: String)

    @Query("""
        SELECT $JOIN_COLS
        FROM etfs e
        LEFT JOIN quotes q ON q.ticker = e.ticker
        WHERE e.isWatchlist = 1
        ORDER BY e.ticker
    """)
    fun watchlistFlow(): Flow<List<EtfWithQuote>>

    @Query("""
        SELECT $JOIN_COLS
        FROM etfs e
        LEFT JOIN quotes q ON q.ticker = e.ticker
        WHERE e.isHamilton = 1
        ORDER BY q.dividend_yield DESC
    """)
    fun hamiltonFlow(): Flow<List<EtfWithQuote>>

    @Query("""
        SELECT $JOIN_COLS
        FROM etfs e
        LEFT JOIN quotes q ON q.ticker = e.ticker
        WHERE e.isLeveraged = 1
        ORDER BY e.sector, e.ticker
    """)
    fun leveragedFlow(): Flow<List<EtfWithQuote>>

    @Query("""
        SELECT $JOIN_COLS
        FROM etfs e
        LEFT JOIN quotes q ON q.ticker = e.ticker
        WHERE e.ticker = :ticker
    """)
    fun withQuoteFlow(ticker: String): Flow<EtfWithQuote?>

    @Query("SELECT ticker FROM etfs WHERE isWatchlist = 1 OR isHamilton = 1 OR isLeveraged = 1")
    suspend fun tickersToRefresh(): List<String>
}

@Dao
interface QuoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quotes: List<QuoteEntity>)

    @Query("SELECT * FROM quotes WHERE ticker = :ticker")
    suspend fun byTicker(ticker: String): QuoteEntity?
}

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: PriceAlertEntity): Long

    @Query("SELECT * FROM alerts WHERE enabled = 1")
    suspend fun enabled(): List<PriceAlertEntity>

    @Query("SELECT * FROM alerts WHERE ticker = :ticker")
    fun forTickerFlow(ticker: String): Flow<List<PriceAlertEntity>>

    @Query("UPDATE alerts SET lastTriggeredAt = :ts WHERE id = :id")
    suspend fun markTriggered(id: Long, ts: Long)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun delete(id: Long)
}

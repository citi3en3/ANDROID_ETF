package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.AlertDao
import com.iurie.etfwatch.data.db.PriceAlertEntity
import com.iurie.etfwatch.data.db.QuoteDao
import javax.inject.Inject
import javax.inject.Singleton

data class TriggeredAlert(val alert: PriceAlertEntity, val price: Double)

@Singleton
class AlertRepository @Inject constructor(
    private val alertDao: AlertDao,
    private val quoteDao: QuoteDao,
) {
    fun forTicker(ticker: String) = alertDao.forTickerFlow(ticker)

    suspend fun upsert(alert: PriceAlertEntity) = alertDao.upsert(alert)
    suspend fun delete(id: Long) = alertDao.delete(id)

    suspend fun checkAll(): List<TriggeredAlert> {
        val out = mutableListOf<TriggeredAlert>()
        alertDao.enabled().forEach { a ->
            val q = quoteDao.byTicker(a.ticker) ?: return@forEach
            val price = q.price ?: return@forEach
            val hit = when (a.direction) {
                "above" -> price >= a.threshold
                "below" -> price <= a.threshold
                else -> false
            }
            if (hit) {
                alertDao.markTriggered(a.id, System.currentTimeMillis())
                out += TriggeredAlert(a, price)
            }
        }
        return out
    }
}

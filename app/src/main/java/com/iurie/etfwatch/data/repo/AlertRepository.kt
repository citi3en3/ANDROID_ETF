package com.iurie.etfwatch.data.repo

import com.iurie.etfwatch.data.db.AlertDao
import com.iurie.etfwatch.data.db.PriceAlertEntity
import com.iurie.etfwatch.data.db.QuoteDao
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class TriggeredAlert(val alert: PriceAlertEntity, val price: Double)

/** What a single evaluation pass should do with an alert. */
enum class AlertAction { Fire, Rearm, None }

/**
 * Pure evaluation half of the alert logic.
 *
 * Alerts latch: they fire once when the threshold is crossed and stay silent until the price
 * returns to the safe side. Re-checking a still-true condition must not re-notify, otherwise an
 * "above" alert on a rising ETF fires on every refresh cycle, all day.
 */
object AlertEvaluator {

    const val DIRECTION_ABOVE = "above"
    const val DIRECTION_BELOW = "below"

    fun evaluate(direction: String, threshold: Double, armed: Boolean, price: Double): AlertAction {
        val conditionMet = when (direction.lowercase(Locale.ROOT)) {
            DIRECTION_ABOVE -> price >= threshold
            DIRECTION_BELOW -> price <= threshold
            else -> return AlertAction.None
        }
        return when {
            conditionMet && armed -> AlertAction.Fire
            !conditionMet && !armed -> AlertAction.Rearm
            else -> AlertAction.None
        }
    }
}

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
            val price = quoteDao.byTicker(a.ticker)?.price ?: return@forEach
            when (AlertEvaluator.evaluate(a.direction, a.threshold, a.armed, price)) {
                AlertAction.Fire -> {
                    alertDao.markTriggered(a.id, System.currentTimeMillis())
                    out += TriggeredAlert(a, price)
                }
                AlertAction.Rearm -> alertDao.rearm(a.id)
                AlertAction.None -> Unit
            }
        }
        return out
    }
}

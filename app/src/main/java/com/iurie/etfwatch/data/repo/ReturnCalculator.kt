package com.iurie.etfwatch.data.repo

import kotlin.math.abs
import kotlin.math.min

/**
 * Percent return over a lookback window, computed from a series of (epochDay, close) points.
 *
 * The nearest available close is used, because the target day often lands on a weekend or a market
 * holiday. It is only accepted if it is *close enough* to the date asked for: otherwise a fund with
 * three weeks of history would happily report its oldest close as a "2 month return", which reads
 * as a real number and is wrong by any amount.
 */
object ReturnCalculator {

    /** Widest gap tolerated between the requested day and the nearest close. */
    const val MAX_DAY_DRIFT = 5L

    /**
     * Allowed drift never exceeds half the window, so a short window can't be satisfied by a close
     * so recent that the "1 week return" is really a two-day return.
     */
    fun toleranceFor(daysAgo: Long): Long = min(MAX_DAY_DRIFT, daysAgo / 2)

    fun returnPct(
        series: List<Pair<Long, Double>>,
        nowEpochDay: Long,
        daysAgo: Long,
        price: Double,
    ): Double? {
        if (series.isEmpty()) return null
        val target = nowEpochDay - daysAgo
        val nearest = series.minByOrNull { abs(it.first - target) } ?: return null
        if (abs(nearest.first - target) > toleranceFor(daysAgo)) return null
        val close = nearest.second
        if (close <= 0.0) return null
        return ((price - close) / close) * 100.0
    }
}

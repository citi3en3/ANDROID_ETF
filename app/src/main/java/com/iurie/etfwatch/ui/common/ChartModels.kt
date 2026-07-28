package com.iurie.etfwatch.ui.common

enum class ChartStyle(val label: String) {
    Candlestick("Candlestick"),
    HeikinAshi("Heikin Ashi"),
    Line("Line"),
}

data class ChartPoint(
    val x: Float,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val label: String = "",
)

/**
 * Heikin-Ashi smoothing.
 *
 * ```
 * haClose = (open + high + low + close) / 4
 * haOpen  = first bar ? (open + close) / 2 : (prevHaOpen + prevHaClose) / 2
 * haHigh  = max(high, haOpen, haClose)
 * haLow   = min(low,  haOpen, haClose)
 * ```
 * Each bar depends on the previous *derived* bar, so this has to fold rather than map in isolation.
 */
internal fun List<ChartPoint>.toHeikinAshi(): List<ChartPoint> {
    var prevOpen: Float? = null
    var prevClose: Float? = null
    return map { point ->
        val haClose = (point.open + point.high + point.low + point.close) / 4f
        val po = prevOpen
        val pc = prevClose
        val haOpen = if (po == null || pc == null) (point.open + point.close) / 2f else (po + pc) / 2f
        val haHigh = maxOf(point.high, haOpen, haClose)
        val haLow = minOf(point.low, haOpen, haClose)
        prevOpen = haOpen
        prevClose = haClose
        ChartPoint(point.x, haOpen, haHigh, haLow, haClose, point.label)
    }
}

package com.iurie.etfwatch.ui.common

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.Utils

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

@Composable
fun MpCandleChart(
    points: List<ChartPoint>,
    style: ChartStyle,
    label: String,
    modifier: Modifier = Modifier,
) {
    if (style == ChartStyle.Line) {
        MpLineChart(points = points, label = label, modifier = modifier)
        return
    }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            CandleStickChart(ctx).apply {
                description = Description().apply { text = "" }
                legend.isEnabled = true
                axisRight.isEnabled = false
                setNoDataText("Loading…")
                setTouchEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                extraBottomOffset = 8f
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    isGranularityEnabled = true
                    labelRotationAngle = -35f
                    setAvoidFirstLastClipping(true)
                    textColor = Color.LTGRAY
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.argb(40, 255, 255, 255)
                    textColor = Color.LTGRAY
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String = "$" + String.format("%.2f", value)
                    }
                }
            }
        },
        update = { chart ->
            val candlePoints = if (style == ChartStyle.HeikinAshi) points.toHeikinAshi() else points
            if (candlePoints.isEmpty()) {
                chart.clear()
                chart.setNoDataText("No data")
                chart.invalidate()
                return@AndroidView
            }
            val entries = candlePoints.map { CandleEntry(it.x, it.high, it.low, it.open, it.close) }
            val labels = candlePoints.map { it.label }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            val desiredLabels = 6
            chart.xAxis.setLabelCount(minOf(desiredLabels, labels.size).coerceAtLeast(2), false)
            val ds = CandleDataSet(entries, "$label ${style.label}").apply {
                shadowWidth = 0.8f
                decreasingColor = Color.rgb(211, 47, 47)
                decreasingPaintStyle = android.graphics.Paint.Style.FILL
                increasingColor = Color.rgb(27, 135, 59)
                increasingPaintStyle = android.graphics.Paint.Style.FILL
                neutralColor = Color.rgb(117, 117, 117)
                setShadowColorSameAsCandle(true)
                setDrawValues(false)
            }
            chart.data = CandleData(ds).apply { setValueTextSize(10f) }
            chart.invalidate()
        },
    )
}

@Composable
fun MiniCandleChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            CandleStickChart(ctx).apply {
                description = Description().apply { text = "" }
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.isEnabled = false
                xAxis.isEnabled = false
                setNoDataText("")
                setTouchEnabled(false)
                setDrawGridBackground(false)
                setDrawBorders(false)
                description.isEnabled = false
                minOffset = 0f
            }
        },
        update = { chart ->
            if (points.isEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }
            val entries = points.map { CandleEntry(it.x, it.high, it.low, it.open, it.close) }
            val ds = CandleDataSet(entries, "").apply {
                shadowWidth = 1f
                decreasingColor = Color.rgb(211, 47, 47)
                decreasingPaintStyle = Paint.Style.FILL
                increasingColor = Color.rgb(27, 135, 59)
                increasingPaintStyle = Paint.Style.FILL
                neutralColor = Color.rgb(117, 117, 117)
                setShadowColorSameAsCandle(true)
                setDrawValues(false)
                isHighlightEnabled = false
            }
            chart.data = CandleData(ds)
            chart.invalidate()
        },
    )
}

@Composable
private fun MpLineChart(
    points: List<ChartPoint>,
    label: String,
    modifier: Modifier = Modifier,
) {
    val green = Color.rgb(27, 135, 59)
    val greenFill = Color.argb(60, 27, 135, 59)
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            Utils.init(ctx)
            LineChart(ctx).apply {
                description = Description().apply { text = "" }
                legend.isEnabled = false
                axisRight.isEnabled = false
                setNoDataText("Loading…")
                setTouchEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                extraBottomOffset = 8f
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    isGranularityEnabled = true
                    labelRotationAngle = -35f
                    setAvoidFirstLastClipping(true)
                    textColor = Color.LTGRAY
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = Color.argb(40, 200, 200, 200)
                    textColor = Color.LTGRAY
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String =
                            "$" + String.format("%.2f", value)
                    }
                }
            }
        },
        update = { chart ->
            if (points.isEmpty()) {
                chart.clear()
                chart.setNoDataText("No data")
                chart.invalidate()
                return@AndroidView
            }
            val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.close) }
            val labels = points.map { it.label }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.setLabelCount(minOf(6, labels.size).coerceAtLeast(2), false)

            val ds = LineDataSet(entries, label).apply {
                color = green
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                // Fill under the line with a gradient-like semi-transparent green
                setDrawFilled(true)
                fillColor = greenFill
                fillAlpha = 60
                highLightColor = Color.WHITE
            }
            chart.data = LineData(ds)
            chart.invalidate()
        },
    )
}

private fun List<ChartPoint>.toHeikinAshi(): List<ChartPoint> {
    var previousOpen: Float? = null
    var previousClose: Float? = null
    return map { point ->
        val haClose = (point.open + point.high + point.low + point.close) / 4f
        val haOpen = if (previousOpen == null || previousClose == null) {
            (point.open + point.close) / 2f
        } else {
            (previousOpen!! + previousClose!!) / 2f
        }
        val haHigh = maxOf(point.high, haOpen, haClose)
        val haLow = minOf(point.low, haOpen, haClose)
        previousOpen = haOpen
        previousClose = haClose
        ChartPoint(point.x, haOpen, haHigh, haLow, haClose, point.label)
    }
}

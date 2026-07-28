package com.iurie.etfwatch.ui.common

import android.graphics.Paint
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
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
import com.iurie.etfwatch.ui.theme.TrendColors
import java.util.Locale

/**
 * Colours handed to the MPAndroidChart views.
 *
 * These are resolved from the Compose theme and applied in `update` rather than `factory`: the
 * factory block runs once, so colours baked in there survive a light/dark switch and leave the
 * axes illegible. Axis labels used to be hardcoded to LTGRAY, which is invisible on a light
 * background.
 */
private data class ChartTheme(
    val axisText: Int,
    val grid: Int,
    val bull: Int,
    val bear: Int,
    val neutral: Int,
    val highlight: Int,
)

@Composable
private fun rememberChartTheme(): ChartTheme = ChartTheme(
    axisText = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
    grid = MaterialTheme.colorScheme.outlineVariant.toArgb(),
    bull = TrendColors.bull.toArgb(),
    bear = TrendColors.bear.toArgb(),
    neutral = MaterialTheme.colorScheme.outline.toArgb(),
    highlight = MaterialTheme.colorScheme.primary.toArgb(),
)

private fun currencyFormatter() = object : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = String.format(Locale.US, "$%.2f", value)
}

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
    val theme = rememberChartTheme()
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
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    valueFormatter = currencyFormatter()
                }
            }
        },
        update = { chart ->
            chart.xAxis.textColor = theme.axisText
            chart.axisLeft.textColor = theme.axisText
            chart.axisLeft.gridColor = theme.grid
            chart.legend.textColor = theme.axisText

            val candlePoints = if (style == ChartStyle.HeikinAshi) points.toHeikinAshi() else points
            if (candlePoints.isEmpty()) {
                chart.clear()
                chart.setNoDataTextColor(theme.axisText)
                chart.setNoDataText("No data")
                chart.invalidate()
                return@AndroidView
            }
            val entries = candlePoints.map { CandleEntry(it.x, it.high, it.low, it.open, it.close) }
            val labels = candlePoints.map { it.label }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.setLabelCount(minOf(DESIRED_X_LABELS, labels.size).coerceAtLeast(2), false)
            val ds = CandleDataSet(entries, "$label ${style.label}").apply {
                shadowWidth = 0.8f
                decreasingColor = theme.bear
                decreasingPaintStyle = Paint.Style.FILL
                increasingColor = theme.bull
                increasingPaintStyle = Paint.Style.FILL
                neutralColor = theme.neutral
                highLightColor = theme.highlight
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
    val theme = rememberChartTheme()
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
                decreasingColor = theme.bear
                decreasingPaintStyle = Paint.Style.FILL
                increasingColor = theme.bull
                increasingPaintStyle = Paint.Style.FILL
                neutralColor = theme.neutral
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
    val theme = rememberChartTheme()
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
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
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    valueFormatter = currencyFormatter()
                }
            }
        },
        update = { chart ->
            chart.xAxis.textColor = theme.axisText
            chart.axisLeft.textColor = theme.axisText
            chart.axisLeft.gridColor = theme.grid

            if (points.isEmpty()) {
                chart.clear()
                chart.setNoDataTextColor(theme.axisText)
                chart.setNoDataText("No data")
                chart.invalidate()
                return@AndroidView
            }
            val entries = points.mapIndexed { i, p -> Entry(i.toFloat(), p.close) }
            val labels = points.map { it.label }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            chart.xAxis.setLabelCount(minOf(DESIRED_X_LABELS, labels.size).coerceAtLeast(2), false)

            // Colour the series by overall direction so a losing period doesn't read as green.
            val rising = points.last().close >= points.first().close
            val lineColor = if (rising) theme.bull else theme.bear
            val ds = LineDataSet(entries, label).apply {
                color = lineColor
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 60
                highLightColor = theme.highlight
            }
            chart.data = LineData(ds)
            chart.invalidate()
        },
    )
}

private const val DESIRED_X_LABELS = 6

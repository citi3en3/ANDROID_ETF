package com.iurie.etfwatch.ui.common

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

data class ChartPoint(val x: Float, val y: Float)

@Composable
fun MpLineChart(
    points: List<ChartPoint>,
    label: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxWidth().height(260.dp),
        factory = { ctx ->
            LineChart(ctx).apply {
                description = Description().apply { text = "" }
                legend.isEnabled = true
                axisRight.isEnabled = false
                setNoDataText("Loading…")
                setTouchEnabled(true)
                setPinchZoom(true)
            }
        },
        update = { chart ->
            val entries = points.map { Entry(it.x, it.y) }
            val ds = LineDataSet(entries, label).apply {
                color = Color.rgb(33, 150, 243)
                lineWidth = 1.6f
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                setDrawFilled(true)
                fillAlpha = 40
                fillColor = Color.rgb(33, 150, 243)
            }
            chart.data = LineData(ds)
            chart.invalidate()
        },
    )
}

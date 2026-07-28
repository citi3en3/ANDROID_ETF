package com.iurie.etfwatch.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeikinAshiTest {

    private fun p(open: Float, high: Float, low: Float, close: Float, x: Float = 0f) =
        ChartPoint(x = x, open = open, high = high, low = low, close = close)

    @Test
    fun `empty input yields empty output`() {
        assertTrue(emptyList<ChartPoint>().toHeikinAshi().isEmpty())
    }

    @Test
    fun `first bar seeds the open from its own open and close`() {
        val result = listOf(p(10f, 12f, 9f, 11f)).toHeikinAshi()
        assertEquals(10.5f, result[0].open, 1e-4f) // (10 + 11) / 2
        assertEquals(10.5f, result[0].close, 1e-4f) // (10 + 12 + 9 + 11) / 4
    }

    @Test
    fun `later bars derive their open from the previous derived bar`() {
        val result = listOf(
            p(10f, 12f, 9f, 11f),
            p(11f, 14f, 10f, 13f),
        ).toHeikinAshi()
        val expectedOpen = (result[0].open + result[0].close) / 2f
        assertEquals(expectedOpen, result[1].open, 1e-4f)
        assertEquals(12f, result[1].close, 1e-4f) // (11 + 14 + 10 + 13) / 4
    }

    @Test
    fun `high and low always bracket the derived open and close`() {
        val input = listOf(
            p(10f, 12f, 9f, 11f),
            p(11f, 14f, 10f, 13f),
            p(13f, 13.5f, 8f, 8.5f),
            p(8.5f, 9f, 7f, 7.5f),
        )
        input.toHeikinAshi().forEach { bar ->
            assertTrue("high below open/close: $bar", bar.high >= maxOf(bar.open, bar.close) - 1e-4f)
            assertTrue("low above open/close: $bar", bar.low <= minOf(bar.open, bar.close) + 1e-4f)
        }
    }

    @Test
    fun `x position and label are preserved`() {
        val input = listOf(ChartPoint(x = 7f, open = 1f, high = 2f, low = 0.5f, close = 1.5f, label = "03/14"))
        val result = input.toHeikinAshi()
        assertEquals(7f, result[0].x, 1e-6f)
        assertEquals("03/14", result[0].label)
    }

    @Test
    fun `output length matches input length`() {
        val input = (1..25).map { p(it.toFloat(), it + 1f, it - 1f, it + 0.5f, it.toFloat()) }
        assertEquals(25, input.toHeikinAshi().size)
    }

    @Test
    fun `a flat series stays flat`() {
        val input = List(5) { p(10f, 10f, 10f, 10f) }
        input.toHeikinAshi().forEach {
            assertEquals(10f, it.open, 1e-4f)
            assertEquals(10f, it.close, 1e-4f)
        }
    }
}

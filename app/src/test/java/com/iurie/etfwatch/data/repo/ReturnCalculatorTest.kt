package com.iurie.etfwatch.data.repo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReturnCalculatorTest {

    private val today = 20_000L

    @Test
    fun `exact match computes the percentage change`() {
        val series = listOf((today - 30) to 100.0, today to 110.0)
        val result = ReturnCalculator.returnPct(series, today, 30, price = 110.0)
        assertEquals(10.0, result!!, 1e-9)
    }

    @Test
    fun `negative return is reported`() {
        val series = listOf((today - 30) to 200.0)
        val result = ReturnCalculator.returnPct(series, today, 30, price = 150.0)
        assertEquals(-25.0, result!!, 1e-9)
    }

    @Test
    fun `a weekend gap still resolves`() {
        // Target lands on a Sunday; the nearest close is the Friday two days earlier.
        val series = listOf((today - 32) to 100.0)
        val result = ReturnCalculator.returnPct(series, today, 30, price = 105.0)
        assertEquals(5.0, result!!, 1e-9)
    }

    @Test
    fun `a short history cannot masquerade as a two-month return`() {
        // Only ten days of history: the oldest close is nowhere near the 60-day mark.
        val series = listOf((today - 10) to 100.0, today to 130.0)
        assertNull(ReturnCalculator.returnPct(series, today, 60, price = 130.0))
    }

    @Test
    fun `tolerance never exceeds half the window`() {
        assertEquals(3L, ReturnCalculator.toleranceFor(7))
        assertEquals(5L, ReturnCalculator.toleranceFor(14))
        assertEquals(5L, ReturnCalculator.toleranceFor(60))
    }

    @Test
    fun `a one week window is not satisfied by todays close`() {
        val series = listOf(today to 100.0)
        assertNull(ReturnCalculator.returnPct(series, today, 7, price = 100.0))
    }

    @Test
    fun `empty series yields null`() {
        assertNull(ReturnCalculator.returnPct(emptyList(), today, 30, price = 100.0))
    }

    @Test
    fun `non-positive close yields null instead of dividing by zero`() {
        val series = listOf((today - 30) to 0.0)
        assertNull(ReturnCalculator.returnPct(series, today, 30, price = 100.0))
    }

    @Test
    fun `nearest of several candidates is chosen`() {
        val series = listOf(
            (today - 40) to 50.0,
            (today - 31) to 100.0,
            (today - 5) to 80.0,
        )
        val result = ReturnCalculator.returnPct(series, today, 30, price = 110.0)
        assertEquals(10.0, result!!, 1e-9)
    }
}

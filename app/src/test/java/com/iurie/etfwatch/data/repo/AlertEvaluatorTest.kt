package com.iurie.etfwatch.data.repo

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertEvaluatorTest {

    private fun above(armed: Boolean, price: Double) =
        AlertEvaluator.evaluate("above", threshold = 100.0, armed = armed, price = price)

    private fun below(armed: Boolean, price: Double) =
        AlertEvaluator.evaluate("below", threshold = 100.0, armed = armed, price = price)

    @Test
    fun `above fires when the threshold is crossed`() {
        assertEquals(AlertAction.Fire, above(armed = true, price = 105.0))
    }

    @Test
    fun `above does not fire twice while the condition holds`() {
        assertEquals(AlertAction.None, above(armed = false, price = 105.0))
    }

    @Test
    fun `above re-arms once the price falls back`() {
        assertEquals(AlertAction.Rearm, above(armed = false, price = 95.0))
    }

    @Test
    fun `armed alert below its threshold stays quiet`() {
        assertEquals(AlertAction.None, above(armed = true, price = 95.0))
    }

    @Test
    fun `hitting the threshold exactly counts as crossing`() {
        assertEquals(AlertAction.Fire, above(armed = true, price = 100.0))
        assertEquals(AlertAction.Fire, below(armed = true, price = 100.0))
    }

    @Test
    fun `below fires and re-arms in the opposite direction`() {
        assertEquals(AlertAction.Fire, below(armed = true, price = 95.0))
        assertEquals(AlertAction.None, below(armed = false, price = 95.0))
        assertEquals(AlertAction.Rearm, below(armed = false, price = 105.0))
    }

    @Test
    fun `direction is case insensitive`() {
        assertEquals(
            AlertAction.Fire,
            AlertEvaluator.evaluate("ABOVE", threshold = 100.0, armed = true, price = 105.0),
        )
    }

    @Test
    fun `an unrecognised direction never fires`() {
        assertEquals(
            AlertAction.None,
            AlertEvaluator.evaluate("sideways", threshold = 100.0, armed = true, price = 105.0),
        )
    }

    /** A full cycle: cross, hold, fall back, cross again. Only two notifications should result. */
    @Test
    fun `a full price cycle fires exactly twice`() {
        val prices = listOf(105.0, 106.0, 110.0, 95.0, 90.0, 101.0, 102.0)
        var armed = true
        var fires = 0
        prices.forEach { p ->
            when (above(armed, p)) {
                AlertAction.Fire -> { fires++; armed = false }
                AlertAction.Rearm -> armed = true
                AlertAction.None -> Unit
            }
        }
        assertEquals(2, fires)
    }
}

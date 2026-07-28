package com.iurie.etfwatch.ui.common

import com.iurie.etfwatch.data.db.EtfWithQuote
import kotlin.math.abs

/**
 * Shared list ordering and leverage predicates.
 *
 * Previously each of the three list ViewModels carried its own copy of this `when`, and they had
 * drifted: the 1x filter treated a missing leverage factor as 1x while the 2x/3x filters treated
 * it as 0, so unleveraged funds surfaced under "1x". Leverage is genuinely unknown when the factor
 * is null, and every filter here now agrees on that.
 */
object EtfSorting {

    /** Absolute leverage, or null when the fund declares none. */
    fun magnitude(item: EtfWithQuote): Int? = item.etf.leverageFactor?.let { abs(it) }

    fun isInverse(item: EtfWithQuote): Boolean = (item.etf.leverageFactor ?: 0) < 0

    /** Sorts unknown values last, then breaks ties on ticker so ordering is stable. */
    fun sort(items: List<EtfWithQuote>, mode: SortMode): List<EtfWithQuote> = when (mode) {
        SortMode.Ticker -> items.sortedBy { it.etf.ticker }
        SortMode.Sector -> items.sortedWith(
            compareBy({ it.etf.sector ?: UNKNOWN_SECTOR_SORT_KEY }, { it.etf.ticker })
        )
        SortMode.Price -> items.byDescending { it.quote?.price }
        SortMode.ChangePct -> items.byDescending { it.quote?.changePct }
        SortMode.Yield -> items.byDescending { it.quote?.dividendYield }
        SortMode.MonthReturn -> items.byDescending { it.quote?.monthReturnPct }
        SortMode.TwoMonthReturn -> items.byDescending { it.quote?.twoMonthReturnPct }
        SortMode.Week1Return -> items.byDescending { it.quote?.week1ReturnPct }
        SortMode.Week2Return -> items.byDescending { it.quote?.week2ReturnPct }
        SortMode.Week3Return -> items.byDescending { it.quote?.week3ReturnPct }
        SortMode.Week5Return -> items.byDescending { it.quote?.week5ReturnPct }
        // Leverage modes are filters, not orderings.
        SortMode.Inverse,
        SortMode.NonInverse,
        SortMode.Lev1x,
        SortMode.Lev2x,
        SortMode.Lev3x -> items.sortedBy { it.etf.ticker }
    }

    /** The return value for a period mode, or null if [mode] isn't a return period. */
    fun returnFor(item: EtfWithQuote, mode: SortMode?): Double? {
        val q = item.quote ?: return null
        return when (mode) {
            SortMode.Week1Return -> q.week1ReturnPct
            SortMode.Week2Return -> q.week2ReturnPct
            SortMode.Week3Return -> q.week3ReturnPct
            SortMode.Week5Return -> q.week5ReturnPct
            SortMode.MonthReturn -> q.monthReturnPct
            SortMode.TwoMonthReturn -> q.twoMonthReturnPct
            else -> null
        }
    }

    val RETURN_MODES = listOf(
        SortMode.Week1Return,
        SortMode.Week2Return,
        SortMode.Week3Return,
        SortMode.Week5Return,
        SortMode.MonthReturn,
        SortMode.TwoMonthReturn,
    )

    /** Sorts after every real sector name so unclassified funds land at the bottom. */
    private const val UNKNOWN_SECTOR_SORT_KEY = "￿"

    private inline fun List<EtfWithQuote>.byDescending(
        crossinline selector: (EtfWithQuote) -> Double?,
    ): List<EtfWithQuote> = sortedWith(
        compareByDescending<EtfWithQuote> { selector(it) ?: Double.NEGATIVE_INFINITY }
            .thenBy { it.etf.ticker }
    )
}

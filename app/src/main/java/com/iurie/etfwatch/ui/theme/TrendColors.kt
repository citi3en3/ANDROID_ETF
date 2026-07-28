package com.iurie.etfwatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Up/down colours for prices, returns and candles.
 *
 * Kept in one place because they were previously duplicated as raw hex in four files, and the dark
 * variants exist because the light-theme green/red are too dim to read on the dark surfaces.
 */
object TrendColors {
    val BullLight = Color(0xFF1B873B)
    val BullDark = Color(0xFF4ADE80)
    val BearLight = Color(0xFFD32F2F)
    val BearDark = Color(0xFFFF6B6B)

    val bull: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) BullDark else BullLight

    val bear: Color
        @Composable @ReadOnlyComposable get() = if (isSystemInDarkTheme()) BearDark else BearLight

    @Composable
    @ReadOnlyComposable
    fun forChange(change: Double?, fallback: Color): Color = when {
        change == null -> fallback
        change >= 0 -> bull
        else -> bear
    }
}

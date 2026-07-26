package com.iurie.etfwatch.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SortMode(val label: String) {
    Sector("Sector"),
    MonthReturn("1M %"),
    TwoMonthReturn("2M %"),
    Week1Return("1W %"),
    Week2Return("2W %"),
    Week3Return("3W %"),
    Week5Return("5W %"),
    Ticker("A→Z"),
    Inverse("Inverse"),
    NonInverse("Non-Inverse"),
    Lev1x("1x"),
    Lev2x("2x"),
    Lev3x("3x"),
    Price("Price"),
    ChangePct("Day %"),
    Yield("Yield");
}

@Composable
fun SortFilterBar(
    modes: List<SortMode> = SortMode.entries,
    selected: SortMode,
    onSelect: (SortMode) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        modes.forEach { mode ->
            FilterChip(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
fun MultiFilterBar(
    modes: List<SortMode>,
    selected: Set<SortMode>,
    onToggle: (SortMode) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        modes.forEach { mode ->
            FilterChip(
                selected = mode in selected,
                onClick = { onToggle(mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
fun MultiStringFilterBar(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    if (options.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt in selected,
                onClick = { onToggle(opt) },
                label = { Text(opt) },
            )
        }
    }
}

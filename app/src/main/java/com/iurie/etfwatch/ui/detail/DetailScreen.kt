package com.iurie.etfwatch.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iurie.etfwatch.ui.common.ChartStyle
import com.iurie.etfwatch.ui.common.MpCandleChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    ticker: String,
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAlertDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm) {
        vm.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    val inWatchlist = state.etf?.etf?.isWatchlist == true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.etf?.etf?.ticker ?: ticker) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = vm::toggleWatchlist) {
                        Icon(
                            imageVector = if (inWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (inWatchlist) "Remove from watchlist" else "Add to watchlist",
                            tint = if (inWatchlist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxWidth().verticalScroll(rememberScrollState())) {
            val q = state.etf?.quote
            val change = q?.changePct
            val color = when {
                change == null -> MaterialTheme.colorScheme.onSurface
                change >= 0 -> Color(0xFF1B873B)
                else -> Color(0xFFD32F2F)
            }
            Column(Modifier.padding(16.dp)) {
                Text(state.etf?.etf?.name ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(q?.price?.let { "%.2f".format(it) } ?: "—", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "  " + (change?.let { (if (it >= 0) "+" else "") + "%.2f%%".format(it) } ?: ""),
                        color = color,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.etf?.etf?.sector?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    state.etf?.etf?.leverageFactor?.let { AssistChip(onClick = {}, label = { Text("${it}x") }) }
                    q?.dividendYield?.let { AssistChip(onClick = {}, label = { Text("Yld %.2f%%".format(it)) }) }
                }
            }
            HorizontalDivider()

            // Range chips
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Range.entries.forEach { r ->
                    FilterChip(selected = r == state.range, onClick = { vm.setRange(r) }, label = { Text(r.label) })
                }
            }

            // Chart type toggle + Daily badge
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartStyleToggle(
                    selected = state.chartStyle,
                    onSelect = vm::setChartStyle,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "Daily",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Candle colour legend (hidden for Line style)
            if (state.chartStyle != ChartStyle.Line) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(end = 4.dp).background(Color(0xFF1B873B), RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {}
                        Text("Bullish", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.padding(end = 4.dp).background(Color(0xFFD32F2F), RoundedCornerShape(2.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {}
                        Text("Bearish", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            MpCandleChart(
                points = state.chart,
                style = state.chartStyle,
                label = ticker,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )

            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showAlertDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("  Alert")
                }
            }

            HorizontalDivider()
            Text("Alerts", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
            if (state.alerts.isEmpty()) {
                Text("No alerts.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.alerts.forEach { a ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${a.direction} %.2f".format(a.threshold), Modifier.weight(1f))
                        IconButton(onClick = { vm.removeAlert(a.id) }) { Icon(Icons.Filled.Delete, "Delete alert") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAlertDialog) {
        AlertDialogContent(
            onConfirm = { threshold, direction -> vm.addAlert(threshold, direction); showAlertDialog = false },
            onDismiss = { showAlertDialog = false },
        )
    }
}

@Composable
private fun ChartStyleToggle(
    selected: ChartStyle,
    onSelect: (ChartStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape),
    ) {
        ChartStyle.entries.forEachIndexed { idx, style ->
            val isSelected = style == selected
            val itemShape = when {
                ChartStyle.entries.size == 1 -> shape
                idx == 0 -> RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 0.dp, bottomEnd = 0.dp)
                idx == ChartStyle.entries.lastIndex -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 50.dp, bottomEnd = 50.dp)
                else -> RoundedCornerShape(0.dp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        itemShape,
                    )
                    .clickable { onSelect(style) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = style.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AlertDialogContent(
    onConfirm: (Double, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var threshold by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("above") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = threshold.toDoubleOrNull() != null,
                onClick = { onConfirm(threshold.toDouble(), direction) },
            ) { Icon(Icons.Filled.Add, null); Text(" Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("New price alert") },
        text = {
            Column {
                OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("Threshold") }, singleLine = true)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = direction == "above", onClick = { direction = "above" }, label = { Text("Above") })
                    FilterChip(selected = direction == "below", onClick = { direction = "below" }, label = { Text("Below") })
                }
            }
        },
    )
}

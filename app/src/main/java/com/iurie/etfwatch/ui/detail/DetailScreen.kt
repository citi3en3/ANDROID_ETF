package com.iurie.etfwatch.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import com.iurie.etfwatch.ui.common.MpLineChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    ticker: String,
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.etf?.etf?.ticker ?: ticker) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
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

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Range.entries.forEach { r ->
                    FilterChip(selected = r == state.range, onClick = { vm.setRange(r) }, label = { Text(r.label) })
                }
            }
            MpLineChart(points = state.chart, label = ticker, modifier = Modifier.padding(horizontal = 12.dp))

            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = vm::addToWatchlist) { Text("Add to watchlist") }
                TextButton(onClick = { showAlertDialog = true }) { Text("Set price alert") }
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

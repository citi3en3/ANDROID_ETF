package com.iurie.etfwatch.ui.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iurie.etfwatch.ui.common.EtfRow
import com.iurie.etfwatch.ui.common.SortFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onOpenDetail: (String) -> Unit,
    vm: WatchlistViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Watchlist") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Filled.Add, "Add") }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SortFilterBar(selected = state.sort, onSelect = vm::setSort)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.items.isEmpty()) {
                    EmptyState(text = "Tap + to add an ETF to your watchlist")
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.items, key = { it.etf.ticker }) { item ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    EtfRow(item, onClick = { onOpenDetail(item.etf.ticker) })
                                }
                                IconButton(onClick = { vm.remove(item.etf.ticker) }) {
                                    Icon(Icons.Filled.Delete, "Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTickerDialog(
            results = state.searchResults,
            onSearch = vm::search,
            onPick = { vm.add(it); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTickerDialog(
    results: List<com.iurie.etfwatch.data.remote.SearchHit>,
    onSearch: (String) -> Unit,
    onPick: (com.iurie.etfwatch.data.remote.SearchHit) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add ETF") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    singleLine = true,
                    label = { Text("Ticker or name") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    results.take(10).forEach { hit ->
                        TextButton(onClick = { onPick(hit) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${hit.symbol} — ${hit.name.orEmpty()}", maxLines = 1)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
    )
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text(text, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

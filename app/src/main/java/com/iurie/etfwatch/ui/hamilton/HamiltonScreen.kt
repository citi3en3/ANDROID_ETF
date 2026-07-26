package com.iurie.etfwatch.ui.hamilton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iurie.etfwatch.ui.common.EtfRow
import com.iurie.etfwatch.ui.common.MultiStringFilterBar
import com.iurie.etfwatch.ui.common.SortFilterBar
import com.iurie.etfwatch.ui.common.SortMode

private val hamiltonSortModes = listOf(
    SortMode.Sector,
    SortMode.Yield,
    SortMode.Week1Return,
    SortMode.Week2Return,
    SortMode.Week3Return,
    SortMode.Week5Return,
    SortMode.MonthReturn,
    SortMode.TwoMonthReturn,
    SortMode.Price,
    SortMode.ChangePct,
    SortMode.Ticker,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HamiltonScreen(
    onOpenDetail: (String) -> Unit,
    vm: HamiltonViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    LaunchedEffect(state.sort, state.sectorFilters) { listState.scrollToItem(0) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Hamilton ETFs (CA)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SortFilterBar(modes = hamiltonSortModes, selected = state.sort, onSelect = vm::setSort)
            MultiStringFilterBar(
                options = state.availableSectors,
                selected = state.sectorFilters,
                onToggle = vm::toggleSector,
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.flat.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Hamilton ETFs loaded — pull to refresh.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), state = listState) {
                        if (state.sort == SortMode.Sector) {
                            state.grouped.toSortedMap().forEach { (sector, items) ->
                                stickyHeader(key = "h_$sector") {
                                    Text(
                                        sector,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                                    )
                                }
                                items(items, key = { it.etf.ticker }) { item ->
                                    EtfRow(item, onClick = { onOpenDetail(item.etf.ticker) }, highlight = state.sort)
                                }
                            }
                        } else {
                            items(state.flat, key = { it.etf.ticker }) { item ->
                                EtfRow(item, onClick = { onOpenDetail(item.etf.ticker) }, highlight = state.sort)
                            }
                        }
                    }
                }
            }
        }
    }
}

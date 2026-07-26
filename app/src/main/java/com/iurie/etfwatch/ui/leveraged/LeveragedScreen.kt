package com.iurie.etfwatch.ui.leveraged

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iurie.etfwatch.ui.common.EtfRow
import com.iurie.etfwatch.ui.common.MultiFilterBar
import com.iurie.etfwatch.ui.common.SortFilterBar
import com.iurie.etfwatch.ui.common.SortMode

private val leveragedSortModes = listOf(
    SortMode.Sector,
    SortMode.MonthReturn,
    SortMode.Price,
    SortMode.ChangePct,
    SortMode.Yield,
)

private val leveragedFilterModes = listOf(
    SortMode.Inverse,
    SortMode.NonInverse,
    SortMode.Lev1x,
    SortMode.Lev2x,
    SortMode.Lev3x,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LeveragedScreen(
    onOpenDetail: (String) -> Unit,
    vm: LeveragedViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Leveraged ETFs (US + CA)") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SortFilterBar(modes = leveragedSortModes, selected = state.sort, onSelect = vm::setSort)
            MultiFilterBar(modes = leveragedFilterModes, selected = state.filters, onToggle = vm::toggleFilter)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
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
                                EtfRow(item, onClick = { onOpenDetail(item.etf.ticker) })
                            }
                        }
                    } else {
                        items(state.flat, key = { it.etf.ticker }) { item ->
                            EtfRow(item, onClick = { onOpenDetail(item.etf.ticker) })
                        }
                    }
                }
            }
        }
    }
}

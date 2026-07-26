package com.iurie.etfwatch.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.data.db.PriceAlertEntity
import com.iurie.etfwatch.data.repo.AlertRepository
import com.iurie.etfwatch.data.repo.EtfRepository
import com.iurie.etfwatch.data.repo.QuoteRepository
import com.iurie.etfwatch.ui.common.ChartPoint
import com.iurie.etfwatch.ui.common.ChartStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Range(val days: Int, val label: String) {
    M1(30, "1M"), M3(90, "3M"), M6(180, "6M"), Y1(365, "1Y"), Y5(1825, "5Y");
}

data class DetailUiState(
    val ticker: String = "",
    val etf: EtfWithQuote? = null,
    val range: Range = Range.Y1,
    val chartStyle: ChartStyle = ChartStyle.Candlestick,
    val chart: List<ChartPoint> = emptyList(),
    val alerts: List<PriceAlertEntity> = emptyList(),
    val isLoadingChart: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val etfRepo: EtfRepository,
    private val quoteRepo: QuoteRepository,
    private val alertRepo: AlertRepository,
) : ViewModel() {

    private val ticker: String = savedState.get<String>("ticker").orEmpty()
    private val range = MutableStateFlow(Range.Y1)
    private val chartStyle = MutableStateFlow(ChartStyle.Candlestick)
    private val chart = MutableStateFlow<List<ChartPoint>>(emptyList())
    private val loading = MutableStateFlow(false)

    private val chartState = combine(chartStyle, chart) { style, points -> style to points }

    val state: StateFlow<DetailUiState> =
        combine(
            etfRepo.detail(ticker),
            alertRepo.forTicker(ticker),
            range,
            chartState,
            loading,
        ) { etf, alerts, r, chartState, l ->
            val (style, c) = chartState
            DetailUiState(ticker = ticker, etf = etf, range = r, chartStyle = style, chart = c, alerts = alerts, isLoadingChart = l)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState(ticker = ticker))

    init { setRange(Range.Y1) }

    fun setRange(r: Range) {
        range.value = r
        viewModelScope.launch {
            loading.value = true
            chart.value = quoteRepo.history(ticker, r.days)
            loading.value = false
        }
    }

    fun setChartStyle(style: ChartStyle) { chartStyle.value = style }

    fun addAlert(threshold: Double, direction: String) = viewModelScope.launch {
        alertRepo.upsert(PriceAlertEntity(ticker = ticker, threshold = threshold, direction = direction))
    }

    fun removeAlert(id: Long) = viewModelScope.launch { alertRepo.delete(id) }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    fun toggleWatchlist() = viewModelScope.launch {
        val current = state.value.etf?.etf ?: return@launch
        if (current.isWatchlist) {
            etfRepo.removeFromWatchlist(current.ticker)
            _messages.tryEmit("Removed ${current.ticker} from watchlist")
        } else {
            etfRepo.addToWatchlist(current.ticker, current.name, current.exchange)
            _messages.tryEmit("Added ${current.ticker} to watchlist")
        }
    }

    fun addToWatchlist() = viewModelScope.launch {
        val current = state.value.etf?.etf ?: return@launch
        etfRepo.addToWatchlist(current.ticker, current.name, current.exchange)
        _messages.tryEmit("Added ${current.ticker} to watchlist")
    }
}

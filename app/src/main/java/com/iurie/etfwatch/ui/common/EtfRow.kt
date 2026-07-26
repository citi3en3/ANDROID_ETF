package com.iurie.etfwatch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.iurie.etfwatch.data.db.EtfWithQuote
import kotlin.math.abs

private val BADGE_COLORS = listOf(
    Color(0xFFB71C1C), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFFE65100),
    Color(0xFF4A148C), Color(0xFF00695C), Color(0xFF880E4F), Color(0xFF37474F),
    Color(0xFF1B5E20), Color(0xFF0D47A1), Color(0xFF827717), Color(0xFF4E342E),
)

fun tickerBadgeColor(ticker: String): Color =
    BADGE_COLORS[abs(ticker.hashCode()) % BADGE_COLORS.size]

@Composable
fun EtfRow(
    item: EtfWithQuote,
    onClick: () -> Unit,
    highlight: SortMode? = null,
) {
    val change = item.quote?.changePct
    val changeColor = when {
        change == null -> MaterialTheme.colorScheme.onSurfaceVariant
        change >= 0 -> Color(0xFF1B873B)
        else -> Color(0xFFD32F2F)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Colored ticker badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tickerBadgeColor(item.etf.ticker)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.etf.ticker.take(4),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(4.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        // Ticker + name + metadata
        Column(Modifier.weight(1f)) {
            Text(item.etf.ticker, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                item.etf.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val meta = buildList {
                item.etf.sector?.let { add(it) }
                if (item.etf.isLeveraged && item.etf.leverageFactor != null) add("${item.etf.leverageFactor}x")
                item.quote?.dividendYield?.let { add("Yld ${"%.2f".format(it)}%") }
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Returns get their own line(s); the sorted period leads so it is never
            // the one truncated away.
            val returns = returnPeriods(item, highlight)
            if (returns.isNotEmpty()) {
                Text(
                    returns.joinToString("  ") { (label, v) ->
                        "$label ${if (v >= 0) "+" else ""}${"%.2f".format(v)}%"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        // Price + change (+ the sorted return, so it reads next to the value it ranked on)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                item.quote?.price?.let { "%.2f".format(it) } ?: "—",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                change?.let { "${if (it >= 0) "+" else ""}%.2f%%".format(it) } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = changeColor,
            )
            highlightedReturn(item, highlight)?.let { (label, v) ->
                Text(
                    "$label ${if (v >= 0) "+" else ""}${"%.2f".format(v)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (v >= 0) Color(0xFF1B873B) else Color(0xFFD32F2F),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** Label + value for the period the list is currently sorted by, if it is a return period. */
private fun highlightedReturn(item: EtfWithQuote, highlight: SortMode?): Pair<String, Double>? {
    val q = item.quote ?: return null
    val value = when (highlight) {
        SortMode.Week1Return -> q.week1ReturnPct
        SortMode.Week2Return -> q.week2ReturnPct
        SortMode.Week3Return -> q.week3ReturnPct
        SortMode.Week5Return -> q.week5ReturnPct
        SortMode.MonthReturn -> q.monthReturnPct
        SortMode.TwoMonthReturn -> q.twoMonthReturnPct
        else -> null
    } ?: return null
    return highlight!!.label.removeSuffix(" %") to value
}

/** All available return periods, with the sorted one moved to the front. */
private fun returnPeriods(item: EtfWithQuote, highlight: SortMode?): List<Pair<String, Double>> {
    val q = item.quote ?: return emptyList()
    val all = listOf(
        SortMode.Week1Return to q.week1ReturnPct,
        SortMode.Week2Return to q.week2ReturnPct,
        SortMode.Week3Return to q.week3ReturnPct,
        SortMode.Week5Return to q.week5ReturnPct,
        SortMode.MonthReturn to q.monthReturnPct,
        SortMode.TwoMonthReturn to q.twoMonthReturnPct,
    )
    return all
        .sortedByDescending { (mode, _) -> mode == highlight }
        .mapNotNull { (mode, v) -> v?.let { mode.label.removeSuffix(" %") to it } }
}


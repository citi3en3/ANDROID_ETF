package com.iurie.etfwatch.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.iurie.etfwatch.data.db.EtfWithQuote

@Composable
fun EtfRow(
    item: EtfWithQuote,
    onClick: () -> Unit,
) {
    val change = item.quote?.changePct
    val changeColor = when {
        change == null -> MaterialTheme.colorScheme.onSurfaceVariant
        change >= 0 -> Color(0xFF1B873B)
        else -> Color(0xFFD32F2F)
    }
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.etf.ticker, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(item.etf.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(item.quote?.price?.let { "%.2f".format(it) } ?: "—", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = change?.let { (if (it >= 0) "+" else "") + "%.2f%%".format(it) } ?: "—",
                    color = changeColor,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item.etf.sector?.let {
                AssistChip(onClick = {}, label = { Text(it) }, colors = AssistChipDefaults.assistChipColors())
            }
            if (item.etf.isLeveraged && item.etf.leverageFactor != null) {
                AssistChip(onClick = {}, label = { Text("${item.etf.leverageFactor}x") })
            }
            item.quote?.dividendYield?.let {
                AssistChip(onClick = {}, label = { Text("Yld %.2f%%".format(it)) })
            }
        }
        HorizontalDivider(Modifier.padding(top = 8.dp))
    }
}

package com.iurie.etfwatch.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iurie.etfwatch.data.db.EtfWithQuote
import com.iurie.etfwatch.ui.common.ChartPoint
import com.iurie.etfwatch.ui.common.MiniCandleChart
import com.iurie.etfwatch.ui.common.tickerBadgeColor
import kotlinx.coroutines.delay

private val HeroGreen = Color(0xFF0D3B2E)
private val HeroAccent = Color(0xFF4ADEA0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = vm::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // 1. Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "ETF",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "Smart investing, diversified growth",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Notifications, "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            // 2. Hero carousel
            item {
                WatchlistCarousel(
                    items = state.watchlistItems,
                    sparklines = state.sparklines,
                    onOpenDetail = onOpenDetail,
                )
                Spacer(Modifier.height(24.dp))
            }

            // 3. Quick actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    QuickAction(Icons.Filled.Explore, "Explore ETFs") { onNavigate("leveraged") }
                    QuickAction(Icons.Filled.Star, "Watchlist") { onNavigate("watchlist") }
                    QuickAction(Icons.Filled.PieChart, "My Portfolio") { onNavigate("watchlist") }
                    QuickAction(Icons.Filled.Settings, "Orders") { onNavigate("settings") }
                }
                Spacer(Modifier.height(24.dp))
            }

            // 4. Top ETFs
            item { SectionHeader("Top ETFs") { onNavigate("leveraged") } }

            if (state.topEtfs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No ETFs — pull to refresh.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        state.topEtfs.forEachIndexed { index, item ->
                            HomeEtfRow(
                                item = item,
                                sparkline = state.sparklines[item.etf.ticker] ?: emptyList(),
                                onClick = { onOpenDetail(item.etf.ticker) },
                            )
                            if (index < state.topEtfs.lastIndex) {
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // 5. Promo card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.BarChart,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Diversify. Invest. Grow.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "ETFs offer instant diversification, lower costs, and long-term growth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { onNavigate("leveraged") },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Text("Explore ETFs", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // 6. Your Portfolio
            item { SectionHeader("Your Portfolio") { onNavigate("watchlist") } }

            if (state.watchlistItems.isEmpty()) {
                item {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "No ETFs in watchlist yet.\nSearch and add some from the Watchlist tab!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        // Asset Allocation header
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Asset Allocation",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "By Asset Class",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        state.watchlistItems.take(5).forEachIndexed { index, item ->
                            HomeEtfRow(
                                item = item,
                                sparkline = emptyList(),
                                onClick = { onOpenDetail(item.etf.ticker) },
                            )
                            if (index < minOf(4, state.watchlistItems.lastIndex)) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Internal composables ────────────────────────────────────────────────────

@Composable
private fun WatchlistCarousel(
    items: List<EtfWithQuote>,
    sparklines: Map<String, List<ChartPoint>>,
    onOpenDetail: (String) -> Unit,
) {
    if (items.isEmpty()) {
        // Empty state card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = HeroGreen),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Star, null, tint = HeroAccent, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Add ETFs to your watchlist",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "They will appear here as a carousel",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFADD8C0),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Auto-advance every 4 seconds
    LaunchedEffect(items.size) {
        while (true) {
            delay(4_000)
            currentIndex = (currentIndex + 1) % items.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                val entering = targetState > initialState
                (slideInHorizontally { if (entering) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { if (entering) -it else it } + fadeOut())
            },
            label = "carousel",
        ) { idx ->
            val item = items.getOrNull(idx % items.size) ?: return@AnimatedContent
            val spark = sparklines[item.etf.ticker] ?: emptyList()
            val change = item.quote?.changePct
            val changeColor = if ((change ?: 0.0) >= 0) HeroAccent else Color(0xFFFF6B6B)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDetail(item.etf.ticker) },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = HeroGreen),
            ) {
                Column(Modifier.padding(20.dp)) {
                    // Top row: badge + name + "Today" chip
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tickerBadgeColor(item.etf.ticker)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                item.etf.ticker.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.etf.ticker,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                item.etf.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFADD8C0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x33FFFFFF))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Today", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Price
                    Text(
                        text = item.quote?.price?.let { "$%.2f".format(it) } ?: "—",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    val sign = if ((change ?: 0.0) >= 0) "+" else ""
                    Text(
                        text = change?.let { "$sign${"%.2f".format(it)}% Today" } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = changeColor,
                    )
                    // Sparkline
                    if (spark.size >= 2) {
                        Spacer(Modifier.height(16.dp))
                        MiniCandleChart(
                            points = spark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        )
                    }
                }
            }
        }

        // Dot indicators
        if (items.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (i == currentIndex) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == currentIndex) HeroAccent
                                else HeroAccent.copy(alpha = 0.35f)
                            )
                            .clickable { currentIndex = i },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onSeeAll, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            Text("See All", style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Filled.ChevronRight, null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun HomeEtfRow(item: EtfWithQuote, sparkline: List<ChartPoint>, onClick: () -> Unit) {
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tickerBadgeColor(item.etf.ticker)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.etf.ticker.take(4),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.etf.ticker, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                item.etf.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (sparkline.size >= 2) {
            SparkLine(
                points = sparkline.map { it.close },
                color = changeColor,
                modifier = Modifier
                    .size(width = 64.dp, height = 32.dp)
                    .padding(horizontal = 4.dp),
            )
        } else {
            Spacer(Modifier.width(64.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                item.quote?.price?.let { "%.2f".format(it) } ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                change?.let { "${if (it >= 0) "+" else ""}%.2f%%".format(it) } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = changeColor,
            )
        }
    }
}

@Composable
private fun SparkLine(
    points: List<Float>,
    color: Color,
    fillColor: Color = Color.Transparent,
    modifier: Modifier = Modifier,
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val minVal = points.min()
        val maxVal = points.max()
        val range = (maxVal - minVal).coerceAtLeast(0.01f)

        fun xAt(i: Int) = i.toFloat() / (points.size - 1) * w
        fun yAt(v: Float) = h - ((v - minVal) / range * h * 0.9f) - h * 0.05f

        val fillPath = Path().apply {
            moveTo(xAt(0), yAt(points[0]))
            for (i in 1..points.lastIndex) {
                val x0 = xAt(i - 1); val y0 = yAt(points[i - 1])
                val x1 = xAt(i); val y1 = yAt(points[i])
                val mx = (x0 + x1) / 2f
                cubicTo(mx, y0, mx, y1, x1, y1)
            }
            lineTo(xAt(points.lastIndex), h)
            lineTo(xAt(0), h)
            close()
        }
        if (fillColor != Color.Transparent) {
            drawPath(fillPath, brush = Brush.verticalGradient(listOf(fillColor, Color.Transparent)))
        }

        val linePath = Path().apply {
            moveTo(xAt(0), yAt(points[0]))
            for (i in 1..points.lastIndex) {
                val x0 = xAt(i - 1); val y0 = yAt(points[i - 1])
                val x1 = xAt(i); val y1 = yAt(points[i])
                val mx = (x0 + x1) / 2f
                cubicTo(mx, y0, mx, y1, x1, y1)
            }
        }
        drawPath(linePath, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Dot at the end
        val lastX = xAt(points.lastIndex)
        val lastY = yAt(points.last())
        drawCircle(color = color, radius = 3.dp.toPx(), center = Offset(lastX, lastY))
    }
}

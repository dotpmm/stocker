package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.data.StockerStore
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.Quote
import com.dotpmm.stocker.domain.model.RangeSeries
import com.dotpmm.stocker.domain.model.UpcomingEvent
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.DirectionGlyph
import com.dotpmm.stocker.ui.components.PriceChart
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.components.rememberCountUp
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalSettings
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.directionOf
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import kotlinx.coroutines.launch

/**
 * The per-stock screen: live chart with the range selector, news tone, upcoming events and the
 * filtered news feed, plus the optional Gemini summary.
 */
@Composable
fun StockDetailScreen(
    stock: WatchStock,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onAiSummary: () -> Unit,
    onRemoved: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val store = StockerApp.instance.store
    val settings = LocalSettings.current
    val scope = rememberCoroutineScope()
    val refresh by store.refresh.collectAsState()
    val version by store.dataVersion.collectAsState()
    var rangeIndex by rememberSaveable { mutableIntStateOf(settings.defaultRangeIndex.coerceIn(0, 4)) }
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var confirmRemove by remember { mutableStateOf(false) }
    val hasPrice = stock.market == Market.India

    val quote by rememberQuery<Quote?>(null, stock.ticker) { store.quoteOf(stock) }
    val news by rememberQuery(emptyList<NewsItem>(), stock.ticker, sortIndex) {
        store.newsFor(stock.ticker, top = sortIndex == 1)
    }
    val events by rememberQuery(emptyList<UpcomingEvent>(), stock.ticker) { store.eventsFor(stock.ticker) }
    val spec = StockerStore.ranges[rangeIndex.coerceIn(0, StockerStore.ranges.lastIndex)]
    val series by produceState<RangeSeries?>(null, stock.ticker, spec.range, version) {
        value = if (hasPrice) store.series(stock, spec) else null
    }
    val tone = remember(news) { store.toneCounts(news) }
    val shownPrice = rememberCountUp(quote?.price ?: 0.0)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(StockerIcons.Back, "Back", onBack)
            Text("${stock.ticker} · ${stock.exchange}", style = Type.MicroData.copy(fontSize = 13.sp))
            IconTap(
                StockerIcons.Refresh,
                "Refresh news",
                { store.launchRefresh(stock.ticker) },
            )
        }

        LazyColumn(Modifier.weight(1f), contentPadding = contentPadding) {
            item(key = "price") {
                PriceHeader(
                    stock = stock,
                    quote = quote,
                    series = series,
                    shownPrice = shownPrice,
                    hasPrice = hasPrice,
                    rangeIndex = rangeIndex,
                    onRange = { rangeIndex = it },
                )
            }

            if (settings.showTone) {
                item(key = "tone") {
                    TonePanel(
                        counts = tone,
                        running = refresh.running,
                        onAiSummary = onAiSummary,
                    )
                }
            }

            if (events.isNotEmpty()) {
                item(key = "upcoming") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                        SectionMicro("Upcoming")
                        Spacer(Modifier.height(8.dp))
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            events.take(4).forEach { event ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .pressable(
                                            onClick = { event.articleId?.let(onOpenArticle) },
                                            pressedScale = 0.985f,
                                        ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(Modifier.width(38.dp)) {
                                        Text(
                                            Fmt.dayNumber(event.epochDay),
                                            style = Type.DataLarge.copy(fontSize = 18.sp),
                                        )
                                        Text(
                                            Fmt.monthShort(event.epochDay),
                                            style = Type.MicroData.copy(fontSize = 11.5.sp),
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            event.label,
                                            style = Type.BodyTight.copy(color = Ink.TextPrimary),
                                            maxLines = 2,
                                        )
                                        Text(Fmt.daysUntil(event.epochDay), style = Type.MicroData.copy(fontSize = 11.5.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "news-header") {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    SectionMicro("News")
                    Text("${news.size} stories", style = Type.MicroData.copy(fontSize = 11.5.sp))
                }
            }
            item(key = "news-sort") {
                SegmentedControl(
                    labels = listOf("NEWEST", "TOP"),
                    selectedIndex = sortIndex,
                    onSelect = { sortIndex = it },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            items(news.take(settings.newsPerCompany), key = { it.article.cluster + it.article.id }) { item ->
                NewsRow(item = item, onClick = { onOpenArticle(item.article.id) })
            }

            item(key = "remove") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text(
                        if (confirmRemove) "Tap again to remove ${stock.ticker}" else "Remove from watchlist",
                        style = Type.BodyStrong.copy(
                            color = if (confirmRemove) Ink.DownText else Ink.TextMuted,
                        ),
                        modifier = Modifier
                            .pressable(
                                onClick = {
                                    if (confirmRemove) {
                                        scope.launch {
                                            store.remove(stock.ticker)
                                            onRemoved()
                                        }
                                    } else {
                                        confirmRemove = true
                                    }
                                },
                                pressedScale = 0.97f,
                            )
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceHeader(
    stock: WatchStock,
    quote: Quote?,
    series: RangeSeries?,
    shownPrice: Double,
    hasPrice: Boolean,
    rangeIndex: Int,
    onRange: (Int) -> Unit,
) {
    val settings = LocalSettings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(stock.name, style = Type.Caption)
        Spacer(Modifier.height(6.dp))
        if (!hasPrice) {
            Text("News only for US stocks", style = Type.Meta2())
            return@Column
        }
        val direction = directionOf(series?.changePct ?: quote?.changePct)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (quote != null) Fmt.price(shownPrice) else "—",
                style = Type.DataLarge.copy(fontSize = 34.sp, letterSpacing = (-0.8).sp),
            )
            if (quote != null) {
                Row(
                    Modifier.padding(bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    DirectionGlyph(direction, size = 7.dp)
                    ChangeText(quote.changePct, style = Type.DataSmall, fractionDigits = 2)
                }
            }
        }
        if (series != null && series.points.size >= 2) {
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                val arrow = if (series.changePct >= 0) "▲" else "▼"
                Text(
                    "$arrow ${Fmt.number(kotlin.math.abs(series.changeAbs), 2)} (${Fmt.pct(series.changePct, 2)})",
                    style = Type.MicroData.copy(fontSize = 13.sp, color = direction.textColor2()),
                )
                Text(series.caption, style = Type.MicroData.copy(fontSize = 11.5.sp))
            }
            Spacer(Modifier.height(10.dp))
            PriceChart(
                points = series.points.mapIndexed { i, v ->
                    Point("2026-01-${(i % 28 + 1).toString().padStart(2, '0')}", v.toDouble())
                },
                direction = direction,
                height = 150.dp,
                showFill = settings.chartFill,
                showGlow = settings.chartGlow,
            )
        }
        Spacer(Modifier.height(10.dp))
        SegmentedControl(
            labels = StockerStore.ranges.map { it.label },
            selectedIndex = rangeIndex,
            onSelect = onRange,
        )
    }
}

@Composable
private fun TonePanel(counts: com.dotpmm.stocker.domain.model.ToneCounts, running: Boolean, onAiSummary: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionMicro("News tone · last 3 days")
                Text("${counts.total} stories", style = Type.MicroData.copy(fontSize = 11.5.sp))
            }
            if (counts.total > 0) {
                ToneBar(counts.positive, counts.neutral, counts.negative)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${counts.positive} positive", style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.UpText))
                    Text("${counts.neutral} neutral", style = Type.MicroData.copy(fontSize = 11.5.sp))
                    Text("${counts.negative} negative", style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.DownText))
                }
            } else {
                Text(
                    if (running) "Gathering news…" else "No stories in the last 3 days. Try refreshing.",
                    style = Type.Caption,
                )
            }
            ActionPill(label = "AI summary", onClick = onAiSummary, leading = StockerIcons.Spark)
            Text(
                "Uses your Gemini key · Not financial advice",
                style = Type.MicroData.copy(fontSize = 11.5.sp),
            )
        }
    }
}

private fun Direction.textColor2() = when (this) {
    Direction.Up -> Ink.UpText
    Direction.Down -> Ink.DownText
    Direction.Flat -> Ink.TextSecondary
}

private fun Type.Meta2() = Type.Caption

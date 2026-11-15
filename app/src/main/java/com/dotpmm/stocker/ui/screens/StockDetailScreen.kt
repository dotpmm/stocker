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
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.NewsRow
import com.dotpmm.stocker.ui.components.PrimaryActionBar
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.ToneBar
import com.dotpmm.stocker.ui.components.UnderlineTabs
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    val refresh by store.refresh.collectAsState()
    val version by store.dataVersion.collectAsState()
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    var sortIndex by rememberSaveable { mutableIntStateOf(0) }
    var confirmRemove by remember { mutableStateOf(false) }
    val hasPrice = stock.market == Market.India

    val quote by rememberQuery<Quote?>(null, stock.ticker) { store.quoteOf(stock) }
    val news by rememberQuery(emptyList<NewsItem>(), stock.ticker, sortIndex) { store.newsFor(stock.ticker, top = sortIndex == 1) }
    val events by rememberQuery(emptyList<UpcomingEvent>(), stock.ticker) { store.eventsFor(stock.ticker) }
    val spec = StockerStore.ranges[rangeIndex.coerceIn(0, StockerStore.ranges.lastIndex)]
    val series by produceState<RangeSeries?>(null, stock.ticker, spec.range, version) {
        value = if (hasPrice) store.series(stock, spec) else null
    }
    val tone = remember(news) { store.toneCounts(news) }
    val sideInset = Modifier.padding(horizontal = 20.dp)

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopIconButton(StockerIcons.Back, "Back", onBack)
            Text("${stock.ticker} · ${stock.exchange}", style = StockerText.SectionLabel.copy(letterSpacing = 1.68.sp))
            TopIconButton(
                StockerIcons.Refresh, "Refresh news", { store.launchRefresh(stock.ticker) },
                spinning = refresh.running,
            )
        }

        LazyColumn(Modifier.weight(1f), contentPadding = contentPadding) {
            item(key = "price") {
                Column(sideInset.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stock.name, style = StockerText.Body)
                    if (hasPrice) {
                        val shown = series
                        val shownQuote = quote
                        val color = when {
                            shown != null -> Fmt.changeColor(shown.changePct)
                            shownQuote != null -> Fmt.changeColor(shownQuote.changePct)
                            else -> Obsidian.Neutral
                        }
                        Text(
                            if (shownQuote != null) Fmt.price(shownQuote.price) else "—",
                            style = StockerText.Price,
                        )
                        if (shown != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                                val arrow = if (shown.changePct >= 0) "▲" else "▼"
                                Text(
                                    "$arrow ${Fmt.signed(shown.changeAbs)} (${Fmt.percent(shown.changePct)})",
                                    style = StockerText.Change.copy(color = color),
                                )
                                Text(shown.caption, style = StockerText.Meta)
                            }
                            Sparkline(
                                points = shown.points,
                                color = color,
                                showEndDot = true,
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .fillMaxWidth()
                                    .height(72.dp),
                            )
                        } else {
                            Text(
                                if (shownQuote == null) "Price unavailable right now" else "Loading chart…",
                                style = StockerText.Meta,
                            )
                            Spacer(Modifier.height(86.dp))
                        }
                        UnderlineTabs(
                            labels = StockerStore.ranges.map { it.label },
                            selectedIndex = rangeIndex,
                            onSelect = { rangeIndex = it },
                            fillWidth = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("News only for US stocks", style = StockerText.Meta)
                    }
                }
            }

            item(key = "tone") {
                Column(
                    sideInset.fillMaxWidth().padding(top = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NEWS TONE · LAST 3 DAYS", style = StockerText.SectionLabel)
                        Text("${tone.total} stories", style = StockerText.Meta)
                    }
                    if (tone.total > 0) {
                        ToneBar(tone.positive, tone.neutral, tone.negative)
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("${tone.positive} positive", style = StockerText.Meta.copy(color = Obsidian.TextSecondary))
                            Text("${tone.neutral} neutral", style = StockerText.Meta.copy(color = Obsidian.TextSecondary))
                            Text("${tone.negative} negative", style = StockerText.Meta.copy(color = Obsidian.TextSecondary))
                        }
                    } else {
                        Text(
                            if (refresh.running) "Gathering news…" else "No stories in the last 3 days. Try refreshing.",
                            style = StockerText.Body,
                        )
                    }
                    PrimaryActionBar(
                        label = "AI summary",
                        leadingIcon = StockerIcons.Spark,
                        trailingIcon = StockerIcons.Arrow,
                        onClick = onAiSummary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text("Uses your Gemini key · Not financial advice", style = StockerText.Caption.copy(color = Obsidian.TextSubtle))
                }
            }

            item(key = "upcoming") {
                Column(
                    sideInset.fillMaxWidth().padding(top = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel("UPCOMING")
                    if (events.isEmpty()) {
                        Text("No scheduled events found in recent news.", style = StockerText.Body)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            events.take(3).forEach { event ->
                                Column(
                                    Modifier
                                        .weight(1f)
                                        .pressable(
                                            onClick = { event.articleId?.let(onOpenArticle) },
                                            pressedScale = 0.97f,
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(Fmt.dayNumber(event.epochDay), style = StockerText.DayNumber)
                                        Spacer(Modifier.width(6.dp))
                                        Text(Fmt.monthShort(event.epochDay), style = StockerText.Month, modifier = Modifier.padding(bottom = 6.dp))
                                    }
                                    Text(event.label, style = StockerText.BodyStrong)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "news-header") {
                Column(
                    sideInset.fillMaxWidth().padding(top = 26.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionLabel("NEWS")
                        Text("${news.size} stories · 7 days", style = StockerText.Meta)
                    }
                    UnderlineTabs(
                        labels = listOf("Newest", "Top"),
                        selectedIndex = sortIndex,
                        onSelect = { sortIndex = it },
                    )
                }
            }
            items(news.take(60), key = { it.article.cluster + it.article.id }) { item ->
                NewsRow(
                    item = item,
                    onClick = { onOpenArticle(item.article.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            item(key = "remove") {
                Column(sideInset.fillMaxWidth().padding(top = 28.dp)) {
                    TextAction(
                        label = if (confirmRemove) "Tap again to remove ${stock.ticker}" else "Remove from watchlist",
                        color = if (confirmRemove) Obsidian.Negative else Obsidian.TextSubtle,
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
                    )
                }
            }
        }
    }
}

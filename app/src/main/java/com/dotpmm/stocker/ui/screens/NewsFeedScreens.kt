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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.UpcomingEvent
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalSettings
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import androidx.compose.foundation.shape.RoundedCornerShape

/** Every story across the watchlist, newest first, filterable by market. */
@Composable
fun NewsTab(onOpenArticle: (String) -> Unit, contentPadding: PaddingValues) {
    val store = StockerApp.instance.store
    val settings = LocalSettings.current
    val items by rememberQuery(emptyList<NewsItem>()) { store.allNews() }
    val watchlist by store.watchlist.collectAsState()
    val refresh by store.refresh.collectAsState()
    var filter by rememberSaveable { mutableIntStateOf(0) }

    val markets = remember(watchlist) { watchlist.associate { it.ticker to it.market } }
    val visible = remember(items, filter, markets, settings.newsWindowDays) {
        val cutoff = System.currentTimeMillis() - settings.newsWindowDays * 24L * 3_600_000
        items.filter { item ->
            val fresh = item.article.publishedAt >= cutoff
            val matches = when (filter) {
                1 -> item.tickers.any { markets[it] == Market.India }
                2 -> item.tickers.any { markets[it] == Market.US }
                else -> true
            }
            fresh && matches
        }
    }

    NewsFeedContent(
        items = visible,
        watchlistEmpty = watchlist.isEmpty(),
        windowDays = settings.newsWindowDays,
        contentPadding = contentPadding,
        onOpenArticle = onOpenArticle,
        onRefresh = { store.launchRefresh() },
    )
}

/** Stateless news body, renderable without the store. */
@Composable
fun NewsFeedContent(
    items: List<NewsItem>,
    watchlistEmpty: Boolean,
    windowDays: Int,
    contentPadding: PaddingValues,
    onOpenArticle: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = remember(items, filter) {
        when (filter) {
            1 -> items.filter { it.tickers.isNotEmpty() }
            2 -> items.filter { it.extraSources.isNotEmpty() }
            else -> items
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("News", style = Type.Section)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "${visible.size} stories · last $windowDays days",
                        style = Type.Caption,
                    )
                }
                IconTap(StockerIcons.Refresh, "Refresh now", onRefresh)
            }
        }
        item(key = "switch") {
            SegmentedControl(
                labels = listOf("ALL", "INDIA", "US"),
                selectedIndex = filter,
                onSelect = { filter = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        if (visible.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (watchlistEmpty) "No stocks yet" else "No stories yet",
                            style = Type.PanelTitle.copy(fontSize = 17.sp),
                        )
                        Text(
                            if (watchlistEmpty) {
                                "Add stocks to your watchlist and their news will collect here."
                            } else {
                                "Pull fresh news with the refresh button. Stocker also refreshes in the background."
                            },
                            style = Type.BodyTight,
                        )
                    }
                }
            }
        }
        items(visible, key = { it.article.cluster + it.article.id }) { item ->
            NewsRow(item = item, showTickers = true, onClick = { onOpenArticle(item.article.id) })
        }
        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

/** Scheduled events (results, board meetings, record dates) found in recent coverage. */
@Composable
fun EventsTab(onOpenArticle: (String) -> Unit, contentPadding: PaddingValues) {
    val store = StockerApp.instance.store
    val events by rememberQuery(emptyList<UpcomingEvent>()) { store.eventsUpcoming() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Events", style = Type.Section)
                Spacer(Modifier.height(3.dp))
                Text("Spotted in news · next 120 days", style = Type.Caption)
            }
        }
        if (events.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Nothing scheduled yet", style = Type.PanelTitle.copy(fontSize = 17.sp))
                        Text(
                            "When a story mentions a results date, board meeting or record date for a stock you " +
                                "follow, it appears here.",
                            style = Type.BodyTight,
                        )
                    }
                }
            }
        }
        items(events, key = { it.ticker + it.label + it.epochDay }) { event ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .flat(Ink.Surface1, RoundedCornerShape(16.dp), 16.dp)
                    .pressable(onClick = { event.articleId?.let(onOpenArticle) }, pressedScale = 0.985f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(Modifier.width(42.dp)) {
                    Text(Fmt.dayNumber(event.epochDay), style = Type.DataLarge.copy(fontSize = 20.sp))
                    Text(
                        Fmt.monthShort(event.epochDay),
                        style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.TextSecondary),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(event.ticker, style = Type.Ticker)
                    Text(event.label, style = Type.BodyTight.copy(color = Ink.TextSecondary), maxLines = 2)
                }
                Text(Fmt.daysUntil(event.epochDay), style = Type.MicroData.copy(fontSize = 11.5.sp))
            }
        }
        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

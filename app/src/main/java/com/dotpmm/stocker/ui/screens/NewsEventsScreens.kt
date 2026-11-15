package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.UpcomingEvent
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.NewsRow
import com.dotpmm.stocker.ui.components.UnderlineTabs
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

/** Every story across the watchlist, newest first, filterable by market. */
@Composable
fun NewsTab(onOpenArticle: (String) -> Unit, contentPadding: PaddingValues) {
    val store = StockerApp.instance.store
    val items by rememberQuery(emptyList<NewsItem>()) { store.allNews() }
    val watchlist by store.watchlist.collectAsState()
    val refresh by store.refresh.collectAsState()
    var filter by rememberSaveable { mutableIntStateOf(0) }

    val markets = remember(watchlist) { watchlist.associate { it.ticker to it.market } }
    val visible = remember(items, filter, markets) {
        items.filter { item ->
            when (filter) {
                1 -> item.tickers.any { markets[it] == Market.India }
                2 -> item.tickers.any { markets[it] == Market.US }
                else -> true
            }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            ScreenHeader(
                title = "News",
                subtitle = "${visible.size} stories from the last 7 days",
                trailing = {
                    TopIconButton(StockerIcons.Refresh, "Refresh now", { store.launchRefresh() }, spinning = refresh.running)
                },
            )
        }
        item(key = "tabs") {
            UnderlineTabs(
                labels = listOf("All", "India", "US"),
                selectedIndex = filter,
                onSelect = { filter = it },
                textStyle = StockerText.TabLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 14.dp),
            )
        }
        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyNote(
                    title = if (watchlist.isEmpty()) "No stocks yet" else "No stories yet",
                    body = if (watchlist.isEmpty()) {
                        "Add stocks to your watchlist and their news will collect here."
                    } else {
                        "Pull fresh news with the refresh button. Stocker also refreshes every 12 hours."
                    },
                )
            }
        }
        items(visible, key = { it.article.cluster + it.article.id }) { item ->
            NewsRow(
                item = item,
                showTickers = true,
                onClick = { onOpenArticle(item.article.id) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
            )
        }
    }
}

/** Scheduled events (results, board meetings, record dates) found in recent coverage. */
@Composable
fun EventsTab(onOpenArticle: (String) -> Unit, contentPadding: PaddingValues) {
    val store = StockerApp.instance.store
    val events by rememberQuery(emptyList<UpcomingEvent>()) { store.eventsUpcoming() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            ScreenHeader(title = "Events", subtitle = "Spotted in news · next 120 days")
        }
        if (events.isEmpty()) {
            item(key = "empty") {
                EmptyNote(
                    title = "Nothing scheduled yet",
                    body = "When a story mentions a results date, board meeting or record date for a stock you follow, it appears here.",
                )
            }
        }
        items(events, key = { it.ticker + it.label + it.epochDay }) { event ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(onClick = { event.articleId?.let(onOpenArticle) }, pressedScale = 0.98f, pressedAlpha = 0.6f)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.width(48.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(Fmt.dayNumber(event.epochDay), style = StockerText.DayNumber)
                    Text(Fmt.monthShort(event.epochDay), style = StockerText.Month)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("${event.ticker} · ${event.label}", style = StockerText.Headline)
                    Text(Fmt.daysUntil(event.epochDay), style = StockerText.Caption.copy(color = Obsidian.TextSecondary))
                }
            }
            Spacer(Modifier.padding(0.dp))
        }
    }
}

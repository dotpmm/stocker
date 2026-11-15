package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.MarketIndex
import com.dotpmm.stocker.domain.model.StockRow
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.PrimaryActionBar
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.ToneMarker
import com.dotpmm.stocker.ui.components.UnderlineTabs
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

@Composable
fun WatchlistTab(
    onOpenStock: (String) -> Unit,
    onAddStock: () -> Unit,
    contentPadding: PaddingValues,
) {
    val store = StockerApp.instance.store
    val rows by rememberQuery(emptyList<StockRow>()) { store.watchRows() }
    val indices by rememberQuery(emptyList<MarketIndex>()) { store.indices() }
    val refresh by store.refresh.collectAsState()
    val lastRefresh by store.lastRefresh.collectAsState()
    val watchlist by store.watchlist.collectAsState()

    val status = if (refresh.running) "Refreshing · ${refresh.message}" else Fmt.updated(lastRefresh)
    WatchlistScreen(
        rows = if (watchlist.isEmpty()) emptyList() else rows,
        indices = indices,
        status = status,
        refreshing = refresh.running,
        error = refresh.error,
        onOpenStock = onOpenStock,
        onRefresh = { store.launchRefresh() },
        onAddStock = onAddStock,
        contentPadding = contentPadding,
    )
}

@Composable
fun WatchlistScreen(
    rows: List<StockRow>,
    indices: List<MarketIndex>,
    status: String,
    refreshing: Boolean,
    error: String?,
    onOpenStock: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddStock: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var marketIndex by rememberSaveable { mutableIntStateOf(0) }
    val market = if (marketIndex == 0) Market.India else Market.US
    val visible = rows.filter { it.stock.market == market }

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            ScreenHeader(
                title = "Watchlist",
                subtitle = status,
                trailing = {
                    TopIconButton(StockerIcons.Refresh, "Refresh now", onRefresh, spinning = refreshing)
                    TopIconButton(StockerIcons.Add, "Add stock", onAddStock)
                },
            )
        }
        if (error != null) {
            item(key = "error") {
                Text(
                    error,
                    style = StockerText.Caption.copy(color = Obsidian.Negative),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
                )
            }
        }
        if (indices.isNotEmpty()) {
            item(key = "indices") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    indices.forEach { index ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(index.name, style = StockerText.SectionLabel)
                            Text(Fmt.plain(index.value), style = StockerText.IndexValue)
                            Text(
                                Fmt.arrowPercent(index.changePct),
                                style = StockerText.ChangeSmall.copy(color = Fmt.changeColor(index.changePct)),
                            )
                        }
                    }
                }
            }
        }
        item(key = "tabs") {
            UnderlineTabs(
                labels = listOf("India", "US news"),
                selectedIndex = marketIndex,
                onSelect = { marketIndex = it },
                textStyle = StockerText.TabLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
            )
        }
        if (visible.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        if (rows.isEmpty()) "Your watchlist is empty" else "Nothing here yet",
                        style = StockerText.TabLarge,
                    )
                    Text(
                        if (market == Market.India) {
                            "Add NSE or BSE companies and Stocker will gather their news, tone and upcoming events."
                        } else {
                            "Add US companies to follow their news. Prices are shown for Indian stocks only."
                        },
                        style = StockerText.Body,
                    )
                    PrimaryActionBar(
                        label = "Add stocks",
                        leadingIcon = StockerIcons.Add,
                        trailingIcon = StockerIcons.Arrow,
                        onClick = onAddStock,
                    )
                }
            }
        }
        items(visible, key = { it.stock.ticker }) { row ->
            StockRowItem(row = row, onClick = { onOpenStock(row.stock.ticker) })
        }
    }
}

@Composable
private fun StockRowItem(row: StockRow, onClick: () -> Unit) {
    val stock = row.stock
    val quote = row.quote
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .pressable(onClick, pressedScale = 0.98f, pressedAlpha = 0.55f)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stock.ticker, style = StockerText.Ticker)
            if (stock.market == Market.India) ToneLine(row) else Text(stock.name, style = StockerText.Caption, maxLines = 1)
        }
        if (stock.market == Market.India) {
            if (quote != null) {
                val color = Fmt.changeColor(quote.changePct)
                Sparkline(quote.trend, color, Modifier.size(width = 64.dp, height = 24.dp))
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(Fmt.price(quote.price), style = StockerText.RowPrice)
                    Text(Fmt.arrowPercent(quote.changePct), style = StockerText.ChangeSmall.copy(color = color))
                }
            } else {
                Text("—", style = StockerText.RowPrice.copy(color = Obsidian.TextSubtle))
            }
        } else {
            ToneLine(row)
        }
    }
}

@Composable
private fun ToneLine(row: StockRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (row.articleCount == 0) {
            Text("No news yet", style = StockerText.Caption)
        } else {
            ToneMarker(row.tone)
            Spacer(Modifier.width(6.dp))
            Text("${row.articleCount} stories · ${row.tone.name}", style = StockerText.Caption)
        }
    }
}

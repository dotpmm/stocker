package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.dotpmm.stocker.domain.model.MarketIndex
import com.dotpmm.stocker.domain.model.StockRow
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.DirectionGlyph
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.components.rememberCountUp
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalSettings
import com.dotpmm.stocker.ui.theme.LocalSpacing
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.directionOf
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal

/**
 * The watchlist: live index hero, market switch, and one metal row per followed company with its
 * sparkline, price and news tone. Adding and refreshing live in the header.
 */
@Composable
fun WatchlistTab(
    onOpenStock: (String) -> Unit,
    onAddStock: () -> Unit,
    contentPadding: PaddingValues,
    onBrowseMarkets: () -> Unit = {},
) {
    val store = StockerApp.instance.store
    val settings = LocalSettings.current
    val rows by rememberQuery(emptyList<StockRow>()) { store.watchRows() }
    val indices by rememberQuery(emptyList<MarketIndex>()) { store.indices() }
    val refresh by store.refresh.collectAsState()
    val lastRefresh by store.lastRefresh.collectAsState()
    val watchlist by store.watchlist.collectAsState()

    val status = if (refresh.running) "Refreshing · ${refresh.message}" else Fmt.updated(lastRefresh)
    WatchlistContent(
        rows = if (watchlist.isEmpty()) emptyList() else rows,
        indices = indices,
        status = status,
        refreshing = refresh.running,
        error = refresh.error,
        onOpenStock = onOpenStock,
        onRefresh = { store.launchRefresh() },
        onAddStock = onAddStock,
        contentPadding = contentPadding,
        alphabetically = settings.sortWatchlistAlphabetically,
        compact = settings.compactRows,
        onBrowseMarkets = onBrowseMarkets,
    )
}

/** Stateless watchlist body so the layout is renderable and testable without the network. */
@Composable
fun WatchlistContent(
    rows: List<StockRow>,
    indices: List<MarketIndex>,
    status: String,
    refreshing: Boolean,
    error: String?,
    onOpenStock: (String) -> Unit,
    onRefresh: () -> Unit,
    onAddStock: () -> Unit,
    contentPadding: PaddingValues,
    alphabetically: Boolean = false,
    compact: Boolean = false,
    onBrowseMarkets: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var marketIndex by rememberSaveable { mutableIntStateOf(0) }
    val market = if (marketIndex == 0) Market.India else Market.US
    val visible = remember(rows, market, alphabetically) {
        val filtered = rows.filter { it.stock.market == market }
        if (alphabetically) filtered.sortedBy { it.stock.name } else filtered
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            WatchlistHeader(
                status = status,
                refreshing = refreshing,
                onRefresh = onRefresh,
                onAddStock = onAddStock,
            )
        }
        if (error != null) {
            item(key = "error") {
                Text(
                    error,
                    style = Type.Caption.copy(color = Ink.DownText),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
        }
        if (indices.isNotEmpty()) {
            item(key = "indices") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    indices.forEach { index -> IndexPlate(index, Modifier.weight(1f)) }
                }
            }
        }
        item(key = "switch") {
            SegmentedControl(
                labels = listOf("INDIA", "US NEWS"),
                selectedIndex = marketIndex,
                onSelect = { marketIndex = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        if (visible.isEmpty()) {
            item(key = "empty") {
                EmptyWatchlist(market = market, onAddStock = onAddStock, onBrowseMarkets = onBrowseMarkets)
            }
        }
        items(visible, key = { it.stock.ticker }) { row ->
            WatchRow(row = row, compact = compact, onClick = { onOpenStock(row.stock.ticker) })
        }
        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun WatchlistHeader(
    status: String,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onAddStock: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Watchlist", style = Type.Section)
            Spacer(Modifier.height(3.dp))
            Text(status, style = Type.Caption, maxLines = 1)
        }
        IconTap(StockerIcons.Refresh, "Refresh now", onRefresh)
        Spacer(Modifier.width(8.dp))
        IconTap(StockerIcons.Add, "Add a stock", onAddStock)
    }
}

/** One index as a compact plate with its numeral counting up. */
@Composable
private fun IndexPlate(index: MarketIndex, modifier: Modifier = Modifier) {
    val shown = rememberCountUp(index.value)
    val direction = directionOf(index.changePct)
    Column(
        modifier
            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(index.name.uppercase(), style = Type.MicroData.copy(fontSize = 11.5.sp))
        Text(Fmt.index(shown), style = Type.DataLarge.copy(fontSize = 18.sp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            DirectionGlyph(direction, size = 6.dp)
            ChangeText(index.changePct, style = Type.DataSmall, fractionDigits = 2)
        }
    }
}

/** One followed company: ticker, name, news tone, sparkline and price. */
@Composable
private fun WatchRow(row: StockRow, compact: Boolean, onClick: () -> Unit) {
    val stock = row.stock
    val quote = row.quote
    val spacing = LocalSpacing.current
    val direction = directionOf(quote?.changePct)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = spacing.rowGap / 2)
            .flat(Ink.Surface1, RoundedCornerShape(16.dp), 16.dp)
            .pressable(onClick = onClick, pressedScale = 0.985f)
            .heightIn(min = if (compact) 52.dp else 64.dp)
            .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stock.ticker, style = Type.Ticker)
            if (!compact) {
                if (stock.market == Market.India) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (row.articleCount > 0) {
                            ToneMarker(row.tone)
                            Text(
                                "${row.articleCount} stories",
                                style = Type.MicroData.copy(fontSize = 11.5.sp),
                                maxLines = 1,
                            )
                        } else {
                            Text("No news yet", style = Type.MicroData.copy(fontSize = 11.5.sp))
                        }
                    }
                } else {
                    Text(stock.name, style = Type.Caption, maxLines = 1)
                }
            }
        }
        if (stock.market == Market.India && quote != null) {
            Sparkline(
                points = quote.trend,
                direction = direction,
                modifier = Modifier.size(width = 56.dp, height = if (compact) 18.dp else 24.dp),
                strokeWidth = 1.2.dp,
            )
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(Fmt.price(quote.price), style = Type.Data)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DirectionGlyph(direction, size = 5.dp)
                    ChangeText(quote.changePct, style = Type.MicroData.copy(fontSize = 13.sp, color = direction.textColor()), showGlyph = false)
                }
            }
        } else if (stock.market == Market.US) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ToneMarker(row.tone)
                Text("${row.articleCount}", style = Type.DataSmall)
            }
        }
    }
}

@Composable
private fun EmptyWatchlist(market: Market, onAddStock: () -> Unit, onBrowseMarkets: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Your watchlist is empty", style = Type.PanelTitle)
        Text(
            if (market == Market.India) "Add NSE or BSE companies to collect their news and prices."
            else "Add US companies to follow their news.",
            style = Type.BodyTight,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionPill(label = "Add stocks", onClick = onAddStock, leading = StockerIcons.Add)
            ActionPill(label = "Markets", onClick = onBrowseMarkets, leading = StockerIcons.Compass)
        }
    }
}

private fun Direction.textColor() = when (this) {
    Direction.Up -> Ink.UpText
    Direction.Down -> Ink.DownText
    Direction.Flat -> Ink.TextSecondary
}

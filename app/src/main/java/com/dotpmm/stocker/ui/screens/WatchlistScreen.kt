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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.domain.model.MarketIndex
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.Stock
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.ToneMarker
import com.dotpmm.stocker.ui.components.UnderlineTabs
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

@Composable
fun WatchlistScreen(
    stocks: List<Stock>,
    indices: List<MarketIndex>,
    updatedLabel: String,
    onOpenStock: (Stock) -> Unit,
    onRefresh: () -> Unit,
    onAddStock: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var marketIndex by rememberSaveable { mutableIntStateOf(0) }
    val market = if (marketIndex == 0) Market.India else Market.US
    val visible = stocks.filter { it.market == market }

    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Watchlist", style = StockerText.ScreenTitle)
                    Text(updatedLabel, style = StockerText.Meta)
                }
                Row {
                    TopIconButton(StockerIcons.Refresh, "Refresh now", onRefresh)
                    TopIconButton(StockerIcons.Add, "Add stock", onAddStock)
                }
            }
        }
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
        item(key = "tabs") {
            UnderlineTabs(
                labels = listOf("India", "US news"),
                selectedIndex = marketIndex,
                onSelect = { marketIndex = it },
                textStyle = StockerText.TabLarge,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
            )
        }
        items(visible, key = { it.ticker }) { stock ->
            StockRow(stock = stock, onClick = if (stock.price != null) ({ onOpenStock(stock) }) else null)
        }
    }
}

@Composable
private fun StockRow(stock: Stock, onClick: (() -> Unit)?) {
    val base = Modifier
        .fillMaxWidth()
        .heightIn(min = 76.dp)
    Row(
        modifier = (if (onClick != null) base.pressable(onClick, pressedScale = 0.98f, pressedAlpha = 0.55f) else base)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stock.ticker, style = StockerText.Ticker)
            if (stock.price != null) ToneLine(stock) else Text(stock.name, style = StockerText.Caption)
        }
        if (stock.price != null && stock.changePct != null) {
            val color = Fmt.changeColor(stock.changePct)
            Sparkline(stock.trend, color, Modifier.size(width = 64.dp, height = 24.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(Fmt.price(stock.price), style = StockerText.RowPrice)
                Text(Fmt.arrowPercent(stock.changePct), style = StockerText.ChangeSmall.copy(color = color))
            }
        } else {
            ToneLine(stock)
        }
    }
}

@Composable
private fun ToneLine(stock: Stock) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ToneMarker(stock.tone)
        Spacer(Modifier.width(6.dp))
        Text("${stock.articleCount} articles · ${stock.tone.name}", style = StockerText.Caption)
    }
}

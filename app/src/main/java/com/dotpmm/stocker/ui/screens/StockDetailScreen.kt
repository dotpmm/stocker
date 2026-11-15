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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.StockDetail
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.PrimaryActionBar
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.ToneBar
import com.dotpmm.stocker.ui.components.ToneMarker
import com.dotpmm.stocker.ui.components.UnderlineTabs
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

@Composable
fun StockDetailScreen(
    detail: StockDetail,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAiSummary: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) }
    val series = detail.ranges[rangeIndex.coerceIn(0, detail.ranges.lastIndex)]
    val stock = detail.stock
    val color = Fmt.changeColor(series.changePct)
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
            Text("${stock.ticker} · NSE", style = StockerText.SectionLabel.copy(letterSpacing = 1.68.sp))
            TopIconButton(StockerIcons.Refresh, "Refresh news", onRefresh)
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            item(key = "price") {
                Column(sideInset.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stock.name, style = StockerText.Body)
                    Text(Fmt.price(stock.price ?: 0.0), style = StockerText.Price)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        val arrow = if (series.changePct >= 0) "▲" else "▼"
                        Text(
                            "$arrow ${Fmt.signed(series.changeAbs)} (${Fmt.percent(series.changePct)})",
                            style = StockerText.Change.copy(color = color),
                        )
                        Text(series.caption, style = StockerText.Meta)
                    }
                    Sparkline(
                        points = series.points,
                        color = color,
                        showEndDot = true,
                        modifier = Modifier
                            .padding(top = 14.dp)
                            .fillMaxWidth()
                            .height(72.dp),
                    )
                    UnderlineTabs(
                        labels = detail.ranges.map { it.label },
                        selectedIndex = rangeIndex,
                        onSelect = { rangeIndex = it },
                        fillWidth = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item(key = "tone") {
                val tone = detail.toneCounts
                Column(sideInset.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NEWS TONE · LAST 12H", style = StockerText.SectionLabel)
                        Text("${tone.total} articles", style = StockerText.Meta)
                    }
                    ToneBar(tone.positive, tone.neutral, tone.negative)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("${tone.positive} positive", style = StockerText.Meta.copy(color = Obsidian.TextSecondary))
                        Text("${tone.neutral} neutral", style = StockerText.Meta.copy(color = Obsidian.TextSecondary))
                        Text("${tone.negative} negative", style = StockerText.Meta.copy(color = Obsidian.TextSecondary))
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
                Column(sideInset.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("UPCOMING")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        detail.events.forEach { event ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(event.day, style = StockerText.DayNumber)
                                    Spacer(Modifier.width(6.dp))
                                    Text(event.month, style = StockerText.Month, modifier = Modifier.padding(bottom = 6.dp))
                                }
                                Text(event.label, style = StockerText.BodyStrong)
                            }
                        }
                    }
                }
            }

            item(key = "news") {
                Column(sideInset.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        SectionLabel("NEWS")
                        Text("Newest first", style = StockerText.Meta)
                    }
                    detail.news.forEach { NewsRow(it) }
                }
            }
        }
    }
}

@Composable
private fun NewsRow(item: NewsItem) {
    Column(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = {}, pressedScale = 0.98f, pressedAlpha = 0.6f),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("${item.source} · ${item.age}", style = StockerText.Caption)
        Text(item.headline, style = StockerText.Headline)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.tag, style = StockerText.Caption)
            Text("·", style = StockerText.Caption)
            ToneMarker(item.tone)
            Text(item.tone.name, style = StockerText.Caption.copy(color = Obsidian.TextSecondary))
            if (item.extraSources > 0) {
                Text("·", style = StockerText.Caption)
                Text("+${item.extraSources} sources", style = StockerText.Caption)
            }
        }
    }
}

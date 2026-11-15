package com.dotpmm.stocker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

/** One story. Source and age, headline, then event tag, tone and how many other outlets ran it. */
@Composable
fun NewsRow(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTickers: Boolean = false,
) {
    val a = item.article
    val top = buildString {
        append(a.source).append(" · ").append(Fmt.age(a.publishedAt))
        if (showTickers && item.tickers.isNotEmpty()) append(" · ").append(item.tickers.take(3).joinToString(", "))
    }
    Column(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, pressedScale = 0.98f, pressedAlpha = 0.6f),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(top, style = StockerText.Caption, maxLines = 1)
        Text(a.title, style = StockerText.Headline, maxLines = 4)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(a.tag, style = StockerText.Caption)
            Text("·", style = StockerText.Caption)
            ToneMarker(a.tone)
            Text(a.tone.name, style = StockerText.Caption.copy(color = Obsidian.TextSecondary))
            if (item.extraSources.isNotEmpty()) {
                Text("·", style = StockerText.Caption)
                Text("+${item.extraSources.size} sources", style = StockerText.Caption)
            }
        }
    }
}

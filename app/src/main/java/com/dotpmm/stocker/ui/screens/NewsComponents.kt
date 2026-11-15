package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.domain.model.NewsItem
import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalSettings
import com.dotpmm.stocker.ui.theme.LocalSpacing
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal

/** Tone marker: a square rule, never a coloured dot (design.md §9.4 shape pairing). */
@Composable
fun ToneMarker(tone: Tone, size: androidx.compose.ui.unit.Dp = 5.dp) {
    Box(
        Modifier
            .size(size)
            .background(
                when (tone) {
                    Tone.Positive -> Ink.Up
                    Tone.Negative -> Ink.Down
                    Tone.Neutral -> Ink.TextMuted
                },
            ),
    )
}

/** Positive / neutral / negative share as one thin bar. */
@Composable
fun ToneBar(positive: Int, neutral: Int, negative: Int, modifier: Modifier = Modifier) {
    val total = (positive + neutral + negative).coerceAtLeast(1)
    Row(
        modifier.fillMaxWidth().height(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (positive > 0) {
            Box(Modifier.weight(positive.toFloat() / total).height(3.dp).background(Ink.Up))
        }
        if (neutral > 0) {
            Box(Modifier.weight(neutral.toFloat() / total).height(3.dp).background(Ink.TextMuted))
        }
        if (negative > 0) {
            Box(Modifier.weight(negative.toFloat() / total).height(3.dp).background(Ink.Down))
        }
    }
}

/**
 * One story. Source and age as a micro-label, the headline in the body face, then the event tag,
 * tone marker and the "also reported by" count — all on a metal plate.
 */
@Composable
fun NewsRow(
    item: NewsItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTickers: Boolean = false,
) {
    val settings = LocalSettings.current
    val spacing = LocalSpacing.current
    val article = item.article
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = spacing.rowGap / 2)
            .flat(Ink.Surface1, RoundedCornerShape(16.dp), 16.dp)
            .pressable(onClick = onClick, pressedScale = 0.985f)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildString {
                    append(article.source)
                    append(" · ")
                    append(Fmt.age(article.publishedAt))
                    if (showTickers && item.tickers.isNotEmpty()) {
                        append(" · ")
                        append(item.tickers.take(3).joinToString(", "))
                    }
                },
                style = Type.MicroData.copy(fontSize = 11.5.sp),
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            article.title,
            style = Type.BodyTight.copy(color = Ink.TextPrimary, fontSize = 14.sp, lineHeight = 20.sp),
            maxLines = 4,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (settings.showEventTags && article.tag.isNotBlank()) {
                Text(
                    article.tag,
                    style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.TextSecondary),
                    modifier = Modifier
                        .flat(Ink.Surface2, RoundedCornerShape(999.dp), 999.dp)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            if (settings.showTone) {
                ToneMarker(article.tone)
                Text(
                    article.tone.name.uppercase(),
                    style = Type.MicroData.copy(
                        fontSize = 11.5.sp,
                        color = when (article.tone) {
                            Tone.Positive -> Ink.UpText
                            Tone.Negative -> Ink.DownText
                            Tone.Neutral -> Ink.TextMuted
                        },
                    ),
                )
            }
            Spacer(Modifier.weight(1f))
            if (item.extraSources.isNotEmpty()) {
                Text(
                    "+${item.extraSources.size} sources",
                    style = Type.MicroData.copy(fontSize = 11.5.sp),
                )
            }
        }
    }
}

/** A stat pair: label above a mono value. */
@Composable
fun StatBlock(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = Ink.TextPrimary) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label.uppercase(), style = Type.MicroData.copy(fontSize = 11.5.sp))
        Text(value, style = Type.Data.copy(color = valueColor), maxLines = 1)
    }
}

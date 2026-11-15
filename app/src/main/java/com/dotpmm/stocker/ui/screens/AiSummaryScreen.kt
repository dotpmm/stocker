package com.dotpmm.stocker.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.ai.AiPoint
import com.dotpmm.stocker.domain.ai.AiSummary
import com.dotpmm.stocker.domain.ai.GeminiException
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.ui.components.PrimaryActionBar
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText
import kotlin.coroutines.cancellation.CancellationException

private sealed interface AiUiState {
    data object Loading : AiUiState
    data object NoKey : AiUiState
    data class Failed(val message: String) : AiUiState
    data class Done(val summary: AiSummary, val articles: List<Article>) : AiUiState
}

/** Optional Gemini summary of a stock's recent news. Needs the user's own key. */
@Composable
fun AiSummaryScreen(
    stock: WatchStock,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = StockerApp.instance.store
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<AiUiState>(AiUiState.Loading) }

    LaunchedEffect(attempt) {
        if (!store.hasGeminiKey()) {
            state = AiUiState.NoKey
            return@LaunchedEffect
        }
        state = AiUiState.Loading
        state = try {
            val (summary, articles) = store.aiSummary(stock.ticker)
            AiUiState.Done(summary, articles)
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeminiException) {
            AiUiState.Failed(e.message ?: "Gemini could not summarise this stock.")
        } catch (e: Exception) {
            AiUiState.Failed("Something went wrong. Check your connection and try again.")
        }
    }

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
            Text("AI SUMMARY · ${stock.ticker}", style = StockerText.SectionLabel.copy(letterSpacing = 1.68.sp))
            TopIconButton(
                StockerIcons.Refresh, "Run again", { attempt++ },
                spinning = state is AiUiState.Loading,
            )
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(stock.name, style = StockerText.Body)
            when (val s = state) {
                AiUiState.Loading -> Pulse("Reading recent stories…")
                AiUiState.NoKey -> {
                    Text("Add your Gemini key", style = StockerText.TabLarge)
                    Text(
                        "AI summaries use your own free Gemini API key from aistudio.google.com. It is stored only on this phone.",
                        style = StockerText.Body,
                    )
                    PrimaryActionBar(
                        label = "Open settings",
                        leadingIcon = StockerIcons.Spark,
                        trailingIcon = StockerIcons.Arrow,
                        onClick = onOpenSettings,
                    )
                }
                is AiUiState.Failed -> {
                    Text("Couldn't summarise", style = StockerText.TabLarge)
                    Text(s.message, style = StockerText.Body)
                    PrimaryActionBar(
                        label = "Try again",
                        leadingIcon = StockerIcons.Refresh,
                        trailingIcon = StockerIcons.Arrow,
                        onClick = { attempt++ },
                    )
                }
                is AiUiState.Done -> SummaryBody(s, onOpenArticle)
            }
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
    }
}

@Composable
private fun Pulse(text: String) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha",
    )
    Text(text, style = StockerText.TabLarge, modifier = Modifier.graphicsLayer { this.alpha = alpha })
}

@Composable
private fun SummaryBody(done: AiUiState.Done, onOpenArticle: (String) -> Unit) {
    val s = done.summary
    val stanceColor: Color = when (s.stance) {
        "positive" -> Obsidian.Positive
        "negative" -> Obsidian.Negative
        else -> Obsidian.TextSecondary
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("AI VIEW")
        Text(s.stance.replaceFirstChar { it.uppercase() }, style = StockerText.Price.copy(fontSize = 38.sp, color = stanceColor))
    }
    if (s.summary.isNotBlank()) Text(s.summary, style = StockerText.ReaderBody)
    PointSection("WHAT'S DRIVING IT", s.drivers, done.articles, onOpenArticle)
    PointSection("RISKS", s.risks, done.articles, onOpenArticle)
    PointSection("TO WATCH", s.upcoming, done.articles, onOpenArticle)
    Text(
        "Generated by Gemini from ${done.articles.size} headlines and summaries. It can be wrong and is not financial advice.",
        style = StockerText.Caption.copy(color = Obsidian.TextSubtle),
    )
}

@Composable
private fun PointSection(
    title: String,
    points: List<AiPoint>,
    articles: List<Article>,
    onOpenArticle: (String) -> Unit,
) {
    if (points.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionLabel(title)
        points.forEach { point ->
            val refs = point.refs.mapNotNull { articles.getOrNull(it - 1) }
            val sources = refs.map { it.source }.distinct().joinToString(", ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(
                        onClick = { refs.firstOrNull()?.let { onOpenArticle(it.id) } },
                        pressedScale = 0.98f,
                        pressedAlpha = 0.6f,
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.padding(top = 8.dp).size(6.dp).background(Obsidian.Accent))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(point.text, style = StockerText.Point)
                    if (sources.isNotEmpty()) Text(sources, style = StockerText.Caption)
                }
            }
        }
    }
}

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.ai.AiPoint
import com.dotpmm.stocker.domain.ai.AiSummary
import com.dotpmm.stocker.domain.ai.GeminiException
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
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
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(StockerIcons.Back, "Back", onBack)
            Text("AI SUMMARY · ${stock.ticker}", style = Type.MicroData.copy(fontSize = 13.sp))
            IconTap(StockerIcons.Refresh, "Run again", { attempt++ })
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(stock.name, style = Type.Caption)
            when (val s = state) {
                AiUiState.Loading -> Breathing("Reading recent stories…")
                AiUiState.NoKey -> KeyPrompt(onOpenSettings)
                is AiUiState.Failed -> Failure(s.message) { attempt++ }
                is AiUiState.Done -> SummaryBody(s, onOpenArticle)
            }
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
    }
}

/** The loading state breathes rather than spinning (design.md §8.2 bans spinners). */
@Composable
private fun Breathing(text: String) {
    val reduced = LocalReducedMotion.current
    if (reduced) {
        Text(text, style = Type.PanelTitle.copy(fontSize = 17.sp))
        return
    }
    val transition = rememberInfiniteTransition(label = "breathe")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "breatheAlpha",
    )
    Text(
        text,
        style = Type.PanelTitle.copy(fontSize = 17.sp),
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
    )
}

@Composable
private fun KeyPrompt(onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Add your Gemini key", style = Type.PanelTitle.copy(fontSize = 17.sp))
        Text(
            "AI summaries use your own free Gemini API key from aistudio.google.com. It is stored " +
                "only on this phone and used only when you tap the button.",
            style = Type.BodyTight,
        )
        ActionPill(label = "Open settings", onClick = onOpenSettings, leading = StockerIcons.Spark)
    }
}

@Composable
private fun Failure(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Couldn't summarise", style = Type.PanelTitle.copy(fontSize = 17.sp))
        Text(message, style = Type.BodyTight)
        ActionPill(label = "Try again", onClick = onRetry, leading = StockerIcons.Refresh)
    }
}

@Composable
private fun SummaryBody(done: AiUiState.Done, onOpenArticle: (String) -> Unit) {
    val s = done.summary
    val stanceColor = when (s.stance) {
        "positive" -> Ink.UpText
        "negative" -> Ink.DownText
        else -> Ink.TextSecondary
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionMicro("AI view")
        Text(
            s.stance.replaceFirstChar { it.uppercase() },
            style = Type.Section.copy(color = stanceColor),
        )
    }
    if (s.summary.isNotBlank()) Text(s.summary, style = Type.Body)
    PointSection("What's driving it", s.drivers, done.articles, onOpenArticle)
    PointSection("Risks", s.risks, done.articles, onOpenArticle)
    PointSection("To watch", s.upcoming, done.articles, onOpenArticle)
    Text(
        "Generated by Gemini from ${done.articles.size} headlines and summaries. It can be wrong and " +
            "is not financial advice.",
        style = Type.MicroData.copy(fontSize = 11.5.sp),
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
    Column(
        Modifier
            .fillMaxWidth()
            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionMicro(title)
        points.forEach { point ->
            val refs = point.refs.mapNotNull { articles.getOrNull(it - 1) }
            val sources = refs.map { it.source }.distinct().joinToString(", ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(
                        onClick = { refs.firstOrNull()?.let { onOpenArticle(it.id) } },
                        pressedScale = 0.985f,
                    ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.padding(top = 7.dp).size(4.dp).background(Ink.TextPrimary))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(point.text, style = Type.BodyTight.copy(color = Ink.TextPrimary))
                    if (sources.isNotEmpty()) {
                        Text(sources, style = Type.MicroData.copy(fontSize = 11.5.sp))
                    }
                }
            }
        }
    }
}

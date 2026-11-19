package com.dotpmm.stocker.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.research.HeatmapSector
import com.dotpmm.stocker.research.MacroSnapshot
import com.dotpmm.stocker.research.ResearchRepository
import com.dotpmm.stocker.research.SectorCard
import com.dotpmm.stocker.research.Verdict
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.DirectionGlyph
import com.dotpmm.stocker.ui.components.RollingNumber
import com.dotpmm.stocker.ui.components.SearchField
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.VerdictPill
import com.dotpmm.stocker.ui.components.OrbitBorder
import com.dotpmm.stocker.ui.components.entrance
import com.dotpmm.stocker.ui.components.rememberBreath
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.components.rememberCountUp
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Metal
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.NumeralGradient
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.ambientGlow
import com.dotpmm.stocker.ui.theme.borderTraveler
import com.dotpmm.stocker.ui.theme.directionOf
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import com.dotpmm.stocker.ui.theme.textColor
import kotlinx.coroutines.launch
import com.dotpmm.stocker.ui.theme.color

/**
 * Screen 1 — Macro (FUNCTIONALITY.md §3.1).
 *
 * Hero NIFTY numeral with the gradient fill and a live sparkline, the six sector plates with their
 * market-cap-weighted P/E verdicts. The interdependence graph is available as a setting for
 * anyone who wants the flow story; it is off by default on a phone screen.
 */
@Composable
fun MacroScreen(
    contentPadding: PaddingValues,
    onOpenSector: (String) -> Unit,
    onOpenStock: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { ResearchRepository(context) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<MacroSnapshot?>(null) }
    var heatmap by remember { mutableStateOf(emptyList<HeatmapSector>()) }
    var loading by remember { mutableStateOf(true) }
    var heatmapLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        loading = snapshot == null
        heatmapLoading = heatmap.isEmpty()
        error = null
        kotlinx.coroutines.coroutineScope {
            launch {
                try {
                    snapshot = repository.macro()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    error = e.message ?: "Couldn't reach the market data."
                } finally {
                    loading = false
                }
            }
            launch {
                heatmap = runCatching { repository.heatmap() }.getOrDefault(emptyList())
                heatmapLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }
    // Macro data auto-refreshes every 10 minutes while the screen is visible (§6).
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10 * 60_000L)
            runCatching { snapshot = repository.macro() }
            runCatching { repository.heatmap() }.onSuccess { heatmap = it }
        }
    }

    MacroScreenContent(
        snapshot = snapshot,
        loading = loading,
        error = error,
        contentPadding = contentPadding,
        onOpenSector = onOpenSector,
        onOpenStock = onOpenStock,
        repository = repository,
        heatmap = heatmap,
        heatmapLoading = heatmapLoading,
        onRetry = { scope.launch { load() } },
    )
}

/** Stateless Macro body — driven entirely by the passed snapshot so it is renderable in tests. */
@Composable
fun MacroScreenContent(
    snapshot: MacroSnapshot?,
    loading: Boolean,
    error: String?,
    contentPadding: PaddingValues,
    onOpenSector: (String) -> Unit,
    onOpenStock: (String) -> Unit,
    repository: ResearchRepository?,
    heatmap: List<HeatmapSector> = emptyList(),
    heatmapLoading: Boolean = false,
    onRetry: () -> Unit = {},
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            MacroHeader(
                onPick = { onOpenStock(it.symbol) },
                repository = repository,
            )
        }

        item(key = "hero") {
            Box(Modifier.entrance(delayMs = 120)) {
                HeroPanel(snapshot?.hero?.value, snapshot?.hero?.changePct, snapshot?.hero?.sparkline.orEmpty())
            }
        }

        item(key = "sectors-label") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Macro sectors", style = Type.Section)
                Text("P/E vs 5Y", style = Type.MicroData.copy(fontSize = 13.sp))
            }
        }

        if (loading && snapshot == null) {
            items(3, key = { "skeleton$it" }) { SkeletonSectorCard() }
        }

        error?.let { message ->
            item(key = "error") {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    ErrorCard(
                        title = "Market data unavailable",
                        body = "$message The cache will serve stale data meanwhile.",
                        onRetry = onRetry,
                    )
                }
            }
        }

        // A stale/offline snapshot explains itself inline rather than silently rendering dashes.
        snapshot?.staleNote?.let { note ->
            item(key = "stale") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .flat(Ink.Surface1, RoundedCornerShape(16.dp), 16.dp)
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(note, style = Type.Caption.copy(color = Ink.TextSecondary), modifier = Modifier.weight(1f))
                    Text(
                        "Retry",
                        style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextPrimary),
                        modifier = Modifier.pressable(onClick = onRetry, pressedScale = 0.94f),
                    )
                }
            }
        }

        // itemsIndexed avoids the O(n) indexOf() that used to run once per row per recomposition.
        itemsIndexed(snapshot?.sectors.orEmpty(), key = { _, card -> card.id }) { index, card ->
            // Staggered emergence: each plate rises 12dp and settles, 60ms apart (design.md §8.2).
            Box(Modifier.entrance(delayMs = 60 * index, rise = 12f)) {
                SectorPlate(card = card, onClick = { onOpenSector(card.id) })
            }
        }

        item(key = "heatmap") {
            Box(Modifier.entrance(delayMs = 120)) {
                MarketHeatmap(
                    sectors = heatmap,
                    loading = heatmapLoading,
                    repository = repository,
                    onOpenStock = onOpenStock,
                )
            }
        }

        item(key = "tail") { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MacroHeader(
    onPick: (com.dotpmm.stocker.research.SearchResult) -> Unit,
    repository: ResearchRepository?,
) {
    var text by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<com.dotpmm.stocker.research.SearchResult>()) }

    LaunchedEffect(text, repository) {
        val repo = repository
        if (text.trim().length < 2 || repo == null) {
            results = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(180)
        results = runCatching { repo.search(text) }.getOrDefault(emptyList())
    }

    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
        Box {
            SearchField(
                query = text,
                onQueryChange = { text = it },
                onSearch = { text = "" },
                results = results,
                onPick = onPick,
                showResults = text.trim().length >= 2,
                onSubmit = { results.firstOrNull()?.let(onPick) },
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/**
 * The hero: a huge tabular numeral with the vertical white gradient, its direction badge, and an
 * inline 60-point sparkline. The numeral counts up once; the panel carries the only border light
 * traveller on the screen.
 */
@Composable
private fun HeroPanel(value: Double?, changePct: Double?, sparkline: List<Float>) {
    val direction = directionOf(changePct)
    val shown = rememberCountUp(value ?: 0.0)
    // While the hero has no price yet the panel is "thinking": the glow breathes and a dot orbits
    // the border. Both stop the moment data lands (design.md §8.2).
    val computing = value == null
    val breath = rememberBreath(computing)
    val travel by androidx.compose.animation.core.rememberInfiniteTransition(label = "traveler")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                androidx.compose.animation.core.tween(7000, easing = androidx.compose.animation.core.LinearEasing),
            ),
            label = "travelerX",
        )

    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .metal(Ink.Surface1, RoundedCornerShape(26.dp), 26.dp)
                // The glow is clipped to the plate, so it follows every corner on all four sides.
                .ambientGlow(RoundedCornerShape(26.dp), 26.dp, intensity = if (computing) breath else 1f)
                .borderTraveler(direction.color(), travel),
        ) {
            Box(Modifier.fillMaxWidth().padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NIFTY 50", style = Type.Micro.copy(color = Ink.TextSecondary, letterSpacing = 1.6.sp))
                        Text("  ·  NSE", style = Type.MicroData.copy(fontSize = 13.sp))
                    }
                    Spacer(Modifier.height(10.dp))
                    if (value == null) {
                        // No price yet: say so plainly rather than showing a lone em dash beside a
                        // half-drawn row, which reads as a layout bug.
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (computing) "Fetching the index…" else "Index unavailable",
                            style = Type.PanelTitle.copy(fontSize = 18.sp, color = Ink.TextSecondary),
                        )
                        Spacer(Modifier.height(16.dp))
                    } else Row(verticalAlignment = Alignment.Bottom) {
                        // The hero numeral rolls its digits rather than snapping (design.md §8.2);
                        // sized so a 5-digit index plus two decimals fits a 360dp phone unclipped.
                        RollingNumber(
                            text = Fmt.index(shown),
                            style = Type.Hero.copy(
                                brush = NumeralGradient,
                                fontSize = 44.sp,
                                lineHeight = 48.sp,
                            ),
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Row(
                            Modifier.padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            ChangeText(changePct, style = Type.DataSmall, fractionDigits = 2)
                        }
                    }
                    if (sparkline.size >= 2) {
                        Spacer(Modifier.height(14.dp))
                        Sparkline(
                            points = sparkline,
                            direction = direction,
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            strokeWidth = 1.6.dp,
                        )
                    }
                }
            }
        }
    }
}

/** One bento sector plate: name + count, P/E vs baseline, verdict pill, sparkline. */
@Composable
private fun SectorPlate(card: SectorCard, onClick: () -> Unit) {
    val direction = directionOf(card.yearChangePct)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .flat(Ink.Surface1, RoundedCornerShape(20.dp), 20.dp)
            .pressable(onClick = onClick, pressedScale = 0.975f)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(card.name, style = Type.PanelTitle.copy(fontSize = 17.sp))
                Text("${card.nCompanies} cos", style = Type.MicroData)
            }
            VerdictPill(card.badge)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.ratio(card.peCurrent), style = Type.Data.copy(color = Ink.TextPrimary))
                    Text("vs ${Fmt.ratio(card.pe5y)}", style = Type.DataSmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ChangeText(card.yearChangePct, style = Type.DataSmall)
                }
            }
            if (card.sparkline.size >= 2) {
                Sparkline(
                    points = card.sparkline.takeLast(60),
                    direction = direction,
                    modifier = Modifier.width(104.dp).height(34.dp),
                )
            }
        }
    }
}

@Composable
private fun SkeletonSectorCard() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .flat(Ink.Surface1, RoundedCornerShape(20.dp), 20.dp)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBlock(Modifier.fillMaxWidth(0.55f), height = 16.dp)
            SkeletonBlock(Modifier.fillMaxWidth(0.38f), height = 12.dp)
            SkeletonBlock(Modifier.fillMaxWidth(0.7f), height = 12.dp)
        }
    }
}

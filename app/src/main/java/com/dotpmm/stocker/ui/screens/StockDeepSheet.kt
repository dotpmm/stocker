package com.dotpmm.stocker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dotpmm.stocker.research.Band
import com.dotpmm.stocker.research.EarningsDot
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.research.ResearchRepository
import com.dotpmm.stocker.research.StockDeep
import com.dotpmm.stocker.research.StockSeries
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.DirectionGlyph
import com.dotpmm.stocker.ui.components.PriceChart
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Direction
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.directionOf
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import kotlinx.coroutines.launch
import com.dotpmm.stocker.ui.theme.textColor

private val RANGES = listOf("5D", "1M", "6M", "1Y")

/**
 * Screen 3 — the deep-dive (FUNCTIONALITY.md §3.3). A bottom sheet at ≥92% height over a scrim,
 * under the status bar. Verdict banner → narrative → gauge → fundamentals → the noise-filter chart
 * with its range chips and earnings dots → the Google Finance action.
 */
@Composable
fun StockDeepSheet(
    symbol: String,
    onClose: () -> Unit,
    onOpenSector: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { ResearchRepository(context) }
    val scope = rememberCoroutineScope()
    var data by remember(symbol) { mutableStateOf<StockDeep?>(null) }
    var error by remember(symbol) { mutableStateOf<String?>(null) }
    var rangeIndex by remember { mutableIntStateOf(3) }
    var series by remember(symbol) { mutableStateOf<StockSeries?>(null) }

    LaunchedEffect(symbol) {
        error = null
        data = null
        series = null
        rangeIndex = 3
        try {
            data = repository.stock(symbol)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Couldn't load this stock."
        }
    }

    // One effect owns the series: it fires on open and again whenever a chip changes, refetching
    // history and re-running the full chart choreography. Keeping it single avoids fetching 1y twice.
    LaunchedEffect(symbol, rangeIndex) {
        series = runCatching { repository.series(symbol, RANGES[rangeIndex].lowercase()) }.getOrNull()
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .pressable(onClick = onClose, pressedScale = 1f),
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .metal(Ink.Surface2, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), 28.dp),
            ) {
                DeepDiveContent(
                    deep = data,
                    error = error,
                    loading = data == null && error == null,
                    seriesPoints = series?.points.orEmpty(),
                    seriesDots = series?.dots.orEmpty(),
                    rangeIndex = rangeIndex,
                    onRangeSelected = { rangeIndex = it },
                    onClose = onClose,
                    onOpenSector = onOpenSector,
                    onGoogleFinance = {
                        val url = "https://www.google.com/finance/quote/${symbol}:NSE"
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    },
                    onRetry = {
                        scope.launch {
                            error = null
                            runCatching { repository.stock(symbol) }
                                .onSuccess { data = it }
                                .onFailure { error = it.message }
                        }
                    },
                )
            }
        }
    }
}

/**
 * The deep-dive body: scrollable, stateless, and driven entirely by the passed payload so the
 * whole panel can be rendered and inspected without any network access.
 */
@Composable
fun DeepDiveContent(
    deep: StockDeep?,
    seriesPoints: List<Point>,
    seriesDots: List<EarningsDot>,
    rangeIndex: Int,
    onRangeSelected: (Int) -> Unit,
    onClose: () -> Unit,
    onOpenSector: (String) -> Unit,
    onGoogleFinance: () -> Unit,
    error: String? = null,
    loading: Boolean = false,
    onRetry: () -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 18.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .background(Ink.Surface3, RoundedCornerShape(999.dp)),
            )
        }

        if (loading) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SkeletonBlock(Modifier.fillMaxWidth(0.6f), height = 22.dp)
                SkeletonBlock(Modifier.fillMaxWidth(0.35f), height = 14.dp)
                SkeletonBlock(Modifier.fillMaxWidth(), height = 120.dp, corner = 16.dp)
                SkeletonBlock(Modifier.fillMaxWidth(), height = 200.dp, corner = 16.dp)
            }
            return@Column
        }

        if (error != null) {
            Box(Modifier.padding(20.dp)) {
                ErrorCard(title = "Deep-dive unavailable", body = error, onRetry = onRetry)
            }
            return@Column
        }

        deep ?: return@Column

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(deep.sector.uppercase())
                        if (!deep.curated) append(" · UNCURATED")
                        if (deep.note.isNotBlank()) append(" · ${deep.note}")
                    },
                    style = Type.MicroData.copy(fontSize = 13.sp),
                )
                Spacer(Modifier.height(6.dp))
                Text(deep.name, style = Type.Header)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(deep.symbol, style = Type.Ticker.copy(color = Ink.TextSecondary))
                    if (deep.sectorId != null) {
                        Text(
                            "Open sector",
                            style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextSecondary),
                            modifier = Modifier
                                .pressable(onClick = { onOpenSector(deep.sectorId) }, pressedScale = 0.94f)
                                .padding(vertical = 2.dp),
                        )
                    }
                }
            }
            IconTap(StockerIcons.Close, "Close", onClose)
        }

        Spacer(Modifier.height(16.dp))
        VerdictBanner(deep)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.padding(horizontal = 20.dp)) {
            SectionMicro("Narrative")
            Spacer(Modifier.height(8.dp))
            Text(deep.narrative, style = Type.Body)
        }

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ValuationGauge(deep, Modifier.weight(1f))
            Fundamentals(deep, Modifier.weight(1.15f))
        }

        Spacer(Modifier.height(22.dp))
        ChartPanel(
            symbol = deep.symbol,
            deep = deep,
            points = seriesPoints,
            dots = seriesDots,
            rangeIndex = rangeIndex,
            onRangeSelected = onRangeSelected,
        )

        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .metal(Ink.Surface3, RoundedCornerShape(999.dp), 999.dp, lifted = true)
                    .pressable(onClick = onGoogleFinance, pressedScale = 0.97f)
                    .padding(horizontal = 22.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Open in Google Finance", style = Type.ButtonLabel)
                Text("↗", style = Type.DataSmall.copy(color = Ink.TextMuted))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Information only, never financial advice.",
            style = Type.Caption.copy(fontSize = 12.5.sp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        )
    }
}

/** The "reality check" strip: verdict word, tinted by band, plus the P/E comparison. */
@Composable
private fun VerdictBanner(deep: StockDeep) {
    val direction = when (deep.reality.band) {
        Band.Over -> Direction.Down
        Band.Under -> Direction.Up
        else -> Direction.Flat
    }
    val tint = when (deep.reality.band) {
        Band.Over -> Ink.DownDim
        Band.Under -> Ink.UpDim
        else -> Color.White.copy(alpha = 0.04f)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(tint, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionGlyph(direction, size = 8.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            deep.reality.verdict,
            style = Type.ButtonLabel.copy(color = direction.textColor()),
            modifier = Modifier.weight(1f),
        )
        Text(
            "P/E ${Fmt.ratio(deep.pe)} vs ${Fmt.ratio(deep.reality.sectorAvg)}",
            style = Type.MicroData.copy(fontSize = 13.sp),
        )
    }
}

private fun Direction.textColorRef(): Color = when (this) {
    Direction.Up -> Ink.UpText
    Direction.Down -> Ink.DownText
    Direction.Flat -> Ink.TextSecondary
}

private fun Direction.textColor2(): Color = textColorRef()

/** Semi-circle speedometer: three bands, seven ticks, a needle that arrives 420 ms in. */
@Composable
private fun ValuationGauge(deep: StockDeep, modifier: Modifier = Modifier) {
    val needle = remember(deep.symbol) { Animatable(0f) }
    LaunchedEffect(deep.symbol) {
        needle.snapTo(0f)
        kotlinx.coroutines.delay(420)
        needle.animateTo(deep.reality.pct.toFloat(), tween(900, easing = Motion.Fluid))
    }
    Column(modifier) {
        SectionMicro("Valuation")
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                .padding(12.dp),
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(112.dp)) {
                val cx = size.width / 2f
                val cy = size.height - 12.dp.toPx()
                val radius = minOf(size.width / 2f, size.height) - 14.dp.toPx()
                val thickness = 9.dp.toPx()
                val arcs = listOf(
                    Ink.Up to 0f,
                    Ink.TextPrimary.copy(alpha = 0.55f) to 60f,
                    Ink.Down to 120f,
                )
                for ((color, start) in arcs) {
                    drawArc(
                        color = color.copy(alpha = 0.85f),
                        startAngle = 180f + start,
                        sweepAngle = 58f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = thickness,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    )
                }
                // Seven tick marks.
                for (i in 0..6) {
                    val angle = Math.toRadians(180.0 + i * 30.0)
                    val inner = radius - thickness - 3.dp.toPx()
                    val outer = radius - thickness - 7.dp.toPx()
                    drawLine(
                        Ink.TextMuted.copy(alpha = 0.4f),
                        androidx.compose.ui.geometry.Offset(
                            (cx + inner * Math.cos(angle)).toFloat(),
                            (cy + inner * Math.sin(angle)).toFloat(),
                        ),
                        androidx.compose.ui.geometry.Offset(
                            (cx + outer * Math.cos(angle)).toFloat(),
                            (cy + outer * Math.sin(angle)).toFloat(),
                        ),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                // Needle.
                val angle = Math.toRadians(180.0 + needle.value * 180.0)
                val len = radius - thickness - 2.dp.toPx()
                val tip = androidx.compose.ui.geometry.Offset(
                    (cx + len * Math.cos(angle)).toFloat(),
                    (cy + len * Math.sin(angle)).toFloat(),
                )
                drawLine(
                    Ink.TextPrimary,
                    androidx.compose.ui.geometry.Offset(cx, cy),
                    tip,
                    strokeWidth = 1.8.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawCircle(Ink.TextPrimary, radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
                drawCircle(Ink.Surface1, radius = 1.2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(cx, cy))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("UNDER", style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.UpText))
            Text("FAIR", style = Type.MicroData.copy(fontSize = 11.5.sp))
            Text("PERFECTION", style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.DownText))
        }
    }
}

@Composable
private fun Fundamentals(deep: StockDeep, modifier: Modifier = Modifier) {
    val direction = directionOf(deep.chgPct)
    Column(modifier) {
        SectionMicro("Fundamentals")
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            StatRow("Price", deep.ltp?.let { Fmt.price(it) } ?: "—") {
                ChangeText(deep.chgPct, style = Type.MicroData.copy(fontSize = 13.sp, color = direction.textColorRef()), fractionDigits = 2)
            }
            StatRow("Mkt cap", deep.mcapCr?.let { "${Fmt.crore(it)} Cr" } ?: "—")
            StatRow("P/E", Fmt.ratio(deep.pe))
            StatRow("Own 5Y P/E", deep.pe5yOwn?.let { Fmt.ratio(it) } ?: "—")
            StatRow("Sector P/E", Fmt.ratio(deep.peSectorAvg))
            StatRow("Rev CAGR", deep.revCagr5y?.let { Fmt.pct(it) } ?: "—")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = Type.MicroData.copy(fontSize = 13.sp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = Type.DataSmall.copy(color = Ink.TextPrimary))
            trailing?.invoke()
        }
    }
}

/** The noise filter: range chips + a chart whose draw-in re-runs on every range change. */
@Composable
private fun ChartPanel(
    symbol: String,
    deep: StockDeep,
    points: List<Point>,
    dots: List<EarningsDot>,
    rangeIndex: Int,
    onRangeSelected: (Int) -> Unit,
) {
    var scrubIndex by remember { mutableStateOf<Int?>(null) }

    val change = remember(points) {
        if (points.size < 2) null else (points.last().c - points.first().c) / points.first().c * 100
    }
    val direction = directionOf(change)
    // While a range is refetching, the panel shows visible "thinking" instead of a blank box.
    val computing = points.size < 2
    val breath = com.dotpmm.stocker.ui.components.rememberBreath(computing)

    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        com.dotpmm.stocker.ui.components.OrbitBorder(
            active = computing,
            modifier = Modifier.matchParentSize(),
            radius = 22.dp,
        )
        Column(
            Modifier
                .fillMaxWidth()
                .flat(Ink.Surface1, RoundedCornerShape(22.dp), 22.dp)
                .padding(14.dp),
        ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                SectionMicro("Noise filter")
                Spacer(Modifier.height(3.dp))
                ChangeText(change, style = Type.Data, fractionDigits = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(deep.symbol, style = Type.Ticker)
                Text("${points.size} pts", style = Type.MicroData.copy(fontSize = 11.5.sp))
            }
        }
        Spacer(Modifier.height(10.dp))

        if (points.size < 2) {
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                Text("No price history available", style = Type.Caption)
            }
        } else {
            val settings = com.dotpmm.stocker.ui.theme.LocalSettings.current
            PriceChart(
                points = points,
                direction = direction,
                height = 190.dp,
                earningsDots = if (settings.earningsDots) dots.map { it.date to it.surprise } else emptyList(),
                onScrub = { scrubIndex = it },
                showFill = settings.chartFill,
                showGlow = settings.chartGlow,
            )
            val active = scrubIndex?.let { dots.indexOfFirst { dot -> dot.date <= points.getOrNull(it)?.datePart.orEmpty() }.takeIf { d -> d >= 0 }?.let { dots[it] } }
            Box(Modifier.fillMaxWidth().height(34.dp).padding(top = 6.dp)) {
                if (active != null) {
                    EarningsTooltip(active)
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Fmt.chartDate(points.first().t), style = Type.MicroData.copy(fontSize = 11.5.sp))
                        Text(Fmt.chartDate(points[points.size / 2].t), style = Type.MicroData.copy(fontSize = 11.5.sp))
                        Text(Fmt.chartDate(points.last().t), style = Type.MicroData.copy(fontSize = 11.5.sp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            labels = RANGES,
            selectedIndex = rangeIndex,
            onSelect = onRangeSelected,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(7.dp).height(7.dp).background(Ink.Void, RoundedCornerShape(4.dp)))
            Box(
                Modifier.width(7.dp).height(7.dp).background(Ink.Down.copy(alpha = 0.7f), RoundedCornerShape(4.dp)),
            )
            Text("earnings dates", style = Type.MicroData.copy(fontSize = 11.5.sp))
        }
        activeDotInfo(dots)?.let {
            Text(it, style = Type.Caption.copy(fontSize = 12.5.sp))
        }
        }
    }
}

private fun activeDotInfo(dots: List<EarningsDot>): String? {
    val latest = dots.lastOrNull() ?: return null
    val surprise = latest.surprise
    val beat = surprise != null && surprise >= 0
    val head = when {
        surprise == null -> "Last earnings · ${Fmt.chartDate(latest.date)}"
        beat -> "Beat by ${Fmt.pct(surprise)} · ${Fmt.chartDate(latest.date)}"
        else -> "Missed by ${Fmt.pct(surprise)} · ${Fmt.chartDate(latest.date)}"
    }
    val tail = latest.dropPct?.let { " — price ${if (it >= 0) "rose" else "dropped"} ${Fmt.pct(it)}" } ?: ""
    return head + tail
}

@Composable
private fun EarningsTooltip(dot: EarningsDot) {
    val surprise = dot.surprise
    val text = when {
        surprise == null -> "Earnings date · ${Fmt.chartDate(dot.date)}"
        surprise >= 0 -> "Beat by ${Fmt.pct(surprise)} · ${Fmt.chartDate(dot.date)}"
        else -> "Missed by ${Fmt.pct(surprise)} · ${Fmt.chartDate(dot.date)}"
    }
    val tail = dot.dropPct?.let { " · price ${if (it >= 0) "rose" else "dropped"} ${Fmt.pct(it)}" }.orEmpty()
    Text(
        text + tail,
        style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextSecondary),
        modifier = Modifier
            .flat(Ink.Surface2, RoundedCornerShape(999.dp), 999.dp)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

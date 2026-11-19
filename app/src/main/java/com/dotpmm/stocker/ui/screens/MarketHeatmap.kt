package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.dotpmm.stocker.research.HeatmapSector
import com.dotpmm.stocker.research.HeatmapStock
import com.dotpmm.stocker.research.ResearchRepository
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.Sparkline
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.components.squarify
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.directionOf
import com.dotpmm.stocker.ui.theme.heatColor
import com.dotpmm.stocker.ui.theme.flat

private const val MAX_TILES_PER_SECTOR = 10

/**
 * The market heatmap below the macro plates. Every sector is its own squarified treemap: tile area
 * follows market cap, fill follows the day move. Tapping a tile pins a quote card next to it; the
 * card's "Details" action opens the existing deep-dive sheet.
 */
@Composable
fun MarketHeatmap(
    sectors: List<HeatmapSector>,
    loading: Boolean,
    repository: ResearchRepository?,
    onOpenStock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 38.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Market heatmap", style = Type.Section)
            Text("Size · mcap", style = Type.MicroData.copy(fontSize = 13.sp))
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Colour = day change · tap a tile for the quote",
                style = Type.Caption.copy(fontSize = 12.5.sp),
                modifier = Modifier.weight(1f),
            )
            HeatLegend()
        }

        if (loading && sectors.isEmpty()) {
            repeat(3) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .height(124.dp)
                        .flat(Ink.Surface1, RoundedCornerShape(10.dp), 10.dp),
                ) {
                    SkeletonBlock(
                        Modifier.padding(14.dp),
                        height = 96.dp,
                        corner = 6.dp,
                    )
                }
            }
        } else if (sectors.isEmpty()) {
            Text(
                "The heatmap will appear once quotes load.",
                style = Type.Caption,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        } else {
            sectors.forEachIndexed { index, sector ->
                SectorHeatBlock(
                    sector = sector,
                    repository = repository,
                    onOpenStock = onOpenStock,
                    modifier = Modifier.padding(top = if (index == 0) 4.dp else 12.dp),
                )
            }
        }
    }
}

@Composable
private fun HeatLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("−5%", style = Type.MicroData.copy(fontSize = 10.sp))
        Box(
            Modifier
                .width(54.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(heatColor(-5.0), Ink.Surface3, heatColor(5.0)),
                    ),
                ),
        )
        Text("+5%", style = Type.MicroData.copy(fontSize = 10.sp))
    }
}

@Composable
private fun SectorHeatBlock(
    sector: HeatmapSector,
    repository: ResearchRepository?,
    onOpenStock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = remember(sector) {
        sector.stocks.sortedByDescending { it.mcapCr ?: 0.0 }.take(MAX_TILES_PER_SECTOR)
    }
    var selected by remember(sector.name) { mutableStateOf<HeatmapStock?>(null) }
    var anchor by remember(sector.name) { mutableStateOf<IntRect?>(null) }
    var trend by remember(sector.name) { mutableStateOf(emptyList<Float>()) }
    var trendLoading by remember(sector.name) { mutableStateOf(false) }
    val bounds = remember(sector.name) { HashMap<String, IntRect>() }

    LaunchedEffect(selected?.symbol, repository) {
        val stock = selected
        if (stock == null || repository == null) {
            trend = emptyList()
            trendLoading = false
            return@LaunchedEffect
        }
        trend = emptyList()
        trendLoading = true
        trend = runCatching {
            repository.sparkline(stock.symbol)
        }.getOrDefault(emptyList())
        trendLoading = false
    }

    val height = when {
        tiles.size <= 2 -> 84.dp
        tiles.size <= 4 -> 108.dp
        tiles.size <= 6 -> 128.dp
        else -> 148.dp
    }

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                displaySectorName(sector.name),
                style = Type.Micro.copy(fontSize = 12.sp, color = Ink.TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            ChangeText(
                sector.changePct,
                style = Type.MicroData.copy(fontSize = 12.sp),
                fractionDigits = 2,
            )
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(height)
                .clip(RoundedCornerShape(9.dp))
                .background(Ink.Surface1),
        ) {
            val density = LocalDensity.current
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val cells = remember(tiles, widthPx, heightPx) {
                squarify(tiles.map { (it.mcapCr ?: 1.0).toFloat() }, widthPx, heightPx)
            }
            cells.forEach { cell ->
                val stock = tiles[cell.index]
                val cellWidth = with(density) { cell.width.toDp() }
                val cellHeight = with(density) { cell.height.toDp() }
                HeatTile(
                    stock = stock,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    selected = selected?.symbol == stock.symbol,
                    modifier = Modifier
                        .offset(
                            x = with(density) { cell.x.toDp() },
                            y = with(density) { cell.y.toDp() },
                        )
                        .size(cellWidth, cellHeight)
                        .padding(1.dp),
                    onPlaced = { bounds[stock.symbol] = it },
                    onClick = {
                        selected = stock
                        anchor = bounds[stock.symbol]
                    },
                )
            }
        }
        Spacer(Modifier.height(2.dp))
    }

    val stock = selected
    val spot = anchor
    if (stock != null && spot != null) {
        val gap = with(LocalDensity.current) { 10.dp.roundToPx() }
        val margin = with(LocalDensity.current) { 12.dp.roundToPx() }
        Popup(
            popupPositionProvider = TilePopupPositionProvider(spot, gap, margin),
            onDismissRequest = { selected = null },
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = true,
                dismissOnBackPress = true,
            ),
        ) {
            HeatmapPopover(
                sectorName = displaySectorName(sector.name),
                stock = stock,
                trend = trend,
                trendLoading = trendLoading,
                onClose = { selected = null },
                onDetails = {
                    selected = null
                    onOpenStock(stock.symbol)
                },
            )
        }
    }
}

@Composable
private fun HeatTile(
    stock: HeatmapStock,
    cellWidth: Dp,
    cellHeight: Dp,
    selected: Boolean,
    modifier: Modifier,
    onPlaced: (IntRect) -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(3.dp)
    val labelSize = when {
        cellWidth >= 92.dp && cellHeight >= 52.dp -> 12.5f
        cellWidth >= 58.dp && cellHeight >= 32.dp -> 10.5f
        cellWidth >= 36.dp && cellHeight >= 19.dp -> 8.5f
        else -> 0f
    }
    // Shrink long tickers to fit their tile rather than clipping them mid-letter.
    val fittedSize = with(LocalDensity.current) {
        val usable = (cellWidth - 6.dp).toPx().coerceAtLeast(1f)
        (usable / (stock.symbol.length.coerceAtLeast(1) * 0.64f)).toSp().value
    }
    val symbolSize = if (fittedSize >= 7.2f) minOf(labelSize, fittedSize) else 0f
    val showChange = symbolSize >= 10f && cellHeight >= 34.dp

    Box(
        modifier
            .onGloballyPositioned { onPlaced(it.boundsInWindow().toIntRect()) }
            .clip(shape)
            .background(heatColor(stock.changePct))
            .then(
                if (selected) Modifier.border(1.5.dp, Ink.TextPrimary, shape) else Modifier,
            )
            .pressable(onClick = onClick, pressedScale = 0.97f, haptic = false)
            .padding(horizontal = 3.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (symbolSize <= 0f) return@Box
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stock.symbol,
                style = Type.Ticker.copy(
                    fontSize = symbolSize.sp,
                    color = Color.White.copy(alpha = 0.96f),
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
            if (showChange) {
                Text(
                    text = signedPct(stock.changePct),
                    style = Type.MicroData.copy(
                        fontSize = (symbolSize - 1.2f).sp,
                        color = Color.White.copy(alpha = 0.82f),
                        letterSpacing = 0.sp,
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

@Composable
private fun HeatmapPopover(
    sectorName: String,
    stock: HeatmapStock,
    trend: List<Float>,
    trendLoading: Boolean,
    onClose: () -> Unit,
    onDetails: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val direction = directionOf(stock.changePct)
    Column(
        Modifier
            .width(274.dp)
            .shadow(18.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(Ink.Surface2)
            .border(1.dp, Ink.BorderFocus.copy(alpha = 0.35f), shape)
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(stock.symbol, style = Type.Ticker.copy(fontSize = 17.sp))
                Spacer(Modifier.height(2.dp))
                Text(
                    stock.name,
                    style = Type.Caption.copy(fontSize = 12.5.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(sectorName.uppercase(), style = Type.MicroData.copy(fontSize = 10.sp))
            }
            Box(
                Modifier
                    .size(28.dp)
                    .flat(Ink.Surface1, RoundedCornerShape(9.dp), 9.dp)
                    .pressable(onClick = onClose, pressedScale = 0.92f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    StockerIcons.Close,
                    contentDescription = "Close quote card",
                    tint = Ink.TextMuted,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                stock.price?.let { Fmt.price(it) } ?: "—",
                style = Type.DataLarge.copy(fontSize = 24.sp),
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(8.dp))
            ChangeText(
                stock.changePct,
                style = Type.DataSmall.copy(fontSize = 13.sp),
                fractionDigits = 2,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        if (trend.size >= 2) {
            Sparkline(
                points = trend,
                direction = direction,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                strokeWidth = 1.4.dp,
            )
        } else {
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.CenterStart) {
                Text(
                    if (trendLoading) "Loading 5D trend…" else "5D trend unavailable",
                    style = Type.Caption.copy(fontSize = 12.sp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PopoverStat("VOLUME", Fmt.volume(stock.volume), Modifier.weight(1f))
            PopoverStat(
                "DAY RANGE",
                if (stock.dayLow != null && stock.dayHigh != null) {
                    "${Fmt.price(stock.dayLow)} – ${Fmt.price(stock.dayHigh)}"
                } else {
                    "—"
                },
                Modifier.weight(1.35f),
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Ink.Surface3)
                .pressable(onClick = onDetails, pressedScale = 0.98f)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Details", style = Type.BodyStrong)
                Text(
                    "Full fundamentals & chart",
                    style = Type.Caption.copy(fontSize = 11.sp),
                )
            }
            Icon(
                StockerIcons.Arrow,
                contentDescription = null,
                tint = Ink.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun PopoverStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = Type.Micro.copy(fontSize = 10.5.sp))
        Spacer(Modifier.height(2.dp))
        Text(value, style = Type.DataSmall.copy(fontSize = 12.sp), maxLines = 1)
    }
}

/** Places the pinned quote card beside the tile, then clamps it inside the window. */
private class TilePopupPositionProvider(
    private val tile: IntRect,
    private val gap: Int,
    private val margin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val spaceRight = windowSize.width - tile.right
        val spaceLeft = tile.left
        val placeRight = spaceRight >= popupContentSize.width + gap + margin ||
            spaceRight >= spaceLeft
        val rawX = if (placeRight) {
            tile.right + gap
        } else {
            tile.left - gap - popupContentSize.width
        }
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val x = rawX.coerceIn(margin, maxX)
        val centeredY = tile.top + (tile.height - popupContentSize.height) / 2
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val y = centeredY.coerceIn(margin, maxY)
        return IntOffset(x, y)
    }
}

private fun Rect.toIntRect(): IntRect = IntRect(
    left = left.toInt(),
    top = top.toInt(),
    right = right.toInt(),
    bottom = bottom.toInt(),
)

private fun signedPct(value: Double?): String {
    if (value == null) return "—"
    val sign = if (value > 0) "+" else if (value < 0) "−" else ""
    return sign + String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(value)) + "%"
}

private fun displaySectorName(raw: String): String = when (raw) {
    "Information Technology" -> "Information Technology"
    "Fast Moving Consumer Goods" -> "FMCG"
    "Oil Gas & Consumable Fuels" -> "Oil, Gas & Fuels"
    "Automobile and Auto Components" -> "Automobile"
    "Telecommunication" -> "Telecom"
    "Construction Materials" -> "Cement & Materials"
    "Financial Services" -> "Financials"
    else -> raw
}

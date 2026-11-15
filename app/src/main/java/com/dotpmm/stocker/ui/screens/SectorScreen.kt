package com.dotpmm.stocker.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.dotpmm.stocker.research.CompanyRow
import com.dotpmm.stocker.research.ResearchRepository
import com.dotpmm.stocker.research.SectorDetail
import com.dotpmm.stocker.research.SectorFilters
import com.dotpmm.stocker.research.SortBy
import com.dotpmm.stocker.research.applyFilters
import com.dotpmm.stocker.research.loadFilters
import com.dotpmm.stocker.research.persist
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.ChangeText
import com.dotpmm.stocker.ui.components.DirectionGlyph
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.Sparkline
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

/**
 * Screen 2 — Sector drill-down (FUNCTIONALITY.md §3.2). Header with the index sparkline, a
 * collapsible filter section whose state persists across reloads, and a table of every mapped
 * stock in the sector with curated names first-class.
 */
@Composable
fun SectorScreen(
    sectorId: String,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenStock: (String) -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { ResearchRepository(context) }
    val scope = rememberCoroutineScope()
    var detail by remember(sectorId) { mutableStateOf<SectorDetail?>(null) }
    var filters by remember { mutableStateOf(loadFilters(context)) }
    var filtersOpen by remember { mutableStateOf(false) }
    var loading by remember(sectorId) { mutableStateOf(true) }

    LaunchedEffect(sectorId) {
        loading = true
        detail = runCatching { repository.sector(sectorId) }.getOrNull()
        loading = false
    }

    val rows = remember(detail, filters) {
        applyFilters(detail?.companies.orEmpty(), filters)
    }

    SectorScreenContent(
        detail = detail,
        rows = rows,
        loading = loading,
        contentPadding = contentPadding,
        onBack = onBack,
        onOpenStock = onOpenStock,
        filters = filters,
        onFiltersChange = {
            filters = it
            it.persist(context)
        },
        filtersOpen = filtersOpen,
        onToggleFilters = { filtersOpen = !filtersOpen },
    )
}

/** Stateless Sector body so the layout can be rendered and verified without the network. */
@Composable
fun SectorScreenContent(
    detail: SectorDetail?,
    rows: List<CompanyRow>,
    loading: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenStock: (String) -> Unit,
    filters: SectorFilters = SectorFilters.DEFAULT,
    onFiltersChange: (SectorFilters) -> Unit = {},
    filtersOpen: Boolean = false,
    onToggleFilters: () -> Unit = {},
) {
    var localFilters by remember { mutableStateOf(filters) }
    val effective = if (filters == SectorFilters.DEFAULT && localFilters != SectorFilters.DEFAULT) {
        localFilters
    } else {
        filters
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "bar") {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconTap(StockerIcons.Back, "Back to macro", onBack)
                Spacer(Modifier.weight(1f))
                IconTap(
                    StockerIcons.Filter,
                    "Filters",
                    onToggleFilters,
                    tint = if (filtersOpen) Ink.TextPrimary else Ink.TextMuted,
                )
            }
        }

        item(key = "title") {
            val sector = detail
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text(
                    "NSE · ${sector?.nUniverse ?: 0} companies (${sector?.nCurated ?: 0} curated)",
                    style = Type.MicroData.copy(fontSize = 13.sp),
                )
                Spacer(Modifier.height(8.dp))
                Text(sector?.name ?: "Sector", style = Type.Section)
                if (sector != null && sector.blurb.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(sector.blurb, style = Type.BodyTight)
                }
                if (sector != null) {
                    Spacer(Modifier.height(12.dp))
                    IndexPanel(sector)
                }
            }
        }

        item(key = "filters") {
            AnimatedVisibility(
                visible = filtersOpen,
                enter = expandVertically(tween(320, easing = Motion.Emerge)) + fadeIn(tween(220)),
                exit = shrinkVertically(tween(240, easing = Motion.Exit)) + fadeOut(tween(140)),
            ) {
                FilterPanel(
                    filters = effective,
                    count = rows.size,
                    onChange = {
                        localFilters = it
                        onFiltersChange(it)
                    },
                )
            }
        }

        if (loading) {
            items(4, key = { "sk$it" }) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SkeletonBlock(Modifier.fillMaxWidth(), height = 44.dp, corner = 10.dp)
                }
            }
        }

        item(key = "table-head") {
            if (!loading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionMicro("${rows.size} survive · ₹Cr")
                    Text(
                        "sorted by ${effective.sort.label.lowercase()}",
                        style = Type.MicroData.copy(fontSize = 13.sp),
                    )
                }
            }
        }

        items(rows, key = { it.symbol }) { row ->
            CompanyTableRow(row = row, onClick = { onOpenStock(row.symbol) })
        }

        if (!loading && rows.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.padding(20.dp)) {
                    ErrorCard(
                        title = "No companies survive these filters",
                        body = "Reset to the shipped defaults to see the Coffee-Can core again.",
                    )
                }
            }
        }
        item(key = "tail") { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun IndexPanel(sector: SectorDetail) {
    val direction = if (sector.sparkline.size >= 2) {
        directionOf((sector.sparkline.last() - sector.sparkline.first()).toDouble())
    } else {
        Direction.Flat
    }
    Box(
        Modifier
            .fillMaxWidth()
            .flat(Ink.Surface1, RoundedCornerShape(20.dp), 20.dp)
            .padding(14.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SectionMicro("Sector index · 1Y")
                Text("P/E 5Y baseline ${Fmt.ratio(sector.pe5yAvg)}", style = Type.MicroData.copy(fontSize = 13.sp))
            }
            Spacer(Modifier.height(10.dp))
            Sparkline(
                points = sector.sparkline,
                direction = direction,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                strokeWidth = 1.8.dp,
            )
        }
    }
}

/** Structure / Valuation / Growth & Size groups, moat + sort selects, and the survive counter. */
@Composable
private fun FilterPanel(filters: SectorFilters, count: Int, onChange: (SectorFilters) -> Unit) {
    val moats = remember {
        listOf("Any", "Brand Trust", "Cost Advantage", "Network Effect", "Switching Cost", "Regulatory Barrier", "Scale", "None")
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .flat(Ink.Surface2, RoundedCornerShape(20.dp), 20.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$count survive", style = Type.PanelTitle.copy(fontSize = 16.sp))
            Spacer(Modifier.weight(1f))
            Text(
                "Reset",
                style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextPrimary),
                modifier = Modifier.pressable(onClick = { onChange(SectorFilters.DEFAULT) }, pressedScale = 0.94f),
            )
        }
        Spacer(Modifier.height(6.dp))

        SectionMicro("Structure")
        ToggleRow("Coffee-Can Core", filters.core) { onChange(filters.copy(core = it)) }
        ToggleRow("Monopolies / Duopolies", filters.monopoly) { onChange(filters.copy(monopoly = it)) }
        ToggleRow("Has Cultural Moat", filters.cultural) { onChange(filters.copy(cultural = it)) }
        ToggleRow("Defensive Anchor", filters.defensive) { onChange(filters.copy(defensive = it)) }

        Spacer(Modifier.height(10.dp))
        SectionMicro("Valuation")
        ToggleRow("P/E Below Own 5Y Avg", filters.peBelowOwn) { onChange(filters.copy(peBelowOwn = it)) }
        ToggleRow("P/E Below 20", filters.peBelow20) { onChange(filters.copy(peBelow20 = it)) }

        Spacer(Modifier.height(10.dp))
        SectionMicro("Growth & Size")
        ToggleRow("Rev CAGR Above 15%", filters.cagrAbove15) { onChange(filters.copy(cagrAbove15 = it)) }
        ToggleRow("Large Cap · ₹50k Cr+", filters.largeCap) { onChange(filters.copy(largeCap = it)) }

        Spacer(Modifier.height(14.dp))
        SectionMicro("Moat type")
        Spacer(Modifier.height(4.dp))
        SegmentedControl(
            labels = listOf("Any", "Brand", "Cost", "Switch"),
            selectedIndex = when {
                filters.moat.startsWith("Brand") -> 1
                filters.moat.startsWith("Cost") -> 2
                filters.moat.startsWith("Switch") -> 3
                else -> 0
            },
            onSelect = { index ->
                val moat = when (index) {
                    1 -> "Brand Trust"
                    2 -> "Cost Advantage"
                    3 -> "Switching Cost"
                    else -> "Any"
                }
                onChange(filters.copy(moat = moat))
            },
        )

        Spacer(Modifier.height(14.dp))
        SectionMicro("Sort by")
        Spacer(Modifier.height(4.dp))
        SegmentedControl(
            labels = SortBy.entries.map { it.label },
            selectedIndex = SortBy.entries.indexOf(filters.sort),
            onSelect = { onChange(filters.copy(sort = SortBy.entries[it])) },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .pressable(onClick = { onToggle(!checked) }, pressedScale = 0.99f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = Type.BodyTight.copy(color = if (checked) Ink.TextPrimary else Ink.TextSecondary),
            modifier = Modifier.weight(1f),
        )
        SwitchMetal(checked)
    }
}

/** A metal toggle: the knob slides on a spring, the track brightens when on. */
@Composable
private fun SwitchMetal(checked: Boolean) {
    val knobX by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = if (androidx.compose.ui.platform.LocalContext.current.let { true }) {
            tween(260, easing = Motion.Fluid)
        } else {
            tween(260)
        },
        label = "toggleKnob",
    )
    val track by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) Ink.Surface3 else Ink.Surface1,
        animationSpec = tween(Motion.Fast),
        label = "toggleTrack",
    )
    Box(
        Modifier
            .size(width = 38.dp, height = 22.dp)
            .background(track, RoundedCornerShape(999.dp))
            .then(
                Modifier.background(
                    if (checked) Color.White.copy(alpha = 0.05f) else Color.Transparent,
                    RoundedCornerShape(999.dp),
                ),
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = knobX)
                .size(18.dp)
                .background(if (checked) Ink.TextPrimary else Ink.TextMuted, RoundedCornerShape(999.dp)),
        )
    }
}

/** One table row: ticker + name, market cap, P/E with its own 5Y line, CAGR, and moat pills. */
@Composable
private fun CompanyTableRow(row: CompanyRow, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, pressedScale = 0.99f)
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.symbol, style = Type.Ticker)
                    if (row.curated) {
                        Text("  CURATED", style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.TextMuted))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(row.name, style = Type.Caption, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Fmt.crore(row.mcapCr), style = Type.Data)

            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    "P/E ${Fmt.ratio(row.pe)}",
                    style = Type.DataSmall.copy(
                        color = if (row.peBelowOwnAvg) Ink.UpText else Ink.TextSecondary,
                    ),
                )
                Text("5Y ${Fmt.ratio(row.pe5yOwn)}", style = Type.MicroData.copy(fontSize = 11.5.sp))
            }
            Column {
                Text("CAGR", style = Type.MicroData.copy(fontSize = 11.5.sp))
                ChangeText(row.revCagr5y, style = Type.DataSmall, showGlyph = false)
            }
            if (row.moat != "None") {
                Text(
                    row.moat,
                    style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.TextSecondary),
                    modifier = Modifier
                        .flat(Ink.Surface2, RoundedCornerShape(999.dp), 999.dp)
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            if (row.monopoly || row.cultural || row.defensive) {
                Text(
                    buildString {
                        if (row.monopoly) append("MONO ")
                        if (row.cultural) append("CULT ")
                        if (row.defensive) append("DEF")
                    }.trim(),
                    style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.TextMuted),
                )
            }
        }
        row.note.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = Type.Caption.copy(fontSize = 13.sp, color = Ink.TextMuted))
        }
    }
}

package com.dotpmm.stocker.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.research.ResearchRepository
import com.dotpmm.stocker.research.SearchResult
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.SearchField
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import kotlinx.coroutines.launch
import androidx.compose.foundation.background

/**
 * Discovery beyond the macro flow: browse the curated constituents by sector and search the full
 * Nifty-200 universe. Research happens in place — there is no watchlist anywhere (design.md §12
 * rule 17), so this is purely a launch pad into the deep-dive.
 */
@Composable
fun NewsScreen(contentPadding: PaddingValues, onOpenStock: (String) -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { ResearchRepository(context) }
    val curated = remember(repository) { repository.data() }
    val universe = remember(repository) { repository.universeStocks() }

    ResearchBrowseContent(
        sectors = curated.sectors,
        universeCount = universe.size,
        contentPadding = contentPadding,
        onOpenStock = onOpenStock,
        search = { q -> runCatching { repository.search(q) }.getOrDefault(emptyList()) },
    )
}

/**
 * Stateless browse body: a search field over the full universe plus the curated sectors as
 * expandable plates. Research happens in place — every row opens the deep-dive.
 */
@Composable
fun ResearchBrowseContent(
    sectors: List<com.dotpmm.stocker.research.CuratedSector>,
    universeCount: Int,
    contentPadding: PaddingValues,
    onOpenStock: (String) -> Unit,
    search: suspend (String) -> List<SearchResult> = { emptyList() },
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<SearchResult>()) }
    var expandedSector by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(180)
        results = search(query)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Research", style = Type.Section)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Any of the $universeCount Nifty-200 names is deep-diveable — curated or not.",
                    style = Type.Caption,
                )
                Spacer(Modifier.height(14.dp))
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { query = "" },
                    placeholder = "Search ticker or company…",
                    results = results,
                    onPick = { onOpenStock(it.symbol) },
                    showResults = query.trim().length >= 2,
                    onSubmit = { results.firstOrNull()?.let { onOpenStock(it.symbol) } },
                )
            }
        }

        if (query.trim().length >= 2) {
            item(key = "results-label") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    SectionMicro(if (results.isEmpty()) "No matches" else "${results.size} results")
                    if (results.isNotEmpty()) Text("tap to open", style = Type.MicroData.copy(fontSize = 13.sp))
                }
            }
            items(results, key = { "r${it.symbol}" }) { hit ->
                ResultRow(hit = hit, onClick = { onOpenStock(hit.symbol) })
            }
        } else {
            item(key = "curated-label") {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    SectionMicro("Curated sectors")
                }
            }
            items(sectors, key = { "s${it.id}" }) { sector ->
                val open = expandedSector == sector.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 5.dp)
                        .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressable(
                                onClick = { expandedSector = if (open) null else sector.id },
                                pressedScale = 0.99f,
                            )
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(sector.name, style = Type.PanelTitle.copy(fontSize = 16.sp))
                            Text(
                                "${sector.companies.size} curated · P/E 5Y ${Fmt.ratio(sector.pe5yAvg)}",
                                style = Type.MicroData.copy(fontSize = 13.sp),
                            )
                        }
                        Text(if (open) "−" else "+", style = Type.Data.copy(color = Ink.TextMuted))
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = open) {
                        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
                            sector.companies.forEach { company ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .pressable(onClick = { onOpenStock(company.symbol) }, pressedScale = 0.99f)
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(company.symbol, style = Type.Ticker, modifier = Modifier.width(108.dp))
                                    Text(
                                        company.name,
                                        style = Type.Caption,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (company.isCore) {
                                        Box(Modifier.size(4.dp).backgroundDot())
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "tail") { Spacer(Modifier.height(10.dp)) }
    }
}
private fun Modifier.backgroundDot(): Modifier = this.background(Ink.Up)

@Composable
private fun ResultRow(hit: SearchResult, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, pressedScale = 0.99f)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(hit.symbol, style = Type.Ticker, modifier = Modifier.width(108.dp))
        Column(Modifier.weight(1f)) {
            Text(hit.name, style = Type.BodyTight.copy(color = Ink.TextPrimary), maxLines = 1)
            Text(hit.sector, style = Type.MicroData.copy(fontSize = 13.sp), maxLines = 1)
        }
    }
}

/**
 * Settings/handoff screen: what this surface does, the design contract it follows, and the
 * remaining Stocker product notes. Nothing here is a watchlist control.
 */
@Composable
fun ResearchSettingsScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var clearing by remember { mutableStateOf(false) }
    var cleared by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("About", style = Type.Section)
                Spacer(Modifier.height(4.dp))
                Text("Monochrome Exchange · metal + blur", style = Type.Caption)
            }
        }

        item(key = "what") {
            Panel(title = "What this is") {
                Text(
                    "A macro-first Indian equities research surface. The economic cycle comes first, stocks last; " +
                        "the valuation gauge and the 5Y revenue CAGR are the noise filter. Research happens in " +
                        "place — there is no watchlist, no collection, no save-to-watch surface.",
                    style = Type.BodyTight,
                )
            }
        }

        item(key = "flow") {
            Panel(title = "How to read it") {
                Text(
                    "Markets shows the NIFTY hero and six macro sectors, each judged by its market-cap-weighted " +
                        "P/E against a 5Y baseline. Tap a sector for every mapped stock in it, filtered by the " +
                        "Coffee-Can toggles. Tap any row for the deep-dive: verdict, gauge, fundamentals, and a " +
                        "noise-filter chart with earnings dots.",
                    style = Type.BodyTight,
                )
            }
        }

        item(key = "design") {
            Panel(title = "Design contract") {
                Text(
                    "One 118° beam from the top-left. Every raised surface is a top-lit metal plate with a bright " +
                        "upper bevel. Red and green appear only for market direction, always with a ▲/▼ glyph. " +
                        "Prices and tickers are IBM Plex Mono with tabular numerals. Spinners are banned — loading " +
                        "sweeps and orbiters instead. Reduced motion is fully honoured.",
                    style = Type.BodyTight,
                )
            }
        }

        item(key = "data") {
            Panel(title = "Data") {
                Text(
                    "The curated layer (sectors, moats, narratives, dependency weights) ships with the app. " +
                        "Quotes, fundamentals and price history come from Yahoo Finance's public endpoints, cached " +
                        "with the documented TTLs and served stale through outages so the screen never goes blank.",
                    style = Type.BodyTight,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Sector P/E is a market-cap-weighted trailing P/E of the curated constituents. Own 5Y P/E is " +
                        "an approximation from four fiscal years of EPS against fiscal-year-end prices.",
                    style = Type.Caption,
                )
            }
        }

        item(key = "cache") {
            Panel(title = "Stored on this device") {
                Text(
                    "A research TTL cache. No account, no tracking, nothing uploaded.",
                    style = Type.BodyTight,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .pressable(
                            onClick = {
                                if (clearing) {
                                    com.dotpmm.stocker.data.db.StockerDb(context).clearResearchCache()
                                    cleared = true
                                    clearing = false
                                } else {
                                    clearing = true
                                }
                            },
                            pressedScale = 0.97f,
                        )
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        when {
                            cleared -> "Cache cleared"
                            clearing -> "Tap again to clear cached research"
                            else -> "Clear cached research"
                        },
                        style = Type.BodyStrong.copy(
                            color = if (clearing) Ink.DownText else Ink.TextSecondary,
                        ),
                    )
                }
            }
        }

        item(key = "disclaimer") {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    "Information only, never financial advice. Nothing here is an offer or a recommendation " +
                        "to buy or sell any security. Verdicts are mechanical thresholds, not opinions.",
                    style = Type.Caption.copy(fontSize = 13.sp),
                )
            }
        }
        item(key = "tail") { Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
private fun Panel(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
            .padding(16.dp),
    ) {
        SectionMicro(title)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

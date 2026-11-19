package com.dotpmm.stocker.ui.screens

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.data.db.StockerDb
import com.dotpmm.stocker.data.net.Sources
import com.dotpmm.stocker.domain.model.SourceHealth
import com.dotpmm.stocker.settings.AccentIntensity
import com.dotpmm.stocker.settings.AppSettings
import com.dotpmm.stocker.settings.BaseTone
import com.dotpmm.stocker.settings.Density
import com.dotpmm.stocker.settings.GrainLevel
import com.dotpmm.stocker.settings.MetalFinish
import com.dotpmm.stocker.settings.MotionLevel
import com.dotpmm.stocker.settings.SettingsStore
import com.dotpmm.stocker.settings.ShapeLanguage
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.SegmentedControl
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.components.sweepShimmer
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import kotlinx.coroutines.launch

/**
 * Settings — every appearance and behaviour knob in one place, all live.
 *
 * Nothing here is decorative: each control writes to [SettingsStore] and the theme or a screen
 * reads it back immediately. Grouped so the screen stays scannable despite its length.
 */
@Composable
fun SettingsScreen(settingsStore: SettingsStore, contentPadding: PaddingValues) {
    val settings = settingsStore.settings
    val store = StockerApp.instance.store
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val refresh by store.refresh.collectAsState()
    val lastRefresh by store.lastRefresh.collectAsState()
    val health by rememberQuery(emptyList<SourceHealth>()) { store.health() }
    var showAllSources by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }

    fun update(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
        savedFlash = true
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Settings", style = Type.Section)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Appearance and behaviour · everything on this phone",
                    style = Type.Caption,
                )
            }
        }

        // ── Appearance ─────────────────────────────────────────────
        settingsSection("Appearance") {
            ChoiceRow(
                label = "Base tone",
                hint = "AMOLED is darkest that still reads as metal.",
                options = BaseTone.entries.map { it.label },
                selected = BaseTone.entries.indexOf(settings.baseTone),
                onSelect = { update { s -> s.copy(baseTone = BaseTone.entries[it]) } },
            )
            ChoiceRow(
                label = "Metal finish",
                hint = "Strength of the lit edge and sheen.",
                options = MetalFinish.entries.map { it.label },
                selected = MetalFinish.entries.indexOf(settings.metalFinish),
                onSelect = { update { s -> s.copy(metalFinish = MetalFinish.entries[it]) } },
            )
            ChoiceRow(
                label = "Grain",
                hint = "Grain over gradients, prevents banding.",
                options = GrainLevel.entries.map { it.label },
                selected = GrainLevel.entries.indexOf(settings.grain),
                onSelect = { update { s -> s.copy(grain = GrainLevel.entries[it]) } },
            )
            ChoiceRow(
                label = "Direction accent",
                hint = "Brightness of the gain/loss pair.",
                options = AccentIntensity.entries.map { it.label },
                selected = AccentIntensity.entries.indexOf(settings.accentIntensity),
                onSelect = { update { s -> s.copy(accentIntensity = AccentIntensity.entries[it]) } },
            )
            ChoiceRow(
                label = "Shape language",
                hint = "Corner radius across the app.",
                options = ShapeLanguage.entries.map { it.label },
                selected = ShapeLanguage.entries.indexOf(settings.shape),
                onSelect = { update { s -> s.copy(shape = ShapeLanguage.entries[it]) } },
            )
            ChoiceRow(
                label = "Density",
                hint = "Row height and padding.",
                options = Density.entries.map { it.label },
                selected = Density.entries.indexOf(settings.density),
                onSelect = { update { s -> s.copy(density = Density.entries[it]) } },
            )
            ChoiceRow(
                label = "Motion",
                hint = "Full, reduced, or off.",
                options = MotionLevel.entries.map { it.label },
                selected = MotionLevel.entries.indexOf(settings.motionLevel),
                onSelect = { update { s -> s.copy(motionLevel = MotionLevel.entries[it]) } },
            )
            ToggleRow("Ambient glow", "Slow beam behind the hero.", settings.ambientGlow) {
                update { s -> s.copy(ambientGlow = it) }
            }
            ToggleRow("Border light traveller", "Streak along the hero edge.", settings.borderTraveler) {
                update { s -> s.copy(borderTraveler = it) }
            }
            ToggleRow("Search burst", "Dot aura on search focus.", settings.searchBurst) {
                update { s -> s.copy(searchBurst = it) }
            }
            ToggleRow("Haptics", "Short tick on press.", settings.haptics) {
                update { s -> s.copy(haptics = it) }
            }
            ToggleRow(
                "Direction glyphs",
                "Show ▲/▼ with percentages.",
                settings.directionGlyphs,
            ) { update { s -> s.copy(directionGlyphs = it) } }
            ToggleRow(
                "Swipe between tabs",
                "Drag sideways to change tab.",
                settings.swipeTabs,
            ) { update { s -> s.copy(swipeTabs = it) } }
            ToggleRow(
                "Smart start",
                "Empty watchlist opens Markets.",
                settings.smartStart,
            ) { update { s -> s.copy(smartStart = it) } }
        }

        // ── Markets screen ─────────────────────────────────────────
        settingsSection("Markets screen") {
            ChoiceRow(
                label = "Macro auto-refresh",
                hint = "Markets reload interval.",
                options = listOf("5 min", "10 min", "20 min", "30 min"),
                selected = when (settings.macroRefreshMinutes) {
                    5 -> 0
                    10 -> 1
                    20 -> 2
                    else -> 3
                },
                onSelect = {
                    update { s ->
                        s.copy(macroRefreshMinutes = listOf(5, 10, 20, 30)[it])
                    }
                },
            )
            ToggleRow(
                "Include universe peers",
                "Include all mapped Nifty-200 names.",
                settings.includeUniversePeers,
            ) { update { s -> s.copy(includeUniversePeers = it) } }
            ToggleRow(
                "Live search",
                "Search beyond the shipped list.",
                settings.liveSearch,
            ) { update { s -> s.copy(liveSearch = it) } }
        }

        // ── Charts ─────────────────────────────────────────────────
        settingsSection("Charts") {
            ChoiceRow(
                label = "Default range",
                hint = "Opening chart range.",
                options = listOf("1D", "1W", "1M", "3M", "1Y"),
                selected = settings.defaultRangeIndex,
                onSelect = { update { s -> s.copy(defaultRangeIndex = it) } },
            )
            ToggleRow("Under-curve fill", "Fill below the price line.", settings.chartFill) {
                update { s -> s.copy(chartFill = it) }
            }
            ToggleRow("Line underglow", "Light cast by the line.", settings.chartGlow) {
                update { s -> s.copy(chartGlow = it) }
            }
            ToggleRow(
                "Earnings dots",
                "Mark earnings dates.",
                settings.earningsDots,
            ) { update { s -> s.copy(earningsDots = it) } }
            ToggleRow(
                "52-week range",
                "52-week high/low line.",
                settings.show52Week,
            ) { update { s -> s.copy(show52Week = it) } }
        }

        // ── News ───────────────────────────────────────────────────
        settingsSection("News") {
            ChoiceRow(
                label = "Story window",
                hint = "Hide older stories.",
                options = listOf("3 days", "7 days", "14 days", "30 days"),
                selected = when (settings.newsWindowDays) {
                    3 -> 0
                    7 -> 1
                    14 -> 2
                    else -> 3
                },
                onSelect = {
                    update { s -> s.copy(newsWindowDays = listOf(3, 7, 14, 30)[it]) }
                },
            )
            ChoiceRow(
                label = "Stories per company",
                hint = "Headlines per stock.",
                options = listOf("30", "60", "100", "150"),
                selected = when (settings.newsPerCompany) {
                    30 -> 0
                    60 -> 1
                    100 -> 2
                    else -> 3
                },
                onSelect = {
                    update { s -> s.copy(newsPerCompany = listOf(30, 60, 100, 150)[it]) }
                },
            )
            ToggleRow(
                "Cluster duplicates",
                "Merge the same story across outlets.",
                settings.clusterDuplicates,
            ) { update { s -> s.copy(clusterDuplicates = it) } }
            ToggleRow("Tone markers", "Show tone markers.", settings.showTone) {
                update { s -> s.copy(showTone = it) }
            }
            ToggleRow("Event tags", "Show event tags.", settings.showEventTags) {
                update { s -> s.copy(showEventTags = it) }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${settings.newsSources.size} of ${Sources.generic.size} sources on",
                    style = Type.MicroData.copy(fontSize = 13.sp),
                )
                Text(
                    if (showAllSources) "HIDE LIST" else "CHOOSE SOURCES",
                    style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextPrimary),
                    modifier = Modifier.pressable(
                        onClick = { showAllSources = !showAllSources },
                        pressedScale = 0.96f,
                    ).padding(vertical = 4.dp),
                )
            }
        }

        if (showAllSources) {
            item(key = "sources-head") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text("Sources", style = Type.Micro.copy(fontSize = 13.sp))
                }
            }
            items(Sources.generic, key = { "src-${it.id}" }) { source ->
                val on = source.id in settings.newsSources
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                        .flat(Ink.Surface1, RoundedCornerShape(12.dp), 12.dp)
                        .pressable(
                            onClick = {
                                update { s ->
                                    s.copy(
                                        newsSources = if (on) s.newsSources - source.id
                                        else s.newsSources + source.id,
                                    )
                                }
                            },
                            pressedScale = 0.99f,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            source.name,
                            style = Type.BodyTight.copy(
                                color = if (on) Ink.TextPrimary else Ink.TextMuted,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            "${source.market.name} · credibility ${(source.credibility * 100).toInt()}%",
                            style = Type.MicroData.copy(fontSize = 11.5.sp),
                        )
                    }
                    CheckMark(on)
                }
            }
        }

        // ── Refresh ────────────────────────────────────────────────
        settingsSection("Refresh") {
            ToggleRow(
                "Background refresh",
                "Refresh while closed.",
                settings.backgroundRefresh,
            ) { update { s -> s.copy(backgroundRefresh = it) } }
            ChoiceRow(
                label = "Refresh interval",
                hint = "Background interval.",
                options = listOf("1 h", "3 h", "6 h", "12 h", "24 h"),
                selected = when (settings.refreshHours) {
                    1 -> 0
                    3 -> 1
                    6 -> 2
                    12 -> 3
                    else -> 4
                },
                onSelect = {
                    update { s -> s.copy(refreshHours = listOf(1, 3, 6, 12, 24)[it]) }
                },
            )
            ToggleRow(
                "Long cache",
                "Cache longer for instant revisits.",
                settings.longCache,
            ) { update { s -> s.copy(longCache = it) } }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (refresh.running) "Refreshing · ${refresh.message}" else Fmt.updated(lastRefresh),
                    style = Type.MicroData.copy(fontSize = 13.sp),
                )
                ActionPill(
                    label = if (refresh.running) "Refreshing" else "Refresh now",
                    onClick = { store.launchRefresh() },
                    leading = StockerIcons.Refresh,
                )
            }
        }

        // ── Watchlist ──────────────────────────────────────────────
        settingsSection("Watchlist") {
            ToggleRow("Compact rows", "Tighter rows.", settings.compactRows) {
                update { s -> s.copy(compactRows = it) }
            }
            ToggleRow(
                "Sort alphabetically",
                "Sort by company name.",
                settings.sortWatchlistAlphabetically,
            ) { update { s -> s.copy(sortWatchlistAlphabetically = it) } }
        }

        // ── AI ─────────────────────────────────────────────────────
        item(key = "ai") { AiSection(settingsStore) }

        // ── Source health ──────────────────────────────────────────
        item(key = "health-head") {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 6.dp)) {
                Text("Source health", style = Type.Micro.copy(fontSize = 13.sp))
                Spacer(Modifier.height(4.dp))
                Text(
                    "Checked on every refresh. A red square means that feed failed last time.",
                    style = Type.Caption,
                )
            }
        }
        if (health.isEmpty()) {
            item(key = "health-empty") {
                Text(
                    "Nothing checked yet. Add a stock and refresh.",
                    style = Type.Caption,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        items(health, key = { it.id }) { HealthRow(it) }

        // ── Data ───────────────────────────────────────────────────
        item(key = "data") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
                Text(
                    "Data",
                    style = Type.Micro.copy(fontSize = 13.sp),
                    modifier = Modifier.padding(top = 26.dp, bottom = 8.dp),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .flat(Ink.Surface1, RoundedCornerShape(16.dp), 16.dp)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        if (savedFlash) "Settings saved" else "Changes apply immediately",
                        style = Type.MicroData.copy(fontSize = 11.5.sp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            "CLEAR RESEARCH CACHE",
                            style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextPrimary),
                            modifier = Modifier.pressable(
                                onClick = {
                                    scope.launch {
                                        StockerDb(context).clearResearchCache()
                                    }
                                },
                                pressedScale = 0.96f,
                            ).padding(vertical = 4.dp),
                        )
                        Text(
                            "CLEAR NEWS",
                            style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextMuted),
                            modifier = Modifier.pressable(
                                onClick = { scope.launch { store.clearNews() } },
                                pressedScale = 0.96f,
                            ).padding(vertical = 4.dp),
                        )
                    }
                    Text(
                        if (confirmReset) "TAP AGAIN TO RESET EVERY SETTING" else "RESET ALL SETTINGS",
                        style = Type.MicroData.copy(
                            fontSize = 13.sp,
                            color = if (confirmReset) Ink.DownText else Ink.TextMuted,
                        ),
                        modifier = Modifier.pressable(
                            onClick = {
                                if (confirmReset) {
                                    settingsStore.reset()
                                    confirmReset = false
                                } else {
                                    confirmReset = true
                                }
                            },
                            pressedScale = 0.96f,
                        ).padding(vertical = 4.dp),
                    )
                }
            }
        }

        item(key = "about") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    "Information only, never financial advice. Sentiment tags and verdicts are " +
                        "produced by simple rules and can be wrong. Headlines belong to their publishers.",
                    style = Type.MicroData.copy(fontSize = 11.5.sp),
                )
            }
        }
        item(key = "tail") { Spacer(Modifier.height(12.dp)) }
    }
}

// ── Building blocks ──────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.settingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    item(key = "sec-$title") {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 5.dp),
        ) {
            Text(
                title.uppercase(),
                style = Type.Micro.copy(fontSize = 13.sp),
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .metal(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                content()
            }
        }
    }
}

/** A labelled option set rendered as a segmented control. */
@Composable
private fun ChoiceRow(
    label: String,
    hint: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = Type.BodyTight.copy(color = Ink.TextPrimary))
        Text(hint, style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextSecondary))
        SegmentedControl(
            labels = options.map { it.uppercase() },
            selectedIndex = selected.coerceIn(0, options.lastIndex),
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ToggleRow(label: String, hint: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .pressable(onClick = { onToggle(!checked) }, pressedScale = 0.99f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = Type.BodyTight.copy(color = if (checked) Ink.TextPrimary else Ink.TextSecondary),
            )
            Text(hint, style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextSecondary))
        }
        Spacer(Modifier.width(12.dp))
        Toggle(checked)
    }
}

/** A metal toggle: the knob slides on a spring, the track brightens when on. */
@Composable
private fun Toggle(checked: Boolean) {
    val knobX by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 17.dp else 2.dp,
        animationSpec = androidx.compose.animation.core.tween(260, easing = com.dotpmm.stocker.ui.theme.Motion.Fluid),
        label = "toggleKnob",
    )
    val track by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) Ink.Surface3 else Ink.Surface2,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "toggleTrack",
    )
    Box(
        Modifier
            .size(width = 36.dp, height = 21.dp)
            .background(track, RoundedCornerShape(999.dp)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = knobX)
                .size(17.dp)
                .background(if (checked) Ink.TextPrimary else Ink.TextMuted, RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun CheckMark(on: Boolean) {
    Box(
        Modifier
            .size(18.dp)
            .background(if (on) Ink.Surface3 else Ink.Surface2, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (on) Text("✓", style = Type.MicroData.copy(fontSize = 12.5.sp, color = Ink.TextPrimary))
    }
}

@Composable
private fun AiSection(settingsStore: SettingsStore) {
    val store = StockerApp.instance.store
    var key by remember { mutableStateOf(store.prefs.geminiKey) }
    var model by remember { mutableStateOf(store.prefs.geminiModel) }
    var reveal by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
        Text(
            "GEMINI · OPTIONAL AI SUMMARY",
            style = Type.Micro.copy(fontSize = 13.sp),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .flat(Ink.Surface1, RoundedCornerShape(18.dp), 18.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Paste a free API key from aistudio.google.com. It is stored only on this device and " +
                    "used only when you tap AI summary.",
                style = Type.MicroData.copy(fontSize = 11.5.sp),
            )
            SettingsField(
                value = key,
                onValueChange = { key = it; saved = false },
                placeholder = "Gemini API key",
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
            )
            SettingsField(
                value = model,
                onValueChange = { model = it; saved = false },
                placeholder = "Model (blank = newest Flash)",
                keyboardType = KeyboardType.Text,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SAVE",
                    style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.TextPrimary),
                    modifier = Modifier.pressable(
                        onClick = {
                            store.prefs.geminiKey = key
                            store.prefs.geminiModel = model
                            settingsStore.update { s -> s.copy(aiModel = model) }
                            saved = true
                        },
                        pressedScale = 0.96f,
                    ).padding(vertical = 4.dp),
                )
                Text(
                    if (reveal) "HIDE" else "SHOW",
                    style = Type.MicroData.copy(fontSize = 13.sp),
                    modifier = Modifier.pressable(
                        onClick = { reveal = !reveal },
                        pressedScale = 0.96f,
                    ).padding(vertical = 4.dp),
                )
                Text(
                    "CLEAR",
                    style = Type.MicroData.copy(fontSize = 13.sp),
                    modifier = Modifier.pressable(
                        onClick = {
                            key = ""
                            model = ""
                            store.prefs.geminiKey = ""
                            store.prefs.geminiModel = ""
                            settingsStore.update { s -> s.copy(aiModel = "") }
                            saved = false
                        },
                        pressedScale = 0.96f,
                    ).padding(vertical = 4.dp),
                )
                if (saved) Text("Saved", style = Type.MicroData.copy(fontSize = 13.sp, color = Ink.UpText))
            }
        }
    }
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .flat(Ink.Surface2, RoundedCornerShape(12.dp), 12.dp)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = Type.BodyTight.copy(color = Ink.TextMuted))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = Type.BodyTight.copy(color = Ink.TextPrimary),
            cursorBrush = SolidColor(Ink.TextPrimary),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HealthRow(h: SourceHealth) {
    val color = when {
        h.lastError != null -> Ink.Down
        h.lastSuccess > 0 -> Ink.Up
        else -> Ink.TextMuted
    }
    val detail = when {
        h.lastError != null && h.lastSuccess > 0 ->
            "Failed ${Fmt.age(h.lastAttempt)} ago (${h.lastError}) · last worked ${Fmt.age(h.lastSuccess)} ago"
        h.lastError != null -> "Failed ${Fmt.age(h.lastAttempt)} ago (${h.lastError})"
        else -> "${h.items} items · ${Fmt.age(h.lastSuccess)} ago"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 5.dp).size(5.dp).background(color))
        Column(Modifier.weight(1f)) {
            Text(h.name, style = Type.BodyTight.copy(color = Ink.TextPrimary))
            Text(detail, style = Type.MicroData.copy(fontSize = 11.5.sp))
        }
    }
}

/**
 * A render-only preview of the settings screen with representative controls. Used by the visual
 * harness so the settings layout can be inspected without a device.
 */
@Composable
fun SettingsPreviewContent() {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Settings", style = Type.Section)
                Spacer(Modifier.height(3.dp))
                Text("Appearance and behaviour · everything on this phone", style = Type.Caption)
            }
        }
        settingsSection("Appearance") {
            ChoiceRow(
                label = "Base tone",
                hint = "AMOLED is darkest that still reads as metal.",
                options = BaseTone.entries.map { it.label },
                selected = 0,
                onSelect = {},
            )
            ChoiceRow(
                label = "Metal finish",
                hint = "Strength of the lit edge and sheen.",
                options = MetalFinish.entries.map { it.label },
                selected = 2,
                onSelect = {},
            )
            ChoiceRow(
                label = "Density",
                hint = "Row height and padding.",
                options = Density.entries.map { it.label },
                selected = 1,
                onSelect = {},
            )
            ToggleRow("Ambient glow", "Slow beam behind the hero.", true) {}
            ToggleRow("Haptics", "Short tick on press.", true) {}
            ToggleRow(
                "Direction glyphs",
                "Show ▲/▼ with percentages.",
                true,
            ) {}
        }
        settingsSection("Charts") {
            ToggleRow("Under-curve fill", "Fill below the price line.", true) {}
            ToggleRow("Line underglow", "Light cast by the line.", true) {}
        }
    }
}

package com.dotpmm.stocker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.ui.components.FloatingNavBar
import com.dotpmm.stocker.ui.components.NavItem
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.screens.AddStockScreen
import com.dotpmm.stocker.ui.screens.AiSummaryScreen
import com.dotpmm.stocker.ui.screens.EventsTab
import com.dotpmm.stocker.ui.screens.MacroScreen
import com.dotpmm.stocker.ui.screens.NewsTab
import com.dotpmm.stocker.ui.screens.ReaderScreen
import com.dotpmm.stocker.ui.screens.SectorScreen
import com.dotpmm.stocker.ui.screens.SettingsScreen
import com.dotpmm.stocker.ui.screens.StockDeepSheet
import com.dotpmm.stocker.ui.screens.StockDetailScreen
import com.dotpmm.stocker.ui.screens.WatchlistTab
import com.dotpmm.stocker.ui.theme.LocalReducedMotion
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.StockerTheme

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Watchlist("Watchlist", StockerIcons.Watchlist),
    Markets("Markets", StockerIcons.Compass),
    News("News", StockerIcons.News),
    Events("Events", StockerIcons.Events),
    Settings("Settings", StockerIcons.Settings),
}

/**
 * App shell. Five tabs behind the floating nav, plus a back stack for the detail, reader and AI
 * screens kept as a string so it survives rotation and process death.
 *
 * On first launch an empty watchlist opens Markets instead, because a blank list is a worse first
 * screen than a populated market view. Once the user adds anything, Watchlist becomes the start.
 *
 * Route grammar: `d:TICKER` stock detail, `r:ID` reader, `a:TICKER` AI summary, `add` add-stock.
 */
@Composable
fun StockerRoot() {
    val settingsStore = remember { StockerApp.instance.settings }
    val settings by androidx.compose.runtime.derivedStateOf { settingsStore.settings }
    val store = remember { StockerApp.instance.store }
    val watchlist by store.watchlist.collectAsState()

    // Smart start: choose the opening tab once, after the watchlist has loaded.
    var startResolved by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(Tab.Watchlist.ordinal) }
    if (!startResolved) {
        // isLoaded is true once prefs have been read, so this only runs against real data.
        if (settings.smartStart && watchlist.isEmpty()) {
            tab = Tab.Markets.ordinal
        }
        startResolved = true
    }
    var stackRaw by rememberSaveable { mutableStateOf("") }
    var sectorId by rememberSaveable { mutableStateOf<String?>(null) }
    var deepSymbol by rememberSaveable { mutableStateOf<String?>(null) }

    val stack = if (stackRaw.isEmpty()) emptyList() else stackRaw.split("|")
    val top: String? = stack.lastOrNull()

    fun push(route: String) {
        stackRaw = if (stackRaw.isEmpty()) route else "$stackRaw|$route"
    }

    fun pop() {
        val parts = if (stackRaw.isEmpty()) emptyList() else stackRaw.split("|")
        stackRaw = parts.dropLast(1).joinToString("|")
    }

    val reduced = LocalReducedMotion.current
    val navItems = remember { Tab.entries.map { NavItem(it.label, it.icon) } }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = 104.dp + bottomInset)
    val showNav = top == null && sectorId == null && deepSymbol == null
    val tabsSwipable = showNav && settings.swipeTabs

    BackHandler(enabled = deepSymbol != null) { deepSymbol = null }
    BackHandler(enabled = deepSymbol == null && sectorId != null) { sectorId = null }
    BackHandler(enabled = deepSymbol == null && sectorId == null && top != null) { pop() }

    StockerTheme(settings = settings) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            AnimatedContent(
                targetState = Triple(top, sectorId, tab),
                transitionSpec = {
                    val (toRoute, toSector, toTab) = targetState
                    val (fromRoute, fromSector, fromTab) = initialState
                    val enteringSector = toSector != null && fromSector == null
                    val leavingSector = toSector == null && fromSector != null
                    val enteringRoute = toRoute != null && fromRoute == null
                    val leavingRoute = toRoute == null && fromRoute != null
                    val duration = if (reduced) 140 else Motion.Base
                    when {
                        enteringRoute || leavingRoute ->
                            (fadeIn(tween(duration, easing = Motion.Emerge)) +
                                slideInVertically(tween(duration, easing = Motion.Fluid)) { it / 12 }) togetherWith
                                fadeOut(tween(duration / 2, easing = Motion.Exit))
                        enteringSector ->
                            (fadeIn(tween(duration, easing = Motion.Emerge)) +
                                slideInHorizontally(tween(duration, easing = Motion.Fluid)) { it / 6 }) togetherWith
                                fadeOut(tween(duration / 2, easing = Motion.Exit))
                        leavingSector ->
                            fadeIn(tween(duration, easing = Motion.Emerge)) togetherWith
                                (fadeOut(tween(duration, easing = Motion.Exit)) +
                                    slideOutHorizontally(tween(duration, easing = Motion.Exit)) { it / 6 })
                        // Tab changes slide with the direction of travel, so a swipe feels causal.
                        else -> {
                            val forward = toTab > fromTab
                            (fadeIn(tween(duration, easing = Motion.Fluid)) +
                                slideInHorizontally(tween(duration, easing = Motion.Fluid)) {
                                    if (forward) it / 5 else -it / 5
                                }) togetherWith
                                (fadeOut(tween(duration / 2, easing = Motion.Exit)) +
                                    slideOutHorizontally(tween(duration, easing = Motion.Exit)) {
                                        if (forward) -it / 5 else it / 5
                                    })
                        }
                    }
                },
                label = "screens",
            ) { target ->
                val route = target.first
                val currentSector = target.second
                val currentTab = target.third
                when {
                    route == "add" -> AddStockScreen(onBack = { pop() })
                    route != null && route.startsWith("d:") -> {
                        val ticker = route.removePrefix("d:")
                        val stock = watchlist.firstOrNull { it.ticker == ticker }
                        if (stock != null) {
                            StockDetailScreen(
                                stock = stock,
                                onBack = { pop() },
                                onOpenArticle = { push("r:$it") },
                                onAiSummary = { push("a:$ticker") },
                                onRemoved = { pop() },
                                contentPadding = contentPadding,
                            )
                        }
                    }
                    route != null && route.startsWith("a:") -> {
                        val ticker = route.removePrefix("a:")
                        val stock = watchlist.firstOrNull { it.ticker == ticker }
                        if (stock != null) {
                            AiSummaryScreen(
                                stock = stock,
                                onBack = { pop() },
                                onOpenSettings = {
                                    stackRaw = ""
                                    tab = Tab.Settings.ordinal
                                },
                                onOpenArticle = { push("r:$it") },
                            )
                        }
                    }
                    route != null && route.startsWith("r:") -> ReaderScreen(
                        articleId = route.removePrefix("r:"),
                        onBack = { pop() },
                        onOpenArticle = { push("r:$it") },
                    )
                    currentSector != null -> SectorScreen(
                        sectorId = currentSector,
                        contentPadding = contentPadding,
                        onBack = { sectorId = null },
                        onOpenStock = { deepSymbol = it },
                    )
                    else -> {
                        // Swipe lives on the tab container so it never fights a list's own
                        // horizontal gestures: it only commits on a decisive, mostly-horizontal fling.
                        val swipe = if (tabsSwipable) {
                            Modifier.tabSwipe(
                                onSwipeLeft = {
                                    if (tab < Tab.entries.lastIndex) tab += 1
                                },
                                onSwipeRight = {
                                    if (tab > 0) tab -= 1
                                },
                            )
                        } else {
                            Modifier
                        }
                        Box(Modifier.fillMaxSize().then(swipe)) {
                            when (Tab.entries[currentTab]) {
                                Tab.Watchlist -> WatchlistTab(
                                    onOpenStock = { push("d:$it") },
                                    onAddStock = { push("add") },
                                    contentPadding = contentPadding,
                                    onBrowseMarkets = { tab = Tab.Markets.ordinal },
                                )
                                Tab.Markets -> MacroScreen(
                                    contentPadding = contentPadding,
                                    onOpenSector = { sectorId = it },
                                    onOpenStock = { deepSymbol = it },
                                )
                                Tab.News -> NewsTab(onOpenArticle = { push("r:$it") }, contentPadding = contentPadding)
                                Tab.Events -> EventsTab(onOpenArticle = { push("r:$it") }, contentPadding = contentPadding)
                                Tab.Settings -> SettingsScreen(
                                    settingsStore = settingsStore,
                                    contentPadding = contentPadding,
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showNav,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
                enter = slideInVertically(tween(if (reduced) 140 else 420, easing = Motion.Emerge)) { it } +
                    fadeIn(tween(240)),
                exit = slideOutVertically(tween(220, easing = Motion.Exit)) { it / 2 } + fadeOut(tween(160)),
            ) {
                FloatingNavBar(
                    items = navItems,
                    selectedIndex = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            deepSymbol?.let { symbol ->
                StockDeepSheet(
                    symbol = symbol,
                    onClose = { deepSymbol = null },
                    onOpenSector = { id ->
                        deepSymbol = null
                        sectorId = id
                    },
                )
            }
        }
    }
}

/**
 * A horizontal fling that switches tabs.
 *
 * The threshold is deliberately high (a quarter of the width, and mostly horizontal) so scrolling a
 * list or dragging a chart never changes tab by accident. Vertical drags are ignored outright.
 */
private fun Modifier.tabSwipe(onSwipeLeft: () -> Unit, onSwipeRight: () -> Unit): Modifier =
    composed {
        val density = LocalDensity.current
        val threshold = with(density) { 88.dp.toPx() }
        var accumulated = 0f
        var vertical = 0f
        val left = rememberUpdatedState(onSwipeLeft)
        val right = rememberUpdatedState(onSwipeRight)

        this.pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragStart = {
                    accumulated = 0f
                    vertical = 0f
                },
                onDragEnd = {
                    when {
                        accumulated <= -threshold -> left.value()
                        accumulated >= threshold -> right.value()
                    }
                    accumulated = 0f
                },
                onDragCancel = {
                    accumulated = 0f
                    vertical = 0f
                },
                onHorizontalDrag = { _, dragAmount ->
                    accumulated += dragAmount
                },
            )
        }
    }

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.ui.components.FloatingNavBar
import com.dotpmm.stocker.ui.components.NavItem
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.screens.AddStockScreen
import com.dotpmm.stocker.ui.screens.AiSummaryScreen
import com.dotpmm.stocker.ui.screens.EventsTab
import com.dotpmm.stocker.ui.screens.NewsTab
import com.dotpmm.stocker.ui.screens.ReaderScreen
import com.dotpmm.stocker.ui.screens.SettingsTab
import com.dotpmm.stocker.ui.screens.StockDetailScreen
import com.dotpmm.stocker.ui.screens.WatchlistTab
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Obsidian

/**
 * App shell: four tabs and a floating nav bar. Detail, reader, AI summary and add-stock open on a back stack
 * kept as a string ("d:RELIANCE|r:abc123"), which survives rotation and process death.
 */
@Composable
fun StockerRoot() {
    val store = remember { StockerApp.instance.store }
    val watchlist by store.watchlist.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var stackRaw by rememberSaveable { mutableStateOf("") }
    val stack = if (stackRaw.isEmpty()) emptyList() else stackRaw.split("|")
    val top: String? = stack.lastOrNull()

    fun push(route: String) {
        stackRaw = if (stackRaw.isEmpty()) route else "$stackRaw|$route"
    }

    fun pop() {
        val parts = if (stackRaw.isEmpty()) emptyList() else stackRaw.split("|")
        stackRaw = parts.dropLast(1).joinToString("|")
    }

    val navItems = remember {
        listOf(
            NavItem("Watchlist", StockerIcons.Watchlist),
            NavItem("News", StockerIcons.News),
            NavItem("Events", StockerIcons.Events),
            NavItem("Settings", StockerIcons.Settings),
        )
    }

    LaunchedEffect(Unit) { store.refreshIfStale() }
    BackHandler(enabled = top != null) { pop() }

    // The floating bar is 64 dp + 16 dp above and below; leave room so content scrolls clear of it.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = 96.dp + bottomInset)
    val showNav = top == null || top.startsWith("d:")
    val screenKey = top ?: "tab:$tab"

    Box(Modifier.fillMaxSize().background(Obsidian.Background)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            AnimatedContent(
                targetState = screenKey,
                transitionSpec = {
                    val easing = Motion.Emphasized
                    val toTab = targetState.startsWith("tab:")
                    val fromTab = initialState.startsWith("tab:")
                    when {
                        toTab && fromTab -> fadeIn(tween(240, easing = easing)) togetherWith fadeOut(tween(160, easing = easing))
                        toTab -> fadeIn(tween(300, easing = easing)) togetherWith
                            (fadeOut(tween(200, easing = easing)) + slideOutHorizontally(tween(300, easing = easing)) { it / 8 })
                        else -> (fadeIn(tween(300, easing = easing)) + slideInHorizontally(tween(380, easing = easing)) { it / 8 }) togetherWith
                            fadeOut(tween(200, easing = easing))
                    }
                },
                label = "screens",
            ) { key ->
                when {
                    key == "tab:0" -> WatchlistTab(
                        onOpenStock = { push("d:$it") },
                        onAddStock = { push("add") },
                        contentPadding = contentPadding,
                    )
                    key == "tab:1" -> NewsTab(onOpenArticle = { push("r:$it") }, contentPadding = contentPadding)
                    key == "tab:2" -> EventsTab(onOpenArticle = { push("r:$it") }, contentPadding = contentPadding)
                    key == "tab:3" -> SettingsTab(contentPadding = contentPadding)
                    key == "add" -> AddStockScreen(onBack = { pop() })
                    key.startsWith("d:") -> {
                        val stock = watchlist.firstOrNull { it.ticker == key.removePrefix("d:") }
                        if (stock != null) {
                            StockDetailScreen(
                                stock = stock,
                                onBack = { pop() },
                                onOpenArticle = { push("r:$it") },
                                onAiSummary = { push("a:${stock.ticker}") },
                                onRemoved = { pop() },
                                contentPadding = contentPadding,
                            )
                        }
                    }
                    key.startsWith("a:") -> {
                        val stock = watchlist.firstOrNull { it.ticker == key.removePrefix("a:") }
                        if (stock != null) {
                            AiSummaryScreen(
                                stock = stock,
                                onBack = { pop() },
                                onOpenSettings = {
                                    stackRaw = ""
                                    tab = 3
                                },
                                onOpenArticle = { push("r:$it") },
                            )
                        }
                    }
                    key.startsWith("r:") -> ReaderScreen(
                        articleId = key.removePrefix("r:"),
                        onBack = { pop() },
                        onOpenArticle = { push("r:$it") },
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(300, easing = Motion.Emphasized)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(200, easing = Motion.Emphasized)) { it } + fadeOut(tween(150)),
        ) {
            FloatingNavBar(
                items = navItems,
                selectedIndex = tab,
                onSelect = {
                    tab = it
                    stackRaw = ""
                },
            )
        }
    }
}

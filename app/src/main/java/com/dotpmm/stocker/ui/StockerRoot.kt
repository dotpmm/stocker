package com.dotpmm.stocker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.data.sample.SampleData
import com.dotpmm.stocker.ui.components.FloatingNavBar
import com.dotpmm.stocker.ui.components.NavItem
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.screens.StockDetailScreen
import com.dotpmm.stocker.ui.screens.WatchlistScreen
import com.dotpmm.stocker.ui.theme.Motion
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

/** App shell: four tabs, a floating nav bar, and watchlist -> stock detail navigation. */
@Composable
fun StockerRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openTicker by rememberSaveable { mutableStateOf<String?>(null) }
    val navItems = remember {
        listOf(
            NavItem("Watchlist", StockerIcons.Watchlist),
            NavItem("News", StockerIcons.News),
            NavItem("Events", StockerIcons.Events),
            NavItem("Settings", StockerIcons.Settings),
        )
    }
    BackHandler(enabled = openTicker != null) { openTicker = null }

    // The floating bar is 64 dp + 16 dp above and below; leave room so content scrolls clear of it.
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(bottom = 96.dp + bottomInset)

    Box(Modifier.fillMaxSize().background(Obsidian.Background)) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            if (tab == 0) {
                AnimatedContent(
                    targetState = openTicker,
                    transitionSpec = {
                        val easing = Motion.Emphasized
                        if (targetState != null) {
                            (fadeIn(tween(300, easing = easing)) +
                                slideInHorizontally(tween(380, easing = easing)) { it / 8 }) togetherWith
                                fadeOut(tween(200, easing = easing))
                        } else {
                            fadeIn(tween(300, easing = easing)) togetherWith
                                (fadeOut(tween(200, easing = easing)) +
                                    slideOutHorizontally(tween(300, easing = easing)) { it / 8 })
                        }
                    },
                    label = "watchlistToDetail",
                ) { ticker ->
                    val stock = SampleData.stocks.firstOrNull { it.ticker == ticker }
                    if (stock == null) {
                        WatchlistScreen(
                            stocks = SampleData.stocks,
                            indices = SampleData.indices,
                            updatedLabel = SampleData.UPDATED_LABEL,
                            onOpenStock = { openTicker = it.ticker },
                            onRefresh = {},
                            onAddStock = {},
                            contentPadding = contentPadding,
                        )
                    } else {
                        StockDetailScreen(
                            detail = SampleData.detailFor(stock),
                            onBack = { openTicker = null },
                            onRefresh = {},
                            onAiSummary = {},
                            contentPadding = contentPadding,
                        )
                    }
                }
            } else {
                ComingSoon(navItems[tab].label)
            }
        }
        FloatingNavBar(
            items = navItems,
            selectedIndex = tab,
            onSelect = {
                tab = it
                openTicker = null
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = StockerText.TabLarge)
            Text("Coming soon", style = StockerText.Body)
        }
    }
}

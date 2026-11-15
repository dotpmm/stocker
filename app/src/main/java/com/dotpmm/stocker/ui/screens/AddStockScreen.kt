package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.data.Popular
import com.dotpmm.stocker.data.net.Yahoo
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.LocalSettings
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * Add a stock by name or ticker. Built-in suggestions work offline; Yahoo search finds the rest
 * when live search is enabled in settings.
 */
@Composable
fun AddStockScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = StockerApp.instance.store
    val settings = LocalSettings.current
    val watchlist by store.watchlist.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WatchStock>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(180)
        runCatching { focus.requestFocus() }
    }

    LaunchedEffect(query, settings.liveSearch) {
        notice = null
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(350)
        val local = Popular.search(q)
        val remote = if (!settings.liveSearch) {
            emptyList()
        } else {
            try {
                Yahoo.search(q).map { hit ->
                    val known = Popular.all.firstOrNull { it.ticker == hit.ticker && it.market == hit.market }
                    if (known != null) hit.copy(aliases = known.aliases) else hit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notice = "Online search is unavailable. Showing built-in suggestions."
                emptyList()
            }
        }
        results = (remote + local).distinctBy { it.ticker + it.market.name }
        searching = false
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(StockerIcons.Back, "Back", onBack)
            Spacer(Modifier.width(8.dp))
            Text("ADD A STOCK", style = Type.Micro.copy(letterSpacing = 1.6.sp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .flat(Ink.Surface1, RoundedCornerShape(999.dp), 999.dp)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", style = Type.Data.copy(color = Ink.TextMuted))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Search company or ticker", style = Type.BodyTight.copy(color = Ink.TextMuted))
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = Type.BodyTight.copy(color = Ink.TextPrimary),
                    cursorBrush = SolidColor(Ink.TextPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {}),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        }

        val showing = if (query.trim().length < 2) Popular.all else results
        val heading = when {
            query.trim().length < 2 -> "Popular"
            searching -> "Searching…"
            else -> "Results"
        }

        LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
            item(key = "heading") {
                Text(
                    heading.uppercase(),
                    style = Type.Micro.copy(fontSize = 13.sp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            notice?.let { text ->
                item(key = "notice") {
                    Text(
                        text,
                        style = Type.Caption,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            if (showing.isEmpty() && !searching && query.trim().length >= 2) {
                item(key = "none") {
                    Text(
                        "No match. Try the company name or the NSE ticker.",
                        style = Type.Caption,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            items(showing, key = { it.ticker + it.market.name }) { stock ->
                val added = watchlist.any { it.ticker == stock.ticker }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .flat(Ink.Surface1, RoundedCornerShape(14.dp), 14.dp)
                        .pressable(
                            onClick = { if (!added) store.add(stock) },
                            pressedScale = 0.985f,
                        )
                        .heightIn(min = 56.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stock.ticker, style = Type.Ticker)
                        Text(
                            "${stock.name} · ${stock.exchange}" +
                                if (stock.market == Market.US) " · news only" else "",
                            style = Type.MicroData.copy(fontSize = 11.5.sp),
                            maxLines = 1,
                        )
                    }
                    Text(
                        if (added) "ADDED" else "ADD",
                        style = Type.MicroData.copy(
                            fontSize = 13.sp,
                            color = if (added) Ink.TextMuted else Ink.TextPrimary,
                        ),
                    )
                }
            }
        }
    }
}

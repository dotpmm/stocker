package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.data.Popular
import com.dotpmm.stocker.data.net.Yahoo
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/** Search by company name or ticker. Built-in suggestions work offline; Yahoo search finds the rest. */
@Composable
fun AddStockScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val store = StockerApp.instance.store
    val watchlist by store.watchlist.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WatchStock>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            focus.requestFocus()
        } catch (e: Exception) {
            // The field may not be attached yet; the user can tap it.
        }
    }

    LaunchedEffect(query) {
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
        val remote = try {
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
        results = (remote + local).distinctBy { it.ticker + it.market.name }
        searching = false
    }

    Column(modifier.fillMaxSize().imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopIconButton(StockerIcons.Back, "Back", onBack)
            Text("ADD STOCK", style = StockerText.SectionLabel, modifier = Modifier.padding(start = 8.dp))
        }
        UnderlineField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Search company or ticker",
            modifier = Modifier.padding(horizontal = 20.dp),
            fieldModifier = Modifier.focusRequester(focus),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {}),
        )
        val showing: List<WatchStock>
        val heading: String
        if (query.trim().length < 2) {
            showing = Popular.all
            heading = "POPULAR"
        } else {
            showing = results
            heading = if (searching) "SEARCHING…" else "RESULTS"
        }
        LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
            item(key = "heading") {
                Text(
                    heading,
                    style = StockerText.SectionLabel,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
                )
            }
            if (notice != null) {
                item(key = "notice") {
                    Text(
                        notice ?: "",
                        style = StockerText.Caption.copy(color = Obsidian.TextSubtle),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }
            if (showing.isEmpty() && !searching && query.trim().length >= 2) {
                item(key = "none") {
                    Text(
                        "No match. Try the company name or the NSE ticker.",
                        style = StockerText.Body,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            items(showing, key = { it.ticker + it.market.name }) { stock ->
                val added = watchlist.any { it.ticker == stock.ticker }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .pressable(onClick = { if (!added) store.add(stock) }, pressedScale = 0.98f, pressedAlpha = 0.6f)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stock.ticker, style = StockerText.Ticker)
                        Text(
                            "${stock.name} · ${stock.exchange}" + if (stock.market == Market.US) " · news only" else "",
                            style = StockerText.Caption,
                            maxLines = 1,
                        )
                    }
                    Text(
                        if (added) "Added" else "Add",
                        style = StockerText.BodyStrong.copy(color = if (added) Obsidian.TextSubtle else Obsidian.Accent),
                    )
                }
            }
        }
    }
}

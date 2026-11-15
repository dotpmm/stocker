package com.dotpmm.stocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.model.SourceHealth
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.PrimaryActionBar
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText
import kotlinx.coroutines.launch

@Composable
fun SettingsTab(contentPadding: PaddingValues) {
    val store = StockerApp.instance.store
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val refresh by store.refresh.collectAsState()
    val lastRefresh by store.lastRefresh.collectAsState()
    val health by rememberQuery(emptyList<SourceHealth>()) { store.health() }

    var key by remember { mutableStateOf(store.prefs.geminiKey) }
    var model by remember { mutableStateOf(store.prefs.geminiModel) }
    var reveal by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item(key = "header") { ScreenHeader(title = "Settings", subtitle = "Everything stays on this phone") }

        item(key = "gemini") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("GEMINI · OPTIONAL AI SUMMARY", Modifier.padding(top = 22.dp))
                Text(
                    "Paste a free API key from aistudio.google.com. It is stored only on this device and used only when you tap AI summary.",
                    style = StockerText.Body,
                )
                UnderlineField(
                    value = key,
                    onValueChange = { key = it; saved = false },
                    placeholder = "Gemini API key",
                    visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                UnderlineField(
                    value = model,
                    onValueChange = { model = it; saved = false },
                    placeholder = "Model (leave empty for the newest Flash)",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextAction("Save", onClick = {
                        store.prefs.geminiKey = key
                        store.prefs.geminiModel = model
                        saved = true
                    })
                    TextAction(if (reveal) "Hide key" else "Show key", onClick = { reveal = !reveal }, color = Obsidian.TextSecondary)
                    TextAction("Clear", onClick = {
                        key = ""
                        model = ""
                        store.prefs.geminiKey = ""
                        store.prefs.geminiModel = ""
                        saved = false
                    }, color = Obsidian.TextSecondary)
                    if (saved) Text("Saved", style = StockerText.Caption.copy(color = Obsidian.Positive))
                }
            }
        }

        item(key = "refresh") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("REFRESH", Modifier.padding(top = 22.dp))
                Text(
                    "News and prices refresh automatically every 12 hours when you are online, and when you open the app after a while.",
                    style = StockerText.Body,
                )
                Text(
                    if (refresh.running) "Refreshing · ${refresh.message}" else Fmt.updated(lastRefresh),
                    style = StockerText.Meta.copy(color = Obsidian.TextSecondary),
                )
                PrimaryActionBar(
                    label = if (refresh.running) "Refreshing…" else "Refresh now",
                    leadingIcon = StockerIcons.Refresh,
                    trailingIcon = StockerIcons.Arrow,
                    onClick = { store.launchRefresh() },
                )
            }
        }

        item(key = "sources-label") {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("SOURCE HEALTH")
                Text("Checked on every refresh. A red square means that feed failed last time.", style = StockerText.Body)
            }
        }
        if (health.isEmpty()) {
            item(key = "sources-empty") {
                Text(
                    "Nothing checked yet. Add a stock and refresh.",
                    style = StockerText.Caption,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        items(health, key = { it.id }) { h ->
            HealthRow(h)
        }

        item(key = "data") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("DATA", Modifier.padding(top = 22.dp))
                TextAction(
                    label = if (confirmClear) "Tap again to delete saved news" else "Clear saved news and charts",
                    color = if (confirmClear) Obsidian.Negative else Obsidian.TextSecondary,
                    onClick = {
                        if (confirmClear) {
                            scope.launch { store.clearNews() }
                            confirmClear = false
                        } else {
                            confirmClear = true
                        }
                    },
                )
            }
        }

        item(key = "about") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("ABOUT", Modifier.padding(top = 22.dp))
                Text("Stocker $versionName", style = StockerText.BodyStrong)
                Text(
                    "Free and on-device. No account, no tracking. Headlines belong to their publishers and open at the source.",
                    style = StockerText.Body,
                )
                Text(
                    "Information only, never financial advice. Tone tags and detected events come from simple keyword rules and can be wrong.",
                    style = StockerText.Body,
                )
                Text("github.com/dotpmm/stocker", style = StockerText.Caption.copy(color = Obsidian.TextSubtle))
            }
        }
    }
}

@Composable
private fun HealthRow(h: SourceHealth) {
    val color = when {
        h.lastError != null -> Obsidian.Negative
        h.lastSuccess > 0 -> Obsidian.Positive
        else -> Obsidian.Neutral
    }
    val detail = when {
        h.lastError != null && h.lastSuccess > 0 -> "Failed ${Fmt.age(h.lastAttempt)} ago (${h.lastError}) · last worked ${Fmt.age(h.lastSuccess)} ago"
        h.lastError != null -> "Failed ${Fmt.age(h.lastAttempt)} ago (${h.lastError})"
        else -> "${h.items} items · ${Fmt.age(h.lastSuccess)} ago"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 6.dp).size(6.dp).background(color))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(h.name, style = StockerText.BodyStrong)
            Text(detail, style = StockerText.Caption)
        }
    }
}

package com.dotpmm.stocker.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.PrimaryActionBar
import com.dotpmm.stocker.ui.components.ToneMarker
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Obsidian
import com.dotpmm.stocker.ui.theme.StockerText

private sealed interface BodyState {
    data object Loading : BodyState
    data object Unavailable : BodyState
    data class Ready(val paragraphs: List<String>) : BodyState
}

/** Clean reading view: headline, tone, extracted text when the publisher allows it, related coverage, link out. */
@Composable
fun ReaderScreen(
    articleId: String,
    onBack: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = StockerApp.instance.store
    val context = LocalContext.current
    val article by produceState<Article?>(null, articleId) { value = store.article(articleId) }
    val related by produceState(emptyList<Article>(), article) {
        val a = article
        value = if (a == null) emptyList() else store.clusterMembers(a)
    }
    var body by remember(articleId) { mutableStateOf<BodyState>(BodyState.Loading) }
    LaunchedEffect(article?.id) {
        val a = article ?: return@LaunchedEffect
        body = BodyState.Loading
        val paragraphs = store.readBody(a)
        body = if (paragraphs == null) BodyState.Unavailable else BodyState.Ready(paragraphs)
    }

    fun openOriginal(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // No browser installed; nothing else to do.
        }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopIconButton(StockerIcons.Back, "Back", onBack)
            Text("STORY", style = StockerText.SectionLabel)
            TopIconButton(StockerIcons.External, "Open original", { article?.let { openOriginal(it.url) } })
        }
        val a = article
        if (a == null) {
            EmptyNote("Story not found", "It may have been cleared from saved news.")
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("${a.source} · ${Fmt.dateTime(a.publishedAt)}", style = StockerText.Caption)
            Text(a.title, style = StockerText.ReaderTitle)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(a.tag, style = StockerText.Caption)
                Text("·", style = StockerText.Caption)
                ToneMarker(a.tone)
                Text(a.tone.name, style = StockerText.Caption.copy(color = Obsidian.TextSecondary))
            }

            when (val state = body) {
                is BodyState.Ready -> {
                    state.paragraphs.forEach { Text(it, style = StockerText.ReaderBody) }
                    Text(
                        "Reader view pulled from ${a.source}. Open the original for the full page, images and updates.",
                        style = StockerText.Caption.copy(color = Obsidian.TextSubtle),
                    )
                }
                BodyState.Loading -> {
                    if (a.summary.isNotBlank()) Text(a.summary, style = StockerText.ReaderBody)
                    Text("Loading the full text…", style = StockerText.Caption)
                }
                BodyState.Unavailable -> {
                    if (a.summary.isNotBlank()) {
                        Text(a.summary, style = StockerText.ReaderBody)
                    } else {
                        Text(
                            "This publisher does not allow an in-app preview. Open the original to read the full story.",
                            style = StockerText.Body,
                        )
                    }
                }
            }

            if (related.isNotEmpty()) {
                Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("ALSO REPORTED BY")
                    related.take(6).forEach { r ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .pressable(onClick = { onOpenArticle(r.id) }, pressedScale = 0.98f, pressedAlpha = 0.6f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("${r.source} · ${Fmt.age(r.publishedAt)}", style = StockerText.Caption)
                            Text(r.title, style = StockerText.BodyStrong)
                        }
                    }
                }
            }

            PrimaryActionBar(
                label = "Open original",
                leadingIcon = StockerIcons.External,
                trailingIcon = StockerIcons.Arrow,
                onClick = { openOriginal(a.url) },
                modifier = Modifier.padding(top = 10.dp),
            )
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
    }
}

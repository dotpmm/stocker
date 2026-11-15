package com.dotpmm.stocker.ui.screens

import android.content.ActivityNotFoundException
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.dotpmm.stocker.StockerApp
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.ui.Fmt
import com.dotpmm.stocker.ui.components.pressable
import com.dotpmm.stocker.ui.icons.StockerIcons
import com.dotpmm.stocker.ui.theme.Ink
import com.dotpmm.stocker.ui.theme.Type
import com.dotpmm.stocker.ui.theme.flat
import com.dotpmm.stocker.ui.theme.metal

private sealed interface BodyState {
    data object Loading : BodyState
    data object Unavailable : BodyState
    data class Ready(val paragraphs: List<String>) : BodyState
}

/** Clean reading view: headline, tone, extracted text where allowed, related coverage, link out. */
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
        openWebLink(context, url)
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 14.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTap(StockerIcons.Back, "Back", onBack)
            Text("STORY", style = Type.MicroData.copy(fontSize = 13.sp))
            IconTap(StockerIcons.External, "Open original", { article?.let { openOriginal(it.url) } })
        }
        val a = article
        if (a == null) {
            Column(Modifier.padding(20.dp)) {
                Text("Story not found", style = Type.PanelTitle)
                Text("It may have been cleared from saved news.", style = Type.Caption)
            }
            return@Column
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("${a.source} · ${Fmt.dateTime(a.publishedAt)}", style = Type.MicroData.copy(fontSize = 11.5.sp))
            Text(
                a.title,
                style = Type.Header.copy(fontSize = 23.sp, lineHeight = 29.sp),
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (a.tag.isNotBlank()) {
                    Text(
                        a.tag,
                        style = Type.MicroData.copy(fontSize = 11.5.sp, color = Ink.TextSecondary),
                        modifier = Modifier
                            .flat(Ink.Surface2, RoundedCornerShape(999.dp), 999.dp)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
                ToneMarker(a.tone)
                Text(
                    a.tone.name.uppercase(),
                    style = Type.MicroData.copy(
                        fontSize = 11.5.sp,
                        color = when (a.tone) {
                            com.dotpmm.stocker.domain.model.Tone.Positive -> Ink.UpText
                            com.dotpmm.stocker.domain.model.Tone.Negative -> Ink.DownText
                            else -> Ink.TextMuted
                        },
                    ),
                )
            }

            when (val state = body) {
                is BodyState.Ready -> {
                    state.paragraphs.forEach {
                        Text(it, style = Type.Body.copy(fontSize = 16.sp, lineHeight = 26.sp))
                    }
                    Text(
                        "Reader view pulled from ${a.source}. Open the original for the full page, " +
                            "images and updates.",
                        style = Type.MicroData.copy(fontSize = 11.5.sp),
                    )
                }
                BodyState.Loading -> {
                    if (a.summary.isNotBlank()) Text(a.summary, style = Type.Body)
                    Text("Loading the full text…", style = Type.Caption)
                }
                BodyState.Unavailable -> {
                    if (a.summary.isNotBlank()) {
                        Text(a.summary, style = Type.Body)
                    } else {
                        Text(
                            "This publisher does not allow an in-app preview. Open the original to read " +
                                "the full story.",
                            style = Type.Body,
                        )
                    }
                }
            }

            if (related.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp)) {
                    SectionMicro("Also reported by")
                    Spacer(Modifier.height(8.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .flat(Ink.Surface1, RoundedCornerShape(16.dp), 16.dp)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        related.take(6).forEach { r ->
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .pressable(onClick = { onOpenArticle(r.id) }, pressedScale = 0.985f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    "${r.source} · ${Fmt.age(r.publishedAt)}",
                                    style = Type.MicroData.copy(fontSize = 11.5.sp),
                                )
                                Text(
                                    r.title,
                                    style = Type.BodyTight.copy(color = Ink.TextPrimary),
                                    maxLines = 3,
                                )
                            }
                        }
                    }
                }
            }

            ActionPill(
                label = "Open original",
                onClick = { openOriginal(a.url) },
                leading = StockerIcons.External,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(28.dp).navigationBarsPadding())
        }
    }
}

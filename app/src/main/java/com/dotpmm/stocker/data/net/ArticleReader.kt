package com.dotpmm.stocker.data.net

import com.dotpmm.stocker.domain.nlp.TextUtil

/** Best-effort "reader mode": pulls paragraph text out of a publisher page when the user opens a story. */
object ArticleReader {
    private val article = Regex("<article[^>]*>(.*?)</article>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val paragraph = Regex("<p[^>]*>(.*?)</p>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val scripts = Regex("<(script|style|noscript|svg|figure|aside|nav|footer)[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val noise = listOf(
        "cookie", "subscribe", "advertisement", "sign in", "log in", "newsletter", "all rights reserved",
        "follow us", "download the app", "read more:", "also read", "click here", "whatsapp",
    )

    /** Returns paragraphs, or null when the page is a Google News wrapper, blocked, or has too little text. */
    suspend fun fetch(url: String): List<String>? {
        if (url.contains("news.google.com")) return null
        val response = Http.get(url, gateMs = 400, maxBytes = 2_500_000)
        if (!response.ok || response.finalUrl.contains("news.google.com")) return null
        val html = scripts.replace(response.body, " ")
        val scope = article.find(html)?.groupValues?.get(1) ?: html
        val paragraphs = paragraph.findAll(scope)
            .map { TextUtil.stripHtml(it.groupValues[1]) }
            .filter { it.length >= 60 }
            .filter { p -> noise.none { p.lowercase().contains(it) } }
            .distinct()
            .take(40)
            .toList()
        val chars = paragraphs.sumOf { it.length }
        return if (paragraphs.size >= 2 && chars >= 300) paragraphs else null
    }
}

package com.dotpmm.stocker.data.net

import android.util.Xml
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import org.xmlpull.v1.XmlPullParser

/** One entry of an RSS or Atom feed, before any analysis. */
data class RawItem(
    val title: String,
    val link: String,
    val summaryHtml: String,
    val publishedAt: Long,
    val source: String?,
)

object FeedParser {
    fun parse(xml: String, now: Long = System.currentTimeMillis()): List<RawItem> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml.trimStart('﻿', ' ', '\n', '\r', '\t')))
        val items = ArrayList<RawItem>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && (parser.name == "item" || parser.name == "entry")) {
                readItem(parser, parser.name, now)?.let { items.add(it) }
            }
            event = parser.next()
        }
        return items
    }

    private fun readItem(p: XmlPullParser, endName: String, now: Long): RawItem? {
        var title = ""
        var link = ""
        var summary = ""
        var date = 0L
        var source: String? = null
        while (true) {
            val event = p.next()
            if (event == XmlPullParser.END_DOCUMENT) break
            if (event == XmlPullParser.END_TAG && p.name == endName) break
            if (event != XmlPullParser.START_TAG) continue
            when (p.name) {
                "title" -> title = readText(p)
                "link" -> {
                    val href = p.getAttributeValue(null, "href")
                    val rel = p.getAttributeValue(null, "rel")
                    val text = readText(p)
                    if (!href.isNullOrEmpty()) {
                        if (link.isEmpty() || rel == null || rel == "alternate") link = href
                    } else if (text.isNotBlank()) {
                        link = text.trim()
                    }
                }
                "description", "summary" -> {
                    val text = readText(p)
                    if (text.length > summary.length) summary = text
                }
                "content:encoded", "content" -> {
                    val text = readText(p)
                    if (summary.isEmpty()) summary = text
                }
                "pubDate", "published", "updated", "dc:date" -> {
                    val parsed = parseDate(readText(p).trim())
                    if (parsed > 0 && (date == 0L || p.name != "updated")) date = parsed
                }
                "source" -> source = readText(p).trim().ifEmpty { null }
                else -> {}
            }
        }
        if (title.isBlank() || link.isBlank()) return null
        return RawItem(
            title = title.trim(),
            link = link.trim(),
            summaryHtml = summary,
            publishedAt = if (date > 0) date else now,
            source = source,
        )
    }

    /** Reads the text of the current element, including nested text, and leaves the parser on its end tag. */
    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> sb.append(p.text)
                XmlPullParser.END_DOCUMENT -> return sb.toString()
                else -> {}
            }
        }
        return sb.toString()
    }

    private val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
    )

    fun parseDate(text: String): Long {
        if (text.isEmpty()) return 0L
        for (pattern in patterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.ENGLISH)
                if (pattern.endsWith("'Z'") || pattern == "yyyy-MM-dd HH:mm:ss") {
                    format.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = format.parse(text)
                if (date != null) return date.time
            } catch (e: Exception) {
                // try the next pattern
            }
        }
        return 0L
    }
}

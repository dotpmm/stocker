package com.dotpmm.stocker.domain.nlp

import java.security.MessageDigest

object TextUtil {
    private val tags = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")
    private val entity = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
    private val nonWord = Regex("[^a-z0-9 ]+")

    private val named = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "rsquo" to "'", "lsquo" to "'", "ldquo" to "\"", "rdquo" to "\"", "ndash" to "-",
        "mdash" to "-", "hellip" to "...", "rupee" to "₹", "pound" to "£", "euro" to "€",
    )

    fun decodeEntities(text: String): String = entity.replace(text) { m ->
        val body = m.groupValues[1]
        when {
            body.startsWith("#x") || body.startsWith("#X") ->
                body.substring(2).toIntOrNull(16)?.let { codePointToString(it) } ?: m.value
            body.startsWith("#") -> body.substring(1).toIntOrNull()?.let { codePointToString(it) } ?: m.value
            else -> named[body.lowercase()] ?: m.value
        }
    }

    private fun codePointToString(cp: Int): String =
        if (cp in 1..0x10FFFF) String(Character.toChars(cp)) else ""

    fun stripHtml(html: String): String {
        if (html.isEmpty()) return ""
        val withoutTags = tags.replace(html, " ")
        val decoded = decodeEntities(withoutTags).replace('￼', ' ').replace(' ', ' ')
        return spaces.replace(decoded, " ").trim()
    }

    /** Lowercase, punctuation removed: the form used for duplicate detection. */
    fun normalizeTitle(title: String): String =
        spaces.replace(nonWord.replace(title.lowercase(), " "), " ").trim()

    private val stop = setOf(
        "the", "and", "for", "with", "from", "that", "this", "its", "are", "was", "will", "has", "have",
        "after", "over", "into", "amid", "says", "said", "new", "may", "can", "not", "but", "you", "your",
        "how", "why", "what", "who", "all", "out", "about", "than", "more", "most", "news", "live",
    )

    fun tokens(normalized: String): Set<String> =
        normalized.split(' ').filter { it.length > 2 && it !in stop }.toSet()

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val smaller = if (a.size <= b.size) a else b
        val larger = if (a.size <= b.size) b else a
        var shared = 0
        for (t in smaller) if (t in larger) shared++
        return shared.toDouble() / (a.size + b.size - shared)
    }

    /**
     * Duplicate score between two headlines' token sets, 0 when they are not the same story.
     * Syndicated copies reword headlines, so require high containment of the shorter one and a fair Jaccard overlap.
     */
    fun similarity(a: Set<String>, b: Set<String>): Double {
        val smaller = minOf(a.size, b.size)
        if (smaller < 4) return 0.0
        var shared = 0
        for (t in (if (a.size <= b.size) a else b)) if (t in (if (a.size <= b.size) b else a)) shared++
        val containment = shared.toDouble() / smaller
        val jaccard = shared.toDouble() / (a.size + b.size - shared)
        return if (containment >= 0.8 && jaccard >= 0.5) jaccard else 0.0
    }

    fun hash(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(text.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }

    /** Drops tracking parameters so the same article from two feeds shares one id. */
    fun canonicalUrl(url: String): String {
        val noFragment = url.substringBefore('#')
        val base = noFragment.substringBefore('?')
        val query = noFragment.substringAfter('?', "")
        if (query.isEmpty()) return base
        if (base.contains("news.google.com")) return noFragment
        val kept = query.split('&').filter { p ->
            val key = p.substringBefore('=').lowercase()
            !(key.startsWith("utm_") || key in setOf("ref", "src", "cmpid", "from", "fbclid", "gclid", "ocid", "mc_cid"))
        }
        return if (kept.isEmpty()) base else base + "?" + kept.joinToString("&")
    }
}

object Names {
    private val suffix = Regex("[,.\\s]+(limited|ltd|corporation|corp|incorporated|inc|plc|co)\\.?$", RegexOption.IGNORE_CASE)

    fun clean(name: String): String {
        var out = name.trim()
        while (true) {
            val next = suffix.replace(out, "").trim()
            if (next == out) break
            out = next
        }
        return out.trim(',', '.', ' ').ifEmpty { name.trim() }
    }
}

package com.dotpmm.stocker.domain.nlp

import com.dotpmm.stocker.domain.model.Tone

/**
 * Rule-based headline sentiment (no AI). A small finance lexicon scored on the title (double weight)
 * and the summary. It is deliberately simple and can be wrong; the UI labels it as a tone, not advice.
 */
object Sentiment {
    private val words: Map<String, Int> = HashMap<String, Int>().apply {
        fun pos(w: Int, vararg list: String) = list.forEach { put(it, w) }
        fun neg(w: Int, vararg list: String) = list.forEach { put(it, -w) }
        pos(2, "surge", "surges", "surged", "soar", "soars", "soared", "jump", "jumps", "jumped", "rally", "rallies",
            "rallied", "skyrocket", "skyrockets", "upgrade", "upgrades", "upgraded", "outperform", "bullish",
            "record", "turnaround", "beats", "beat", "tops", "topped", "breakout")
        pos(1, "gain", "gains", "gained", "rise", "rises", "rose", "climb", "climbs", "climbed", "advance", "advances",
            "strong", "stronger", "growth", "grows", "grew", "wins", "win", "won", "bags", "secures", "secured",
            "expands", "expansion", "approval", "approved", "approves", "dividend", "bonus", "boost", "boosts",
            "recover", "recovers", "recovery", "rebound", "rebounds", "positive", "raises", "raised", "buyback",
            "improves", "improved", "higher", "upbeat", "optimistic", "profitable", "narrows", "buy", "accumulate",
            "milestone", "launches", "launch", "partnership", "order", "orders", "robust", "healthy", "optimism")
        neg(2, "plunge", "plunges", "plunged", "tumble", "tumbles", "tumbled", "slump", "slumps", "slumped",
            "crash", "crashes", "crashed", "downgrade", "downgrades", "downgraded", "underperform", "bearish",
            "fraud", "scam", "default", "defaults", "bankruptcy", "insolvency", "collapse", "collapses", "misses",
            "miss", "missed", "probe", "raid", "raids", "ban", "banned", "bans", "sinks", "sink", "sank")
        neg(1, "fall", "falls", "fell", "drop", "drops", "dropped", "decline", "declines", "declined", "slide", "slides",
            "loss", "losses", "weak", "weaker", "weakness", "sell", "selloff", "penalty", "fine", "fined", "lawsuit",
            "cut", "cuts", "resigns", "resignation", "quits", "halt", "halts", "warning", "warns", "concern",
            "concerns", "risk", "risks", "slowdown", "layoffs", "recall", "shutdown", "delay", "delays", "delayed",
            "lower", "lowers", "dips", "dip", "dipped", "pressure", "fears", "fear", "volatile", "outflow", "outflows",
            "disappoints", "disappointing", "shortfall", "negative", "debt", "controversy", "slows", "lowest",
            "worst", "trouble", "investigation", "notice", "violation", "violations")
    }

    private val phrases: List<Pair<String, Int>> = listOf(
        "all-time high" to 2, "all time high" to 2, "record high" to 2, "52-week high" to 1, "52 week high" to 1,
        "swings to profit" to 2, "beats estimates" to 1, "above estimates" to 1, "target price raised" to 2,
        "52-week low" to -1, "52 week low" to -1, "all-time low" to -2, "record low" to -2, "profit booking" to -1,
        "short seller" to -2, "misses estimates" to -1, "below estimates" to -1, "swings to loss" to -2,
        "show cause" to -2, "stake sale" to -1, "fails to" to -2,
    )

    private val negators = setOf("not", "no", "never", "without", "hardly")
    private val splitter = Regex("[^a-z0-9]+")

    private fun scoreText(text: String): Int {
        if (text.isEmpty()) return 0
        val lower = text.lowercase()
        val tokens = lower.split(splitter).filter { it.isNotEmpty() }
        var sum = 0
        for (i in tokens.indices) {
            var weight = words[tokens[i]] ?: continue
            val negated = (1..2).any { k -> i - k >= 0 && tokens[i - k] in negators }
            if (negated) weight = -weight
            sum += weight
        }
        for ((phrase, weight) in phrases) if (lower.contains(phrase)) sum += weight
        return sum.coerceIn(-6, 6)
    }

    fun analyze(title: String, summary: String): Tone {
        val score = 2 * scoreText(title) + scoreText(summary)
        return when {
            score >= 2 -> Tone.Positive
            score <= -2 -> Tone.Negative
            else -> Tone.Neutral
        }
    }
}

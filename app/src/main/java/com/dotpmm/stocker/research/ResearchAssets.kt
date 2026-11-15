package com.dotpmm.stocker.research

import android.content.Context
import java.time.LocalDate
import org.json.JSONObject

/** Reads the shipped curated dataset and the Nifty-200 universe out of the APK assets. */
object ResearchAssets {
    fun curated(context: Context): CuratedData {
        val raw = context.assets.open("curated.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val orderArray = root.getJSONArray("sector_order")
        val order = (0 until orderArray.length()).map { orderArray.getString(it) }
        val sectorsArray = root.getJSONArray("sectors")
        val sectors = (0 until sectorsArray.length()).map { i ->
            val o = sectorsArray.getJSONObject(i)
            val cosArray = o.getJSONArray("companies")
            CuratedSector(
                id = o.getString("id"),
                name = o.getString("name"),
                indexSymbol = o.getString("index_symbol"),
                pe5yAvg = o.getDouble("pe_5y_avg"),
                blurb = o.optString("blurb"),
                companies = (0 until cosArray.length()).map { j ->
                    val c = cosArray.getJSONObject(j)
                    CuratedCompany(
                        symbol = c.getString("symbol"),
                        name = c.getString("name"),
                        moat = c.optString("moat", "None"),
                        monopoly = c.optBoolean("monopoly"),
                        cultural = c.optBoolean("cultural"),
                        defensive = c.optBoolean("defensive"),
                        note = c.optString("note"),
                    )
                },
            )
        }
        val depsArray = root.getJSONArray("dependencies")
        val dependencies = (0 until depsArray.length()).map { i ->
            val d = depsArray.getJSONObject(i)
            Dependency(d.getString("from"), d.getString("to"), d.getDouble("w"))
        }
        val narrativesObj = root.getJSONObject("narratives")
        val narratives = narrativesObj.keys().asSequence().associateWith { narrativesObj.getString(it) }
        return CuratedData(
            sectorOrder = order,
            sectors = sectors,
            dependencies = dependencies,
            narratives = narratives,
            fallbackNarrative = root.getString("fallback_narrative"),
        )
    }

    fun universe(context: Context): List<UniverseStock> {
        val raw = context.assets.open("nifty200.json").bufferedReader().use { it.readText() }
        val stocks = JSONObject(raw).getJSONArray("stocks")
        return (0 until stocks.length()).mapNotNull { i ->
            val o = stocks.getJSONObject(i)
            val symbol = o.optString("symbol")
            if (symbol.isEmpty()) null else UniverseStock(
                symbol = symbol,
                name = o.optString("company").ifEmpty { symbol },
                industry = o.optString("industry"),
            )
        }
    }
}

/** A dated close. [t] is "YYYY-MM-DD" daily, "YYYY-MM-DD HH:MM" intraday. */
data class Point(val t: String, val c: Double) {
    /** Intraday series are detected by the space in the timestamp (FUNCTIONALITY.md §4). */
    val intraday: Boolean get() = t.contains(' ')
    val datePart: String get() = t.substringBefore(' ')
}

/** One earnings date and the realized reaction around it. */
data class EarningsDot(val date: String, val surprise: Double?, val dropPct: Double?)

object ResearchMath {
    /**
     * Sector card badge thresholds (FUNCTIONALITY.md §3.1):
     * 1.12× of baseline → overheated, 0.92× → discounted, else consolidating.
     */
    fun badge(peNow: Double?, peAvg: Double): Verdict = when {
        peNow == null -> Verdict.Consolidating
        peNow >= peAvg * 1.12 -> Verdict.Overheated
        peNow <= peAvg * 0.92 -> Verdict.Discounted
        else -> Verdict.Consolidating
    }

    /** Market-cap weighted trailing P/E of the constituents' pairs that carry both values. */
    fun weightedPe(pairs: List<Triple<String, Double?, Double?>>): Double? {
        var num = 0.0
        var den = 0.0
        for ((_, pe, mcap) in pairs) {
            if (pe != null && mcap != null && pe > 0 && mcap > 0) {
                num += pe * mcap
                den += mcap
            }
        }
        return if (den > 0) (num / den).round1() else null
    }

    /** 1-year change = last vs first close of the index series. */
    fun yearChange(points: List<Point>): Double? {
        if (points.size < 2) return null
        val first = points.first().c
        val last = points.last().c
        if (first <= 0) return null
        return ((last / first - 1) * 100).round1()
    }

    /** Hero change: last vs previous point *within the series* (FUNCTIONALITY.md §3.1). */
    fun heroChange(points: List<Point>): Double? {
        if (points.size < 2) return null
        val prev = points[points.size - 2].c
        val last = points.last().c
        if (prev <= 0) return null
        return ((last / prev - 1) * 100).round2()
    }

    /**
     * Own 5Y average P/E (documented approximation, FUNCTIONALITY.md §5): pair each fiscal year's
     * positive diluted EPS with that calendar year's last close, need ≥2 valid years.
     */
    fun own5yPe(epsByYear: List<Pair<Int, Double>>, fiveYearCloses: List<Point>): Double? {
        if (epsByYear.size < 2) return null
        val closeByYear = HashMap<String, Double>()
        for (p in fiveYearCloses) closeByYear[p.t.take(4)] = p.c
        val ratios = epsByYear.mapNotNull { (year, eps) ->
            val px = closeByYear[year.toString()]
            if (px != null && px > 0 && eps > 0) px / eps else null
        }
        if (ratios.size < 2) return null
        return (ratios.sum() / ratios.size).round1()
    }

    /** 5Y revenue CAGR from oldest→newest annual revenue, needing ≥2 year gaps. */
    fun revenueCagr(revenuesOldestFirst: List<Double>): Double? {
        val vals = revenuesOldestFirst.filter { it > 0 }
        if (vals.size < 3) return null
        val years = vals.size - 1
        if (years < 2 || vals.first() <= 0) return null
        return ((Math.pow(vals.last() / vals.first(), 1.0 / years) - 1) * 100).round1()
    }

    /**
     * Reality check verdict + gauge position (FUNCTIONALITY.md §5, §3.3).
     * Gauge spans 0.55×–1.85× the sector average.
     */
    fun realityCheck(pe: Double?, sectorAvg: Double, ownAvg: Double?): Reality {
        if (pe == null || pe <= 0) {
            return Reality("NO DATA", Band.Flat, 0.5, sectorAvg)
        }
        val lo = sectorAvg * 0.55
        val hi = sectorAvg * 1.85
        val pct = ((pe - lo) / (hi - lo)).coerceIn(0.0, 1.0)
        val verdict = when {
            ownAvg != null && ownAvg > 0 && pe < ownAvg * 0.92 -> "DISCOUNTED" to Band.Under
            pe >= sectorAvg * 1.25 -> "PRICED FOR PERFECTION" to Band.Over
            pe <= sectorAvg * 0.9 -> "DISCOUNTED" to Band.Under
            else -> "FAIR VALUE" to Band.Mid
        }
        return Reality(verdict.first, verdict.second, pct, sectorAvg)
    }

    /**
     * Realized price reaction to an earnings date: bisect to the next trading day, then compare the
     * close 3 trading days later (FUNCTIONALITY.md §5).
     */
    fun dropFor(date: String, history: List<Point>): Double? {
        if (history.isEmpty()) return null
        val sorted = history.sortedBy { it.t }
        val i0 = lowerBound(sorted.map { it.t }, date)
        if (i0 < 0 || i0 >= sorted.size || i0 + 3 >= sorted.size) return null
        val base = sorted[i0].c
        val after = sorted[i0 + 3].c
        if (base <= 0) return null
        return ((after / base - 1) * 100).round1()
    }

    /** First index whose value is >= [target]. */
    private fun lowerBound(sorted: List<String>, target: String): Int {
        var lo = 0
        var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (sorted[mid] < target) lo = mid + 1 else hi = mid
        }
        return lo
    }

    /**
     * Wire format: downsample every series to ≤[max] points (FUNCTIONALITY.md §5). The reference
     * used floor division, which overshoots the documented cap; ceiling division actually honours it.
     * The final point is always kept so the chart's endpoint is the real last close.
     */
    fun downsample(points: List<Point>, max: Int = 140): List<Point> {
        if (points.size <= max) return points
        val step = maxOf(1, Math.ceil(points.size.toDouble() / max).toInt())
        val out = ArrayList<Point>(max + 1)
        var i = 0
        while (i < points.size) {
            out.add(points[i])
            i += step
        }
        if (out.lastOrNull() !== points.last()) out.add(points.last())
        return out
    }

    fun formatDate(date: LocalDate): String = date.toString()

    fun Double.round1(): Double = Math.round(this * 10.0) / 10.0
    fun Double.round2(): Double = Math.round(this * 100.0) / 100.0
}

enum class Verdict { Overheated, Discounted, Consolidating }

enum class Band { Under, Mid, Over, Flat }

data class Reality(val verdict: String, val band: Band, val pct: Double, val sectorAvg: Double)

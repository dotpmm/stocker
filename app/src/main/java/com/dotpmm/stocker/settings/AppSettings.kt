package com.dotpmm.stocker.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** How much of the metal finish the surfaces carry. */
enum class MetalFinish(val label: String) {
    Off("Flat"),
    Subtle("Subtle"),
    Standard("Standard"),
    Pronounced("Pronounced"),
}

/** Base luminance of the charcoal room. AMOLED forces the darkest option. */
enum class BaseTone(val label: String) {
    Amoled("AMOLED"),
    Void("Deep"),
    Charcoal("Charcoal"),
    Graphite("Graphite"),
}

/** Grain strength over gradient surfaces (design.md §5 caps this at 0.07). */
enum class GrainLevel(val label: String) {
    Off("Off"),
    Whisper("Whisper"),
    Fine("Fine"),
    Heavy("Heavy"),
}

/** How strongly the direction pair reads. Muted is the shipped default. */
enum class AccentIntensity(val label: String) {
    Muted("Muted"),
    Standard("Standard"),
    Vivid("Vivid"),
}

/** Corner language for every plate and pill. */
enum class ShapeLanguage(val label: String) {
    Sharp("Sharp"),
    Soft("Soft"),
    Rounded("Rounded"),
    Pill("Pill"),
}

/** How much motion the app uses, on top of the OS reduced-motion setting. */
enum class MotionLevel(val label: String) {
    Full("Full"),
    Reduced("Reduced"),
    Off("Off"),
}

/** Density of the information layout. */
enum class Density(val label: String) {
    Compact("Compact"),
    Comfortable("Comfortable"),
    Airy("Airy"),
}

/**
 * The complete appearance + behaviour configuration.
 *
 * Every field here is honoured by the theme or the screens — there are no decorative toggles. The
 * whole object is persisted in one go under `app-settings` and read back with defaults filled in,
 * so adding a field later never resets the user's other choices.
 */
data class AppSettings(
    // ── Appearance ──────────────────────────────────────────────
    val baseTone: BaseTone = BaseTone.Void,
    val metalFinish: MetalFinish = MetalFinish.Standard,
    val grain: GrainLevel = GrainLevel.Fine,
    val accentIntensity: AccentIntensity = AccentIntensity.Muted,
    val shape: ShapeLanguage = ShapeLanguage.Rounded,
    val density: Density = Density.Comfortable,
    /** Radiates from the hero panel only — one per viewport, per design.md §3.3. */
    val ambientGlow: Boolean = true,
    /** The slow streak along the hero's top edge (design.md §8.2). */
    val borderTraveler: Boolean = true,
    /** The 1050 ms dot-field burst on the search box (design.md §12 rule 13). */
    val searchBurst: Boolean = true,
    /** Subtle haptic tick on every press. */
    val haptics: Boolean = true,
    /** Show the ▲/▼ glyph next to percentages. Turning this off is a readability regression. */
    val directionGlyphs: Boolean = true,
    val motionLevel: MotionLevel = MotionLevel.Full,
    /** Apply the metal finish to the floating nav bar too. Off keeps it a flat plate. */
    val metalNavBar: Boolean = true,
    /** Swipe left/right across a screen to change tab. */
    val swipeTabs: Boolean = true,
    /** Open on Markets when the watchlist is empty, Watchlist otherwise. */
    val smartStart: Boolean = true,

    // ── Data & refresh ──────────────────────────────────────────
    val backgroundRefresh: Boolean = true,
    /** Hours between automatic news refreshes. */
    val refreshHours: Int = 12,
    /** Minutes between automatic macro re-fetches while the screen is open. */
    val macroRefreshMinutes: Int = 10,
    /** Keeps the research TTL cache long so repeat visits are instant. */
    val longCache: Boolean = true,
    /** Fetch the extra Nifty-200 peers in sector tables, not just curated names. */
    val includeUniversePeers: Boolean = true,
    /** Show live Yahoo hits in search beyond the shipped universe. */
    val liveSearch: Boolean = true,

    // ── News ────────────────────────────────────────────────────
    val newsSources: Set<String> = DEFAULT_SOURCES,
    /** Hide stories older than this many days. */
    val newsWindowDays: Int = 7,
    /** Cluster duplicate coverage across outlets ("+3 sources"). */
    val clusterDuplicates: Boolean = true,
    /** Stories per company on the detail screen. */
    val newsPerCompany: Int = 60,
    /** Show the tone marker and the tone bar. */
    val showTone: Boolean = true,
    /** Show event tags (Results, Rating, M&A…). */
    val showEventTags: Boolean = true,

    // ── Prices ──────────────────────────────────────────────────
    /** Default chart range index for the 1D/1W/1M/3M/1Y selector. */
    val defaultRangeIndex: Int = 0,
    /** Draw the under-curve gradient fill on charts. */
    val chartFill: Boolean = true,
    /** Draw the underglow on chart lines. */
    val chartGlow: Boolean = true,
    /** Show earnings-date dots on the deep-dive chart. */
    val earningsDots: Boolean = true,
    /** Show the 52-week range line in the deep-dive. */
    val show52Week: Boolean = true,

    // ── Watchlist ───────────────────────────────────────────────
    val compactRows: Boolean = false,
    /** Sort the watchlist by name instead of the order added. */
    val sortWatchlistAlphabetically: Boolean = false,

    // ── AI ──────────────────────────────────────────────────────
    val aiModel: String = "",
) {
    /**
     * The effective darkest setting. AMOLED means "as dark as possible while still metal", so the
     * surface ramp is forced to near-black rather than merely dimmed.
     */
    val effectiveTone: BaseTone get() = baseTone

    val motionEnabled: Boolean
        get() = motionLevel != MotionLevel.Off

    companion object {
        val DEFAULT_SOURCES = setOf(
            "et-markets", "et-stocks", "et-stock-news", "et-corporate",
            "mint-markets", "mint-companies", "bl-markets", "bl-companies",
            "pulse", "ndtv-profit", "investing-in",
            "sebi", "rbi",
            "gn-moneycontrol", "gn-business-standard", "gn-cnbctv18", "gn-financial-express",
            "cnbc-us", "marketwatch-us",
        )
    }
}

/**
 * Persists [AppSettings] under a single key. Every read merges over the defaults, so a new field
 * appears with its default instead of resetting everything else.
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("app-settings", Context.MODE_PRIVATE)

    var settings by mutableStateOf(load())
        private set

    fun load(): AppSettings {
        val d = AppSettings()
        fun b(key: String, fallback: Boolean) = prefs.getBoolean(key, fallback)
        fun i(key: String, fallback: Int) = prefs.getInt(key, fallback)
        fun s(key: String, fallback: String) = prefs.getString(key, fallback) ?: fallback
        fun <T : Enum<T>> e(key: String, values: Array<T>, fallback: T): T {
            val name = prefs.getString(key, null) ?: return fallback
            return values.firstOrNull { it.name == name } ?: fallback
        }
        return AppSettings(
            baseTone = e("baseTone", BaseTone.entries.toTypedArray(), d.baseTone),
            metalFinish = e("metalFinish", MetalFinish.entries.toTypedArray(), d.metalFinish),
            grain = e("grain", GrainLevel.entries.toTypedArray(), d.grain),
            accentIntensity = e("accentIntensity", AccentIntensity.entries.toTypedArray(), d.accentIntensity),
            shape = e("shape", ShapeLanguage.entries.toTypedArray(), d.shape),
            density = e("density", Density.entries.toTypedArray(), d.density),
            ambientGlow = b("ambientGlow", d.ambientGlow),
            borderTraveler = b("borderTraveler", d.borderTraveler),
            searchBurst = b("searchBurst", d.searchBurst),
            haptics = b("haptics", d.haptics),
            directionGlyphs = b("directionGlyphs", d.directionGlyphs),
            motionLevel = e("motionLevel", MotionLevel.entries.toTypedArray(), d.motionLevel),
            metalNavBar = b("metalNavBar", d.metalNavBar),
            swipeTabs = b("swipeTabs", d.swipeTabs),
            smartStart = b("smartStart", d.smartStart),
            backgroundRefresh = b("backgroundRefresh", d.backgroundRefresh),
            refreshHours = i("refreshHours", d.refreshHours).coerceIn(1, 24),
            macroRefreshMinutes = i("macroRefreshMinutes", d.macroRefreshMinutes).coerceIn(1, 60),
            longCache = b("longCache", d.longCache),
            includeUniversePeers = b("includeUniversePeers", d.includeUniversePeers),
            liveSearch = b("liveSearch", d.liveSearch),
            newsSources = prefs.getStringSet("newsSources", d.newsSources) ?: d.newsSources,
            newsWindowDays = i("newsWindowDays", d.newsWindowDays).coerceIn(1, 30),
            clusterDuplicates = b("clusterDuplicates", d.clusterDuplicates),
            newsPerCompany = i("newsPerCompany", d.newsPerCompany).coerceIn(10, 200),
            showTone = b("showTone", d.showTone),
            showEventTags = b("showEventTags", d.showEventTags),
            defaultRangeIndex = i("defaultRangeIndex", d.defaultRangeIndex).coerceIn(0, 4),
            chartFill = b("chartFill", d.chartFill),
            chartGlow = b("chartGlow", d.chartGlow),
            earningsDots = b("earningsDots", d.earningsDots),
            show52Week = b("show52Week", d.show52Week),
            compactRows = b("compactRows", d.compactRows),
            sortWatchlistAlphabetically = b("sortWatchlistAlphabetically", d.sortWatchlistAlphabetically),
            aiModel = s("aiModel", d.aiModel),
        )
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(settings)
        settings = next
        persist(next)
    }

    fun reset() {
        settings = AppSettings()
        prefs.edit().clear().apply()
        persist(settings)
    }

    private fun persist(s: AppSettings) {
        prefs.edit()
            .putString("baseTone", s.baseTone.name)
            .putString("metalFinish", s.metalFinish.name)
            .putString("grain", s.grain.name)
            .putString("accentIntensity", s.accentIntensity.name)
            .putString("shape", s.shape.name)
            .putString("density", s.density.name)
            .putBoolean("ambientGlow", s.ambientGlow)
            .putBoolean("borderTraveler", s.borderTraveler)
            .putBoolean("searchBurst", s.searchBurst)
            .putBoolean("haptics", s.haptics)
            .putBoolean("directionGlyphs", s.directionGlyphs)
            .putString("motionLevel", s.motionLevel.name)
            .putBoolean("metalNavBar", s.metalNavBar)
            .putBoolean("swipeTabs", s.swipeTabs)
            .putBoolean("smartStart", s.smartStart)
            .putBoolean("backgroundRefresh", s.backgroundRefresh)
            .putInt("refreshHours", s.refreshHours)
            .putInt("macroRefreshMinutes", s.macroRefreshMinutes)
            .putBoolean("longCache", s.longCache)
            .putBoolean("includeUniversePeers", s.includeUniversePeers)
            .putBoolean("liveSearch", s.liveSearch)
            .putStringSet("newsSources", s.newsSources)
            .putInt("newsWindowDays", s.newsWindowDays)
            .putBoolean("clusterDuplicates", s.clusterDuplicates)
            .putInt("newsPerCompany", s.newsPerCompany)
            .putBoolean("showTone", s.showTone)
            .putBoolean("showEventTags", s.showEventTags)
            .putInt("defaultRangeIndex", s.defaultRangeIndex)
            .putBoolean("chartFill", s.chartFill)
            .putBoolean("chartGlow", s.chartGlow)
            .putBoolean("earningsDots", s.earningsDots)
            .putBoolean("show52Week", s.show52Week)
            .putBoolean("compactRows", s.compactRows)
            .putBoolean("sortWatchlistAlphabetically", s.sortWatchlistAlphabetically)
            .putString("aiModel", s.aiModel)
            .apply()
    }
}

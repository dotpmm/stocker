package com.dotpmm.stocker.research

/**
 * The editable, persisted filter set for the sector table (FUNCTIONALITY.md §3.2).
 *
 * Shipped defaults: Coffee-Can Core ON, everything else off, sort by market cap. The state
 * survives reloads under the `cc-filters` key; resetting restores exactly these defaults.
 */
data class SectorFilters(
    val core: Boolean = true,
    val monopoly: Boolean = false,
    val cultural: Boolean = false,
    val defensive: Boolean = false,
    val peBelowOwn: Boolean = false,
    val peBelow20: Boolean = false,
    val cagrAbove15: Boolean = false,
    val largeCap: Boolean = false,
    val moat: String = "Any",
    val sort: SortBy = SortBy.MktCap,
) {
    companion object {
        /** The values a reset restores — never a blank table. */
        val DEFAULT = SectorFilters()

        const val LARGE_CAP_CR = 50_000.0
        const val PE_CAP = 20.0
        const val CAGR_FLOOR = 15.0
    }
}

enum class SortBy(val label: String) {
    MktCap("Mkt cap"),
    PeAsc("P/E"),
    CagrDesc("CAGR"),
    Ticker("A–Z"),
}

/**
 * Pure filter + sort. Kept out of the composable so the semantics are unit-testable.
 *
 * Missing data never silently passes a filter: P/E sorts push nulls to the end, and CAGR /
 * large-cap comparisons treat null as the worst possible value (FUNCTIONALITY.md §3.2).
 */
fun applyFilters(rows: List<CompanyRow>, filters: SectorFilters): List<CompanyRow> {
    val filtered = rows.filter { row ->
        (!filters.core || row.isCore) &&
            (!filters.monopoly || row.monopoly) &&
            (!filters.cultural || row.cultural) &&
            (!filters.defensive || row.defensive) &&
            (!filters.peBelowOwn || row.peBelowOwnAvg) &&
            (!filters.peBelow20 || (row.pe != null && row.pe in 0.0..SectorFilters.PE_CAP)) &&
            (!filters.cagrAbove15 || ((row.revCagr5y ?: Double.NEGATIVE_INFINITY) > SectorFilters.CAGR_FLOOR)) &&
            (!filters.largeCap || ((row.mcapCr ?: 0.0) >= SectorFilters.LARGE_CAP_CR)) &&
            (filters.moat == "Any" || row.moat == filters.moat)
    }
    return when (filters.sort) {
        SortBy.MktCap -> filtered.sortedByDescending { it.mcapCr ?: 0.0 }
        // Nulls sort away rather than masquerading as the cheapest.
        SortBy.PeAsc -> filtered.sortedBy { it.pe ?: 1e9 }
        SortBy.CagrDesc -> filtered.sortedByDescending { it.revCagr5y ?: Double.NEGATIVE_INFINITY }
        SortBy.Ticker -> filtered.sortedBy { it.symbol }
    }
}

private const val PREFS_KEY = "cc-filters"

/** Restores the persisted mix over the shipped defaults; missing keys fall back to defaults. */
fun loadFilters(context: android.content.Context): SectorFilters {
    val prefs = context.getSharedPreferences(PREFS_KEY, android.content.Context.MODE_PRIVATE)
    val defaults = SectorFilters.DEFAULT
    return SectorFilters(
        core = prefs.getBoolean("core", defaults.core),
        monopoly = prefs.getBoolean("monopoly", defaults.monopoly),
        cultural = prefs.getBoolean("cultural", defaults.cultural),
        defensive = prefs.getBoolean("defensive", defaults.defensive),
        peBelowOwn = prefs.getBoolean("peBelowOwn", defaults.peBelowOwn),
        peBelow20 = prefs.getBoolean("peBelow20", defaults.peBelow20),
        cagrAbove15 = prefs.getBoolean("cagrAbove15", defaults.cagrAbove15),
        largeCap = prefs.getBoolean("largeCap", defaults.largeCap),
        moat = prefs.getString("moat", defaults.moat) ?: defaults.moat,
        sort = runCatching { SortBy.valueOf(prefs.getString("sort", defaults.sort.name)!!) }
            .getOrDefault(defaults.sort),
    )
}

/** Persists under the single `cc-filters` key so the mix survives reloads (§12 rule 16). */
fun SectorFilters.persist(context: android.content.Context) {
    context.getSharedPreferences(PREFS_KEY, android.content.Context.MODE_PRIVATE).edit()
        .putBoolean("core", core)
        .putBoolean("monopoly", monopoly)
        .putBoolean("cultural", cultural)
        .putBoolean("defensive", defensive)
        .putBoolean("peBelowOwn", peBelowOwn)
        .putBoolean("peBelow20", peBelow20)
        .putBoolean("cagrAbove15", cagrAbove15)
        .putBoolean("largeCap", largeCap)
        .putString("moat", moat)
        .putString("sort", sort.name)
        .apply()
}

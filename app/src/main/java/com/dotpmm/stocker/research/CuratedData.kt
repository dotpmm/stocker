package com.dotpmm.stocker.research

/** One curated constituent — the qualitative layer (moat taxonomy + Coffee-Can flags). */
data class CuratedCompany(
    val symbol: String,
    val name: String,
    val moat: String = "None",
    val monopoly: Boolean = false,
    val cultural: Boolean = false,
    val defensive: Boolean = false,
    val note: String = "",
) {
    /** Monopoly OR cultural OR defensive — the "Coffee-Can Core" filter definition. */
    val isCore: Boolean get() = monopoly || cultural || defensive
}

data class CuratedSector(
    val id: String,
    val name: String,
    val indexSymbol: String,
    val pe5yAvg: Double,
    val blurb: String,
    val companies: List<CuratedCompany>,
)

/** A provider → consumer edge in the interdependence graph. */
data class Dependency(val from: String, val to: String, val weight: Double)

/** One Nifty-200 entry. [industry] is the raw NSE classification. */
data class UniverseStock(val symbol: String, val name: String, val industry: String)

/** The static curated layer, loaded from `assets/curated.json`. Product logic — port verbatim. */
data class CuratedData(
    val sectorOrder: List<String>,
    val sectors: List<CuratedSector>,
    val dependencies: List<Dependency>,
    val narratives: Map<String, String>,
    val fallbackNarrative: String,
) {
    val byId: Map<String, CuratedSector> = sectors.associateBy { it.id }

    private val symbolIndex: Map<String, Pair<CuratedSector, CuratedCompany>> = buildMap {
        for (sector in sectors) for (company in sector.companies) put(company.symbol, sector to company)
    }

    fun find(symbol: String): Pair<CuratedSector, CuratedCompany>? = symbolIndex[symbol]

    /** Sector id for an NSE industry label, or null when it maps to no macro bucket. */
    fun sectorForIndustry(industry: String): String? = INDUSTRY_MAP[industry]

    /** Verbatim copy of the reference narrative fallback (FUNCTIONALITY.md §2.1). */
    fun narrativeFor(
        symbol: String,
        name: String,
        sectorName: String,
        moat: String,
        monopoly: Boolean,
        cultural: Boolean,
        defensive: Boolean,
    ): String {
        narratives[symbol]?.let { return it }
        val flags = buildString {
            if (monopoly) append(" It enjoys monopoly/duopoly economics.")
            if (cultural) append(" It carries a cultural moat.")
            if (defensive) append(" It is a defensive anchor.")
        }
        return fallbackNarrative
            .replace("{name}", name)
            .replace("{sector}", sectorName)
            .replace("{moat}", moat)
            .replace("{flags}", flags)
    }

    companion object {
        /** NSE industry → macro sector. Industries absent here stay searchable but ungrouped. */
        val INDUSTRY_MAP: Map<String, String> = mapOf(
            "Financial Services" to "banking",
            "Information Technology" to "it",
            "Telecommunication" to "it",
            "Power" to "power",
            "Capital Goods" to "power",
            "Oil Gas & Consumable Fuels" to "power",
            "Construction" to "power",
            "Construction Materials" to "power",
            "Fast Moving Consumer Goods" to "fmcg",
            "Consumer Services" to "retail",
            "Consumer Durables" to "retail",
            "Automobile and Auto Components" to "retail",
            "Textiles" to "retail",
            "Healthcare" to "pharma",
        )

        /** Yahoo's own sector label → P/E baseline for uncurated deep-dives (FUNCTIONALITY.md §2.2). */
        val YF_SECTOR_PE: Map<String, Double> = mapOf(
            "Financial Services" to 16.0, "Technology" to 26.0, "Healthcare" to 30.0,
            "Consumer Defensive" to 38.0, "Consumer Cyclical" to 42.0, "Consumer Durables" to 42.0,
            "Consumer Services" to 42.0, "Energy" to 12.0, "Utilities" to 20.0,
            "Industrials" to 30.0, "Basic Materials" to 25.0, "Real Estate" to 40.0,
            "Communication Services" to 30.0, "Automobile" to 24.0,
        )

        const val DEFAULT_SECTOR_PE = 25.0
    }
}

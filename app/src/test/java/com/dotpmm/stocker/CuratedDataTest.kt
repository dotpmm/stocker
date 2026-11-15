package com.dotpmm.stocker

import com.dotpmm.stocker.research.CuratedCompany
import com.dotpmm.stocker.research.CuratedData
import com.dotpmm.stocker.research.CuratedSector
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * The curated layer is shipped product logic (FUNCTIONALITY.md §2.1). These tests read the actual
 * APK asset so a bad edit to `curated.json` fails the build rather than silently shipping.
 */
class CuratedDataTest {

    companion object {
        private lateinit var data: CuratedData
        private lateinit var universe: List<JSONObject>

        @BeforeClass
        @JvmStatic
        fun load() {
            val curated = File("src/main/assets/curated.json").readText()
            val root = JSONObject(curated)
            val order = root.getJSONArray("sector_order").let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }
            val sectors = root.getJSONArray("sectors").let { arr ->
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    val cos = o.getJSONArray("companies")
                    CuratedSector(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        indexSymbol = o.getString("index_symbol"),
                        pe5yAvg = o.getDouble("pe_5y_avg"),
                        blurb = o.optString("blurb"),
                        companies = (0 until cos.length()).map { j ->
                            val c = cos.getJSONObject(j)
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
            }
            val deps = root.getJSONArray("dependencies").let { arr ->
                (0 until arr.length()).map { i ->
                    val d = arr.getJSONObject(i)
                    Triple(d.getString("from"), d.getString("to"), d.getDouble("w"))
                }
            }
            val narrativesObj = root.getJSONObject("narratives")
            data = CuratedData(
                sectorOrder = order,
                sectors = sectors,
                dependencies = deps.map { com.dotpmm.stocker.research.Dependency(it.first, it.second, it.third) },
                narratives = narrativesObj.keys().asSequence().associateWith { narrativesObj.getString(it) },
                fallbackNarrative = root.getString("fallback_narrative"),
            )
            universe = JSONObject(File("src/main/assets/nifty200.json").readText())
                .getJSONArray("stocks")
                .let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
        }
    }

    @Test
    fun `there are exactly six macro sectors in the documented order`() {
        assertEquals(
            listOf("fmcg", "it", "power", "retail", "banking", "pharma"),
            data.sectorOrder,
        )
        assertEquals(6, data.sectors.size)
    }

    @Test
    fun `sector identities and baselines match the spec table`() {
        val expected = mapOf(
            "fmcg" to ("FMCG" to 38.0),
            "it" to ("Information Technology" to 26.0),
            "power" to ("Power & Capital Goods" to 30.0),
            "retail" to ("Retail & Discretionary" to 42.0),
            "banking" to ("Banking & Financials" to 16.0),
            "pharma" to ("Pharma & Healthcare" to 30.0),
        )
        for ((id, pair) in expected) {
            val sector = data.byId[id]!!
            assertEquals(pair.first, sector.name)
            assertEquals(pair.second, sector.pe5yAvg, 1e-9)
        }
    }

    @Test
    fun `index symbols are raw Yahoo indices never nse suffixed`() {
        val expected = mapOf(
            "fmcg" to "^CNXFMCG",
            "it" to "^CNXIT",
            "power" to "^CNXINFRA",
            "retail" to "^CNXCONSUM",
            "banking" to "^NSEBANK",
            "pharma" to "^CNXPHARMA",
        )
        for ((id, symbol) in expected) {
            assertEquals(symbol, data.byId[id]!!.indexSymbol)
            assertTrue("index symbols must start with ^", data.byId[id]!!.indexSymbol.startsWith("^"))
        }
    }

    @Test
    fun `curated constituent counts match the documented table`() {
        val expected = mapOf(
            "fmcg" to 8, "it" to 8, "power" to 10,
            "retail" to 8, "banking" to 8, "pharma" to 8,
        )
        for ((id, count) in expected) {
            assertEquals("$id constituent count", count, data.byId[id]!!.companies.size)
        }
    }

    @Test
    fun `exactly eight dependency edges with the documented weights`() {
        assertEquals(8, data.dependencies.size)
        val expected = setOf(
            Triple("banking", "power", 0.8),
            Triple("banking", "retail", 0.7),
            Triple("it", "banking", 0.6),
            Triple("retail", "fmcg", 0.5),
            Triple("banking", "fmcg", 0.4),
            Triple("retail", "pharma", 0.4),
            Triple("it", "retail", 0.3),
            Triple("power", "fmcg", 0.2),
        )
        val actual = data.dependencies.map { Triple(it.from, it.to, it.weight) }.toSet()
        assertEquals(expected, actual)
    }

    @Test
    fun `every dependency endpoint is a real sector`() {
        for (dep in data.dependencies) {
            assertNotNull("missing sector ${dep.from}", data.byId[dep.from])
            assertNotNull("missing sector ${dep.to}", data.byId[dep.to])
        }
    }

    @Test
    fun `every symbol resolves to exactly one sector`() {
        val seen = HashMap<String, String>()
        for (sector in data.sectors) {
            for (company in sector.companies) {
                val previous = seen.put(company.symbol, sector.id)
                assertNull("${company.symbol} mapped twice ($previous and ${sector.id})", previous)
            }
        }
        assertEquals(50, seen.size)
    }

    @Test
    fun `moat values are drawn from the Coffee-Can taxonomy`() {
        val allowed = setOf(
            "Brand Trust", "Cost Advantage", "Network Effect",
            "Switching Cost", "Regulatory Barrier", "Scale", "None",
        )
        for (sector in data.sectors) {
            for (company in sector.companies) {
                assertTrue("bad moat '${company.moat}' on ${company.symbol}", company.moat in allowed)
            }
        }
    }

    @Test
    fun `every sector has a usable editorial blurb`() {
        for (sector in data.sectors) {
            assertTrue("${sector.id} blurb too short", sector.blurb.length > 40)
        }
    }

    @Test
    fun `core flag is monopoly or cultural or defensive`() {
        val itc = data.find("ITC")!!.second
        assertTrue("ITC is a monopoly + defensive", itc.isCore)
        assertTrue(itc.monopoly && itc.defensive)

        val titan = data.find("TITAN")!!.second
        assertTrue("Titan carries the cultural moat", titan.cultural)
        assertTrue(titan.isCore)

        val wipro = data.find("WIPRO")!!.second
        assertFalse("Wipro is none of the three core flags", wipro.isCore)
    }

    @Test
    fun `reliance keeps its conglomerate note`() {
        val reliance = data.find("RELIANCE")!!.second
        assertTrue(reliance.note.contains("Conglomerate proxy"))
    }

    @Test
    fun `curated narratives are used verbatim when present`() {
        val sector = data.find("TITAN")!!.first
        val company = data.find("TITAN")!!.second
        val narrative = data.narrativeFor("TITAN", company.name, sector.name, company.moat, false, true, false)
        assertTrue(narrative.startsWith("Titan runs Tanishq"))
        assertEquals(data.narratives["TITAN"], narrative)
    }

    @Test
    fun `fallback narrative interpolates all four placeholders`() {
        // WIPRO has no hand-written narrative, so the template runs.
        val sector = data.find("WIPRO")!!.first
        val company = data.find("WIPRO")!!.second
        val narrative = data.narrativeFor("WIPRO", company.name, sector.name, company.moat, false, false, false)
        assertTrue("name substituted", narrative.contains(company.name))
        assertTrue("sector substituted", narrative.contains(sector.name))
        assertTrue("moat substituted", narrative.contains(company.moat))
        assertFalse("no raw placeholders left", narrative.contains("{"))
    }

    @Test
    fun `fallback narrative appends the flags that apply`() {
        // POLYCAB is a monopoly with no hand-written narrative, so the template runs.
        val sector = data.find("POLYCAB")!!.first
        val company = data.find("POLYCAB")!!.second
        assertTrue("POLYCAB should be a monopoly", company.monopoly)
        val narrative = data.narrativeFor("POLYCAB", company.name, sector.name, company.moat, true, false, false)
        assertTrue(narrative.contains("monopoly/duopoly economics"))
        assertFalse(narrative.contains("cultural moat"))
        assertFalse(narrative.contains("defensive anchor"))
    }

    @Test
    fun `fallback narrative stays silent when no flag applies`() {
        val sector = data.find("WIPRO")!!.first
        val company = data.find("WIPRO")!!.second
        val narrative = data.narrativeFor("WIPRO", company.name, sector.name, company.moat, false, false, false)
        assertFalse(narrative.contains("monopoly/duopoly economics"))
        assertFalse(narrative.contains("cultural moat"))
        assertFalse(narrative.contains("defensive anchor"))
    }

    @Test
    fun `industry map folds NSE labels into the six sectors`() {
        val map = CuratedData.INDUSTRY_MAP
        assertEquals("banking", map["Financial Services"])
        assertEquals("it", map["Information Technology"])
        assertEquals("it", map["Telecommunication"])
        assertEquals("power", map["Oil Gas & Consumable Fuels"])
        assertEquals("fmcg", map["Fast Moving Consumer Goods"])
        assertEquals("retail", map["Automobile and Auto Components"])
        assertEquals("pharma", map["Healthcare"])
        // Metals and Realty are deliberately ungrouped but still searchable.
        assertNull(map["Metals & Mining"])
        assertNull(map["Realty"])
    }

    @Test
    fun `industry map only targets real sectors`() {
        for ((_, sid) in CuratedData.INDUSTRY_MAP) {
            assertNotNull("industry maps to unknown sector $sid", data.byId[sid])
        }
    }

    @Test
    fun `yahoo sector pe baselines cover the labels we can receive`() {
        val map = CuratedData.YF_SECTOR_PE
        assertEquals(16.0, map["Financial Services"]!!, 1e-9)
        assertEquals(26.0, map["Technology"]!!, 1e-9)
        assertEquals(42.0, map["Consumer Cyclical"]!!, 1e-9)
        assertEquals(24.0, map["Automobile"]!!, 1e-9)
        assertEquals(25.0, CuratedData.DEFAULT_SECTOR_PE, 1e-9)
    }

    // ── Universe layer ───────────────────────────────────────────

    @Test
    fun `the universe ships two hundred nifty names`() {
        assertEquals(200, universe.size)
    }

    @Test
    fun `every universe row has a symbol and industry`() {
        for (row in universe) {
            assertTrue("symbol missing", row.optString("symbol").isNotEmpty())
            assertTrue("industry missing", row.optString("industry").isNotEmpty())
        }
    }

    @Test
    fun `universe symbols are unique`() {
        val symbols = universe.map { it.getString("symbol") }
        assertEquals(symbols.size, symbols.toSet().size)
    }

    @Test
    fun `every mapped industry in the universe belongs to a real sector`() {
        for (row in universe) {
            val sid = CuratedData.INDUSTRY_MAP[row.getString("industry")] ?: continue
            assertNotNull("universe maps to unknown sector $sid", data.byId[sid])
        }
    }

    @Test
    fun `banking is the largest mapped sector which the spec calls out`() {
        val counts = HashMap<String, Int>()
        for (row in universe) {
            val sid = CuratedData.INDUSTRY_MAP[row.getString("industry")] ?: continue
            counts[sid] = (counts[sid] ?: 0) + 1
        }
        assertEquals("banking", counts.maxByOrNull { it.value }!!.key)
    }

    @Test
    fun `curated names also live in the universe so rows are not duplicated`() {
        val universeSymbols = universe.map { it.getString("symbol") }.toSet()
        // A handful of curated names sit outside Nifty-200, but the overlap must be large.
        val curated = data.sectors.flatMap { s -> s.companies.map { it.symbol } }
        val overlap = curated.count { it in universeSymbols }
        assertTrue("expected most curated names to be in Nifty-200, got $overlap/50", overlap >= 40)
    }
}

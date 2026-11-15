package com.dotpmm.stocker

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.dotpmm.stocker.research.Band
import com.dotpmm.stocker.research.CompanyRow
import com.dotpmm.stocker.research.Dependency
import com.dotpmm.stocker.research.EarningsDot
import com.dotpmm.stocker.research.MacroSnapshot
import com.dotpmm.stocker.research.Point
import com.dotpmm.stocker.research.Reality
import com.dotpmm.stocker.research.SectorCard
import com.dotpmm.stocker.research.SectorDetail
import com.dotpmm.stocker.research.StockDeep
import com.dotpmm.stocker.research.Verdict
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Fixture data shaped exactly like the live engine's payloads, for render verification. */
object ScreenFixtures {
val indices = listOf(
        com.dotpmm.stocker.domain.model.MarketIndex("NIFTY 50", 25144.3, -0.61),
        com.dotpmm.stocker.domain.model.MarketIndex("SENSEX", 82110.5, -0.42),
    )

    val watchRows = listOf(
        com.dotpmm.stocker.domain.model.StockRow(
            com.dotpmm.stocker.domain.model.WatchStock(
                "RELIANCE", "Reliance Industries", com.dotpmm.stocker.domain.model.Market.India,
                "RELIANCE.NS", "NSE",
            ),
            com.dotpmm.stocker.domain.model.Quote(1339.7, -1.24, wave(1.0, 22, 1350.0, 12.0)),
            7, com.dotpmm.stocker.domain.model.Tone.Positive,
        ),
        com.dotpmm.stocker.domain.model.StockRow(
            com.dotpmm.stocker.domain.model.WatchStock(
                "HDFCBANK", "HDFC Bank", com.dotpmm.stocker.domain.model.Market.India,
                "HDFCBANK.NS", "NSE",
            ),
            com.dotpmm.stocker.domain.model.Quote(804.0, 0.86, wave(2.0, 22, 800.0, 8.0)),
            4, com.dotpmm.stocker.domain.model.Tone.Neutral,
        ),
        com.dotpmm.stocker.domain.model.StockRow(
            com.dotpmm.stocker.domain.model.WatchStock(
                "TCS", "Tata Consultancy Services", com.dotpmm.stocker.domain.model.Market.India,
                "TCS.NS", "NSE",
            ),
            com.dotpmm.stocker.domain.model.Quote(2407.5, 1.42, wave(3.0, 22, 2380.0, 20.0)),
            11, com.dotpmm.stocker.domain.model.Tone.Negative,
        ),
    )

    val newsItems = listOf(
        "Reliance Retail lines up ₹8,000 crore expansion across tier-2 cities",
        "HDFC Bank raises ₹15,000 crore via infrastructure bonds",
        "TCS wins multi-year deal with European insurer",
        "SEBI tightens disclosure norms for listed subsidiaries",
        "Maruti reports record monthly dispatches on SUV demand",
    ).mapIndexed { i, title ->
        com.dotpmm.stocker.domain.model.NewsItem(
            com.dotpmm.stocker.domain.model.Article(
                id = "a$i",
                title = title,
                summary = "",
                url = "https://example.com/$i",
                source = listOf("Economic Times", "Mint", "BusinessLine", "NDTV Profit", "SEBI")[i],
                publishedAt = System.currentTimeMillis() - (i + 1) * 3_600_000L,
                tag = listOf("Results", "Rating", "Order win", "Regulatory", "M&A")[i],
                tone = listOf(
                    com.dotpmm.stocker.domain.model.Tone.Positive,
                    com.dotpmm.stocker.domain.model.Tone.Neutral,
                    com.dotpmm.stocker.domain.model.Tone.Positive,
                    com.dotpmm.stocker.domain.model.Tone.Negative,
                    com.dotpmm.stocker.domain.model.Tone.Neutral,
                )[i],
                credibility = 0.9,
                cluster = "c$i",
            ),
            tickers = listOf("RELIANCE", "HDFCBANK", "TCS", "SBIN", "MARUTI").subList(i, i + 1),
            extraSources = if (i % 2 == 0) listOf("Mint", "BusinessLine") else emptyList(),
            impact = 0.8,
        )
    }

    private fun wave(seed: Double, count: Int = 120, base: Double = 100.0, amp: Double = 8.0): List<Float> =
        (0 until count).map { i ->
            (base + Math.sin((i + seed) / 7.0) * amp + i * (amp / count)).toFloat()
        }

    val sectors = listOf(
        SectorCard("fmcg", "FMCG", 40.9, 38.0, -6.8, Verdict.Consolidating, 8, "Staples compound through cycles.", wave(0.0)),
        SectorCard("it", "Information Technology", 20.3, 26.0, 10.4, Verdict.Discounted, 8, "Switching-cost annuities.", wave(1.0)),
        SectorCard("power", "Power & Capital Goods", 52.0, 30.0, 1.3, Verdict.Overheated, 10, "India's capex cycle.", wave(2.0)),
        SectorCard("retail", "Retail & Discretionary", 37.6, 42.0, 5.8, Verdict.Discounted, 8, "Unorganized to organized.", wave(3.0)),
        SectorCard("banking", "Banking & Financials", 17.0, 16.0, 3.8, Verdict.Consolidating, 8, "A leveraged moat.", wave(4.0)),
        SectorCard("pharma", "Pharma & Healthcare", 49.4, 30.0, 16.1, Verdict.Overheated, 8, "US generics knife-fight.", wave(5.0)),
    )

    val macro = MacroSnapshot(
        hero = com.dotpmm.stocker.research.Hero(25144.3, -0.61, wave(0.5, 60, 24000.0, 900.0)),
        sectors = sectors,
        dependencies = listOf(
            Dependency("banking", "power", 0.8),
            Dependency("banking", "retail", 0.7),
            Dependency("it", "banking", 0.6),
            Dependency("retail", "fmcg", 0.5),
            Dependency("banking", "fmcg", 0.4),
            Dependency("retail", "pharma", 0.4),
            Dependency("it", "retail", 0.3),
            Dependency("power", "fmcg", 0.2),
        ),
    )

    val companyRows = listOf(
        CompanyRow("HDFCBANK", "HDFC Bank", 17.57, 16.8, false, 1240756.0, 27.5, "Scale", false, true, true, "", true),
        CompanyRow("ICICIBANK", "ICICI Bank", 19.1, 18.4, false, 1137160.0, 24.1, "Scale", false, false, true, "", true),
        CompanyRow("SBIN", "State Bank of India", 11.2, 9.8, false, 996907.0, 18.9, "Scale", false, false, false, "", true),
        CompanyRow("BAJFINANCE", "Bajaj Finance", 31.4, 34.2, true, 695210.0, 32.4, "None", false, false, false, "", false),
        CompanyRow("LICI", "Life Insurance Corp", 9.8, null, false, 505114.0, 6.2, "None", false, false, false, "", false),
        CompanyRow("KOTAKBANK", "Kotak Mahindra Bank", 22.6, 25.1, true, 486027.0, 21.0, "Brand Trust", false, true, true, "", true),
        CompanyRow("AXISBANK", "Axis Bank", 12.4, 13.9, true, 414537.0, 16.7, "Scale", false, false, false, "", true),
        CompanyRow("BAJAJFINSV", "Bajaj Finserv", 28.9, 30.2, true, 298446.0, 25.5, "None", false, false, false, "", false),
    )

    val sectorDetail = SectorDetail(
        id = "banking",
        name = "Banking & Financials",
        pe5yAvg = 16.0,
        blurb = "A bank is a leveraged moat: deposit franchise is everything. Cheap on book " +
            "rarely means cheap in reality — watch credit costs, not the P/B trap.",
        sparkline = wave(4.0, 120, 58000.0, 3000.0),
        companies = companyRows,
        nCurated = 8,
        nUniverse = 49,
    )

    private val history = (0 until 248).map { i ->
        val day = java.time.LocalDate.of(2025, 11, 13).plusDays(i.toLong())
        Point(day.toString(), 3826.64 + Math.sin(i / 12.0) * 240 + i * 4.2)
    }

    val stock = StockDeep(
        symbol = "TITAN",
        name = "Titan Company",
        sector = "Retail & Discretionary",
        sectorId = "retail",
        curated = true,
        note = "",
        ltp = 4844.9,
        chgPct = -1.56,
        pe = 61.15,
        pe5yOwn = 93.1,
        peSectorAvg = 42.0,
        mcapCr = 429894.0,
        revCagr5y = 29.2,
        moat = "Brand Trust",
        monopoly = false,
        cultural = true,
        defensive = false,
        narrative = "Titan runs Tanishq, the jewelry brand that converted India's unorganized " +
            "gold market into a trusted, hallmarking-led chain — a textbook " +
            "unorganized-to-organized transition. It belongs in a Coffee Can because the brand " +
            "compounds family savings into annuity-like store economics, but you pay a permanent " +
            "premium for that quality.",
        reality = Reality("DISCOUNTED", Band.Under, 0.6969, 42.0),
        priceHistory = history,
        earningsDots = listOf(
            EarningsDot("2025-11-03", 3.5, 1.0),
            EarningsDot("2026-02-10", 4.7, -2.1),
            EarningsDot("2026-05-08", -15.0, -9.3),
            EarningsDot("2026-08-07", 39.7, 3.2),
            EarningsDot("2026-11-05", 18.9, 2.8),
        ),
        sparkline = history.takeLast(60).map { it.c.toFloat() },
    )
}

/**
 * Renders the three real screens with fixture data. This is how the design is verified on a
 * machine where the Android emulator cannot run: real Compose measure/layout/draw, real pixels.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class, qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenRenderTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File("build/screenshots").apply { mkdirs() }

    private fun shoot(name: String) {
        compose.waitForIdle()
        val activity = compose.activity
        activity.runOnUiThread {
            val root = activity.window.decorView
            val width = if (root.width > 0) root.width else 1080
            val height = if (root.height > 0) root.height else 2340
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            File(outDir, "$name.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `macro screen renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.MacroScreenContent(
                    snapshot = ScreenFixtures.macro,
                    loading = false,
                    error = null,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    onOpenSector = {},
                    onOpenStock = {},
                    repository = null,
                )
            }
        }
        shoot("10-macro")
    }

    @Test
    fun `sector screen renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.SectorScreenContent(
                    detail = ScreenFixtures.sectorDetail,
                    rows = ScreenFixtures.companyRows,
                    loading = false,
                    contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
                    onBack = {},
                    onOpenStock = {},
                    filtersOpen = true,
                )
            }
        }
        shoot("11-sector")
    }

    @Test
    fun `deep dive sheet renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.DeepDiveContent(
                    deep = ScreenFixtures.stock,
                    seriesPoints = ScreenFixtures.stock.priceHistory,
                    seriesDots = ScreenFixtures.stock.earningsDots,
                    rangeIndex = 3,
                    onRangeSelected = {},
                    onClose = {},
                    onOpenSector = {},
                    onGoogleFinance = {},
                )
            }
        }
        shoot("12-deepdive")
    }

    @Test
    fun `interdependence graph renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    com.dotpmm.stocker.ui.components.InterdependenceGraph(
                        sectors = ScreenFixtures.macro.sectors,
                        dependencies = ScreenFixtures.macro.dependencies,
                        onOpenSector = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        shoot("13-graph")
    }

    @Test
    fun `news and research screen renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.ResearchBrowseContent(
                    sectors = ScreenFixtures.macro.sectors.map { card ->
                        com.dotpmm.stocker.research.CuratedSector(
                            id = card.id,
                            name = card.name,
                            indexSymbol = "^CNX",
                            pe5yAvg = card.pe5y,
                            blurb = card.blurb,
                            companies = ScreenFixtures.companyRows.take(4).map { row ->
                                com.dotpmm.stocker.research.CuratedCompany(
                                    symbol = row.symbol,
                                    name = row.name,
                                    moat = row.moat,
                                    monopoly = row.monopoly,
                                    cultural = row.cultural,
                                    defensive = row.defensive,
                                )
                            },
                        )
                    },
                    universeCount = 200,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    onOpenStock = {},
                )
            }
        }
        shoot("14-research")
    }

    @Test
    fun `sector table rows render`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.SectorScreenContent(
                    detail = ScreenFixtures.sectorDetail,
                    rows = ScreenFixtures.companyRows,
                    loading = false,
                    contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
                    onBack = {},
                    onOpenStock = {},
                    filtersOpen = false,
                )
            }
        }
        shoot("11b-sector-table")
    }

    @Test
    fun `macro offline state renders with an explanation`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.MacroScreenContent(
                    snapshot = com.dotpmm.stocker.research.MacroSnapshot(
                        hero = com.dotpmm.stocker.research.Hero(null, null, emptyList()),
                        sectors = emptyList(),
                        dependencies = ScreenFixtures.macro.dependencies,
                        staleNote = "Couldn't reach the market data. Check your connection — cached " +
                            "values will reappear as soon as one request succeeds.",
                    ),
                    loading = false,
                    error = null,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    onOpenSector = {},
                    onOpenStock = {},
                    repository = null,
                )
            }
        }
        shoot("15-macro-offline")
    }

    @Test
    fun `macro loading state renders skeletons`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.MacroScreenContent(
                    snapshot = null,
                    loading = true,
                    error = null,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    onOpenSector = {},
                    onOpenStock = {},
                    repository = null,
                )
            }
        }
        shoot("16-macro-loading")
    }

    @Test
    fun `watchlist renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.WatchlistContent(
                    rows = ScreenFixtures.watchRows,
                    indices = ScreenFixtures.indices,
                    status = "Updated 2h ago",
                    refreshing = false,
                    error = null,
                    onOpenStock = {},
                    onRefresh = {},
                    onAddStock = {},
                    contentPadding = PaddingValues(bottom = 24.dp),
                )
            }
        }
        shoot("20-watchlist")
    }

    @Test
    fun `news feed renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.NewsFeedContent(
                    items = ScreenFixtures.newsItems,
                    watchlistEmpty = false,
                    windowDays = 7,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    onOpenArticle = {},
                    onRefresh = {},
                )
            }
        }
        shoot("21-news")
    }

    @Test
    fun `settings renders`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme {
                com.dotpmm.stocker.ui.screens.SettingsPreviewContent()
            }
        }
        shoot("22-settings")
    }

    @Test
    fun `amoled tone renders much darker`() {
        compose.setContent {
            com.dotpmm.stocker.ui.theme.StockerTheme(
                settings = com.dotpmm.stocker.settings.AppSettings(
                    baseTone = com.dotpmm.stocker.settings.BaseTone.Amoled,
                ),
            ) {
                com.dotpmm.stocker.ui.screens.MacroScreenContent(
                    snapshot = ScreenFixtures.macro,
                    loading = false,
                    error = null,
                    contentPadding = PaddingValues(bottom = 24.dp),
                    onOpenSector = {},
                    onOpenStock = {},
                    repository = null,
                )
            }
        }
        shoot("23-macro-amoled")
    }
}

package com.dotpmm.stocker

import com.dotpmm.stocker.data.net.YahooResearch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Live integration checks against the real Yahoo endpoints.
 *
 * These are opt-in (`-Dstocker.live=true`) because they need the network and the public endpoints
 * are unofficial — a red run here means Yahoo changed something, not that the app is broken.
 * Everything asserted is a shape the UI depends on.
 */
class LiveDataTest {

    private val live: Boolean = System.getProperty("stocker.live", "false").toBoolean()

    @Test
    fun `nse symbol helper never suffixes indices`() {
        assertEquals("RELIANCE.NS", YahooResearch.nseSymbol("RELIANCE"))
        assertEquals("^NSEI", YahooResearch.nseSymbol("^NSEI"))
        assertEquals("^CNXIT", YahooResearch.nseSymbol("^CNXIT"))
        assertEquals("^CNXFMCG", YahooResearch.nseSymbol("^CNXFMCG"))
    }

    @Test
    fun `daily history parses into dated closes`() = live {
        val payload = YahooResearch.history("RELIANCE", "1y", "1d")
        assertNotNull(payload)
        assertTrue("history has points", payload!!.closes.size > 100)
        assertTrue("daily stamps have no time part", payload.closes.all { !it.first.contains(' ') })
        assertTrue("closes are positive", payload.closes.all { it.second > 0 })
    }

    @Test
    fun `intraday history stamps carry a time`() = live {
        val payload = YahooResearch.history("RELIANCE", "5d", "15m")
        assertNotNull(payload)
        assertTrue("5d has bars", payload!!.closes.isNotEmpty())
        assertTrue("15m stamps include HH:MM", payload.closes.all { it.first.contains(' ') })
    }

    @Test
    fun `index history works without an nse suffix`() = live {
        val payload = YahooResearch.history("^NSEI", "1y", "1d")
        assertNotNull("^NSEI must resolve raw, never ^NSEI.NS", payload)
        assertTrue(payload!!.closes.size > 100)
    }

    @Test
    fun `profile returns pe and market cap for a curated name`() = live {
        val profile = YahooResearch.profile("TITAN")
        assertNotNull("profile requires the cookie/crumb handshake", profile)
        assertNotNull("trailing P/E present", profile!!.pe)
        assertNotNull("market cap present", profile.mcap)
        assertTrue("P/E is plausible", profile.pe!! in 1.0..500.0)
        assertTrue("market cap is plausible", profile.mcap!! > 1e11)
    }

    @Test
    fun `batched quotes return fundamentals for many symbols at once`() = live {
        val symbols = listOf("HDFCBANK", "ICICIBANK", "SBIN", "KOTAKBANK", "AXISBANK")
        val quotes = YahooResearch.quotes(symbols)
        assertTrue("batch returned rows", quotes.size >= 4)
        val hdfc = quotes["HDFCBANK"]
        assertNotNull(hdfc)
        assertNotNull("batched P/E", hdfc!!.pe)
        assertNotNull("batched mcap", hdfc.mcap)
    }

    @Test
    fun `financials give a revenue cagr and annual eps`() = live {
        val fin = YahooResearch.financials("TITAN")
        assertNotNull(fin)
        assertNotNull("5Y CAGR computed", fin!!.cagr)
        assertTrue("EPS history present", fin.epsByYear.isNotEmpty())
        assertTrue("annual EPS years look sane", fin.epsByYear.all { it.first in 2015..2035 })
    }

    @Test
    fun `earnings returns dated surprises`() = live {
        val earnings = YahooResearch.earnings("TITAN")
        assertTrue("some earnings dates", earnings.isNotEmpty())
        assertTrue("dates are ISO", earnings.all { it.first.matches(Regex("""\d{4}-\d{2}-\d{2}""")) })
        assertTrue("at least one surprise reported", earnings.any { it.second != null })
    }

    @Test
    fun `search finds nse names and filters out foreign listings`() = live {
        val hits = YahooResearch.search("titan")
        assertTrue("some hits", hits.isNotEmpty())
        val onNse = hits.firstOrNull { it.symbol == "TITAN" }
        assertNotNull("Titan resolves on NSE", onNse)
        assertEquals("NSE", onNse!!.exchange)
        assertTrue("only NSE/BSE rows survive", hits.all { it.exchange == "NSE" || it.exchange == "BSE" })
    }

    @Test
    fun `an uncurated nse name still resolves a profile`() = live {
        // ADANIENT sits in Metals & Mining — searchable, deep-diveable, but in no macro sector.
        val profile = YahooResearch.profile("ADANIENT")
        assertNotNull(profile)
        assertNotNull(profile!!.name)
        assertTrue(profile.name!!.isNotEmpty())
    }

    /**
     * Runs [block] only when opted in, and converts a throttle (429) into an inconclusive skip.
     * Yahoo's public endpoints rate-limit aggressively; that is not an app defect, so it must not
     * fail the build. Any other failure is a real regression.
     */
    private fun live(block: suspend () -> Unit) {
        assumeTrue("live Yahoo tests are opt-in (-Dstocker.live=true)", live)
        runBlocking {
            val code = com.dotpmm.stocker.data.net.Http.get(
                "https://query1.finance.yahoo.com/v8/finance/chart/RELIANCE.NS?range=5d&interval=1d",
                headers = mapOf("User-Agent" to USER_AGENT),
                timeoutMs = 20_000,
            ).code
            assumeTrue("inconclusive: Yahoo answered $code (rate limit / block)", code == 200)
            block()
        }
    }

    private companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 6) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

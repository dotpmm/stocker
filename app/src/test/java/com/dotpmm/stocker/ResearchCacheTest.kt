package com.dotpmm.stocker

import com.dotpmm.stocker.data.db.StockerDb
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The research cache contract (FUNCTIONALITY.md §4): a value is served only inside its TTL, and a
 * stale copy is still readable so an outage never leaves a blank screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
class ResearchCacheTest {

    private fun db() = StockerDb(ApplicationProvider.getApplicationContext())

    @Test
    fun `a fresh value is served inside its ttl`() {
        val db = db()
        db.clearResearchCache()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "idx:^NSEI", """{"ok":true}""", now)

        val hit = db.cacheGet("research_cache", "idx:^NSEI", now - 60_000)
        assertEquals("""{"ok":true}""", hit)
    }

    @Test
    fun `an expired value is not served through the small threshold`() {
        val db = db()
        db.clearResearchCache()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "q:RELIANCE", """{"ltp":1.0}""", now - 10 * 60_000)

        // 2-minute TTL: a 10-minute-old row is stale.
        assertNull(db.cacheGet("research_cache", "q:RELIANCE", now - 120_000))
    }

    @Test
    fun `a stale value is still readable with the any-age threshold`() {
        val db = db()
        db.clearResearchCache()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "q:RELIANCE", """{"ltp":1.0}""", now - 30 * 24 * 3_600_000L)

        // Threshold 0 = "any age is better than an empty screen".
        assertEquals("""{"ltp":1.0}""", db.cacheGet("research_cache", "q:RELIANCE", 0L))
    }

    @Test
    fun `writing the same key replaces the value`() {
        val db = db()
        db.clearResearchCache()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "info:TITAN", """{"pe":60.0}""", now)
        db.cachePut("research_cache", "info:TITAN", """{"pe":61.5}""", now)

        assertEquals("""{"pe":61.5}""", db.cacheGet("research_cache", "info:TITAN", now - 1000))
    }

    @Test
    fun `keys are independent`() {
        val db = db()
        db.clearResearchCache()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "info:A", "alpha", now)
        db.cachePut("research_cache", "info:B", "beta", now)

        assertEquals("alpha", db.cacheGet("research_cache", "info:A", 0L))
        assertEquals("beta", db.cacheGet("research_cache", "info:B", 0L))
    }

    @Test
    fun `an unknown key is null, never empty`() {
        val db = db()
        db.clearResearchCache()
        assertNull(db.cacheGet("research_cache", "info:NEVER_FETCHED", 0L))
    }

    @Test
    fun `clearCache drops everything`() {
        val db = db()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "info:A", "alpha", now)
        db.cachePut("research_cache", "info:B", "beta", now)
        db.clearResearchCache()

        assertNull(db.cacheGet("research_cache", "info:A", 0L))
        assertNull(db.cacheGet("research_cache", "info:B", 0L))
    }

    @Test
    fun `prune drops only the rows past the cutoff`() {
        val db = db()
        db.clearResearchCache()
        val now = System.currentTimeMillis()
        db.cachePut("research_cache", "old", "x", now - 30L * 24 * 3_600_000)
        db.cachePut("research_cache", "new", "y", now)

        db.prune(now - 7L * 24 * 3_600_000, 0L)

        assertNull("old row pruned", db.cacheGet("research_cache", "old", 0L))
        assertEquals("y", db.cacheGet("research_cache", "new", 0L))
    }

    @Test
    fun `the store survives being reopened, which is what stale-serve depends on`() {
        val now = System.currentTimeMillis()
        db().apply {
            clearResearchCache()
            cachePut("research_cache", "hist1y:TITAN", "[{\"t\":\"2026-01-01\",\"c\":100.0}]", now)
            close()
        }
        // A new helper instance reads the same file — the cache must not be in-memory only.
        val reopened = db()
        val hit = reopened.cacheGet("research_cache", "hist1y:TITAN", now - 1000)
        assertTrue("value persisted to disk", hit != null && hit.contains("100.0"))
    }
}

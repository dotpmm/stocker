package com.dotpmm.stocker.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dotpmm.stocker.domain.model.Article
import com.dotpmm.stocker.domain.model.SourceHealth
import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.domain.model.UpcomingEvent

/** An article joined with one linked ticker. */
data class LinkedArticle(val article: Article, val ticker: String, val strength: Double)

/** Lightweight record used for duplicate detection. */
data class KnownArticle(val id: String, val cluster: String, val title: String, val publishedAt: Long)

class StockerDb(context: Context) : SQLiteOpenHelper(context.applicationContext, "stocker.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE articles(
                id TEXT PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, url TEXT NOT NULL,
                source TEXT NOT NULL, published_at INTEGER NOT NULL, tag TEXT NOT NULL, tone INTEGER NOT NULL,
                credibility REAL NOT NULL, cluster TEXT NOT NULL, fetched_at INTEGER NOT NULL)""",
        )
        db.execSQL("CREATE INDEX idx_articles_pub ON articles(published_at)")
        db.execSQL("CREATE INDEX idx_articles_cluster ON articles(cluster)")
        db.execSQL(
            """CREATE TABLE article_stock(
                article_id TEXT NOT NULL, ticker TEXT NOT NULL, strength REAL NOT NULL,
                PRIMARY KEY(article_id, ticker))""",
        )
        db.execSQL("CREATE INDEX idx_link_ticker ON article_stock(ticker)")
        db.execSQL(
            """CREATE TABLE events(
                id TEXT PRIMARY KEY, ticker TEXT NOT NULL, epoch_day INTEGER NOT NULL,
                label TEXT NOT NULL, article_id TEXT)""",
        )
        db.execSQL(
            """CREATE TABLE quotes(
                symbol TEXT PRIMARY KEY, price REAL NOT NULL, change_pct REAL NOT NULL,
                trend TEXT NOT NULL, fetched_at INTEGER NOT NULL)""",
        )
        db.execSQL("CREATE TABLE chart_cache(k TEXT PRIMARY KEY, fetched_at INTEGER NOT NULL, body TEXT NOT NULL)")
        db.execSQL("CREATE TABLE ai_cache(k TEXT PRIMARY KEY, created_at INTEGER NOT NULL, body TEXT NOT NULL)")
        db.execSQL(
            """CREATE TABLE source_health(
                id TEXT PRIMARY KEY, name TEXT NOT NULL, last_attempt INTEGER NOT NULL,
                last_success INTEGER NOT NULL, last_error TEXT, items INTEGER NOT NULL)""",
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 only so far.
    }

    fun <T> inTransaction(block: () -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val result = block()
            db.setTransactionSuccessful()
            return result
        } finally {
            db.endTransaction()
        }
    }

    private fun toneOf(value: Int): Tone = when (value) {
        1 -> Tone.Positive
        -1 -> Tone.Negative
        else -> Tone.Neutral
    }

    private fun toneValue(tone: Tone): Int = when (tone) {
        Tone.Positive -> 1
        Tone.Negative -> -1
        Tone.Neutral -> 0
    }

    // ---- articles ----

    fun hasArticle(id: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM articles WHERE id=? LIMIT 1", arrayOf(id)).use { it.moveToFirst() }

    fun insertArticle(a: Article, fetchedAt: Long) {
        val values = ContentValues().apply {
            put("id", a.id)
            put("title", a.title)
            put("summary", a.summary)
            put("url", a.url)
            put("source", a.source)
            put("published_at", a.publishedAt)
            put("tag", a.tag)
            put("tone", toneValue(a.tone))
            put("credibility", a.credibility)
            put("cluster", a.cluster)
            put("fetched_at", fetchedAt)
        }
        writableDatabase.insertWithOnConflict("articles", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun upsertLink(articleId: String, ticker: String, strength: Double) {
        val db = writableDatabase
        db.execSQL(
            "INSERT OR IGNORE INTO article_stock(article_id, ticker, strength) VALUES(?,?,?)",
            arrayOf<Any>(articleId, ticker, strength),
        )
        db.execSQL(
            "UPDATE article_stock SET strength=? WHERE article_id=? AND ticker=? AND strength<?",
            arrayOf<Any>(strength, articleId, ticker, strength),
        )
    }

    fun upsertEvent(id: String, ticker: String, epochDay: Long, label: String, articleId: String?) {
        val values = ContentValues().apply {
            put("id", id)
            put("ticker", ticker)
            put("epoch_day", epochDay)
            put("label", label)
            put("article_id", articleId)
        }
        writableDatabase.insertWithOnConflict("events", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun recentForDedupe(sinceMs: Long): List<KnownArticle> =
        readableDatabase.rawQuery(
            "SELECT id, cluster, title, published_at FROM articles WHERE published_at>=?",
            arrayOf(sinceMs.toString()),
        ).use { c ->
            val out = ArrayList<KnownArticle>()
            while (c.moveToNext()) out.add(KnownArticle(c.getString(0), c.getString(1), c.getString(2), c.getLong(3)))
            out
        }

    private fun articleFrom(c: Cursor): Article = Article(
        id = c.getString(0),
        title = c.getString(1),
        summary = c.getString(2),
        url = c.getString(3),
        source = c.getString(4),
        publishedAt = c.getLong(5),
        tag = c.getString(6),
        tone = toneOf(c.getInt(7)),
        credibility = c.getDouble(8),
        cluster = c.getString(9),
    )

    private val articleColumns =
        "a.id, a.title, a.summary, a.url, a.source, a.published_at, a.tag, a.tone, a.credibility, a.cluster"

    /** Articles linked to [ticker] (or to any ticker when null), newest first. */
    fun linked(ticker: String?, sinceMs: Long, limit: Int): List<LinkedArticle> {
        val where = StringBuilder("a.published_at>=?")
        val args = arrayListOf(sinceMs.toString())
        if (ticker != null) {
            where.append(" AND s.ticker=?")
            args.add(ticker)
        }
        val sql = "SELECT $articleColumns, s.ticker, s.strength FROM articles a " +
            "JOIN article_stock s ON s.article_id=a.id WHERE $where ORDER BY a.published_at DESC LIMIT $limit"
        return readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            val out = ArrayList<LinkedArticle>()
            while (c.moveToNext()) out.add(LinkedArticle(articleFrom(c), c.getString(10), c.getDouble(11)))
            out
        }
    }

    fun article(id: String): Article? =
        readableDatabase.rawQuery("SELECT $articleColumns FROM articles a WHERE a.id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) articleFrom(c) else null
        }

    fun clusterMembers(cluster: String): List<Article> =
        readableDatabase.rawQuery(
            "SELECT $articleColumns FROM articles a WHERE a.cluster=? ORDER BY a.credibility DESC, a.published_at DESC",
            arrayOf(cluster),
        ).use { c ->
            val out = ArrayList<Article>()
            while (c.moveToNext()) out.add(articleFrom(c))
            out
        }

    // ---- events ----

    fun events(ticker: String?, fromDay: Long, toDay: Long): List<UpcomingEvent> {
        val where = StringBuilder("epoch_day>=? AND epoch_day<=?")
        val args = arrayListOf(fromDay.toString(), toDay.toString())
        if (ticker != null) {
            where.append(" AND ticker=?")
            args.add(ticker)
        }
        return readableDatabase.rawQuery(
            "SELECT ticker, epoch_day, label, article_id FROM events WHERE $where ORDER BY epoch_day ASC",
            args.toTypedArray(),
        ).use { c ->
            val out = ArrayList<UpcomingEvent>()
            while (c.moveToNext()) {
                out.add(UpcomingEvent(c.getString(0), c.getLong(1), c.getString(2), if (c.isNull(3)) null else c.getString(3)))
            }
            out
        }
    }

    // ---- quotes and chart cache ----

    data class QuoteRow(val price: Double, val changePct: Double, val trend: List<Float>, val fetchedAt: Long)

    fun saveQuote(symbol: String, price: Double, changePct: Double, trend: List<Float>, now: Long) {
        val values = ContentValues().apply {
            put("symbol", symbol)
            put("price", price)
            put("change_pct", changePct)
            put("trend", trend.joinToString(","))
            put("fetched_at", now)
        }
        writableDatabase.insertWithOnConflict("quotes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun quote(symbol: String): QuoteRow? =
        readableDatabase.rawQuery(
            "SELECT price, change_pct, trend, fetched_at FROM quotes WHERE symbol=?", arrayOf(symbol),
        ).use { c ->
            if (!c.moveToFirst()) {
                null
            } else {
                val trend = c.getString(2).split(',').mapNotNull { it.toFloatOrNull() }
                QuoteRow(c.getDouble(0), c.getDouble(1), trend, c.getLong(3))
            }
        }

    fun cacheGet(table: String, key: String, minTime: Long): String? {
        val timeColumn = if (table == "chart_cache") "fetched_at" else "created_at"
        return readableDatabase.rawQuery(
            "SELECT body FROM $table WHERE k=? AND $timeColumn>=?", arrayOf(key, minTime.toString()),
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }

    fun cachePut(table: String, key: String, body: String, now: Long) {
        val values = ContentValues().apply {
            put("k", key)
            put(if (table == "chart_cache") "fetched_at" else "created_at", now)
            put("body", body)
        }
        writableDatabase.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---- source health ----

    fun recordHealth(id: String, name: String, ok: Boolean, items: Int, error: String?, now: Long) {
        val previous = readableDatabase.rawQuery("SELECT last_success FROM source_health WHERE id=?", arrayOf(id))
            .use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
        val values = ContentValues().apply {
            put("id", id)
            put("name", name)
            put("last_attempt", now)
            put("last_success", if (ok) now else previous)
            put("last_error", if (ok) null else error)
            put("items", if (ok) items else 0)
        }
        writableDatabase.insertWithOnConflict("source_health", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun health(): List<SourceHealth> =
        readableDatabase.rawQuery(
            "SELECT id, name, last_attempt, last_success, last_error, items FROM source_health ORDER BY name", null,
        ).use { c ->
            val out = ArrayList<SourceHealth>()
            while (c.moveToNext()) {
                out.add(
                    SourceHealth(
                        c.getString(0), c.getString(1), c.getLong(2), c.getLong(3),
                        if (c.isNull(4)) null else c.getString(4), c.getInt(5),
                    ),
                )
            }
            out
        }

    // ---- housekeeping ----

    fun removeStockData(ticker: String) {
        inTransaction {
            writableDatabase.delete("article_stock", "ticker=?", arrayOf(ticker))
            writableDatabase.delete("events", "ticker=?", arrayOf(ticker))
            writableDatabase.execSQL("DELETE FROM articles WHERE id NOT IN (SELECT article_id FROM article_stock)")
        }
    }

    fun prune(olderThanMs: Long, eventsBeforeDay: Long) {
        inTransaction {
            val db = writableDatabase
            db.execSQL("DELETE FROM articles WHERE published_at<?", arrayOf<Any>(olderThanMs))
            db.execSQL("DELETE FROM article_stock WHERE article_id NOT IN (SELECT id FROM articles)")
            db.execSQL("DELETE FROM events WHERE epoch_day<?", arrayOf<Any>(eventsBeforeDay))
            db.execSQL("DELETE FROM chart_cache WHERE fetched_at<?", arrayOf<Any>(olderThanMs))
            db.execSQL("DELETE FROM ai_cache WHERE created_at<?", arrayOf<Any>(olderThanMs))
        }
    }

    fun clearNews() {
        inTransaction {
            val db = writableDatabase
            db.execSQL("DELETE FROM articles")
            db.execSQL("DELETE FROM article_stock")
            db.execSQL("DELETE FROM events")
            db.execSQL("DELETE FROM ai_cache")
            db.execSQL("DELETE FROM chart_cache")
        }
    }
}

package com.dotpmm.stocker.data

import android.content.Context
import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.WatchStock
import org.json.JSONArray
import org.json.JSONObject

/** Small settings store on SharedPreferences. The Gemini key never leaves the device except in requests to Gemini. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("stocker", Context.MODE_PRIVATE)

    var geminiKey: String
        get() = prefs.getString("gemini_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_key", value.trim()).apply()

    /** Empty means "pick the newest Flash model automatically". */
    var geminiModel: String
        get() = prefs.getString("gemini_model", "") ?: ""
        set(value) = prefs.edit().putString("gemini_model", value.trim()).apply()

    var lastRefresh: Long
        get() = prefs.getLong("last_refresh", 0L)
        set(value) = prefs.edit().putLong("last_refresh", value).apply()

    fun loadWatchlist(): List<WatchStock> {
        val raw = prefs.getString("watchlist", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                val aliases = o.optJSONArray("aliases")
                WatchStock(
                    ticker = o.getString("ticker"),
                    name = o.getString("name"),
                    market = if (o.optString("market") == "US") Market.US else Market.India,
                    symbol = o.getString("symbol"),
                    exchange = o.optString("exchange"),
                    aliases = if (aliases == null) emptyList() else (0 until aliases.length()).map { aliases.getString(it) },
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWatchlist(list: List<WatchStock>) {
        val array = JSONArray()
        for (s in list) {
            array.put(
                JSONObject().apply {
                    put("ticker", s.ticker)
                    put("name", s.name)
                    put("market", s.market.name)
                    put("symbol", s.symbol)
                    put("exchange", s.exchange)
                    put("aliases", JSONArray(s.aliases))
                },
            )
        }
        prefs.edit().putString("watchlist", array.toString()).apply()
    }
}

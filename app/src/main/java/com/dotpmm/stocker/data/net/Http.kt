package com.dotpmm.stocker.data.net

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HttpResponse(val code: Int, val body: String, val finalUrl: String) {
    val ok: Boolean get() = code in 200..299
}

/** Minimal HTTP client on HttpURLConnection with per-host politeness. No third-party dependencies. */
object Http {
    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0 Mobile Safari/537.36 Stocker/1.5"

    private val locks = ConcurrentHashMap<String, Mutex>()
    private val lastHit = ConcurrentHashMap<String, Long>()

    /** Waits so that requests to one host start at least [minIntervalMs] apart. */
    private suspend fun gate(host: String, minIntervalMs: Long) {
        if (minIntervalMs <= 0) return
        val mutex = locks.getOrPut(host) { Mutex() }
        mutex.withLock {
            val wait = (lastHit[host] ?: 0L) + minIntervalMs - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            lastHit[host] = System.currentTimeMillis()
        }
    }

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        gateMs: Long = 0,
        timeoutMs: Int = 15_000,
        maxBytes: Int = 3_000_000,
    ): HttpResponse = request("GET", url, null, headers, gateMs, timeoutMs, maxBytes)

    suspend fun postJson(
        url: String,
        json: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Int = 60_000,
    ): HttpResponse = request("POST", url, json, headers, 0, timeoutMs, 2_000_000)

    private suspend fun request(
        method: String,
        url: String,
        body: String?,
        headers: Map<String, String>,
        gateMs: Long,
        timeoutMs: Int,
        maxBytes: Int,
    ): HttpResponse {
        val host = URL(url).host
        gate(host, gateMs)
        return withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = timeoutMs
                conn.readTimeout = timeoutMs
                conn.instanceFollowRedirects = true
                conn.requestMethod = method
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.setRequestProperty("Accept", "application/rss+xml, application/xml, application/json, text/html, */*")
                conn.setRequestProperty("Accept-Language", "en-IN,en;q=0.9")
                for ((k, v) in headers) conn.setRequestProperty(k, v)
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = conn.responseCode
                val stream: InputStream? = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.use { readLimited(it, maxBytes, charsetOf(conn.contentType)) } ?: ""
                HttpResponse(code, text, conn.url.toString())
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun charsetOf(contentType: String?): java.nio.charset.Charset {
        val name = contentType
            ?.split(';')
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim('"', ' ')
        return try {
            if (name.isNullOrEmpty()) Charsets.UTF_8 else java.nio.charset.Charset.forName(name)
        } catch (e: Exception) {
            Charsets.UTF_8
        }
    }

    private fun readLimited(input: InputStream, maxBytes: Int, charset: java.nio.charset.Charset): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            total += n
            if (total > maxBytes) throw IOException("Response too large")
            out.write(buffer, 0, n)
        }
        return String(out.toByteArray(), charset)
    }
}

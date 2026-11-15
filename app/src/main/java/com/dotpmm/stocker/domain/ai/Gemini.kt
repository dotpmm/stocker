package com.dotpmm.stocker.domain.ai

import com.dotpmm.stocker.data.net.Http
import com.dotpmm.stocker.domain.model.Article
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

class AiPoint(val text: String, val refs: List<Int>)

class AiSummary(
    val stance: String,
    val summary: String,
    val drivers: List<AiPoint>,
    val risks: List<AiPoint>,
    val upcoming: List<AiPoint>,
    val model: String,
    /** Article ids in the order they were numbered in the prompt (1-based refs index into this). */
    val articleIds: List<String>,
)

class GeminiException(message: String) : Exception(message)

/** Optional AI layer. Uses the user's own free Gemini key; nothing is sent unless the user taps the button. */
object Gemini {
    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"
    private const val FALLBACK_MODEL = "gemini-3.6-flash"
    private var discovered: String? = null

    private fun headers(key: String) = mapOf("x-goog-api-key" to key)

    /** Picks the highest-numbered "flash" model that supports generateContent. */
    private suspend fun discoverModel(key: String): String {
        discovered?.let { return it }
        return try {
            val response = Http.get("$BASE/models?pageSize=200", headers(key))
            if (!response.ok) throw GeminiException(errorText(response.code, response.body))
            val models = JSONObject(response.body).optJSONArray("models") ?: JSONArray()
            var best: String? = null
            var bestVersion = -1.0
            for (i in 0 until models.length()) {
                val m = models.getJSONObject(i)
                val name = m.optString("name").removePrefix("models/")
                val methods = m.optJSONArray("supportedGenerationMethods")
                val supportsGenerate = methods != null && (0 until methods.length()).any { methods.getString(it) == "generateContent" }
                if (!supportsGenerate || !name.startsWith("gemini-") || !name.contains("flash")) continue
                if (listOf("lite", "image", "tts", "live", "audio", "thinking", "exp", "preview", "latest", "robotics", "computer").any { name.contains(it) }) continue
                val version = Regex("gemini-(\\d+(?:\\.\\d+)?)").find(name)?.groupValues?.get(1)?.toDoubleOrNull() ?: continue
                if (version > bestVersion) {
                    bestVersion = version
                    best = name
                }
            }
            (best ?: FALLBACK_MODEL).also { discovered = it }
        } catch (e: GeminiException) {
            throw e
        } catch (e: IOException) {
            FALLBACK_MODEL
        }
    }

    private fun errorText(code: Int, body: String): String {
        val message = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (e: Exception) {
            ""
        }
        return when (code) {
            400 -> if (message.contains("API key", ignoreCase = true)) "Gemini rejected the API key. Check it in Settings." else "Gemini could not process the request. $message".trim()
            401, 403 -> "Gemini rejected the API key or this model is not allowed for it. Check Settings."
            404 -> "That Gemini model was not found. Clear the model field in Settings to pick one automatically."
            429 -> "Gemini rate limit reached. Wait a minute and try again."
            else -> "Gemini error (HTTP $code). $message".trim()
        }
    }

    suspend fun summarize(
        key: String,
        modelPreference: String,
        name: String,
        ticker: String,
        articles: List<Article>,
        ruleTone: String,
        now: Long,
    ): AiSummary {
        val model = modelPreference.ifBlank { discoverModel(key) }
        val prompt = buildPrompt(name, ticker, articles, ruleTone, now)
        val body = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
        }
        val response = try {
            Http.postJson("$BASE/models/$model:generateContent", body.toString(), headers(key))
        } catch (e: IOException) {
            throw GeminiException("Could not reach Gemini. Check your connection.")
        }
        if (!response.ok) throw GeminiException(errorText(response.code, response.body))
        val text = try {
            JSONObject(response.body).getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        } catch (e: Exception) {
            throw GeminiException("Gemini returned an empty answer. Try again.")
        }
        return parse(text, model, articles.map { it.id })
    }

    fun parse(raw: String, model: String, ids: List<String>): AiSummary {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val o = try {
            JSONObject(cleaned)
        } catch (e: Exception) {
            throw GeminiException("Gemini's answer was not in the expected format. Try again.")
        }
        fun points(field: String): List<AiPoint> {
            val array = o.optJSONArray(field) ?: return emptyList()
            return (0 until array.length()).mapNotNull { i ->
                val item = array.opt(i)
                when (item) {
                    is JSONObject -> {
                        val refs = item.optJSONArray("refs")
                        AiPoint(
                            item.optString("text"),
                            if (refs == null) emptyList() else (0 until refs.length()).map { refs.optInt(it) },
                        )
                    }
                    is String -> AiPoint(item, emptyList())
                    else -> null
                }
            }.filter { it.text.isNotBlank() }
        }
        return AiSummary(
            stance = o.optString("stance", "neutral").lowercase(),
            summary = o.optString("summary"),
            drivers = points("drivers"),
            risks = points("risks"),
            upcoming = points("upcoming"),
            model = model,
            articleIds = ids,
        )
    }

    private fun buildPrompt(name: String, ticker: String, articles: List<Article>, ruleTone: String, now: Long): String {
        val sb = StringBuilder()
        sb.append("You summarise recent news about one listed company for a retail investor. ")
        sb.append("Use ONLY the numbered articles below. Do not use outside knowledge and do not predict the share price. ")
        sb.append("Be neutral and concise. If articles conflict, say so. This is information, not investment advice.\n\n")
        sb.append("Company: $name ($ticker)\n")
        sb.append("A simple keyword tagger rated the overall tone as: $ruleTone\n\n")
        sb.append("Return JSON only, in exactly this shape:\n")
        sb.append("{\"stance\":\"positive|negative|neutral|mixed\",\"summary\":\"2-3 sentences\",")
        sb.append("\"drivers\":[{\"text\":\"...\",\"refs\":[1]}],\"risks\":[{\"text\":\"...\",\"refs\":[2]}],")
        sb.append("\"upcoming\":[{\"text\":\"...\",\"refs\":[3]}]}\n")
        sb.append("At most 4 items per list. \"refs\" are article numbers. Use empty lists when there is nothing.\n\n")
        sb.append("Articles:\n")
        articles.forEachIndexed { i, a ->
            val hours = ((now - a.publishedAt) / 3_600_000L).coerceAtLeast(0)
            val age = if (hours < 48) "${hours}h ago" else "${hours / 24}d ago"
            sb.append("[${i + 1}] (${a.source}, $age) ${a.title}")
            if (a.summary.isNotBlank()) sb.append(" — ${a.summary.take(300)}")
            sb.append('\n')
        }
        return sb.toString()
    }

    fun toJson(s: AiSummary): String = JSONObject().apply {
        put("stance", s.stance)
        put("summary", s.summary)
        fun write(name: String, list: List<AiPoint>) {
            put(name, JSONArray().apply {
                list.forEach { p -> put(JSONObject().put("text", p.text).put("refs", JSONArray(p.refs))) }
            })
        }
        write("drivers", s.drivers)
        write("risks", s.risks)
        write("upcoming", s.upcoming)
    }.toString()
}

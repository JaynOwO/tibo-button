package com.tibobutton.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

class ResetApi {
    companion object {
        const val FORECAST_URL = "https://resetbeacon.com/api/forecast"
        const val HISTORY_URL = "https://resetbeacon.com/api/history"
        const val SITE_URL = "https://resetbeacon.com/"
    }

    fun fetchForecast(): ForecastSnapshot {
        val json = JSONObject(get(FORECAST_URL))
        val probabilities = json.optJSONObject("probabilities")
        return ForecastSnapshot(
            calculatedAt = json.optInstant("calculatedAt"),
            validUntil = json.optInstant("validUntil"),
            publicationState = json.optString("publicationState", "unknown"),
            h24 = probabilities?.optJSONObject("h24")?.optNullableInt("display"),
            h48 = probabilities?.optJSONObject("h48")?.optNullableInt("display"),
            signalStage = json.optNullableString("signalStage")
                ?: json.optJSONObject("latestAcknowledgement")?.optNullableString("stage")
        )
    }

    fun fetchHistory(): List<HistoryEvent> {
        val root = JSONObject(get(HISTORY_URL))
        val items = root.optJSONArray("items") ?: JSONArray()
        return buildList {
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val sources = item.optJSONArray("sources")
                val sourceUrl = sources?.optJSONObject(0)?.optNullableString("url")
                val evidence = item.optNullableString("evidenceUrl") ?: sourceUrl

                // Reset Beacon's public contract guarantees event kind/status/scope and announcedAt.
                // A normalized schedule instant is not part of the documented contract, so we
                // probe a few harmless optional field names. If none exists, the UI says
                // "已排期 · 时间见来源" instead of guessing from prose.
                val scheduled = listOf(
                    "scheduledFor", "scheduledAt", "targetAt", "expectedAt",
                    "windowStart", "startsAt", "effectiveAt"
                ).firstNotNullOfOrNull { key -> item.optInstant(key) }

                add(
                    HistoryEvent(
                        eventKind = item.optString("eventKind", ""),
                        status = item.optString("status", ""),
                        scope = item.optString("scope", ""),
                        summary = item.optString("summaryFull", ""),
                        operativeSentence = item.optString("operativeSentence", ""),
                        evidenceUrl = evidence,
                        announcedAt = item.optInstant("announcedAt"),
                        scheduledFor = scheduled
                    )
                )
            }
        }
    }

    private fun get(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TiboButton/0.1 Android")
            useCaches = false
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optInstant(key: String): Instant? {
    val raw = optNullableString(key) ?: return null
    return runCatching { Instant.parse(raw) }.getOrNull()
}

private fun JSONObject.optNullableInt(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return runCatching { getInt(key) }.getOrNull()
}

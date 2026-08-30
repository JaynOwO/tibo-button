package com.tibobutton.app.data

import android.content.Context
import org.json.JSONObject
import java.time.Instant

object WidgetPrefs {
    private const val PREFS = "tibo_widget_cache"
    private const val KEY = "state"

    fun save(context: Context, state: WidgetState) {
        val o = JSONObject().apply {
            put("level", state.level.name)
            putNullable("h24", state.h24)
            putNullable("h48", state.h48)
            putNullable("nextResetAt", state.nextResetAt?.toString())
            put("nextResetKnownButUnparsed", state.nextResetKnownButUnparsed)
            putNullable("lastResetAt", state.lastResetAt?.toString())
            putNullable("evidenceUrl", state.evidenceUrl)
            putNullable("evidenceSummary", state.evidenceSummary)
            putNullable("canonicalHeadline", state.canonicalHeadline)
            putNullable("canonicalSecondLine", state.canonicalSecondLine)
            putNullable("updatedAt", state.updatedAt?.toString())
            put("sourceStale", state.sourceStale)
            putNullable("error", state.error)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, o.toString()).apply()
    }

    fun load(context: Context): WidgetState {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return WidgetState()
        return runCatching {
            val o = JSONObject(raw)
            WidgetState(
                level = runCatching { ResetLevel.valueOf(o.optString("level")) }.getOrDefault(ResetLevel.UNKNOWN),
                h24 = o.optNullableInt("h24"),
                h48 = o.optNullableInt("h48"),
                nextResetAt = o.optInstant("nextResetAt"),
                nextResetKnownButUnparsed = o.optBoolean("nextResetKnownButUnparsed", false),
                lastResetAt = o.optInstant("lastResetAt"),
                evidenceUrl = o.optNullableString("evidenceUrl"),
                evidenceSummary = o.optNullableString("evidenceSummary"),
                canonicalHeadline = o.optNullableString("canonicalHeadline"),
                canonicalSecondLine = o.optNullableString("canonicalSecondLine"),
                updatedAt = o.optInstant("updatedAt"),
                sourceStale = o.optBoolean("sourceStale", false),
                error = o.optNullableString("error")
            )
        }.getOrElse { WidgetState(error = "缓存读取失败") }
    }
}

private fun JSONObject.putNullable(key: String, value: Any?) {
    if (value == null) put(key, JSONObject.NULL) else put(key, value)
}
private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
private fun JSONObject.optNullableInt(key: String): Int? =
    if (!has(key) || isNull(key)) null else runCatching { getInt(key) }.getOrNull()
private fun JSONObject.optInstant(key: String): Instant? =
    optNullableString(key)?.let { runCatching { Instant.parse(it) }.getOrNull() }

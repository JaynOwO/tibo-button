package com.tibobutton.app.data

import android.content.Context

object NotificationPrefs {
    private const val PREFS = "tibo_notification_settings"
    private const val KEY_CONFIRMED = "notify_confirmed"
    private const val KEY_COMPLETED = "notify_completed"
    private const val KEY_LIKELY = "notify_likely"
    private const val KEY_LAST_CONFIRMED = "last_confirmed_fingerprint"
    private const val KEY_LAST_COMPLETED = "last_completed_fingerprint"
    private const val KEY_LAST_LIKELY = "last_likely_fingerprint"

    data class Settings(
        val confirmed: Boolean = true,
        val completed: Boolean = true,
        val likely: Boolean = false
    )

    fun load(context: Context): Settings {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Settings(
            confirmed = p.getBoolean(KEY_CONFIRMED, true),
            completed = p.getBoolean(KEY_COMPLETED, true),
            likely = p.getBoolean(KEY_LIKELY, false)
        )
    }

    fun setConfirmed(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_CONFIRMED, value).apply()

    fun setCompleted(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_COMPLETED, value).apply()

    fun setLikely(context: Context, value: Boolean) =
        prefs(context).edit().putBoolean(KEY_LIKELY, value).apply()

    fun lastConfirmedFingerprint(context: Context): String? = prefs(context).getString(KEY_LAST_CONFIRMED, null)
    fun lastCompletedFingerprint(context: Context): String? = prefs(context).getString(KEY_LAST_COMPLETED, null)
    fun lastLikelyFingerprint(context: Context): String? = prefs(context).getString(KEY_LAST_LIKELY, null)

    fun markConfirmed(context: Context, fingerprint: String) =
        prefs(context).edit().putString(KEY_LAST_CONFIRMED, fingerprint).apply()

    fun markCompleted(context: Context, fingerprint: String) =
        prefs(context).edit().putString(KEY_LAST_COMPLETED, fingerprint).apply()

    fun markLikely(context: Context, fingerprint: String) =
        prefs(context).edit().putString(KEY_LAST_LIKELY, fingerprint).apply()

    fun primeBaseline(context: Context, state: WidgetState) {
        val edit = prefs(context).edit()
        state.lastResetAt?.let { edit.putString(KEY_LAST_COMPLETED, it.toString()) }
        confirmedFingerprint(state)?.let { edit.putString(KEY_LAST_CONFIRMED, it) }
        likelyFingerprint(state)?.let { edit.putString(KEY_LAST_LIKELY, it) }
        edit.apply()
    }

    fun confirmedFingerprint(state: WidgetState): String? {
        if (state.level != ResetLevel.CONFIRMED) return null
        state.nextResetAt?.let { return "time:$it" }
        val canonical = listOfNotNull(state.canonicalHeadline, state.canonicalSecondLine)
            .joinToString("|").trim('|')
        if (canonical.isNotBlank()) return "canonical:$canonical"
        // Evidence URLs can change while the same schedule remains active. Use
        // the displayed source summary when no stronger stable identity exists.
        val summary = state.evidenceSummary?.trim().takeIf { !it.isNullOrBlank() }
        return summary?.let { "summary:$it" }
    }

    fun likelyFingerprint(state: WidgetState): String? {
        if (state.level != ResetLevel.VERY_LIKELY) return null
        val bucket = state.h24?.div(5)?.times(5) ?: -1
        val day = state.updatedAt?.toString()?.take(10) ?: "unknown"
        return "$day|$bucket"
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

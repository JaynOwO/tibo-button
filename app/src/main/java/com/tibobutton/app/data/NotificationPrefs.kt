package com.tibobutton.app.data

import android.content.Context

object NotificationPrefs {
    private const val PREFS = "tibo_notification_settings"
    private const val KEY_CONFIRMED = "notify_confirmed"
    private const val KEY_COMPLETED = "notify_completed"
    private const val KEY_LIKELY = "notify_likely"

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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_CONFIRMED, value).apply()

    fun setCompleted(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_COMPLETED, value).apply()

    fun setLikely(context: Context, value: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_LIKELY, value).apply()
}

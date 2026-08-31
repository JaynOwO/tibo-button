package com.tibobutton.app.update

import android.content.Context

object UpdatePrefs {
    private const val PREFS = "tibo_update_settings"
    private const val KEY_AUTO_CHECK = "auto_check_stable_release"

    fun autoCheckEnabled(context: Context): Boolean = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_AUTO_CHECK, true)

    fun setAutoCheckEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_CHECK, enabled)
            .apply()
    }
}

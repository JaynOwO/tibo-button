package com.tibobutton.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.tibobutton.app.work.WidgetScheduler

abstract class BaseTiboWidgetProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetScheduler.ensurePeriodic(context)
        WidgetScheduler.refreshNow(context)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        WidgetRenderer.updateAll(context)
        WidgetScheduler.ensurePeriodic(context)
        WidgetScheduler.refreshNow(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // Immediate visual confirmation: swap ↻ for a deterministic loading glyph
            // before WorkManager starts the network request. RemoteViews animations are
            // not reliable across launchers, so this must not depend on continuous motion.
            WidgetRenderer.updateAll(context, refreshing = true)
            WidgetScheduler.refreshNow(context)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        val manager = AppWidgetManager.getInstance(context)
        val wide = manager.getAppWidgetIds(ComponentName(context, TiboWideWidgetProvider::class.java))
        val small = manager.getAppWidgetIds(ComponentName(context, TiboSmallWidgetProvider::class.java))
        if (wide.isEmpty() && small.isEmpty()) WidgetScheduler.stopPeriodic(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.tibobutton.app.action.REFRESH"
    }
}

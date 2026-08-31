package com.tibobutton.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.tibobutton.app.MainActivity
import com.tibobutton.app.R
import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.data.WidgetState
import java.time.format.DateTimeFormatter
import java.util.Locale

object WidgetRenderer {
    fun updateAll(context: Context, refreshing: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context)
        val state = WidgetPrefs.load(context)
        val display = WidgetTextFormatter.format(state)

        val wideComponent = ComponentName(context, TiboWideWidgetProvider::class.java)
        manager.getAppWidgetIds(wideComponent).forEach { id ->
            manager.updateAppWidget(id, wideViews(context, state, display, refreshing))
        }

        val smallComponent = ComponentName(context, TiboSmallWidgetProvider::class.java)
        manager.getAppWidgetIds(smallComponent).forEach { id ->
            manager.updateAppWidget(id, smallViews(context, state, display, refreshing))
        }
    }

    private fun wideViews(
        context: Context,
        state: WidgetState,
        display: WidgetTextModel,
        refreshing: Boolean
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_wide).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, display.statusEmoji, display.statusLabel)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.probability, probabilityText(context, display, wide = true))
            setTextViewText(R.id.next_time, nextText(context, display.next, short = false))
            val last = lastText(context, display.last)
            setTextViewText(
                R.id.last_reset,
                if (state.resetsLast7Days > 0) {
                    context.getString(R.string.widget_last_pulse_format, last, state.resetsLast7Days)
                } else {
                    context.getString(R.string.widget_last_format, last)
                }
            )
            setTextViewText(
                R.id.footer,
                if (refreshing) context.getString(R.string.widget_footer_loading) else footerText(context, display)
            )
            setViewVisibility(R.id.refresh, if (refreshing) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.refresh_loading, if (refreshing) View.VISIBLE else View.GONE)
            setAccessibility(context, this, display, refreshing)
            wireClicks(context, this, state, titleClickable = true)
        }
    }

    private fun smallViews(
        context: Context,
        state: WidgetState,
        display: WidgetTextModel,
        refreshing: Boolean
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_small).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, display.statusEmoji, display.statusLabel)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.probability, probabilityText(context, display, wide = false))
            setTextViewText(
                R.id.next_time,
                context.getString(R.string.widget_next_format, nextText(context, display.next, short = true))
            )
            setTextViewText(
                R.id.last_reset,
                context.getString(R.string.widget_last_format, lastText(context, display.last))
            )
            setTextViewText(
                R.id.footer,
                if (refreshing) context.getString(R.string.widget_footer_loading) else footerText(context, display)
            )
            setViewVisibility(R.id.refresh, if (refreshing) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.refresh_loading, if (refreshing) View.VISIBLE else View.GONE)
            setAccessibility(context, this, display, refreshing)
            wireClicks(context, this, state, titleClickable = false)
        }
    }

    private fun setAccessibility(
        context: Context,
        views: RemoteViews,
        display: WidgetTextModel,
        refreshing: Boolean
    ) {
        val next = nextText(context, display.next, short = true)
        val probability = probabilityText(context, display, wide = false)
        val updated = display.updatedAt?.format(timeFormatter) ?: context.getString(R.string.not_available)
        views.setContentDescription(
            R.id.widget_root,
            context.getString(
                R.string.widget_accessibility_format,
                display.statusLabel,
                next,
                probability,
                updated
            )
        )
        views.setContentDescription(
            R.id.refresh,
            context.getString(if (refreshing) R.string.widget_refresh_loading else R.string.widget_refresh_description)
        )
        views.setContentDescription(R.id.refresh_loading, context.getString(R.string.widget_refresh_loading))
    }

    private fun wireClicks(context: Context, views: RemoteViews, state: WidgetState, titleClickable: Boolean) {
        val openApp = PendingIntent.getActivity(
            context, 10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)

        val refresh = Intent(context, TiboWideWidgetProvider::class.java).apply {
            action = BaseTiboWidgetProvider.ACTION_REFRESH
            data = Uri.parse("tibobutton://refresh/${System.currentTimeMillis()}")
        }
        val refreshPending = PendingIntent.getBroadcast(
            context, 11, refresh,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.refresh, refreshPending)

        state.evidenceUrl?.let { url ->
            val openSource = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            val sourcePending = PendingIntent.getActivity(
                context, 12, openSource,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (titleClickable) views.setOnClickPendingIntent(R.id.title, sourcePending)
        }
    }

    private fun nextText(context: Context, next: WidgetNextDisplay, short: Boolean): String = when (next.kind) {
        WidgetNextKind.IN_WINDOW -> context.getString(R.string.widget_next_in_window)
        WidgetNextKind.TODAY -> {
            val clock = next.localTime?.format(clockFormatter).orEmpty()
            if (short) {
                context.getString(R.string.widget_next_today_short, clock)
            } else {
                context.getString(
                    R.string.widget_next_today,
                    clock,
                    countdownCompact(context, next.countdownMinutes)
                )
            }
        }
        WidgetNextKind.FUTURE -> next.localTime?.format(dateTimeFormatter).orEmpty()
        WidgetNextKind.SCHEDULED_UNPARSED -> context.getString(
            if (short) R.string.widget_next_scheduled_short else R.string.widget_next_scheduled
        )
        WidgetNextKind.PROBABILITY_WINDOW -> context.getString(R.string.widget_next_probable)
        WidgetNextKind.UNKNOWN -> context.getString(R.string.widget_next_unknown)
    }

    private fun countdownCompact(context: Context, minutes: Long): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) {
            context.getString(R.string.widget_countdown_hours_minutes, h, m)
        } else {
            context.getString(R.string.widget_countdown_minutes, m)
        }
    }

    private fun lastText(context: Context, last: WidgetLastDisplay): String = when (last.kind) {
        WidgetLastKind.NONE -> context.getString(R.string.not_available)
        WidgetLastKind.MINUTES_AGO -> context.getString(R.string.widget_last_minutes, last.amount)
        WidgetLastKind.HOURS_AGO -> context.getString(R.string.widget_last_hours, last.amount)
        WidgetLastKind.ABSOLUTE -> last.localTime?.format(dateTimeFormatter).orEmpty()
    }

    private fun footerText(context: Context, display: WidgetTextModel): String {
        val updated = display.updatedAt?.format(timeFormatter) ?: context.getString(R.string.not_available)
        val suffix = when {
            display.sourceStale -> context.getString(R.string.widget_footer_stale_suffix)
            display.hasError -> context.getString(R.string.widget_footer_error_suffix)
            else -> ""
        }
        return context.getString(R.string.widget_footer_format, updated, suffix)
    }

    private fun probabilityText(context: Context, display: WidgetTextModel, wide: Boolean): String {
        val h24 = display.h24?.let { context.getString(R.string.percent_value, it) }
            ?: context.getString(R.string.not_available)
        val h48 = display.h48?.let { context.getString(R.string.percent_value, it) }
            ?: context.getString(R.string.not_available)
        return if (wide) {
            context.getString(R.string.widget_probability_wide, h24, h48)
        } else {
            context.getString(R.string.widget_probability_small, h24, h48)
        }
    }

    private fun levelColor(context: Context, level: ResetLevel): Int = ContextCompat.getColor(context, when (level) {
        ResetLevel.CONFIRMED -> R.color.status_confirmed
        ResetLevel.VERY_LIKELY -> R.color.status_likely
        ResetLevel.POSSIBLE -> R.color.status_possible
        ResetLevel.LOW -> R.color.status_low
        ResetLevel.STALE -> R.color.status_stale
        ResetLevel.UNLIKELY, ResetLevel.UNKNOWN -> R.color.status_unknown
    })

    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
}

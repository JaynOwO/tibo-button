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
        WidgetVariant.entries.forEach { variant ->
            val ids = manager.getAppWidgetIds(componentFor(context, variant))
            updateVariant(context, manager, ids, variant, state, refreshing)
        }
    }

    fun update(
        context: Context,
        appWidgetIds: IntArray,
        variant: WidgetVariant,
        refreshing: Boolean = false
    ) {
        if (appWidgetIds.isEmpty()) return
        val manager = AppWidgetManager.getInstance(context)
        updateVariant(context, manager, appWidgetIds, variant, WidgetPrefs.load(context), refreshing)
    }

    private fun updateVariant(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
        variant: WidgetVariant,
        state: WidgetState,
        refreshing: Boolean
    ) {
        if (appWidgetIds.isEmpty()) return
        val display = WidgetVariantFormatter.format(variant, state)
        val views = when (variant) {
            WidgetVariant.STANDARD_WIDE -> wideViews(context, state, display, refreshing)
            WidgetVariant.STANDARD_SMALL -> smallViews(context, state, display, refreshing)
            WidgetVariant.PULSE_ORB -> pulseOrbViews(context, state, display, refreshing)
            WidgetVariant.COMMAND_DECK -> commandDeckViews(context, state, display, refreshing)
        }
        appWidgetIds.forEach { manager.updateAppWidget(it, views) }
    }

    private fun componentFor(context: Context, variant: WidgetVariant): ComponentName = when (variant) {
        WidgetVariant.STANDARD_WIDE -> ComponentName(context, TiboWideWidgetProvider::class.java)
        WidgetVariant.STANDARD_SMALL -> ComponentName(context, TiboSmallWidgetProvider::class.java)
        WidgetVariant.PULSE_ORB -> ComponentName(context, TiboPulseOrbWidgetProvider::class.java)
        WidgetVariant.COMMAND_DECK -> ComponentName(context, TiboCommandDeckWidgetProvider::class.java)
    }

    private fun wideViews(
        context: Context,
        state: WidgetState,
        display: WidgetVariantDisplay,
        refreshing: Boolean
    ): RemoteViews {
        val text = display.text
        return RemoteViews(context.packageName, R.layout.widget_wide).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, text.statusEmoji, text.statusLabel)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.probability, probabilityText(context, text, wide = true))
            setTextViewText(R.id.next_time, nextText(context, text.next, short = false))
            val last = lastText(context, text.last)
            setTextViewText(
                R.id.last_reset,
                if (state.resetsLast7Days > 0) {
                    context.getString(R.string.widget_last_pulse_format, last, state.resetsLast7Days)
                } else {
                    context.getString(R.string.widget_last_format, last)
                }
            )
            setTextViewText(R.id.footer, footerText(context, text, refreshing))
            setRefreshState(refreshing)
            setAccessibility(context, this, display, refreshing)
            wireClicks(context, this, state, titleClickable = true)
        }
    }

    private fun smallViews(
        context: Context,
        state: WidgetState,
        display: WidgetVariantDisplay,
        refreshing: Boolean
    ): RemoteViews {
        val text = display.text
        return RemoteViews(context.packageName, R.layout.widget_small).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, text.statusEmoji, text.statusLabel)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.probability, probabilityText(context, text, wide = false))
            setTextViewText(
                R.id.next_time,
                context.getString(R.string.widget_next_format, nextText(context, text.next, short = true))
            )
            setTextViewText(
                R.id.last_reset,
                context.getString(R.string.widget_last_format, lastText(context, text.last))
            )
            setTextViewText(R.id.footer, footerText(context, text, refreshing))
            setRefreshState(refreshing)
            setAccessibility(context, this, display, refreshing)
            wireClicks(context, this, state, titleClickable = false)
        }
    }

    private fun pulseOrbViews(
        context: Context,
        state: WidgetState,
        display: WidgetVariantDisplay,
        refreshing: Boolean
    ): RemoteViews {
        val text = display.text
        return RemoteViews(context.packageName, R.layout.widget_pulse_orb).apply {
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, text.statusEmoji, text.statusLabel)
            )
            setImageViewResource(R.id.status_orb, levelOrbResource(state.level))
            setTextViewText(R.id.next_time, nextText(context, text.next, short = false))
            setTextViewText(R.id.probability, probabilityText(context, text, wide = false))
            setTextViewText(
                R.id.last_reset,
                if (display.stats.hasHistory) {
                    context.getString(
                        R.string.widget_orb_last_format,
                        lastText(context, text.last),
                        display.stats.resetsLast7Days?.toString() ?: getStringNotAvailable(context)
                    )
                } else {
                    context.getString(R.string.widget_orb_last_unknown_format, lastText(context, text.last))
                }
            )
            setTextViewText(R.id.footer, footerText(context, text, refreshing))
            setRefreshState(refreshing)
            setAccessibility(context, this, display, refreshing)
            wireClicks(context, this, state, titleClickable = true)
        }
    }

    private fun commandDeckViews(
        context: Context,
        state: WidgetState,
        display: WidgetVariantDisplay,
        refreshing: Boolean
    ): RemoteViews {
        val text = display.text
        return RemoteViews(context.packageName, R.layout.widget_command_deck).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, text.statusEmoji, text.statusLabel)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.next_time, nextText(context, text.next, short = false))
            setTextViewText(R.id.command_h24, percentText(context, text.h24))
            setTextViewText(R.id.command_h48, percentText(context, text.h48))
            setTextViewText(
                R.id.last_reset,
                context.getString(R.string.widget_command_last_format, lastText(context, text.last))
            )
            setTextViewText(R.id.command_stats, statsText(context, display.stats))
            setTextViewText(R.id.footer, footerText(context, text, refreshing))
            setRefreshState(refreshing)
            setAccessibility(context, this, display, refreshing)
            wireClicks(context, this, state, titleClickable = true)
        }
    }

    private fun RemoteViews.setRefreshState(refreshing: Boolean) {
        setViewVisibility(R.id.refresh, if (refreshing) View.GONE else View.VISIBLE)
        setViewVisibility(R.id.refresh_loading, if (refreshing) View.VISIBLE else View.GONE)
    }

    private fun setAccessibility(
        context: Context,
        views: RemoteViews,
        display: WidgetVariantDisplay,
        refreshing: Boolean
    ) {
        val text = display.text
        val next = nextText(context, text.next, short = true)
        val probability = probabilityText(context, text, wide = false)
        val h24 = percentText(context, text.h24)
        val h48 = percentText(context, text.h48)
        val updated = text.updatedAt?.format(timeFormatter) ?: getStringNotAvailable(context)
        val rootDescription = when (display.variant) {
            WidgetVariant.STANDARD_WIDE,
            WidgetVariant.STANDARD_SMALL -> context.getString(
                R.string.widget_accessibility_format,
                text.statusLabel,
                next,
                probability,
                updated
            )
            WidgetVariant.PULSE_ORB -> context.getString(
                R.string.widget_variant_accessibility_format,
                context.getString(R.string.widget_title_pulse_orb),
                text.statusLabel,
                next,
                h24,
                h48,
                updated
            )
            WidgetVariant.COMMAND_DECK -> context.getString(
                R.string.widget_variant_accessibility_format,
                context.getString(R.string.widget_title_command_deck),
                text.statusLabel,
                next,
                h24,
                h48,
                updated
            )
        }
        views.setContentDescription(R.id.widget_root, rootDescription)
        views.setContentDescription(
            R.id.refresh,
            context.getString(if (refreshing) R.string.widget_refresh_loading else R.string.widget_refresh_description)
        )
        views.setContentDescription(R.id.refresh_loading, context.getString(R.string.widget_refresh_loading))
        views.setContentDescription(
            R.id.status_label,
            context.getString(R.string.widget_status_accessibility_label, text.statusLabel)
        )
        when (display.variant) {
            WidgetVariant.PULSE_ORB -> views.setContentDescription(
                R.id.status_orb,
                context.getString(R.string.widget_orb_accessibility_format, text.statusLabel)
            )
            WidgetVariant.COMMAND_DECK -> {
                views.setContentDescription(
                    R.id.command_h24,
                    context.getString(
                        R.string.widget_metric_accessibility_format,
                        context.getString(R.string.probability_24h_label),
                        h24
                    )
                )
                views.setContentDescription(
                    R.id.command_h48,
                    context.getString(
                        R.string.widget_metric_accessibility_format,
                        context.getString(R.string.probability_48h_label),
                        h48
                    )
                )
            }
            else -> Unit
        }
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
        WidgetLastKind.NONE -> getStringNotAvailable(context)
        WidgetLastKind.MINUTES_AGO -> context.getString(R.string.widget_last_minutes, last.amount)
        WidgetLastKind.HOURS_AGO -> context.getString(R.string.widget_last_hours, last.amount)
        WidgetLastKind.ABSOLUTE -> last.localTime?.format(dateTimeFormatter).orEmpty()
    }

    private fun footerText(context: Context, display: WidgetTextModel, refreshing: Boolean): String {
        if (refreshing) return context.getString(R.string.widget_footer_loading)
        val updated = display.updatedAt?.format(timeFormatter) ?: getStringNotAvailable(context)
        val suffix = when {
            display.sourceStale -> context.getString(R.string.widget_footer_stale_suffix)
            display.hasError -> context.getString(R.string.widget_footer_error_suffix)
            else -> ""
        }
        return context.getString(R.string.widget_footer_format, updated, suffix)
    }

    private fun probabilityText(context: Context, display: WidgetTextModel, wide: Boolean): String {
        val h24 = percentText(context, display.h24)
        val h48 = percentText(context, display.h48)
        return if (wide) {
            context.getString(R.string.widget_probability_wide, h24, h48)
        } else {
            context.getString(R.string.widget_probability_small, h24, h48)
        }
    }

    private fun percentText(context: Context, value: Int?): String =
        value?.let { context.getString(R.string.percent_value, it) } ?: getStringNotAvailable(context)

    private fun statsText(context: Context, stats: WidgetStatsDisplay): String {
        if (!stats.hasHistory) return context.getString(R.string.widget_stats_placeholder)
        val count = stats.resetsLast7Days?.toString() ?: getStringNotAvailable(context)
        val average = stats.averageIntervalHours?.let {
            context.getString(R.string.widget_hours_compact, it)
        } ?: getStringNotAvailable(context)
        val streak = stats.streakCount?.toString() ?: getStringNotAvailable(context)
        return context.getString(R.string.widget_command_stats_format, count, average, streak)
    }

    private fun getStringNotAvailable(context: Context): String = context.getString(R.string.not_available)

    private fun levelColor(context: Context, level: ResetLevel): Int =
        ContextCompat.getColor(context, levelColorResource(level))

    private fun levelColorResource(level: ResetLevel): Int = when (level) {
        ResetLevel.CONFIRMED -> R.color.status_confirmed
        ResetLevel.VERY_LIKELY -> R.color.status_likely
        ResetLevel.POSSIBLE -> R.color.status_possible
        ResetLevel.LOW -> R.color.status_low
        ResetLevel.STALE -> R.color.status_stale
        ResetLevel.UNLIKELY, ResetLevel.UNKNOWN -> R.color.status_unknown
    }

    private fun levelOrbResource(level: ResetLevel): Int = when (level) {
        ResetLevel.CONFIRMED -> R.drawable.widget_orb_confirmed
        ResetLevel.VERY_LIKELY -> R.drawable.widget_orb_likely
        ResetLevel.POSSIBLE -> R.drawable.widget_orb_possible
        ResetLevel.LOW -> R.drawable.widget_orb_low
        ResetLevel.STALE -> R.drawable.widget_orb_stale
        ResetLevel.UNLIKELY, ResetLevel.UNKNOWN -> R.drawable.widget_orb_core
    }

    private val clockFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
}

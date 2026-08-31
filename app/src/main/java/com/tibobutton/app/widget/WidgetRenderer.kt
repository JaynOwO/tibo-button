package com.tibobutton.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.view.View
import androidx.core.content.ContextCompat
import com.tibobutton.app.MainActivity
import com.tibobutton.app.R
import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.data.WidgetState
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object WidgetRenderer {
    fun updateAll(context: Context, refreshing: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context)
        val state = WidgetPrefs.load(context)

        val wideComponent = ComponentName(context, TiboWideWidgetProvider::class.java)
        manager.getAppWidgetIds(wideComponent).forEach { id ->
            manager.updateAppWidget(id, wideViews(context, state, refreshing))
        }

        val smallComponent = ComponentName(context, TiboSmallWidgetProvider::class.java)
        manager.getAppWidgetIds(smallComponent).forEach { id ->
            manager.updateAppWidget(id, smallViews(context, state, refreshing))
        }
    }

    private fun wideViews(context: Context, state: WidgetState, refreshing: Boolean): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_wide).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, state.level.emoji, state.level.label)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.probability, probabilityText(state, wide = true, context = context))
            setTextViewText(R.id.next_time, nextText(state, short = false))
            val last = formatWhen(state.lastResetAt)
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
                if (refreshing) context.getString(R.string.widget_footer_loading) else footerText(context, state)
            )
            setViewVisibility(R.id.refresh, if (refreshing) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.refresh_loading, if (refreshing) View.VISIBLE else View.GONE)
            wireClicks(context, this, state, titleClickable = true)
        }
    }

    private fun smallViews(context: Context, state: WidgetState, refreshing: Boolean): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_small).apply {
            setTextViewText(
                R.id.status_label,
                context.getString(R.string.widget_status_format, state.level.emoji, state.level.label)
            )
            setTextColor(R.id.status_label, levelColor(context, state.level))
            setTextViewText(R.id.probability, probabilityText(state, wide = false, context = context))
            setTextViewText(
                R.id.next_time,
                context.getString(R.string.widget_next_format, nextText(state, short = true))
            )
            setTextViewText(
                R.id.last_reset,
                context.getString(R.string.widget_last_format, formatWhen(state.lastResetAt))
            )
            setTextViewText(
                R.id.footer,
                if (refreshing) context.getString(R.string.widget_footer_loading) else footerText(context, state)
            )
            setViewVisibility(R.id.refresh, if (refreshing) View.GONE else View.VISIBLE)
            setViewVisibility(R.id.refresh_loading, if (refreshing) View.VISIBLE else View.GONE)
            wireClicks(context, this, state, titleClickable = false)
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

    private fun nextText(state: WidgetState, short: Boolean): String {
        state.nextResetAt?.let { instant ->
            val now = Instant.now()
            val local = instant.atZone(ZoneId.systemDefault())
            val clock = local.format(DateTimeFormatter.ofPattern("HH:mm"))
            val hours = Duration.between(now, instant).toHours()
            return when {
                instant.isBefore(now) -> "正在窗口内"
                hours < 24 -> if (short) "今天 $clock" else "今天 $clock · ${countdownCompact(now, instant)}"
                else -> local.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
            }
        }
        if (state.nextResetKnownButUnparsed) return if (short) "已排期" else "已排期 · 时间见来源"
        return if (state.level == ResetLevel.VERY_LIKELY || state.level == ResetLevel.POSSIBLE) "尚未确定" else "未知"
    }

    private fun countdownCompact(now: Instant, target: Instant): String {
        val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private fun formatWhen(instant: Instant?): String {
        instant ?: return "—"
        val now = Instant.now()
        val minutes = Duration.between(instant, now).toMinutes()
        if (minutes in 0L..59L) return "${minutes.coerceAtLeast(1L)}分钟前"
        val hours = minutes / 60
        if (hours in 1L..23L) return "${hours}小时前"
        return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }

    private fun footerText(context: Context, state: WidgetState): String {
        val updated = state.updatedAt?.atZone(ZoneId.systemDefault())
            ?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—"
        val suffix = when {
            state.sourceStale -> " · ⚠ 数据过期"
            state.error != null -> " · ⚠ 刷新失败，显示缓存"
            else -> ""
        }
        return context.getString(R.string.widget_footer_format, updated, suffix)
    }

    private fun probabilityText(state: WidgetState, wide: Boolean, context: Context): String {
        val h24 = state.h24?.let { "$it%" } ?: "—"
        val h48 = state.h48?.let { "$it%" } ?: "—"
        return if (wide) {
            context.getString(R.string.widget_probability_wide, h24, h48)
        } else {
            context.getString(R.string.widget_probability_small, h24)
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
}

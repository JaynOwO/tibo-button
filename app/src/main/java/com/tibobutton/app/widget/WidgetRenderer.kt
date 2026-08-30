package com.tibobutton.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
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
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val state = WidgetPrefs.load(context)

        val wideComponent = ComponentName(context, TiboWideWidgetProvider::class.java)
        manager.getAppWidgetIds(wideComponent).forEach { id ->
            manager.updateAppWidget(id, wideViews(context, state))
        }

        val smallComponent = ComponentName(context, TiboSmallWidgetProvider::class.java)
        manager.getAppWidgetIds(smallComponent).forEach { id ->
            manager.updateAppWidget(id, smallViews(context, state))
        }
    }

    private fun wideViews(context: Context, state: WidgetState): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_wide).apply {
            setTextViewText(R.id.status_label, "${state.level.emoji} ${state.level.label}")
            setTextColor(R.id.status_label, levelColor(state.level))
            setTextViewText(R.id.probability, probabilityText(state, wide = true))
            setTextViewText(R.id.next_time, nextText(state, short = false))
            setTextViewText(R.id.last_reset, "上次：${formatWhen(state.lastResetAt)}")
            setTextViewText(R.id.footer, footerText(state))
            wireClicks(context, this, state, titleClickable = true)
        }
    }

    private fun smallViews(context: Context, state: WidgetState): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_small).apply {
            setTextViewText(R.id.status_label, "${state.level.emoji} ${state.level.label}")
            setTextColor(R.id.status_label, levelColor(state.level))
            setTextViewText(R.id.probability, probabilityText(state, wide = false))
            setTextViewText(R.id.next_time, "下次：${nextText(state, short = true)}")
            setTextViewText(R.id.last_reset, "上次：${formatWhen(state.lastResetAt)}")
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
            // Title remains an unobtrusive shortcut to the strongest available evidence.
            if (titleClickable) views.setOnClickPendingIntent(R.id.title, sourcePending)
        }
    }

    private fun probabilityText(state: WidgetState, wide: Boolean): String {
        val h24 = state.h24?.let { "$it%" } ?: "—"
        val h48 = state.h48?.let { "$it%" } ?: "—"
        return if (wide) "24H  $h24   48H  $h48" else "24H  $h24"
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

    private fun footerText(state: WidgetState): String {
        val updated = state.updatedAt?.atZone(ZoneId.systemDefault())
            ?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "—"
        val suffix = when {
            state.sourceStale -> " · ⚠ 数据过期"
            state.error != null -> " · ⚠ 刷新失败，显示缓存"
            else -> ""
        }
        return "Reset Beacon · 更新 $updated$suffix"
    }

    private fun levelColor(level: ResetLevel): Int = when (level) {
        ResetLevel.CONFIRMED -> Color.rgb(184, 132, 255)
        ResetLevel.VERY_LIKELY -> Color.rgb(255, 164, 78)
        ResetLevel.POSSIBLE -> Color.rgb(255, 206, 84)
        ResetLevel.LOW -> Color.rgb(112, 164, 255)
        ResetLevel.UNLIKELY -> Color.rgb(174, 182, 194)
        ResetLevel.STALE -> Color.rgb(255, 104, 104)
        ResetLevel.UNKNOWN -> Color.rgb(174, 182, 194)
    }
}

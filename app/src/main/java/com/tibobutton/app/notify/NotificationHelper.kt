package com.tibobutton.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.tibobutton.app.MainActivity
import com.tibobutton.app.R
import com.tibobutton.app.data.NotificationPrefs
import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetState
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object NotificationHelper {
    private const val CHANNEL_ID = "tibo_reset_alerts"
    private const val CHANNEL_NAME = "重置提醒"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Tibo / Reset Beacon 的 Codex 与 ChatGPT Work 共享额度重置提醒"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canNotify(context: Context): Boolean {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    fun maybeNotify(context: Context, old: WidgetState, new: WidgetState) {
        // First successful sync establishes a baseline. Do not replay historical alerts on install.
        if (old.updatedAt == null && old.lastResetAt == null) {
            NotificationPrefs.primeBaseline(context, new)
            return
        }
        if (!canNotify(context)) return

        val settings = NotificationPrefs.load(context)

        val completedChanged = new.lastResetAt != null &&
            (old.lastResetAt == null || new.lastResetAt.isAfter(old.lastResetAt))
        if (completedChanged && settings.completed) {
            val fingerprint = new.lastResetAt.toString()
            if (NotificationPrefs.lastCompletedFingerprint(context) != fingerprint) {
                send(
                    context,
                    2001,
                    "🔴 Tibo 按按钮了",
                    "Reset Beacon 记录到新的共享额度重置。Codex / ChatGPT Work 可以检查一下了。",
                    new.evidenceUrl
                )
                NotificationPrefs.markCompleted(context, fingerprint)
                return
            }
        }

        val scheduleChanged = new.level == ResetLevel.CONFIRMED && (
            old.level != ResetLevel.CONFIRMED ||
                old.nextResetAt != new.nextResetAt ||
                old.nextResetKnownButUnparsed != new.nextResetKnownButUnparsed
            )
        if (scheduleChanged && settings.confirmed) {
            val fingerprint = NotificationPrefs.confirmedFingerprint(new)
            if (fingerprint != null && NotificationPrefs.lastConfirmedFingerprint(context) != fingerprint) {
                val whenText = new.nextResetAt?.atZone(ZoneId.systemDefault())
                    ?.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
                    ?.let { "预计 $it" }
                    ?: "Reset Beacon 已记录明确排期"
                send(context, 2002, "🟣 Tibo Reset 已确定", whenText, new.evidenceUrl)
                NotificationPrefs.markConfirmed(context, fingerprint)
                return
            }
        }

        val becameLikely = new.level == ResetLevel.VERY_LIKELY &&
            old.level != ResetLevel.VERY_LIKELY && old.level != ResetLevel.CONFIRMED
        if (becameLikely && settings.likely) {
            val fingerprint = NotificationPrefs.likelyFingerprint(new)
            if (fingerprint != null && NotificationPrefs.lastLikelyFingerprint(context) != fingerprint) {
                val p = new.h24?.let { "未来 24 小时 $it%" } ?: "未来 24 小时概率升高"
                send(context, 2003, "🟠 Tibo Reset 很可能", p, new.evidenceUrl)
                NotificationPrefs.markLikely(context, fingerprint)
            }
        }
    }

    fun sendTest(context: Context) {
        if (!canNotify(context)) return
        send(context, 2099, "⚡ Tibo Button 测试通知", "通知通道工作正常。下一次按钮有动静就能叫醒你。", null)
    }

    private fun send(context: Context, id: Int, title: String, text: String, url: String?) {
        ensureChannel(context)
        val intent = if (!url.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            Intent(context, MainActivity::class.java)
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }
}

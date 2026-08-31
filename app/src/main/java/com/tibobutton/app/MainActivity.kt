package com.tibobutton.app

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import com.tibobutton.app.data.NotificationPrefs
import com.tibobutton.app.data.ResetApi
import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.data.WidgetState
import com.tibobutton.app.notify.NotificationHelper
import com.tibobutton.app.work.WidgetScheduler
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var evidenceText: TextView
    private lateinit var historyText: TextView
    private lateinit var permissionStatus: TextView
    private lateinit var permissionButton: Button
    private lateinit var evidenceButton: Button
    private lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        NotificationHelper.ensureChannel(this)

        status = findViewById(R.id.status)
        evidenceText = findViewById(R.id.evidenceText)
        historyText = findViewById(R.id.historyText)
        permissionStatus = findViewById(R.id.notificationPermissionStatus)
        permissionButton = findViewById(R.id.notificationPermissionButton)
        evidenceButton = findViewById(R.id.evidenceButton)
        refreshButton = findViewById(R.id.refreshButton)

        val settings = NotificationPrefs.load(this)
        findViewById<CheckBox>(R.id.notifyConfirmed).apply {
            isChecked = settings.confirmed
            setOnCheckedChangeListener { _, checked -> NotificationPrefs.setConfirmed(this@MainActivity, checked) }
        }
        findViewById<CheckBox>(R.id.notifyCompleted).apply {
            isChecked = settings.completed
            setOnCheckedChangeListener { _, checked -> NotificationPrefs.setCompleted(this@MainActivity, checked) }
        }
        findViewById<CheckBox>(R.id.notifyLikely).apply {
            isChecked = settings.likely
            setOnCheckedChangeListener { _, checked -> NotificationPrefs.setLikely(this@MainActivity, checked) }
        }

        refreshButton.setOnClickListener {
            refreshButton.isEnabled = false
            refreshButton.text = "正在刷新…"
            val workId = WidgetScheduler.refreshNow(this)
            val workInfo = WorkManager.getInstance(this).getWorkInfoById(workId)
            workInfo.addListener({
                val info = runCatching { workInfo.get() }.getOrNull()
                if (info?.state?.isFinished == true) {
                    runOnUiThread {
                        renderCached()
                        refreshButton.isEnabled = true
                        refreshButton.text = "立即刷新"
                    }
                }
            }, ContextCompat.getMainExecutor(this))
        }

        evidenceButton.setOnClickListener {
            val url = WidgetPrefs.load(this).evidenceUrl ?: ResetApi.SITE_URL
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        findViewById<Button>(R.id.sourceButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ResetApi.SITE_URL)))
        }

        permissionButton.setOnClickListener { requestOrOpenNotificationSettings() }

        findViewById<Button>(R.id.testNotificationButton).setOnClickListener {
            if (NotificationHelper.canNotify(this)) {
                NotificationHelper.sendTest(this)
                Toast.makeText(this, "测试通知已发送", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "先允许系统通知", Toast.LENGTH_SHORT).show()
                requestOrOpenNotificationSettings()
            }
        }

        WidgetScheduler.ensurePeriodic(this)
        WidgetScheduler.refreshNow(this)
        renderCached()
        updateNotificationPermissionUi()
    }

    override fun onResume() {
        super.onResume()
        renderCached()
        updateNotificationPermissionUi()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateNotificationPermissionUi()
    }

    private fun requestOrOpenNotificationSettings() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    private fun updateNotificationPermissionUi() {
        val enabled = if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) false else getSystemService(NotificationManager::class.java).areNotificationsEnabled()
        permissionStatus.text = if (enabled) "通知权限：✅ 已开启" else "通知权限：⚠ 未开启"
        permissionButton.text = if (enabled) "打开通知设置" else "允许系统通知"
    }

    private fun renderCached() {
        val s = WidgetPrefs.load(this)
        val fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm").withZone(ZoneId.systemDefault())
        status.text = buildString {
            append("${s.level.emoji} ${s.level.label}\n\n")
            append("24H：${s.h24?.let { "$it%" } ?: "—"}\n")
            append("48H：${s.h48?.let { "$it%" } ?: "—"}\n")
            append("上次重置：${s.lastResetAt?.let(fmt::format) ?: "—"}\n")
            append("下次重置：")
            append(
                when {
                    s.nextResetAt != null -> fmt.format(s.nextResetAt)
                    s.nextResetKnownButUnparsed -> "已排期，具体时间请点来源"
                    else -> "未知"
                }
            )
            append("\n更新：${s.updatedAt?.let(fmt::format) ?: "—"}")
            append("\n\n${statusExplanation(s)}")
            if (s.error != null) append("\n\n⚠ ${s.error}\n当前显示上一次成功缓存。")
        }

        evidenceText.text = buildString {
            append("当前依据\n\n")
            val canonical = listOfNotNull(s.canonicalHeadline, s.canonicalSecondLine).joinToString("\n")
            if (canonical.isNotBlank()) {
                append("Reset Beacon：\n$canonical")
            } else if (!s.evidenceSummary.isNullOrBlank()) {
                append(s.evidenceSummary)
            } else {
                append("暂无额外文字依据；可打开 Reset Beacon 查看当前记录。")
            }
        }
        evidenceButton.isEnabled = s.evidenceUrl != null
        historyText.text = formatHistoryPulse(s, fmt)
    }

    private fun statusExplanation(state: WidgetState): String = when (state.level) {
        ResetLevel.CONFIRMED -> if (state.nextResetAt != null) {
            "说明：Reset Beacon 已给出明确排期；时间已换算为本机时区。"
        } else {
            "说明：已检测到明确排期/确认信号，但没有可靠的机器可读时间，因此不猜具体时刻。"
        }
        ResetLevel.VERY_LIKELY -> "说明：未来 24H 概率已达到 75% 或以上，但目前还没有明确排期。"
        ResetLevel.POSSIBLE -> "说明：存在公开 intent 信号，或 24H 概率处于 45%–74%。"
        ResetLevel.LOW -> "说明：24H 概率处于 20%–44%，暂时更像风声而不是按钮声。"
        ResetLevel.UNLIKELY -> "说明：当前 24H 概率低于 20%，没有足够证据判断近期会重置。"
        ResetLevel.STALE -> "说明：上游预测已过期；旧概率不会继续伪装成实时数据。"
        ResetLevel.UNKNOWN -> "说明：尚未取得足够的有效预测数据。"
    }

    private fun formatHistoryPulse(state: WidgetState, fmt: DateTimeFormatter): String = buildString {
        append("🔥 Reset Pulse\n\n")
        append("近 7 天：${state.resetsLast7Days} 次")
        if (state.streakCount > 0) append(" · 近期连击：${state.streakCount} 次（间隔≤72h）")
        state.averageIntervalHours?.let { hours ->
            append("\n最近记录平均间隔：${formatInterval(hours)}")
        }
        append("\n\n最近 ${state.recentResets.size.coerceAtMost(7)} 次已完成共享重置")
        if (state.recentResets.isEmpty()) {
            append("\n暂无可用历史记录。")
        } else {
            state.recentResets.forEachIndexed { index, item ->
                append("\n${index + 1}. ${fmt.format(item.occurredAt)}")
                if (item.summary.isNotBlank()) append(" · ${item.summary.take(64)}")
            }
        }
        append("\n\n注：历史统计只计算 Reset Beacon 标记为 completed 且属于广泛/全体范围的事件。")
    }

    private fun formatInterval(hours: Long): String {
        if (hours < 24) return "${hours} 小时"
        val days = hours / 24
        val remain = hours % 24
        return if (remain == 0L) "${days} 天" else "${days} 天 ${remain} 小时"
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 42
    }
}

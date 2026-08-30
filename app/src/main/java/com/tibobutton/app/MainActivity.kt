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
import androidx.work.WorkManager
import com.tibobutton.app.data.NotificationPrefs
import com.tibobutton.app.data.ResetApi
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.notify.NotificationHelper
import com.tibobutton.app.work.WidgetScheduler
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var evidenceText: TextView
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
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(workId).observe(this) { info ->
                if (info == null || !info.state.isFinished) return@observe
                renderCached()
                refreshButton.isEnabled = true
                refreshButton.text = "立即刷新"
            }
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
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 42
    }
}

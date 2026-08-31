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
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.tibobutton.app.data.NotificationPrefs
import com.tibobutton.app.data.ResetApi
import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.data.WidgetState
import com.tibobutton.app.notify.NotificationHelper
import com.tibobutton.app.ui.PulseTimelineModel
import com.tibobutton.app.ui.PulseTimelineView
import com.tibobutton.app.update.StableRelease
import com.tibobutton.app.update.UpdateCheckResult
import com.tibobutton.app.update.UpdateDownloadResult
import com.tibobutton.app.update.UpdateManager
import com.tibobutton.app.update.UpdatePrefs
import com.tibobutton.app.work.WidgetScheduler
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var nextResetText: TextView
    private lateinit var h24Text: TextView
    private lateinit var h48Text: TextView
    private lateinit var lastResetText: TextView
    private lateinit var updatedText: TextView
    private lateinit var statusExplanationText: TextView
    private lateinit var statusErrorText: TextView
    private lateinit var evidenceText: TextView
    private lateinit var historyText: TextView
    private lateinit var pulseStatsText: TextView
    private lateinit var pulseEmptyText: TextView
    private lateinit var pulseTimeline: PulseTimelineView
    private lateinit var permissionStatus: TextView
    private lateinit var permissionButton: Button
    private lateinit var evidenceButton: Button
    private lateinit var refreshButton: Button
    private lateinit var refreshLoading: ProgressBar
    private lateinit var installedVersionText: TextView
    private lateinit var latestVersionText: TextView
    private lateinit var updateStatusText: TextView
    private lateinit var releaseNotesText: TextView
    private lateinit var checkUpdateButton: Button
    private lateinit var downloadUpdateButton: Button
    private lateinit var viewReleaseButton: Button
    private lateinit var updateManager: UpdateManager
    private var latestRelease: StableRelease? = null
    private var pendingUpdateAfterPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        updateManager = UpdateManager(this)
        NotificationHelper.ensureChannel(this)

        status = findViewById(R.id.status)
        nextResetText = findViewById(R.id.nextResetText)
        h24Text = findViewById(R.id.h24Text)
        h48Text = findViewById(R.id.h48Text)
        lastResetText = findViewById(R.id.lastResetText)
        updatedText = findViewById(R.id.updatedText)
        statusExplanationText = findViewById(R.id.statusExplanationText)
        statusErrorText = findViewById(R.id.statusErrorText)
        evidenceText = findViewById(R.id.evidenceText)
        historyText = findViewById(R.id.historyText)
        pulseStatsText = findViewById(R.id.pulseStatsText)
        pulseEmptyText = findViewById(R.id.pulseEmptyText)
        pulseTimeline = findViewById(R.id.pulseTimeline)
        permissionStatus = findViewById(R.id.notificationPermissionStatus)
        permissionButton = findViewById(R.id.notificationPermissionButton)
        evidenceButton = findViewById(R.id.evidenceButton)
        refreshButton = findViewById(R.id.refreshButton)
        refreshLoading = findViewById(R.id.refreshLoading)
        installedVersionText = findViewById(R.id.installedVersionText)
        latestVersionText = findViewById(R.id.latestVersionText)
        updateStatusText = findViewById(R.id.updateStatusText)
        releaseNotesText = findViewById(R.id.releaseNotesText)
        checkUpdateButton = findViewById(R.id.checkUpdateButton)
        downloadUpdateButton = findViewById(R.id.downloadUpdateButton)
        viewReleaseButton = findViewById(R.id.viewReleaseButton)
        findViewById<CheckBox>(R.id.autoCheckUpdates).apply {
            isChecked = UpdatePrefs.autoCheckEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, checked ->
                UpdatePrefs.setAutoCheckEnabled(this@MainActivity, checked)
            }
        }
        renderUpdateInitial()

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
            refreshLoading.visibility = View.VISIBLE
            refreshButton.text = getString(R.string.refreshing_with_ellipsis)
            refreshButton.contentDescription = getString(R.string.refreshing)
            val workId = WidgetScheduler.refreshNow(this)
            val workInfo = WorkManager.getInstance(this).getWorkInfoById(workId)
            workInfo.addListener({
                val info = runCatching { workInfo.get() }.getOrNull()
                val retryScheduled = info?.state == WorkInfo.State.ENQUEUED && info.runAttemptCount > 0
                if (info?.state?.isFinished == true || retryScheduled) {
                    runOnUiThread {
                        renderCached()
                        refreshButton.isEnabled = true
                        refreshLoading.visibility = View.GONE
                        refreshButton.text = getString(R.string.refresh_now)
                        refreshButton.contentDescription = getString(R.string.refresh_button_description)
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
                Toast.makeText(this, getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.enable_notifications_first), Toast.LENGTH_SHORT).show()
                requestOrOpenNotificationSettings()
            }
        }

        checkUpdateButton.setOnClickListener { checkForUpdates() }
        downloadUpdateButton.setOnClickListener { startUpdateDownload() }
        viewReleaseButton.setOnClickListener {
            val url = latestRelease?.htmlUrl ?: UpdateManager.LATEST_RELEASE_FALLBACK_URL
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        WidgetScheduler.ensurePeriodic(this)
        WidgetScheduler.refreshNow(this)
        renderCached()
        updateNotificationPermissionUi()
        if (UpdatePrefs.autoCheckEnabled(this)) checkForUpdates()
    }

    override fun onResume() {
        super.onResume()
        renderCached()
        updateNotificationPermissionUi()
        if (pendingUpdateAfterPermission) {
            if (updateManager.canRequestPackageInstalls()) {
                pendingUpdateAfterPermission = false
                startUpdateDownload()
            } else {
                downloadUpdateButton.isEnabled = true
                updateStatusText.text = getString(R.string.update_permission_required)
            }
        }
    }

    override fun onDestroy() {
        updateManager.shutdown()
        super.onDestroy()
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
        permissionStatus.text = getString(
            if (enabled) R.string.notification_permission_enabled
            else R.string.notification_permission_disabled
        )
        permissionButton.text = getString(
            if (enabled) R.string.open_notification_settings
            else R.string.allow_notifications
        )
    }

    private fun renderCached() {
        val s = WidgetPrefs.load(this)
        val fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm").withZone(ZoneId.systemDefault())
        status.text = "${s.level.emoji} ${s.level.label}"
        status.setTextColor(statusColor(s.level))
        nextResetText.text = when {
            s.nextResetAt != null -> fmt.format(s.nextResetAt)
            s.nextResetKnownButUnparsed -> getString(R.string.status_next_scheduled_unparsed)
            else -> getString(R.string.status_next_unknown)
        }
        h24Text.text = s.h24?.let { getString(R.string.percent_value, it) } ?: getString(R.string.not_available)
        h48Text.text = s.h48?.let { getString(R.string.percent_value, it) } ?: getString(R.string.not_available)
        lastResetText.text = getString(
            R.string.last_reset_format,
            s.lastResetAt?.let(fmt::format) ?: getString(R.string.not_available)
        )
        updatedText.text = getString(
            R.string.updated_format,
            s.updatedAt?.let(fmt::format) ?: getString(R.string.not_available)
        )
        statusExplanationText.text = statusExplanation(s)
        status.contentDescription = getString(
            R.string.status_accessibility_format,
            s.level.label,
            nextResetText.text,
            h24Text.text,
            h48Text.text
        )
        if (s.error != null) {
            statusErrorText.visibility = View.VISIBLE
            statusErrorText.text = getString(R.string.status_error_cache_format, s.error)
        } else {
            statusErrorText.visibility = View.GONE
        }

        evidenceText.text = buildString {
            val canonical = listOfNotNull(s.canonicalHeadline, s.canonicalSecondLine).joinToString("\n")
            if (canonical.isNotBlank()) {
                append(getString(R.string.evidence_reset_beacon_format, canonical))
            } else if (!s.evidenceSummary.isNullOrBlank()) {
                append(s.evidenceSummary)
            } else {
                append(getString(R.string.source_unavailable))
            }
        }
        evidenceButton.isEnabled = s.evidenceUrl != null
        val pulseModel = PulseTimelineModel.from(s)
        pulseTimeline.setModel(pulseModel)
        pulseStatsText.text = getString(
            R.string.pulse_stats_format,
            s.resetsLast7Days,
            pulseModel.averageIntervalHours?.let(::formatInterval) ?: getString(R.string.not_available),
            s.streakCount
        )
        pulseEmptyText.visibility = if (pulseModel.hasTimeline) View.GONE else View.VISIBLE
        pulseEmptyText.text = if (pulseModel.points.isEmpty()) {
            getString(R.string.pulse_empty_no_history)
        } else {
            getString(R.string.pulse_empty_one)
        }
        historyText.text = formatHistoryPulse(s, fmt)
    }

    private fun statusColor(level: ResetLevel): Int = ContextCompat.getColor(this, when (level) {
        ResetLevel.CONFIRMED -> R.color.status_confirmed
        ResetLevel.VERY_LIKELY -> R.color.status_likely
        ResetLevel.POSSIBLE -> R.color.status_possible
        ResetLevel.LOW -> R.color.status_low
        ResetLevel.STALE -> R.color.status_stale
        ResetLevel.UNLIKELY, ResetLevel.UNKNOWN -> R.color.status_unknown
    })

    private fun renderUpdateInitial() {
        installedVersionText.text = getString(
            R.string.installed_version_format,
            updateManager.installedVersionName()
        )
        latestVersionText.text = getString(R.string.latest_version_unchecked)
        updateStatusText.text = getString(R.string.update_auto_check_hint)
        releaseNotesText.text = getString(R.string.release_notes_unavailable)
        releaseNotesText.visibility = View.GONE
        downloadUpdateButton.visibility = View.GONE
        viewReleaseButton.isEnabled = true
    }

    private fun checkForUpdates() {
        checkUpdateButton.isEnabled = false
        downloadUpdateButton.visibility = View.GONE
        releaseNotesText.visibility = View.GONE
        updateStatusText.text = getString(R.string.refresh_status_checking)
        releaseNotesText.text = getString(R.string.release_notes_reading)
        val accepted = updateManager.checkLatest { result ->
            checkUpdateButton.isEnabled = true
            when (result) {
                is UpdateCheckResult.Success -> showRelease(result.release)
                is UpdateCheckResult.Failure -> {
                    latestRelease = null
                    latestVersionText.text = getString(R.string.latest_version_check_failed)
                    releaseNotesText.text = getString(R.string.release_notes_unavailable_short)
                    releaseNotesText.visibility = View.GONE
                    updateStatusText.text = getString(R.string.update_check_failed_format, result.message)
                    downloadUpdateButton.visibility = View.GONE
                }
            }
        }
        if (!accepted) {
            checkUpdateButton.isEnabled = true
            updateStatusText.text = getString(R.string.update_check_in_progress)
        }
    }

    private fun showRelease(release: StableRelease) {
        latestRelease = release
        latestVersionText.text = getString(R.string.latest_version_format, release.version)

        val installed = updateManager.installedVersion()
        when {
            installed == null -> {
                updateStatusText.text = getString(R.string.installed_version_invalid)
                downloadUpdateButton.visibility = View.GONE
                releaseNotesText.visibility = View.GONE
            }
            release.version > installed -> {
                updateStatusText.text = getString(R.string.update_available_status)
                downloadUpdateButton.visibility = View.VISIBLE
                downloadUpdateButton.isEnabled = true
                downloadUpdateButton.text = getString(R.string.download_update)
                releaseNotesText.text = if (release.notes.isBlank()) {
                    getString(R.string.release_notes_empty)
                } else {
                    getString(R.string.release_notes_format, cleanReleaseNotes(release.notes))
                }
                releaseNotesText.visibility = View.VISIBLE
            }
            release.version == installed -> {
                updateStatusText.text = getString(R.string.update_latest_status)
                downloadUpdateButton.visibility = View.GONE
                releaseNotesText.visibility = View.GONE
            }
            else -> {
                updateStatusText.text = getString(R.string.update_downgrade_status)
                downloadUpdateButton.visibility = View.GONE
                releaseNotesText.visibility = View.GONE
            }
        }
    }

    private fun cleanReleaseNotes(notes: String): String = notes
        .replace("**", "")
        .replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1")
        .trim()

    private fun startUpdateDownload() {
        val release = latestRelease ?: run {
            updateStatusText.text = getString(R.string.update_check_first)
            return
        }
        val installed = updateManager.installedVersion()
        if (installed == null || release.version <= installed) {
            updateStatusText.text = getString(R.string.update_no_higher_version)
            downloadUpdateButton.visibility = View.GONE
            return
        }

        if (!updateManager.canRequestPackageInstalls()) {
            pendingUpdateAfterPermission = true
            downloadUpdateButton.isEnabled = false
            updateStatusText.text = getString(R.string.update_unknown_sources)
            openUnknownSourcesSettings()
            return
        }

        pendingUpdateAfterPermission = false
        downloadUpdateButton.isEnabled = false
        checkUpdateButton.isEnabled = false
        updateStatusText.text = getString(R.string.update_prepare_download_format, release.version)
        val accepted = updateManager.downloadAndVerify(release) { result ->
            when (result) {
                UpdateDownloadResult.NeedsUnknownSourcesPermission -> {
                    pendingUpdateAfterPermission = true
                    downloadUpdateButton.isEnabled = false
                    updateStatusText.text = getString(R.string.update_unknown_sources_short)
                    openUnknownSourcesSettings()
                }
                is UpdateDownloadResult.Progress -> {
                    val progress = if (result.percent >= 0) {
                        getString(R.string.percent_value, result.percent)
                    } else {
                        getString(R.string.unknown_progress)
                    }
                    downloadUpdateButton.text = getString(R.string.update_download_button_format, progress)
                    updateStatusText.text = getString(
                        R.string.update_download_status_format,
                        release.version,
                        progress
                    )
                }
                UpdateDownloadResult.Verifying -> {
                    downloadUpdateButton.text = getString(R.string.update_verifying_button)
                    updateStatusText.text = getString(R.string.update_verifying_status)
                }
                is UpdateDownloadResult.Verified -> {
                    downloadUpdateButton.text = getString(R.string.update_verified_button)
                    updateStatusText.text = getString(R.string.update_verified_status)
                    runCatching {
                        val uri = updateManager.installerUri(result.apk)
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(intent)
                    }.onFailure {
                        downloadUpdateButton.isEnabled = true
                        downloadUpdateButton.text = getString(R.string.download_update)
                        updateStatusText.text = getString(
                            R.string.update_installer_failed_format,
                            it.message ?: getString(R.string.unknown_error)
                        )
                    }
                }
                is UpdateDownloadResult.Failure -> {
                    checkUpdateButton.isEnabled = true
                    downloadUpdateButton.isEnabled = true
                    downloadUpdateButton.text = getString(R.string.download_update)
                    updateStatusText.text = getString(R.string.update_failed_format, result.message)
                }
            }
        }
        if (!accepted) {
            checkUpdateButton.isEnabled = true
            downloadUpdateButton.isEnabled = true
            downloadUpdateButton.text = getString(R.string.download_update)
            updateStatusText.text = getString(R.string.update_already_downloading)
        }
    }

    private fun openUnknownSourcesSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:$packageName")
        )
        runCatching { startActivity(intent) }.onFailure {
            pendingUpdateAfterPermission = false
            downloadUpdateButton.isEnabled = true
            updateStatusText.text = getString(
                R.string.update_permission_settings_failed_format,
                it.message ?: getString(R.string.unknown_error)
            )
        }
    }

    private fun statusExplanation(state: WidgetState): String = when (state.level) {
        ResetLevel.CONFIRMED -> if (state.nextResetAt != null) {
            getString(R.string.status_explanation_confirmed_timed)
        } else {
            getString(R.string.status_explanation_confirmed_unparsed)
        }
        ResetLevel.VERY_LIKELY -> getString(R.string.status_explanation_very_likely)
        ResetLevel.POSSIBLE -> getString(R.string.status_explanation_possible)
        ResetLevel.LOW -> getString(R.string.status_explanation_low)
        ResetLevel.UNLIKELY -> getString(R.string.status_explanation_unlikely)
        ResetLevel.STALE -> getString(R.string.status_explanation_stale)
        ResetLevel.UNKNOWN -> getString(R.string.status_explanation_unknown)
    }

    private fun formatHistoryPulse(state: WidgetState, fmt: DateTimeFormatter): String = buildString {
        append(getString(R.string.pulse_history_format, state.recentResets.size.coerceAtMost(7)))
        if (state.recentResets.isEmpty()) {
            append("\n${getString(R.string.pulse_history_empty)}")
        } else {
            state.recentResets.forEachIndexed { index, item ->
                val summary = item.summary.takeIf { it.isNotBlank() }?.take(64)
                val itemText = if (summary == null) {
                    getString(R.string.pulse_history_item_time_only, fmt.format(item.occurredAt))
                } else {
                    getString(R.string.pulse_history_item_format, fmt.format(item.occurredAt), summary)
                }
                append("\n${index + 1}. $itemText")
            }
        }
    }

    private fun formatInterval(hours: Long): String {
        if (hours < 24) return getString(R.string.duration_hours, hours)
        val days = hours / 24
        val remain = hours % 24
        return if (remain == 0L) {
            getString(R.string.duration_days, days)
        } else {
            getString(R.string.duration_days_hours, days, remain)
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 42
    }
}

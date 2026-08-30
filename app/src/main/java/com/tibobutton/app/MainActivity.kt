package com.tibobutton.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.tibobutton.app.data.ResetApi
import com.tibobutton.app.data.WidgetPrefs
import com.tibobutton.app.work.WidgetScheduler
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)

        findViewById<Button>(R.id.refreshButton).setOnClickListener {
            WidgetScheduler.refreshNow(this)
            status.postDelayed({ renderCached() }, 1400)
        }
        findViewById<Button>(R.id.sourceButton).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ResetApi.SITE_URL)))
        }

        WidgetScheduler.ensurePeriodic(this)
        WidgetScheduler.refreshNow(this)
        renderCached()
    }

    override fun onResume() {
        super.onResume()
        renderCached()
    }

    private fun renderCached() {
        val s = WidgetPrefs.load(this)
        val fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm").withZone(ZoneId.systemDefault())
        val text = buildString {
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
        status.text = text
    }
}

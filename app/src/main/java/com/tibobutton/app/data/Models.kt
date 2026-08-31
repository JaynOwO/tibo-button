package com.tibobutton.app.data

import java.time.Instant

data class ForecastSnapshot(
    val calculatedAt: Instant?,
    val validUntil: Instant?,
    val publicationState: String,
    val h24: Int?,
    val h48: Int?,
    val signalStage: String?,
    val answerState: String? = null,
    val answerHeadline: String? = null,
    val answerSecondLine: String? = null,
    val answerDeadline: Instant? = null
)

data class HistoryEvent(
    val eventKind: String,
    val status: String,
    val scope: String,
    val summary: String,
    val operativeSentence: String,
    val evidenceUrl: String?,
    val announcedAt: Instant?,
    val scheduledFor: Instant?
)

data class ResetHistoryItem(
    val occurredAt: Instant,
    val scope: String,
    val summary: String,
    val evidenceUrl: String?
)

enum class ResetLevel(val label: String, val emoji: String) {
    CONFIRMED("已确定", "🟣"),
    VERY_LIKELY("很可能", "🟠"),
    POSSIBLE("有可能", "🟡"),
    LOW("可能性较低", "🔵"),
    UNLIKELY("几乎不可能", "⚪"),
    STALE("数据已过期", "⚠"),
    UNKNOWN("暂无数据", "⚪")
}

data class WidgetState(
    val level: ResetLevel = ResetLevel.UNKNOWN,
    val h24: Int? = null,
    val h48: Int? = null,
    val nextResetAt: Instant? = null,
    val nextResetKnownButUnparsed: Boolean = false,
    val lastResetAt: Instant? = null,
    val evidenceUrl: String? = null,
    val evidenceSummary: String? = null,
    val canonicalHeadline: String? = null,
    val canonicalSecondLine: String? = null,
    val recentResets: List<ResetHistoryItem> = emptyList(),
    val resetsLast7Days: Int = 0,
    val averageIntervalHours: Long? = null,
    val streakCount: Int = 0,
    val updatedAt: Instant? = null,
    val sourceStale: Boolean = false,
    val error: String? = null
)

package com.tibobutton.app.data

import java.time.Duration
import java.time.Instant

object ResetClassifier {
    fun build(
        forecast: ForecastSnapshot,
        history: List<HistoryEvent>,
        now: Instant = Instant.now()
    ): WidgetState {
        val stale = forecast.publicationState.equals("stale", ignoreCase = true) ||
            (forecast.validUntil?.isBefore(now) == true)

        val lastCompleted = history
            .asSequence()
            .filter { it.eventKind.equals("completed", true) }
            .filter { isBroadScope(it.scope) }
            .filter { it.announcedAt != null }
            .maxByOrNull { it.announcedAt!! }

        val activeSchedule = history
            .asSequence()
            .filter { it.eventKind.equals("scheduled", true) }
            .filterNot { it.status.lowercase() in setOf("missed", "expired", "cancelled", "canceled") }
            .filter { isBroadScope(it.scope) }
            .filter { it.announcedAt?.isAfter(lastCompleted?.announcedAt ?: Instant.EPOCH) == true }
            .filter { isRecentSchedule(it, now) }
            .maxByOrNull { it.announcedAt ?: Instant.EPOCH }

        val activeIntent = history
            .asSequence()
            .filter { it.eventKind.equals("intent", true) }
            .filterNot { it.status.lowercase() in setOf("missed", "expired", "cancelled", "canceled") }
            .filter { isBroadScope(it.scope) }
            .filter { it.announcedAt?.isAfter(lastCompleted?.announcedAt ?: Instant.EPOCH) == true }
            .filter { it.announcedAt?.let { at -> Duration.between(at, now).toHours() in 0L..36L } == true }
            .maxByOrNull { it.announcedAt ?: Instant.EPOCH }

        val canonicalState = forecast.answerState.orEmpty().lowercase()
        val canonicalSchedule = forecast.answerDeadline != null ||
            canonicalState.contains("schedul") || canonicalState.contains("confirmed")
        val nextResetKnownButUnparsed = canonicalSchedule && forecast.answerDeadline == null &&
            activeSchedule?.scheduledFor == null

        val level = when {
            stale -> ResetLevel.STALE
            canonicalSchedule || activeSchedule != null -> ResetLevel.CONFIRMED
            (forecast.h24 ?: -1) >= 75 -> ResetLevel.VERY_LIKELY
            activeIntent != null -> ResetLevel.POSSIBLE
            (forecast.h24 ?: -1) >= 45 -> ResetLevel.POSSIBLE
            (forecast.h24 ?: -1) >= 20 -> ResetLevel.LOW
            forecast.h24 != null -> ResetLevel.UNLIKELY
            else -> ResetLevel.UNKNOWN
        }

        val primaryEvidence = activeSchedule ?: activeIntent ?: lastCompleted

        return WidgetState(
            level = level,
            h24 = if (stale) null else forecast.h24,
            h48 = if (stale) null else forecast.h48,
            nextResetAt = forecast.answerDeadline ?: activeSchedule?.scheduledFor,
            nextResetKnownButUnparsed = nextResetKnownButUnparsed,
            lastResetAt = lastCompleted?.announcedAt,
            evidenceUrl = primaryEvidence?.evidenceUrl,
            evidenceSummary = null,
            canonicalHeadline = forecast.answerHeadline,
            canonicalSecondLine = forecast.answerSecondLine,
            updatedAt = forecast.calculatedAt ?: now,
            sourceStale = stale
        )
    }

    private fun isBroadScope(scope: String): Boolean {
        val s = scope.trim().lowercase()
        if (s.isBlank()) return false
        return s == "all" || s == "everyone" || s == "all_paid" ||
            s.contains("everyone") || s.contains("all paid")
    }

    private fun isRecentSchedule(event: HistoryEvent, now: Instant): Boolean {
        event.scheduledFor?.let { scheduled ->
            return now.isBefore(scheduled.plus(Duration.ofHours(6)))
        }
        val announced = event.announcedAt ?: return false
        val hours = Duration.between(announced, now).toHours()
        return hours in 0L..48L
    }
}

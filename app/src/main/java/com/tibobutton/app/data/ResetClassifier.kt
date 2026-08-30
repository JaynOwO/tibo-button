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

        val level = when {
            stale -> ResetLevel.STALE
            activeSchedule != null -> ResetLevel.CONFIRMED
            (forecast.h24 ?: -1) >= 75 -> ResetLevel.VERY_LIKELY
            activeIntent != null -> ResetLevel.POSSIBLE
            (forecast.h24 ?: -1) >= 45 -> ResetLevel.POSSIBLE
            (forecast.h24 ?: -1) >= 20 -> ResetLevel.LOW
            forecast.h24 != null -> ResetLevel.UNLIKELY
            else -> ResetLevel.UNKNOWN
        }

        return WidgetState(
            level = level,
            h24 = if (stale) null else forecast.h24,
            h48 = if (stale) null else forecast.h48,
            nextResetAt = activeSchedule?.scheduledFor,
            nextResetKnownButUnparsed = activeSchedule != null && activeSchedule.scheduledFor == null,
            lastResetAt = lastCompleted?.announcedAt,
            evidenceUrl = activeSchedule?.evidenceUrl ?: activeIntent?.evidenceUrl ?: lastCompleted?.evidenceUrl,
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
            // Keep a schedule visible until six hours after its target, matching the API docs'
            // "missed" concept without second-guessing a still-propagating reset.
            return now.isBefore(scheduled.plus(Duration.ofHours(6)))
        }
        val announced = event.announcedAt ?: return false
        val hours = Duration.between(announced, now).toHours()
        return hours in 0L..48L
    }
}

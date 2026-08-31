package com.tibobutton.app.ui

import com.tibobutton.app.data.WidgetState
import java.time.Duration
import java.time.Instant

data class PulsePoint(
    val occurredAt: Instant,
    val intervalHours: Long?
)

data class PulseTimelineModel(
    val points: List<PulsePoint>,
    val averageIntervalHours: Long?
) {
    val hasTimeline: Boolean
        get() = points.size >= 2

    companion object {
        const val MAX_POINTS = 7

        fun from(state: WidgetState): PulseTimelineModel {
            val ordered = state.recentResets
                .sortedBy { it.occurredAt }
                .takeLast(MAX_POINTS)
            val points = ordered.mapIndexed { index, item ->
                val previous = ordered.getOrNull(index - 1)?.occurredAt
                PulsePoint(
                    occurredAt = item.occurredAt,
                    intervalHours = previous?.let { intervalHours(it, item.occurredAt) }
                )
            }
            val intervals = points.mapNotNull { it.intervalHours }
            val calculatedAverage = intervals
                .takeIf { it.isNotEmpty() }
                ?.average()
                ?.toLong()

            return PulseTimelineModel(
                points = points,
                averageIntervalHours = state.averageIntervalHours ?: calculatedAverage
            )
        }

        private fun intervalHours(previous: Instant, current: Instant): Long =
            Duration.between(previous, current).toHours().coerceAtLeast(0)
    }
}

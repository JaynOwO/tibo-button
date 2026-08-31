package com.tibobutton.app.widget

import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetState
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

enum class WidgetNextKind {
    IN_WINDOW,
    TODAY,
    FUTURE,
    SCHEDULED_UNPARSED,
    PROBABILITY_WINDOW,
    UNKNOWN
}

data class WidgetNextDisplay(
    val kind: WidgetNextKind,
    val localTime: ZonedDateTime? = null,
    val countdownMinutes: Long = 0
)

enum class WidgetLastKind {
    NONE,
    MINUTES_AGO,
    HOURS_AGO,
    ABSOLUTE
}

data class WidgetLastDisplay(
    val kind: WidgetLastKind,
    val amount: Long = 0,
    val localTime: ZonedDateTime? = null
)

data class WidgetTextModel(
    val statusEmoji: String,
    val statusLabel: String,
    val h24: Int?,
    val h48: Int?,
    val next: WidgetNextDisplay,
    val last: WidgetLastDisplay,
    val updatedAt: ZonedDateTime?,
    val sourceStale: Boolean,
    val hasError: Boolean
)

/** Pure display-state mapping used by both widget variants and JVM tests. */
object WidgetTextFormatter {
    fun format(
        state: WidgetState,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): WidgetTextModel {
        val probabilitiesStale = state.sourceStale || state.level == ResetLevel.STALE
        return WidgetTextModel(
            statusEmoji = state.level.emoji,
            statusLabel = state.level.label,
            h24 = state.h24.takeUnless { probabilitiesStale },
            h48 = state.h48.takeUnless { probabilitiesStale },
            next = next(state, now, zone),
            last = last(state.lastResetAt, now, zone),
            updatedAt = state.updatedAt?.atZone(zone),
            sourceStale = probabilitiesStale,
            hasError = state.error != null
        )
    }

    private fun next(state: WidgetState, now: Instant, zone: ZoneId): WidgetNextDisplay {
        state.nextResetAt?.let { instant ->
            val local = instant.atZone(zone)
            val until = Duration.between(now, instant)
            return when {
                instant.isBefore(now) -> WidgetNextDisplay(
                    kind = WidgetNextKind.IN_WINDOW,
                    localTime = local
                )
                until.toHours() < 24 -> WidgetNextDisplay(
                    kind = WidgetNextKind.TODAY,
                    localTime = local,
                    countdownMinutes = until.toMinutes().coerceAtLeast(0)
                )
                else -> WidgetNextDisplay(
                    kind = WidgetNextKind.FUTURE,
                    localTime = local
                )
            }
        }
        if (state.nextResetKnownButUnparsed) {
            return WidgetNextDisplay(WidgetNextKind.SCHEDULED_UNPARSED)
        }
        return if (state.level == ResetLevel.VERY_LIKELY || state.level == ResetLevel.POSSIBLE) {
            WidgetNextDisplay(WidgetNextKind.PROBABILITY_WINDOW)
        } else {
            WidgetNextDisplay(WidgetNextKind.UNKNOWN)
        }
    }

    private fun last(instant: Instant?, now: Instant, zone: ZoneId): WidgetLastDisplay {
        instant ?: return WidgetLastDisplay(WidgetLastKind.NONE)
        val minutes = Duration.between(instant, now).toMinutes()
        if (minutes in 0L..59L) {
            return WidgetLastDisplay(WidgetLastKind.MINUTES_AGO, minutes.coerceAtLeast(1L))
        }
        val hours = minutes / 60
        if (hours in 1L..23L) {
            return WidgetLastDisplay(WidgetLastKind.HOURS_AGO, hours)
        }
        return WidgetLastDisplay(WidgetLastKind.ABSOLUTE, localTime = instant.atZone(zone))
    }
}

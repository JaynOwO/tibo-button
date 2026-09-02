package com.tibobutton.app.widget

import com.tibobutton.app.data.WidgetState
import java.time.Instant
import java.time.ZoneId

data class WidgetStatsDisplay(
    val hasHistory: Boolean,
    val resetsLast7Days: Int?,
    val averageIntervalHours: Long?,
    val streakCount: Int?
)

data class WidgetVariantDisplay(
    val variant: WidgetVariant,
    val text: WidgetTextModel,
    val stats: WidgetStatsDisplay
)

/** Pure display mapping shared by every widget layout. */
object WidgetVariantFormatter {
    fun format(
        variant: WidgetVariant,
        state: WidgetState,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): WidgetVariantDisplay {
        val hasHistory = state.recentResets.isNotEmpty()
        return WidgetVariantDisplay(
            variant = variant,
            text = WidgetTextFormatter.format(state, now, zone),
            stats = WidgetStatsDisplay(
                hasHistory = hasHistory,
                resetsLast7Days = state.resetsLast7Days.takeIf { hasHistory },
                averageIntervalHours = state.averageIntervalHours,
                streakCount = state.streakCount.takeIf { it > 0 }
            )
        )
    }
}

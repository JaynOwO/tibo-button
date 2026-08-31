package com.tibobutton.app.ui

import com.tibobutton.app.data.ResetHistoryItem
import com.tibobutton.app.data.WidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PulseTimelineModelTest {
    private val now = Instant.parse("2026-08-30T21:00:00Z")

    @Test fun ordersPointsChronologicallyAndCalculatesIntervals() {
        val state = WidgetState(
            recentResets = listOf(
                item(now, "newest"),
                item(now.minusSeconds(48 * 3600L), "old"),
                item(now.minusSeconds(24 * 3600L), "middle")
            )
        )

        val model = PulseTimelineModel.from(state)

        assertEquals(listOf("old", "middle", "newest"), model.points.map { point ->
            state.recentResets.first { it.occurredAt == point.occurredAt }.summary
        })
        assertEquals(listOf(null, 24L, 24L), model.points.map { it.intervalHours })
        assertEquals(24L, model.averageIntervalHours)
        assertTrue(model.hasTimeline)
    }

    @Test fun capsTimelineAtSevenNewestPoints() {
        val state = WidgetState(
            recentResets = (0..8).map { offset ->
                item(now.minusSeconds(offset * 12 * 3600L), "reset-$offset")
            }
        )

        val model = PulseTimelineModel.from(state)

        assertEquals(7, model.points.size)
        assertEquals(now.minusSeconds(6 * 12 * 3600L), model.points.first().occurredAt)
        assertEquals(now, model.points.last().occurredAt)
    }

    @Test fun emptyAndSinglePointDoNotPretendToHaveACadence() {
        val empty = PulseTimelineModel.from(WidgetState())
        val single = PulseTimelineModel.from(WidgetState(recentResets = listOf(item(now, "only"))))

        assertFalse(empty.hasTimeline)
        assertTrue(empty.points.isEmpty())
        assertNull(empty.averageIntervalHours)
        assertFalse(single.hasTimeline)
        assertNull(single.points.single().intervalHours)
        assertNull(single.averageIntervalHours)
    }

    private fun item(at: Instant, summary: String) = ResetHistoryItem(
        occurredAt = at,
        scope = "all",
        summary = summary,
        evidenceUrl = "https://example.com/$summary"
    )
}

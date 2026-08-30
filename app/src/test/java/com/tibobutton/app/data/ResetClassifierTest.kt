package com.tibobutton.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ResetClassifierTest {
    private val now = Instant.parse("2026-08-30T21:00:00Z")

    @Test fun scheduledBeatsProbability() {
        val forecast = ForecastSnapshot(now, now.plusSeconds(3600), "high", 82, 86, null)
        val history = listOf(
            HistoryEvent(
                eventKind = "scheduled",
                status = "scheduled",
                scope = "all",
                summary = "",
                operativeSentence = "Reset at 6pm",
                evidenceUrl = "https://example.com",
                announcedAt = now.minusSeconds(1800),
                scheduledFor = now.plusSeconds(7200)
            )
        )
        val state = ResetClassifier.build(forecast, history, now)
        assertEquals(ResetLevel.CONFIRMED, state.level)
        assertEquals(now.plusSeconds(7200), state.nextResetAt)
    }

    @Test fun highProbabilityIsVeryLikely() {
        val forecast = ForecastSnapshot(now, now.plusSeconds(3600), "high", 82, 86, null)
        val state = ResetClassifier.build(forecast, emptyList(), now)
        assertEquals(ResetLevel.VERY_LIKELY, state.level)
    }

    @Test fun staleHidesProbability() {
        val forecast = ForecastSnapshot(now.minusSeconds(7200), now.minusSeconds(3600), "stale", 90, 95, null)
        val state = ResetClassifier.build(forecast, emptyList(), now)
        assertEquals(ResetLevel.STALE, state.level)
        assertTrue(state.h24 == null && state.h48 == null)
    }
}

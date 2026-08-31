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

    @Test fun canonicalScheduledAnswerWithoutDeadlineStaysExplicitlyUnparsed() {
        val forecast = ForecastSnapshot(
            calculatedAt = now,
            validUntil = now.plusSeconds(3600),
            publicationState = "high",
            h24 = 12,
            h48 = 34,
            signalStage = null,
            answerState = "scheduled",
            answerHeadline = "已排期",
            answerSecondLine = "具体时间见来源"
        )

        val state = ResetClassifier.build(forecast, emptyList(), now)

        assertEquals(ResetLevel.CONFIRMED, state.level)
        assertTrue(state.nextResetAt == null && state.nextResetKnownButUnparsed)
    }

    @Test fun canonicalDeadlineBecomesNextResetTime() {
        val deadline = now.plusSeconds(7200)
        val forecast = ForecastSnapshot(
            calculatedAt = now,
            validUntil = now.plusSeconds(3600),
            publicationState = "high",
            h24 = 12,
            h48 = 34,
            signalStage = null,
            answerState = "confirmed",
            answerDeadline = deadline
        )

        val state = ResetClassifier.build(forecast, emptyList(), now)

        assertEquals(ResetLevel.CONFIRMED, state.level)
        assertEquals(deadline, state.nextResetAt)
        assertTrue(!state.nextResetKnownButUnparsed)
    }

    @Test fun staleHidesProbability() {
        val forecast = ForecastSnapshot(now.minusSeconds(7200), now.minusSeconds(3600), "stale", 90, 95, null)
        val state = ResetClassifier.build(forecast, emptyList(), now)
        assertEquals(ResetLevel.STALE, state.level)
        assertTrue(state.h24 == null && state.h48 == null)
    }

    @Test fun historyPulseUsesOnlyCompletedBroadResetsAndSortsNewestFirst() {
        val forecast = ForecastSnapshot(now, now.plusSeconds(3600), "high", 10, 20, null)
        val history = listOf(
            completed(now.minusSeconds(6 * 3600L), "all", "Newest"),
            completed(now.minusSeconds(30 * 3600L), "all_paid", "Second"),
            completed(now.minusSeconds(60 * 3600L), "everyone", "Third"),
            completed(now.minusSeconds(100 * 3600L), "all", "Fourth"),
            completed(now.minusSeconds(12 * 3600L), "single_user", "Ignore narrow")
        )

        val state = ResetClassifier.build(forecast, history, now)

        assertEquals(4, state.resetsLast7Days)
        assertEquals(4, state.recentResets.size)
        assertEquals("Newest", state.recentResets.first().summary)
        assertEquals(4, state.streakCount)
        assertEquals(31L, state.averageIntervalHours)
    }

    @Test fun cadenceStreakStopsAfterGapOver72Hours() {
        val forecast = ForecastSnapshot(now, now.plusSeconds(3600), "high", 10, 20, null)
        val history = listOf(
            completed(now.minusSeconds(3 * 3600L), "all", "Newest"),
            completed(now.minusSeconds(27 * 3600L), "all", "Second"),
            completed(now.minusSeconds(110 * 3600L), "all", "Old break")
        )

        val state = ResetClassifier.build(forecast, history, now)

        assertEquals(2, state.streakCount)
    }

    @Test fun cadenceStreakIncludesQualifyingEventsBeyondDisplayedSeven() {
        val forecast = ForecastSnapshot(now, now.plusSeconds(3600), "high", 10, 20, null)
        val history = (1..8).map { index ->
            completed(now.minusSeconds(index * 24 * 3600L), "all", "Reset $index")
        }

        val state = ResetClassifier.build(forecast, history, now)

        assertEquals(7, state.recentResets.size)
        assertEquals(8, state.streakCount)
    }

    @Test fun sevenDayCountExcludesFutureEvents() {
        val forecast = ForecastSnapshot(now, now.plusSeconds(3600), "high", 10, 20, null)
        val history = listOf(
            completed(now.plusSeconds(3600), "all", "Future"),
            completed(now.minusSeconds(7 * 24 * 3600L), "all", "Boundary")
        )

        val state = ResetClassifier.build(forecast, history, now)

        assertEquals(1, state.resetsLast7Days)
    }

    private fun completed(at: Instant, scope: String, summary: String) = HistoryEvent(
        eventKind = "completed",
        status = "completed",
        scope = scope,
        summary = summary,
        operativeSentence = "",
        evidenceUrl = "https://example.com/$summary",
        announcedAt = at,
        scheduledFor = null
    )
}

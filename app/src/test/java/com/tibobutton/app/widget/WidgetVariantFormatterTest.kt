package com.tibobutton.app.widget

import com.tibobutton.app.data.ResetHistoryItem
import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetVariantFormatterTest {
    private val now = Instant.parse("2026-08-30T21:00:00Z")
    private val utc = ZoneId.of("UTC")

    @Test fun bothNewVariantsKeepFreshProbabilities() {
        val state = WidgetState(level = ResetLevel.POSSIBLE, h24 = 7, h48 = 28)

        listOf(WidgetVariant.PULSE_ORB, WidgetVariant.COMMAND_DECK).forEach { variant ->
            val display = WidgetVariantFormatter.format(variant, state, now, utc)

            assertEquals(7, display.text.h24)
            assertEquals(28, display.text.h48)
            assertEquals(variant, display.variant)
        }
    }

    @Test fun newVariantsHideOldProbabilitiesWhenForecastIsStale() {
        val state = WidgetState(
            level = ResetLevel.STALE,
            h24 = 90,
            h48 = 95,
            sourceStale = true
        )

        val display = WidgetVariantFormatter.format(WidgetVariant.PULSE_ORB, state, now, utc)

        assertNull(display.text.h24)
        assertNull(display.text.h48)
        assertTrue(display.text.sourceStale)
    }

    @Test fun emptyAndErrorCachesRemainExplicitWithoutInventedStats() {
        val empty = WidgetVariantFormatter.format(WidgetVariant.COMMAND_DECK, WidgetState(), now, utc)
        val failed = WidgetVariantFormatter.format(
            WidgetVariant.COMMAND_DECK,
            WidgetState(error = "network unavailable"),
            now,
            utc
        )

        assertEquals(WidgetNextKind.UNKNOWN, empty.text.next.kind)
        assertNull(empty.stats.resetsLast7Days)
        assertNull(empty.stats.averageIntervalHours)
        assertNull(empty.stats.streakCount)
        assertTrue(failed.text.hasError)
    }

    @Test fun scheduledWithoutMachineReadableTimeIsNotConvertedToAnExactTime() {
        val display = WidgetVariantFormatter.format(
            WidgetVariant.PULSE_ORB,
            WidgetState(level = ResetLevel.CONFIRMED, nextResetKnownButUnparsed = true),
            now,
            utc
        )

        assertEquals(WidgetNextKind.SCHEDULED_UNPARSED, display.text.next.kind)
        assertNull(display.text.next.localTime)
    }

    @Test fun statsReuseExistingHistoryAndPreserveMissingAverageAsUnavailable() {
        val history = listOf(
            ResetHistoryItem(now.minusSeconds(2 * 3600L), "all", "Newest", null),
            ResetHistoryItem(now.minusSeconds(26 * 3600L), "all", "Previous", null)
        )
        val state = WidgetState(
            recentResets = history,
            resetsLast7Days = 2,
            averageIntervalHours = 24,
            streakCount = 2
        )

        val display = WidgetVariantFormatter.format(WidgetVariant.COMMAND_DECK, state, now, utc)

        assertTrue(display.stats.hasHistory)
        assertEquals(2, display.stats.resetsLast7Days)
        assertEquals(24L, display.stats.averageIntervalHours)
        assertEquals(2, display.stats.streakCount)
    }

    @Test fun oneHistoryRecordDoesNotFabricateAnAverageInterval() {
        val state = WidgetState(
            recentResets = listOf(ResetHistoryItem(now, "all", "Only event", null)),
            resetsLast7Days = 1,
            averageIntervalHours = null,
            streakCount = 1
        )

        val display = WidgetVariantFormatter.format(WidgetVariant.PULSE_ORB, state, now, utc)

        assertTrue(display.stats.hasHistory)
        assertNull(display.stats.averageIntervalHours)
        assertEquals(1, display.stats.streakCount)
    }

    @Test fun scheduledTimesStayInTheRequestedLocalZoneForCompactLayouts() {
        val display = WidgetVariantFormatter.format(
            WidgetVariant.COMMAND_DECK,
            WidgetState(nextResetAt = now.plusSeconds(90 * 60L)),
            now,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(WidgetNextKind.TODAY, display.text.next.kind)
        assertEquals(6, display.text.next.localTime?.hour)
        assertEquals(30, display.text.next.localTime?.minute)
    }
}

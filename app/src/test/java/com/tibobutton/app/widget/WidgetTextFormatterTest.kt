package com.tibobutton.app.widget

import com.tibobutton.app.data.ResetLevel
import com.tibobutton.app.data.WidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WidgetTextFormatterTest {
    private val now = Instant.parse("2026-08-30T21:00:00Z")
    private val utc = ZoneId.of("UTC")

    @Test fun freshSmallWidgetModelRetainsBothProbabilities() {
        val model = WidgetTextFormatter.format(
            WidgetState(level = ResetLevel.POSSIBLE, h24 = 7, h48 = 28),
            now,
            utc
        )

        assertEquals(7, model.h24)
        assertEquals(28, model.h48)
    }

    @Test fun staleModelHidesBothProbabilitiesEvenIfCacheContainsValues() {
        val model = WidgetTextFormatter.format(
            WidgetState(level = ResetLevel.STALE, h24 = 90, h48 = 95, sourceStale = true),
            now,
            utc
        )

        assertNull(model.h24)
        assertNull(model.h48)
    }

    @Test fun staleLevelAlsoHidesProbabilitiesWhenLegacyCacheFlagIsMissing() {
        val model = WidgetTextFormatter.format(
            WidgetState(level = ResetLevel.STALE, h24 = 90, h48 = 95),
            now,
            utc
        )

        assertNull(model.h24)
        assertNull(model.h48)
        assertEquals(true, model.sourceStale)
    }

    @Test fun scheduledWithoutMachineReadableTimeStaysExplicitlyUnparsed() {
        val model = WidgetTextFormatter.format(
            WidgetState(
                level = ResetLevel.CONFIRMED,
                nextResetKnownButUnparsed = true
            ),
            now,
            utc
        )

        assertEquals(WidgetNextKind.SCHEDULED_UNPARSED, model.next.kind)
    }

    @Test fun noDataDoesNotPretendToHaveAResetWindow() {
        val model = WidgetTextFormatter.format(WidgetState(), now, utc)

        assertEquals(WidgetNextKind.UNKNOWN, model.next.kind)
        assertEquals(WidgetLastKind.NONE, model.last.kind)
    }

    @Test fun cacheErrorIsRetainedAsAVisibleDisplayState() {
        val model = WidgetTextFormatter.format(
            WidgetState(
                lastResetAt = now.minusSeconds(2 * 3600L),
                error = "network unavailable"
            ),
            now,
            utc
        )

        assertEquals(WidgetLastKind.HOURS_AGO, model.last.kind)
        assertEquals(2L, model.last.amount)
        assertEquals(true, model.hasError)
    }

    @Test fun countdownUsesLocalTimeAndNeverBecomesNegative() {
        val model = WidgetTextFormatter.format(
            WidgetState(nextResetAt = now.plusSeconds(90 * 60L)),
            now,
            utc
        )

        assertEquals(WidgetNextKind.TODAY, model.next.kind)
        assertEquals(90L, model.next.countdownMinutes)
        assertEquals(22, model.next.localTime?.hour)
        assertEquals(30, model.next.localTime?.minute)
    }

    @Test fun scheduledTimeUsesTheRequestedLocalZone() {
        val model = WidgetTextFormatter.format(
            WidgetState(nextResetAt = now.plusSeconds(90 * 60L)),
            now,
            ZoneId.of("Asia/Shanghai")
        )

        assertEquals(WidgetNextKind.TODAY, model.next.kind)
        assertEquals(6, model.next.localTime?.hour)
        assertEquals(30, model.next.localTime?.minute)
    }
}

package com.scribbles.timesince.presentation.format

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TimeSinceFormatterTest {

    private val utc = TimeZone.UTC

    private fun format(
        start: String,
        now: String,
        amount: Int,
        unit: FrequencyUnit,
    ): String = TimeSinceFormatter.format(
        lastCompletedAt = Instant.parse(start),
        now = Instant.parse(now),
        tz = utc,
        frequency = TaskFrequency(amount, unit),
    )

    @Test
    fun subDayElapsedUsesHoursAndMinutes() {
        assertEquals(
            "5h 30m / 1d",
            format("2026-01-01T00:00:00Z", "2026-01-01T05:30:00Z", 1, FrequencyUnit.DAYS),
        )
    }

    @Test
    fun zeroElapsedShowsZeroMinutes() {
        assertEquals(
            "0m / 3h",
            format("2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", 3, FrequencyUnit.HOURS),
        )
    }

    @Test
    fun negativeElapsedClampsToZero() {
        assertEquals(
            "0m / 1d",
            format("2026-01-01T00:05:00Z", "2026-01-01T00:00:00Z", 1, FrequencyUnit.DAYS),
        )
    }

    @Test
    fun daysBreakIntoWeeksAndDays() {
        // 10 days -> 1 week 3 days.
        assertEquals(
            "1w 3d / 3m",
            format("2026-01-01T00:00:00Z", "2026-01-11T00:00:00Z", 3, FrequencyUnit.MONTHS),
        )
    }

    @Test
    fun wholeWeeksDropTrailingDays() {
        assertEquals(
            "2w / 5d",
            format("2026-01-01T00:00:00Z", "2026-01-15T00:00:00Z", 5, FrequencyUnit.DAYS),
        )
    }

    @Test
    fun keepsOnlyTwoMostSignificantUnits() {
        // Jan 1 -> Feb 10: 1 month + 1 week + 2 days, trimmed to "1m 1w".
        assertEquals(
            "1m 1w / 1y",
            format("2026-01-01T00:00:00Z", "2026-02-10T00:00:00Z", 1, FrequencyUnit.YEARS),
        )
    }

    @Test
    fun yearsLeadAndTrimToTwoUnits() {
        // 2025-01-01 -> 2026-02-10: 1y 1m 1w 2d, trimmed to "1y 1m".
        assertEquals(
            "1y 1m / 2w",
            format("2025-01-01T00:00:00Z", "2026-02-10T00:00:00Z", 2, FrequencyUnit.WEEKS),
        )
    }

    @Test
    fun daysPairWithHours() {
        // 3 days 5 hours -> "3d 5h" (minutes dropped as the third unit).
        assertEquals(
            "3d 5h / 1w",
            format("2026-01-01T00:00:00Z", "2026-01-04T05:30:00Z", 1, FrequencyUnit.WEEKS),
        )
    }

    @Test
    fun collapsesWhenAdjacentUnitIsZero() {
        // Jan 1 -> Feb 3: 1 month + 2 days, but the adjacent unit (weeks) is
        // zero, so the trailing days are dropped -> "1m".
        assertEquals(
            "1m / 1y",
            format("2026-01-01T00:00:00Z", "2026-02-03T00:00:00Z", 1, FrequencyUnit.YEARS),
        )
    }

    @Test
    fun yearWithZeroMonthsCollapsesToYearOnly() {
        // 2025-01-01 -> 2026-01-02: 1y 0m 0w 1d. Months (adjacent to years) is
        // zero, so it collapses to "1y" even though days is non-zero.
        assertEquals(
            "1y / 1y",
            format("2025-01-01T00:00:00Z", "2026-01-02T00:00:00Z", 1, FrequencyUnit.YEARS),
        )
    }
}

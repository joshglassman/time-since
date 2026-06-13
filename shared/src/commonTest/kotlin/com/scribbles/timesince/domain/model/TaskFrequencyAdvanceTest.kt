package com.scribbles.timesince.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Calendar-correct [TaskFrequency.advance] semantics, including DST handling.
 *
 * `America/New_York` springs forward 2026-03-08 02:00 -> 03:00 (loses an hour)
 * and falls back 2026-11-01 02:00 -> 01:00 (gains an hour).
 */
class TaskFrequencyAdvanceTest {

    private val ny = TimeZone.of("America/New_York")
    private val utc = TimeZone.UTC

    private fun at(text: String, tz: TimeZone): Instant =
        LocalDateTime.parse(text).toInstant(tz)

    private fun Instant.localIn(tz: TimeZone): LocalDateTime = toLocalDateTime(tz)

    @Test
    fun daysPreserveWallClockAcrossSpringForward() {
        val from = at("2026-03-07T12:00:00", ny)
        val result = TaskFrequency(1, FrequencyUnit.DAYS).advance(from, ny)
        // Same wall-clock time the next day, even though only 23 real hours pass.
        assertEquals(LocalDateTime.parse("2026-03-08T12:00:00"), result.localIn(ny))
        assertEquals(23.hours, result - from)
    }

    @Test
    fun hoursAdvanceRealTimeAndShiftWallClockAcrossSpringForward() {
        val from = at("2026-03-07T12:00:00", ny)
        val result = TaskFrequency(24, FrequencyUnit.HOURS).advance(from, ny)
        // 24 real hours crosses the lost hour, so wall clock lands at 13:00.
        assertEquals(LocalDateTime.parse("2026-03-08T13:00:00"), result.localIn(ny))
        assertEquals(24.hours, result - from)
    }

    @Test
    fun daysPreserveWallClockAcrossFallBack() {
        val from = at("2026-10-31T12:00:00", ny)
        val result = TaskFrequency(1, FrequencyUnit.DAYS).advance(from, ny)
        assertEquals(LocalDateTime.parse("2026-11-01T12:00:00"), result.localIn(ny))
        // Fall-back day has 25 real hours.
        assertEquals(25.hours, result - from)
    }

    @Test
    fun weeksPreserveWallClockAcrossSpringForward() {
        val from = at("2026-03-05T09:00:00", ny)
        val result = TaskFrequency(1, FrequencyUnit.WEEKS).advance(from, ny)
        assertEquals(LocalDateTime.parse("2026-03-12T09:00:00"), result.localIn(ny))
    }

    @Test
    fun monthOverflowClampsToEndOfMonth() {
        val from = at("2026-01-31T09:00:00", utc)
        val result = TaskFrequency(1, FrequencyUnit.MONTHS).advance(from, utc)
        // 2026 is not a leap year -> Feb 28.
        assertEquals(LocalDateTime.parse("2026-02-28T09:00:00"), result.localIn(utc))
    }

    @Test
    fun leapDayPlusOneYearClampsToFeb28() {
        val from = at("2028-02-29T09:00:00", utc)
        val result = TaskFrequency(1, FrequencyUnit.YEARS).advance(from, utc)
        assertEquals(LocalDateTime.parse("2029-02-28T09:00:00"), result.localIn(utc))
    }

    @Test
    fun yearAdvancePreservesDateForNonLeapDay() {
        val from = at("2026-06-12T07:30:00", utc)
        val result = TaskFrequency(2, FrequencyUnit.YEARS).advance(from, utc)
        assertEquals(LocalDateTime.parse("2028-06-12T07:30:00"), result.localIn(utc))
    }
}

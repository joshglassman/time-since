package com.joshmermelstein.timesince.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RemainingTimeFormatterTest {

    @Test
    fun zeroIsNow() {
        assertEquals("now", RemainingTimeFormatter.format(Duration.ZERO))
    }

    @Test
    fun daysAndHours() {
        assertEquals("3d 4h", RemainingTimeFormatter.format(3.days + 4.hours))
    }

    @Test
    fun daysOnlyWhenHoursAreZero() {
        assertEquals("5d", RemainingTimeFormatter.format(5.days))
    }

    @Test
    fun hoursAndMinutes() {
        assertEquals("5h 30m", RemainingTimeFormatter.format(5.hours + 30.minutes))
    }

    @Test
    fun hoursOnlyWhenMinutesAreZero() {
        assertEquals("2h", RemainingTimeFormatter.format(2.hours))
    }

    @Test
    fun minutesOnly() {
        assertEquals("45m", RemainingTimeFormatter.format(45.minutes))
    }

    @Test
    fun roundsSubMinuteUpToOneMinute() {
        assertEquals("1m", RemainingTimeFormatter.format(30.seconds))
    }

    @Test
    fun negativeShowsOverdue() {
        assertEquals("2h overdue", RemainingTimeFormatter.format(-(2.hours)))
    }

    @Test
    fun negativeDayAndHourOverdue() {
        assertEquals("1d 6h overdue", RemainingTimeFormatter.format(-(1.days + 6.hours)))
    }
}

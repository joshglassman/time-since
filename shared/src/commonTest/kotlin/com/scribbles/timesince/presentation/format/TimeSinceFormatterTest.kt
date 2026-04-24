package com.scribbles.timesince.presentation.format

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TimeSinceFormatterTest {

    @Test
    fun oneDayFiveHoursOverFiveDays() {
        assertEquals(
            "1d 5h / 5d",
            TimeSinceFormatter.format(
                elapsed = 1.days + 5.hours,
                frequency = TaskFrequency(5, FrequencyUnit.DAYS),
            ),
        )
    }

    @Test
    fun zeroElapsedShowsZeroMinutes() {
        assertEquals(
            "0m / 3h",
            TimeSinceFormatter.format(
                elapsed = Duration.ZERO,
                frequency = TaskFrequency(3, FrequencyUnit.HOURS),
            ),
        )
    }

    @Test
    fun elapsedExceedingFrequencyKeepsGoing() {
        assertEquals(
            "7d / 5d",
            TimeSinceFormatter.format(
                elapsed = 7.days,
                frequency = TaskFrequency(5, FrequencyUnit.DAYS),
            ),
        )
    }

    @Test
    fun negativeElapsedClampsToZero() {
        assertEquals(
            "0m / 1d",
            TimeSinceFormatter.format(
                elapsed = -(5.minutes),
                frequency = TaskFrequency(1, FrequencyUnit.DAYS),
            ),
        )
    }

    @Test
    fun weeksFrequencyRendersAsDays() {
        assertEquals(
            "3d / 14d",
            TimeSinceFormatter.format(
                elapsed = 3.days,
                frequency = TaskFrequency(2, FrequencyUnit.WEEKS),
            ),
        )
    }
}

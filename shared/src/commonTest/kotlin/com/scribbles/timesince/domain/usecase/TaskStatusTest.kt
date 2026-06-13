package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.remainingTime
import com.scribbles.timesince.domain.model.status
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class TaskStatusTest {

    @Test
    fun taskWithPlentyOfTimeIsOk() {
        val task = taskWith(frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 2.days
        assertEquals(TaskStatus.OK, task.status(now, TimeZone.UTC))
    }

    @Test
    fun taskNearDeadlineIsDueSoon() {
        val task = taskWith(frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS)
        // 10 days frequency, DUE_SOON threshold is < 10% = < 1 day remaining
        val now = BASE_TIME + 9.days + 12.hours
        assertEquals(TaskStatus.DUE_SOON, task.status(now, TimeZone.UTC))
    }

    @Test
    fun taskPastDeadlineIsOverdue() {
        val task = taskWith(frequencyAmount = 3, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 5.days
        assertEquals(TaskStatus.OVERDUE, task.status(now, TimeZone.UTC))
    }

    @Test
    fun remainingTimeIsPositiveBeforeDeadline() {
        val task = taskWith(frequencyAmount = 7, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 3.days
        assertTrue(task.remainingTime(now, TimeZone.UTC) > kotlin.time.Duration.ZERO)
    }

    @Test
    fun remainingTimeIsNegativeAfterDeadline() {
        val task = taskWith(frequencyAmount = 2, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 5.days
        assertTrue(task.remainingTime(now, TimeZone.UTC) < kotlin.time.Duration.ZERO)
    }

    @Test
    fun remainingTimeIsExactlyCorrect() {
        val task = taskWith(frequencyAmount = 7, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 3.days
        assertEquals(4.days, task.remainingTime(now, TimeZone.UTC))
    }

    @Test
    fun monthlyStatusUsesCalendarCycleLengthNotFixedDuration() {
        // Jan 31 + 1 month -> Feb 28 (a 28-day cycle, not the 30-day approximation).
        val lastCompleted = Instant.parse("2026-01-31T09:00:00Z")
        val task = taskWith(
            lastCompletedAt = lastCompleted,
            frequencyAmount = 1,
            frequencyUnit = FrequencyUnit.MONTHS,
        )
        // 10% of 28 days ~= 2.8 days. Two days out is DUE_SOON.
        val dueSoon = Instant.parse("2026-02-26T09:00:00Z")
        assertEquals(TaskStatus.DUE_SOON, task.status(dueSoon, TimeZone.UTC))
        // Well before the deadline is OK.
        val ok = Instant.parse("2026-02-10T09:00:00Z")
        assertEquals(TaskStatus.OK, task.status(ok, TimeZone.UTC))
        // Past Feb 28 is OVERDUE.
        val overdue = Instant.parse("2026-03-02T09:00:00Z")
        assertEquals(TaskStatus.OVERDUE, task.status(overdue, TimeZone.UTC))
    }

    @Test
    fun yearlyDeadlineIsCalendarCorrect() {
        val lastCompleted = Instant.parse("2026-06-12T07:30:00Z")
        val task = taskWith(
            lastCompletedAt = lastCompleted,
            frequencyAmount = 1,
            frequencyUnit = FrequencyUnit.YEARS,
        )
        val justBefore = Instant.parse("2027-06-11T07:30:00Z")
        assertEquals(TaskStatus.DUE_SOON, task.status(justBefore, TimeZone.UTC))
        val after = Instant.parse("2027-06-13T07:30:00Z")
        assertEquals(TaskStatus.OVERDUE, task.status(after, TimeZone.UTC))
    }
}

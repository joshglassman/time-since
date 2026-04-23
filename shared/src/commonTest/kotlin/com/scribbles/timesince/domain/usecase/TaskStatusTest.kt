package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.remainingTime
import com.scribbles.timesince.domain.model.status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class TaskStatusTest {

    @Test
    fun taskWithPlentyOfTimeIsOk() {
        val task = taskWith(frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 2.days
        assertEquals(TaskStatus.OK, task.status(now))
    }

    @Test
    fun taskNearDeadlineIsDueSoon() {
        val task = taskWith(frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS)
        // 10 days frequency, DUE_SOON threshold is < 10% = < 1 day remaining
        val now = BASE_TIME + 9.days + 12.hours
        assertEquals(TaskStatus.DUE_SOON, task.status(now))
    }

    @Test
    fun taskPastDeadlineIsOverdue() {
        val task = taskWith(frequencyAmount = 3, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 5.days
        assertEquals(TaskStatus.OVERDUE, task.status(now))
    }

    @Test
    fun remainingTimeIsPositiveBeforeDeadline() {
        val task = taskWith(frequencyAmount = 7, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 3.days
        assertTrue(task.remainingTime(now) > kotlin.time.Duration.ZERO)
    }

    @Test
    fun remainingTimeIsNegativeAfterDeadline() {
        val task = taskWith(frequencyAmount = 2, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 5.days
        assertTrue(task.remainingTime(now) < kotlin.time.Duration.ZERO)
    }

    @Test
    fun remainingTimeIsExactlyCorrect() {
        val task = taskWith(frequencyAmount = 7, frequencyUnit = FrequencyUnit.DAYS)
        val now = BASE_TIME + 3.days
        assertEquals(4.days, task.remainingTime(now))
    }
}

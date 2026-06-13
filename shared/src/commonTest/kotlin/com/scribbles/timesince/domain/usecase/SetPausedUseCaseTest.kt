package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.deadline
import com.scribbles.timesince.domain.model.remainingTime
import com.scribbles.timesince.domain.model.status
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class SetPausedUseCaseTest {

    private val repository = FakeTaskRepository()
    private val utc = TimeZone.UTC

    private fun useCaseAt(now: Instant) = SetPausedUseCase(repository, TestClock(now))

    @Test
    fun pauseSetsPausedAtToNow() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        val now = BASE_TIME + 2.days

        useCaseAt(now)("1", paused = true)

        assertEquals(now, repository.getById("1")!!.pausedAt)
    }

    @Test
    fun statusAndRemainingAreFrozenWhilePaused() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        useCaseAt(BASE_TIME + 2.days)("1", paused = true)
        val task = repository.getById("1")!!

        // Evaluate "later" — remaining/status stay as of pausedAt (BASE + 2d).
        val later = BASE_TIME + 20.days
        assertEquals((BASE_TIME + 10.days) - (BASE_TIME + 2.days), task.remainingTime(later, utc))
        assertEquals(TaskStatus.OK, task.status(later, utc))
    }

    @Test
    fun resumeShiftsDeadlineForwardByPausedSpanViaSnoozeWithoutTouchingLastCompleted() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        val deadlineBefore = repository.getById("1")!!.deadline(utc)

        useCaseAt(BASE_TIME + 2.days)("1", paused = true)
        useCaseAt(BASE_TIME + 6.days)("1", paused = false) // paused span = 4 days

        val task = repository.getById("1")!!
        assertNull(task.pausedAt)
        assertEquals(BASE_TIME, task.lastCompletedAt) // untouched
        assertEquals(4.days, task.snooze)
        assertEquals(deadlineBefore + 4.days, task.deadline(utc))
    }

    @Test
    fun pausingAlreadyPausedTaskIsNoOp() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        useCaseAt(BASE_TIME + 2.days)("1", paused = true)
        useCaseAt(BASE_TIME + 5.days)("1", paused = true)

        assertEquals(BASE_TIME + 2.days, repository.getById("1")!!.pausedAt)
    }

    @Test
    fun resumingNonPausedTaskIsNoOp() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        useCaseAt(BASE_TIME + 5.days)("1", paused = false)

        val task = repository.getById("1")!!
        assertNull(task.pausedAt)
        assertEquals(Duration.ZERO, task.snooze)
    }

    @Test
    fun cannotPauseAnArchivedTask() = runTest {
        repository.create(taskWith(id = "1", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS, archived = true))
        useCaseAt(BASE_TIME + 2.days)("1", paused = true)

        assertNull(repository.getById("1")!!.pausedAt)
    }
}

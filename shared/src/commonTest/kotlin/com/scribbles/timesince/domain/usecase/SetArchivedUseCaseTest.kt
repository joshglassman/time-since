package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.status
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class SetArchivedUseCaseTest {

    private val repository = FakeTaskRepository()
    private val useCase = SetArchivedUseCase(repository)
    private val utc = TimeZone.UTC

    @Test
    fun archiveSetsArchivedTrue() = runTest {
        repository.create(taskWith(id = "1"))
        useCase("1", archived = true)
        assertTrue(repository.getById("1")!!.archived)
    }

    @Test
    fun archivingClearsPauseWithoutSnoozeBump() = runTest {
        repository.create(
            taskWith(
                id = "1",
                frequencyAmount = 10,
                frequencyUnit = FrequencyUnit.DAYS,
                pausedAt = BASE_TIME + 2.days,
                snooze = Duration.ZERO,
            ),
        )

        useCase("1", archived = true)

        val task = repository.getById("1")!!
        assertTrue(task.archived)
        assertNull(task.pausedAt)
        assertEquals(Duration.ZERO, task.snooze) // no resume shift applied
    }

    @Test
    fun unarchiveRestoresPriorTimingAndCanBeOverdue() = runTest {
        // Archived task whose deadline is long past.
        repository.create(
            taskWith(
                id = "1",
                lastCompletedAt = BASE_TIME,
                frequencyAmount = 1,
                frequencyUnit = FrequencyUnit.DAYS,
                archived = true,
            ),
        )

        useCase("1", archived = false)

        val task = repository.getById("1")!!
        assertFalse(task.archived)
        assertEquals(BASE_TIME, task.lastCompletedAt) // timing unchanged
        // Well past the 1-day deadline -> overdue, no fresh-completion behavior.
        assertEquals(TaskStatus.OVERDUE, task.status(BASE_TIME + 30.days, utc))
    }

    @Test
    fun archivingAnAlreadyArchivedTaskIsNoOp() = runTest {
        repository.create(taskWith(id = "1", archived = true, snooze = 3.days))
        useCase("1", archived = true)
        assertEquals(3.days, repository.getById("1")!!.snooze)
    }
}

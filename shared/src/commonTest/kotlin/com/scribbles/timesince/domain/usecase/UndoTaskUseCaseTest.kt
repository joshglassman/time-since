package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.data.FakeLocalTaskDataSource
import com.scribbles.timesince.data.TaskRepositoryImpl
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.undo.UndoStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class UndoTaskUseCaseTest {

    // Real repository so update() stamps updatedAt (undo must re-stamp it).
    private val clock = TestClock(BASE_TIME)
    private val repository = TaskRepositoryImpl(FakeLocalTaskDataSource(), clock)
    private val undoStore = UndoStore()
    private val undo = UndoTaskUseCase(repository, undoStore)
    private val complete = CompleteTaskUseCase(repository, clock, undoStore)
    private val snooze = SnoozeTaskUseCase(repository, undoStore, clock, UTC_PROVIDER)

    @Test
    fun undoRestoresLastCompletedAndSnoozeAndReStampsUpdatedAtToWinSync() = runTest {
        repository.create(taskWith(id = "1", lastCompletedAt = BASE_TIME))

        clock.now = BASE_TIME + 5.days
        complete("1")
        val afterComplete = repository.getById("1")!!
        assertEquals(BASE_TIME + 5.days, afterComplete.lastCompletedAt)
        assertEquals(BASE_TIME + 5.days, afterComplete.updatedAt)

        // Undo at a later time: field values restored, but updatedAt is freshly
        // stamped so the undo wins any subsequent sync merge.
        clock.now = BASE_TIME + 10.days
        assertTrue(undo("1"))

        val restored = repository.getById("1")!!
        assertEquals(BASE_TIME, restored.lastCompletedAt)
        assertEquals(BASE_TIME + 10.days, restored.updatedAt) // re-stamped, not the old value
        assertEquals(Duration.ZERO, restored.snooze)
    }

    @Test
    fun undoRestoresPriorSnooze() = runTest {
        repository.create(
            taskWith(id = "1", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS),
        )
        clock.now = BASE_TIME + 5.days
        snooze("1", 3, FrequencyUnit.DAYS)
        assertTrue(repository.getById("1")!!.snooze > Duration.ZERO)

        assertTrue(undo("1"))
        assertEquals(Duration.ZERO, repository.getById("1")!!.snooze)
    }

    @Test
    fun undoHandlesMultipleTasksIndependently() = runTest {
        repository.create(taskWith(id = "1", lastCompletedAt = BASE_TIME))
        repository.create(taskWith(id = "2", lastCompletedAt = BASE_TIME))

        clock.now = BASE_TIME + 2.days
        complete("1")
        clock.now = BASE_TIME + 3.days
        complete("2")

        // Undo only task 1; task 2 keeps its completion.
        assertTrue(undo("1"))
        assertEquals(BASE_TIME, repository.getById("1")!!.lastCompletedAt)
        assertEquals(BASE_TIME + 3.days, repository.getById("2")!!.lastCompletedAt)

        // Task 2's snapshot is still independently available.
        assertTrue(undo("2"))
        assertEquals(BASE_TIME, repository.getById("2")!!.lastCompletedAt)
    }

    @Test
    fun undoWithoutSnapshotReturnsFalse() = runTest {
        repository.create(taskWith(id = "1"))
        assertFalse(undo("1"))
    }

    @Test
    fun undoConsumesSnapshotSoSecondUndoIsNoOp() = runTest {
        repository.create(taskWith(id = "1", lastCompletedAt = BASE_TIME))
        clock.now = BASE_TIME + 5.days
        complete("1")

        assertTrue(undo("1"))
        assertFalse(undo("1"))
    }
}

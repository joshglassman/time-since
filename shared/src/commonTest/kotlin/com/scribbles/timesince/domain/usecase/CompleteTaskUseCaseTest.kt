package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.undo.UndoStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

class CompleteTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val completionTime = BASE_TIME + 5.days
    private val clock = TestClock(completionTime)
    private val undoStore = UndoStore()
    private val useCase = CompleteTaskUseCase(repository, clock, undoStore)

    @Test
    fun completingTaskUpdatesLastCompletedAt() = runTest {
        val task = taskWith(id = "1")
        repository.create(task)

        assertNotEquals(completionTime, task.lastCompletedAt)

        useCase("1")

        val updated = repository.getById("1")!!
        assertEquals(completionTime, updated.lastCompletedAt)
    }

    @Test
    fun completingNonexistentTaskDoesNothing() = runTest {
        val task = taskWith(id = "1")
        repository.create(task)

        useCase("nonexistent")

        val tasks = repository.observeAll().first()
        assertEquals(1, tasks.size)
        assertEquals(BASE_TIME, tasks[0].lastCompletedAt)
    }

    @Test
    fun completingTaskPreservesOtherFields() = runTest {
        val task = taskWith(id = "1", name = "My Task")
        repository.create(task)

        useCase("1")

        val updated = repository.getById("1")!!
        assertEquals("My Task", updated.name)
        assertEquals(task.frequency, updated.frequency)
        assertEquals(task.createdAt, updated.createdAt)
    }

    @Test
    fun completingTaskResetsSnooze() = runTest {
        repository.create(taskWith(id = "1", snooze = 2.days))

        useCase("1")

        assertEquals(Duration.ZERO, repository.getById("1")!!.snooze)
    }

    @Test
    fun completingTaskRecordsUndoSnapshotOfPriorState() = runTest {
        val task = taskWith(id = "1", lastCompletedAt = BASE_TIME, snooze = 3.days)
        repository.create(task)

        useCase("1")

        val snapshot = undoStore.take("1")
        assertNotNull(snapshot)
        assertEquals(BASE_TIME, snapshot.prevLastCompletedAt)
        assertEquals(3.days, snapshot.prevSnooze)
    }
}

package com.joshmermelstein.timesince.domain.usecase

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.time.Duration.Companion.days

class CompleteTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val completionTime = BASE_TIME + 5.days
    private val clock = TestClock(completionTime)
    private val useCase = CompleteTaskUseCase(repository, clock)

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
}

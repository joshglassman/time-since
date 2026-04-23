package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateTaskUseCaseTest {

    private val repository = FakeTaskRepository()
    private val clock = TestClock(BASE_TIME)
    private val useCase = CreateTaskUseCase(repository, clock)

    @Test
    fun createsTaskWithCorrectFields() = runTest {
        val freq = TaskFrequency(3, FrequencyUnit.DAYS)
        useCase("Water plants", freq)

        val tasks = repository.observeAll().first()
        assertEquals(1, tasks.size)

        val task = tasks[0]
        assertEquals("Water plants", task.name)
        assertEquals(freq, task.frequency)
        assertEquals(BASE_TIME, task.lastCompletedAt)
        assertEquals(BASE_TIME, task.createdAt)
        assertTrue(task.id.isNotBlank())
    }

    @Test
    fun createsMultipleTasksWithUniqueIds() = runTest {
        val freq = TaskFrequency(1, FrequencyUnit.WEEKS)
        useCase("Task A", freq)
        useCase("Task B", freq)

        val tasks = repository.observeAll().first()
        assertEquals(2, tasks.size)
        assertTrue(tasks[0].id != tasks[1].id)
    }
}

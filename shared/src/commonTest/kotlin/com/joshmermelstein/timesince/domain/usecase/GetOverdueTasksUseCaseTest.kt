package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.FrequencyUnit
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class GetOverdueTasksUseCaseTest {

    private val repository = FakeTaskRepository()
    private val clock = TestClock(BASE_TIME + 10.days)
    private val useCase = GetOverdueTasksUseCase(repository, clock)

    @Test
    fun returnsOnlyOverdueTasks() = runTest {
        val overdue = taskWith(id = "1", name = "Overdue", frequencyAmount = 3, frequencyUnit = FrequencyUnit.DAYS)
        val ok = taskWith(id = "2", name = "OK", frequencyAmount = 30, frequencyUnit = FrequencyUnit.DAYS)

        repository.create(overdue)
        repository.create(ok)

        val result = useCase()
        assertEquals(1, result.size)
        assertEquals("Overdue", result[0].name)
    }

    @Test
    fun returnsEmptyWhenNoOverdueTasks() = runTest {
        val okTask = taskWith(id = "1", frequencyAmount = 30, frequencyUnit = FrequencyUnit.DAYS)
        repository.create(okTask)

        val result = useCase()
        assertEquals(0, result.size)
    }

    @Test
    fun returnsEmptyWhenNoTasks() = runTest {
        val result = useCase()
        assertEquals(0, result.size)
    }

    @Test
    fun returnsMultipleOverdueTasks() = runTest {
        val overdue1 = taskWith(id = "1", name = "A", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS)
        val overdue2 = taskWith(id = "2", name = "B", frequencyAmount = 5, frequencyUnit = FrequencyUnit.DAYS)

        repository.create(overdue1)
        repository.create(overdue2)

        val result = useCase()
        assertEquals(2, result.size)
    }
}

package com.scribbles.timesince.domain.usecase

import app.cash.turbine.test
import com.scribbles.timesince.domain.model.FrequencyUnit
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.days

class GetSortedTasksUseCaseTest {

    private val repository = FakeTaskRepository()
    private val clock = TestClock(BASE_TIME + 3.days)
    private val useCase = GetSortedTasksUseCase(repository, clock)

    @Test
    fun sortsByRemainingTimeAscending() = runTest {
        val urgent = taskWith(id = "1", name = "Urgent", frequencyAmount = 2, frequencyUnit = FrequencyUnit.DAYS)
        val relaxed = taskWith(id = "2", name = "Relaxed", frequencyAmount = 14, frequencyUnit = FrequencyUnit.DAYS)
        val medium = taskWith(id = "3", name = "Medium", frequencyAmount = 5, frequencyUnit = FrequencyUnit.DAYS)

        repository.create(relaxed)
        repository.create(urgent)
        repository.create(medium)

        useCase().test {
            val sorted = awaitItem()
            assertEquals(listOf("Urgent", "Medium", "Relaxed"), sorted.map { it.name })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyListReturnsEmpty() = runTest {
        useCase().test {
            val result = awaitItem()
            assertEquals(emptyList(), result)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overdueTasksAppearFirst() = runTest {
        val overdue = taskWith(id = "1", name = "Overdue", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS)
        val ok = taskWith(id = "2", name = "OK", frequencyAmount = 30, frequencyUnit = FrequencyUnit.DAYS)

        repository.create(ok)
        repository.create(overdue)

        useCase().test {
            val sorted = awaitItem()
            assertEquals("Overdue", sorted.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

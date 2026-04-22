package com.joshmermelstein.timesince.presentation.tasklist

import app.cash.turbine.test
import com.joshmermelstein.timesince.domain.model.TaskStatus
import com.joshmermelstein.timesince.domain.usecase.BASE_TIME
import com.joshmermelstein.timesince.domain.usecase.CompleteTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.DeleteTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.FakeTaskRepository
import com.joshmermelstein.timesince.domain.usecase.GetSortedTasksUseCase
import com.joshmermelstein.timesince.domain.usecase.TestClock
import com.joshmermelstein.timesince.domain.usecase.taskWith
import com.joshmermelstein.timesince.presentation.installMainDispatcher
import com.joshmermelstein.timesince.presentation.uninstallMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class TaskListViewModelTest {

    private val repository = FakeTaskRepository()
    private val clock = TestClock(BASE_TIME)
    private lateinit var viewModel: TaskListViewModel

    @BeforeTest
    fun setUp() {
        installMainDispatcher()
        viewModel = TaskListViewModel(
            getSortedTasks = GetSortedTasksUseCase(repository, clock),
            completeTask = CompleteTaskUseCase(repository, clock),
            deleteTask = DeleteTaskUseCase(repository),
            clock = clock,
        )
    }

    @AfterTest
    fun tearDown() {
        uninstallMainDispatcher()
    }

    @Test
    fun statePresentsTasksSortedByRemainingTime() = runTest {
        // Two tasks: one due in 1 day, one due in 7 days
        repository.create(taskWith(id = "a", name = "Soon", frequencyAmount = 1))
        repository.create(taskWith(id = "b", name = "Later", frequencyAmount = 7))

        // Advance clock 12 hours so neither is overdue but order matters
        clock.now = BASE_TIME

        viewModel.state.test {
            // Skip initial loading state
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }
            assertEquals(2, current.tasks.size)
            assertEquals("Soon", current.tasks[0].name)
            assertEquals("Later", current.tasks[1].name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun overdueTaskHasOverdueStatus() = runTest {
        repository.create(taskWith(id = "a", frequencyAmount = 1))
        clock.now = BASE_TIME + 2.days

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }
            assertEquals(1, current.tasks.size)
            assertEquals(TaskStatus.OVERDUE, current.tasks[0].status)
            assertTrue(current.tasks[0].remaining.isNegative())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onTaskCompletedResetsLastCompletedAt() = runTest {
        repository.create(taskWith(id = "a", frequencyAmount = 1))
        clock.now = BASE_TIME + 2.days

        viewModel.onTaskCompleted("a")

        val updated = repository.getById("a")
        assertEquals(clock.now, updated?.lastCompletedAt)
    }

    @Test
    fun onTaskDeletedRemovesFromRepository() = runTest {
        repository.create(taskWith(id = "a"))
        repository.create(taskWith(id = "b"))

        viewModel.onTaskDeleted("a")

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading || current.tasks.size != 1) {
                current = awaitItem()
            }
            assertEquals("b", current.tasks[0].id)
            assertFalse(current.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

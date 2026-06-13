package com.scribbles.timesince.presentation.tasklist

import app.cash.turbine.test
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.undo.UndoStore
import com.scribbles.timesince.domain.usecase.CompleteTaskUseCase
import com.scribbles.timesince.domain.usecase.DeleteTaskUseCase
import com.scribbles.timesince.domain.usecase.FakeTaskRepository
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.usecase.GetSortedTasksUseCase
import com.scribbles.timesince.domain.usecase.SnoozeTaskUseCase
import com.scribbles.timesince.domain.usecase.TestClock
import com.scribbles.timesince.domain.usecase.UTC_PROVIDER
import com.scribbles.timesince.domain.usecase.UndoTaskUseCase
import com.scribbles.timesince.domain.usecase.taskWith
import com.scribbles.timesince.presentation.installMainDispatcher
import com.scribbles.timesince.presentation.uninstallMainDispatcher
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
    private val undoStore = UndoStore()
    private lateinit var viewModel: TaskListViewModel

    @BeforeTest
    fun setUp() {
        installMainDispatcher()
        viewModel = TaskListViewModel(
            getSortedTasks = GetSortedTasksUseCase(repository, clock, UTC_PROVIDER),
            completeTask = CompleteTaskUseCase(repository, clock, undoStore),
            deleteTask = DeleteTaskUseCase(repository),
            undoTask = UndoTaskUseCase(repository, undoStore),
            clock = clock,
            timeZoneProvider = UTC_PROVIDER,
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
            assertTrue(current.tasks[0].elapsed > current.tasks[0].frequency.approxDuration())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onTaskCompletedEmitsEvent() = runTest {
        repository.create(taskWith(id = "a", frequencyAmount = 1))

        viewModel.completedTaskEvents.test {
            viewModel.onTaskCompleted("a")
            assertEquals("a", awaitItem())
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
    fun onUndoCompleteRevertsCompletion() = runTest {
        repository.create(taskWith(id = "a", lastCompletedAt = BASE_TIME, frequencyAmount = 1))
        clock.now = BASE_TIME + 2.days

        viewModel.onTaskCompleted("a")
        assertEquals(clock.now, repository.getById("a")?.lastCompletedAt)

        viewModel.onUndoComplete("a")
        assertEquals(BASE_TIME, repository.getById("a")?.lastCompletedAt)
    }

    @Test
    fun snoozedTaskShowsSnoozeIndicator() = runTest {
        repository.create(taskWith(id = "a", snooze = 2.days))

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }
            assertTrue(current.tasks.single().isSnoozed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun undoingASnoozeClearsTheSnoozeIndicator() = runTest {
        val snooze = SnoozeTaskUseCase(repository, undoStore, clock, UTC_PROVIDER)
        val undo = UndoTaskUseCase(repository, undoStore)
        repository.create(taskWith(id = "a", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS))

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) current = awaitItem()
            assertFalse(current.tasks.single().isSnoozed)

            // Snooze -> indicator turns on.
            snooze("a", 5, FrequencyUnit.DAYS)
            while (!current.tasks.single().isSnoozed) current = awaitItem()

            // Undo -> indicator clears (snooze restored to its prior zero).
            undo("a")
            while (current.tasks.single().isSnoozed) current = awaitItem()
            assertFalse(current.tasks.single().isSnoozed)
            cancelAndIgnoreRemainingEvents()
        }
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

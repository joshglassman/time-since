package com.scribbles.timesince.presentation.taskedit

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.undo.UndoStore
import com.scribbles.timesince.domain.usecase.CreateTaskUseCase
import com.scribbles.timesince.domain.usecase.DeleteTaskUseCase
import com.scribbles.timesince.domain.usecase.FakeCategoryRepository
import com.scribbles.timesince.domain.usecase.FakeTaskRepository
import com.scribbles.timesince.domain.usecase.GetCategoriesUseCase
import com.scribbles.timesince.domain.usecase.SetArchivedUseCase
import com.scribbles.timesince.domain.usecase.SetPausedUseCase
import com.scribbles.timesince.domain.usecase.SnoozeTaskUseCase
import com.scribbles.timesince.domain.usecase.TestClock
import com.scribbles.timesince.domain.usecase.UTC_PROVIDER
import com.scribbles.timesince.domain.usecase.UndoTaskUseCase
import com.scribbles.timesince.domain.usecase.UpdateTaskUseCase
import com.scribbles.timesince.domain.usecase.taskWith
import com.scribbles.timesince.presentation.installMainDispatcher
import com.scribbles.timesince.presentation.uninstallMainDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaskEditViewModelTest {

    private val repository = FakeTaskRepository()
    private val categoryRepository = FakeCategoryRepository()
    private val clock = TestClock(BASE_TIME)
    private val undoStore = UndoStore()
    private lateinit var viewModel: TaskEditViewModel

    @BeforeTest
    fun setUp() {
        installMainDispatcher()
        viewModel = TaskEditViewModel(
            repository = repository,
            createTask = CreateTaskUseCase(repository, clock),
            updateTask = UpdateTaskUseCase(repository),
            snoozeTask = SnoozeTaskUseCase(repository, undoStore, clock, UTC_PROVIDER),
            undoTask = UndoTaskUseCase(repository, undoStore),
            setPaused = SetPausedUseCase(repository, clock),
            setArchived = SetArchivedUseCase(repository),
            deleteTask = DeleteTaskUseCase(repository),
            getCategories = GetCategoriesUseCase(categoryRepository),
            undoStore = undoStore,
        )
    }

    @AfterTest
    fun tearDown() {
        uninstallMainDispatcher()
    }

    @Test
    fun loadNullProducesNewTaskState() = runTest {
        viewModel.load(null)
        val state = viewModel.state.value
        assertTrue(state.isNew)
        assertEquals("", state.name)
        assertEquals("1", state.frequencyAmount)
        assertEquals(FrequencyUnit.DAYS, state.frequencyUnit)
        assertFalse(state.canSave)
    }

    @Test
    fun loadExistingPopulatesState() = runTest {
        repository.create(
            taskWith(
                id = "a",
                name = "Existing",
                frequencyAmount = 3,
                frequencyUnit = FrequencyUnit.WEEKS,
            )
        )

        viewModel.load("a")

        val state = viewModel.state.value
        assertFalse(state.isNew)
        assertEquals("Existing", state.name)
        assertEquals("3", state.frequencyAmount)
        assertEquals(FrequencyUnit.WEEKS, state.frequencyUnit)
    }

    @Test
    fun canSaveRequiresNameAndPositiveAmount() = runTest {
        viewModel.load(null)
        assertFalse(viewModel.state.value.canSave)

        viewModel.onNameChanged("Read book")
        assertTrue(viewModel.state.value.canSave)

        viewModel.onFrequencyAmountChanged("0")
        assertFalse(viewModel.state.value.canSave)

        viewModel.onFrequencyAmountChanged("5")
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun frequencyAmountRejectsNonDigits() = runTest {
        viewModel.load(null)
        viewModel.onFrequencyAmountChanged("abc")
        assertEquals("1", viewModel.state.value.frequencyAmount)

        viewModel.onFrequencyAmountChanged("12")
        assertEquals("12", viewModel.state.value.frequencyAmount)
    }

    @Test
    fun saveCreatesNewTask() = runTest {
        viewModel.load(null)
        viewModel.onNameChanged("New task")
        viewModel.onFrequencyAmountChanged("2")
        viewModel.onFrequencyUnitChanged(FrequencyUnit.HOURS)

        viewModel.onSave()

        assertTrue(viewModel.state.value.saved)
        val tasks = repository.observeAll().first()
        assertTrue(tasks.any { it.name == "New task" })
    }

    @Test
    fun saveUpdatesExistingTask() = runTest {
        repository.create(
            taskWith(
                id = "a",
                name = "Original",
                frequencyAmount = 1,
                frequencyUnit = FrequencyUnit.DAYS,
            )
        )

        viewModel.load("a")
        viewModel.onNameChanged("Updated")
        viewModel.onFrequencyAmountChanged("4")
        viewModel.onFrequencyUnitChanged(FrequencyUnit.HOURS)
        viewModel.onSave()

        assertTrue(viewModel.state.value.saved)
        val task = repository.getById("a")
        assertNotNull(task)
        assertEquals("Updated", task.name)
        assertEquals(4, task.frequency.amount)
        assertEquals(FrequencyUnit.HOURS, task.frequency.unit)
    }

    @Test
    fun saveTrimsWhitespaceFromName() = runTest {
        viewModel.load(null)
        viewModel.onNameChanged("  Trimmed  ")
        viewModel.onSave()

        val tasks = repository.observeAll().first()
        assertTrue(tasks.any { it.name == "Trimmed" })
    }

    @Test
    fun saveDoesNothingWhenInvalid() = runTest {
        viewModel.load(null)
        viewModel.onSave()
        assertFalse(viewModel.state.value.saved)
    }

    @Test
    fun newTaskCannotSnooze() = runTest {
        viewModel.load(null)
        assertFalse(viewModel.state.value.canSnooze)
    }

    @Test
    fun snoozeAppliesAndEnablesUndo() = runTest {
        repository.create(
            taskWith(id = "a", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS),
        )
        viewModel.load("a")
        assertTrue(viewModel.state.value.canSnooze)

        viewModel.onSnoozeAmountChanged("5")
        viewModel.onSnooze()

        assertTrue(repository.getById("a")!!.snooze > kotlin.time.Duration.ZERO)
        assertTrue(viewModel.state.value.canUndo)
    }

    @Test
    fun undoRevertsSnoozeAndDisablesUndo() = runTest {
        repository.create(
            taskWith(id = "a", frequencyAmount = 1, frequencyUnit = FrequencyUnit.DAYS),
        )
        viewModel.load("a")
        viewModel.onSnoozeAmountChanged("5")
        viewModel.onSnooze()

        viewModel.onUndo()

        assertEquals(kotlin.time.Duration.ZERO, repository.getById("a")!!.snooze)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun pauseTogglePersistsAndUpdatesState() = runTest {
        repository.create(taskWith(id = "a", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        viewModel.load("a")
        assertFalse(viewModel.state.value.isPaused)

        viewModel.onTogglePause()
        assertTrue(viewModel.state.value.isPaused)
        assertNotNull(repository.getById("a")!!.pausedAt)

        viewModel.onTogglePause()
        assertFalse(viewModel.state.value.isPaused)
        assertEquals(null, repository.getById("a")!!.pausedAt)
    }

    @Test
    fun archiveTogglePersistsAndClearsPausedState() = runTest {
        repository.create(taskWith(id = "a", frequencyAmount = 10, frequencyUnit = FrequencyUnit.DAYS))
        viewModel.load("a")
        viewModel.onTogglePause()
        assertTrue(viewModel.state.value.isPaused)

        viewModel.onToggleArchive()

        assertTrue(viewModel.state.value.isArchived)
        assertFalse(viewModel.state.value.isPaused)
        val task = repository.getById("a")!!
        assertTrue(task.archived)
        assertEquals(null, task.pausedAt)
    }

    @Test
    fun deleteRemovesTaskAndSignalsDeleted() = runTest {
        repository.create(taskWith(id = "a"))
        viewModel.load("a")

        viewModel.onDelete()

        assertTrue(viewModel.state.value.deleted)
        assertEquals(null, repository.getById("a"))
    }

    @Test
    fun categorySelectionIsPersistedOnSave() = runTest {
        categoryRepository.create(
            com.scribbles.timesince.domain.model.Category("c1", "Work", "#1e66f5", BASE_TIME),
        )
        repository.create(taskWith(id = "a"))
        viewModel.load("a")
        assertTrue(viewModel.state.value.categories.any { it.id == "c1" })

        viewModel.onCategorySelected("c1")
        viewModel.onSave()

        assertEquals("c1", repository.getById("a")?.categoryId)
    }

    @Test
    fun lastCompletedAtChangeIsPersistedOnSave() = runTest {
        repository.create(
            taskWith(id = "a", name = "T", frequencyAmount = 1, lastCompletedAt = BASE_TIME),
        )

        viewModel.load("a")
        val newTime = BASE_TIME - kotlin.time.Duration.parseIsoString("PT6H")
        viewModel.onLastCompletedAtChanged(newTime)
        viewModel.onSave()

        val stored = repository.getById("a")
        assertNotNull(stored)
        assertEquals(newTime, stored.lastCompletedAt)
    }
}

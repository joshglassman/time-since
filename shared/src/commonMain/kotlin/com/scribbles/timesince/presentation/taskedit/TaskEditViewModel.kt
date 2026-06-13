package com.scribbles.timesince.presentation.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskFrequency
import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.undo.UndoStore
import com.scribbles.timesince.domain.usecase.CreateTaskUseCase
import com.scribbles.timesince.domain.usecase.SetArchivedUseCase
import com.scribbles.timesince.domain.usecase.SetPausedUseCase
import com.scribbles.timesince.domain.usecase.SnoozeTaskUseCase
import com.scribbles.timesince.domain.usecase.UndoTaskUseCase
import com.scribbles.timesince.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

class TaskEditViewModel(
    private val repository: TaskRepository,
    private val createTask: CreateTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
    private val snoozeTask: SnoozeTaskUseCase,
    private val undoTask: UndoTaskUseCase,
    private val setPaused: SetPausedUseCase,
    private val setArchived: SetArchivedUseCase,
    private val undoStore: UndoStore,
    private val syncCoordinator: SyncCoordinator? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(TaskEditUiState())
    val state: StateFlow<TaskEditUiState> = _state.asStateFlow()

    private var editingTaskId: String? = null

    fun load(taskId: String?) {
        if (taskId == null) {
            _state.value = TaskEditUiState(isNew = true)
            editingTaskId = null
            return
        }
        editingTaskId = taskId
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val task = repository.getById(taskId)
            if (task == null) {
                _state.value = TaskEditUiState(isNew = true)
            } else {
                _state.value = TaskEditUiState(
                    isLoading = false,
                    isNew = false,
                    name = task.name,
                    frequencyAmount = task.frequency.amount.toString(),
                    frequencyUnit = task.frequency.unit,
                    lastCompletedAt = task.lastCompletedAt,
                    // Default the snooze unit to the task's own repetition unit.
                    snoozeUnit = task.frequency.unit,
                    canUndo = undoStore.hasSnapshot(taskId),
                    isPaused = task.pausedAt != null,
                    isArchived = task.archived,
                )
            }
        }
    }

    fun onNameChanged(name: String) {
        _state.update { it.copy(name = name) }
    }

    fun onFrequencyAmountChanged(amount: String) {
        if (amount.isEmpty() || amount.all(Char::isDigit)) {
            _state.update { it.copy(frequencyAmount = amount) }
        }
    }

    fun onFrequencyUnitChanged(unit: FrequencyUnit) {
        _state.update { it.copy(frequencyUnit = unit) }
    }

    fun onLastCompletedAtChanged(instant: Instant) {
        _state.update { it.copy(lastCompletedAt = instant) }
    }

    fun onSnoozeAmountChanged(amount: String) {
        if (amount.isEmpty() || amount.all(Char::isDigit)) {
            _state.update { it.copy(snoozeAmount = amount) }
        }
    }

    fun onSnoozeUnitChanged(unit: FrequencyUnit) {
        _state.update { it.copy(snoozeUnit = unit) }
    }

    fun onSnooze() {
        val current = _state.value
        if (!current.canSnooze) return
        val id = editingTaskId ?: return
        val amount = current.snoozeAmount.toIntOrNull() ?: return
        viewModelScope.launch {
            snoozeTask(id, amount, current.snoozeUnit)
            syncCoordinator?.requestSync()
            _state.update { it.copy(canUndo = true) }
        }
    }

    fun onUndo() {
        val id = editingTaskId ?: return
        viewModelScope.launch {
            val undone = undoTask(id)
            if (undone) {
                syncCoordinator?.requestSync()
                _state.update { it.copy(canUndo = false) }
            }
        }
    }

    fun onTogglePause() {
        val id = editingTaskId ?: return
        val pause = !_state.value.isPaused
        viewModelScope.launch {
            setPaused(id, pause)
            syncCoordinator?.requestSync()
            _state.update { it.copy(isPaused = pause) }
        }
    }

    fun onToggleArchive() {
        val id = editingTaskId ?: return
        val archive = !_state.value.isArchived
        viewModelScope.launch {
            setArchived(id, archive)
            syncCoordinator?.requestSync()
            // Archiving overrides/clears pause.
            _state.update {
                it.copy(isArchived = archive, isPaused = if (archive) false else it.isPaused)
            }
        }
    }

    fun onSave() {
        val current = _state.value
        if (!current.canSave) return
        val frequency = TaskFrequency(
            amount = current.frequencyAmount.toInt(),
            unit = current.frequencyUnit,
        )
        viewModelScope.launch {
            val id = editingTaskId
            if (id == null) {
                createTask(current.name.trim(), frequency)
            } else {
                val existing = repository.getById(id) ?: return@launch
                updateTask(
                    existing.copy(
                        name = current.name.trim(),
                        frequency = frequency,
                        lastCompletedAt = current.lastCompletedAt ?: existing.lastCompletedAt,
                    ),
                )
            }
            syncCoordinator?.requestSync()
            _state.update { it.copy(saved = true) }
        }
    }
}

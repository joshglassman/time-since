package com.joshmermelstein.timesince.presentation.taskedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshmermelstein.timesince.domain.model.FrequencyUnit
import com.joshmermelstein.timesince.domain.model.TaskFrequency
import com.joshmermelstein.timesince.domain.repository.TaskRepository
import com.joshmermelstein.timesince.domain.usecase.CreateTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.UpdateTaskUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskEditViewModel(
    private val repository: TaskRepository,
    private val createTask: CreateTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
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
                updateTask(existing.copy(name = current.name.trim(), frequency = frequency))
            }
            _state.update { it.copy(saved = true) }
        }
    }
}

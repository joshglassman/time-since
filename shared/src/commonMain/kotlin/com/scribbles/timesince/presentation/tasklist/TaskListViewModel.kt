package com.scribbles.timesince.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.domain.usecase.CompleteTaskUseCase
import com.scribbles.timesince.domain.usecase.GetCategoriesUseCase
import com.scribbles.timesince.domain.usecase.GetSortedTasksUseCase
import com.scribbles.timesince.domain.usecase.UndoTaskUseCase
import com.scribbles.timesince.domain.time.TimeZoneProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class TaskListViewModel(
    private val getSortedTasks: GetSortedTasksUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val undoTask: UndoTaskUseCase,
    private val syncCoordinator: SyncCoordinator? = null,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = TimeZoneProvider.System,
) : ViewModel() {

    private val filter = MutableStateFlow<TaskFilter>(TaskFilter.Active)

    val state: StateFlow<TaskListUiState> =
        combine(getSortedTasks(), getCategories(), filter) { tasks, categories, activeFilter ->
            val now = clock.now()
            val tz = timeZoneProvider.current()
            TaskListUiState(
                isLoading = false,
                tasks = tasks.filter { activeFilter.matches(it) }.toListItems(now, tz, categories),
                filter = activeFilter,
                categories = categories.map { CategoryChip(it.id, it.name, it.colorHex, it.icon) },
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TaskListUiState(),
            )

    private val _completedTaskEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val completedTaskEvents: SharedFlow<String> = _completedTaskEvents

    fun onTaskCompleted(taskId: String) {
        viewModelScope.launch {
            completeTask(taskId)
            _completedTaskEvents.emit(taskId)
            syncCoordinator?.requestSync()
        }
    }

    /** Reverts the most recent completion of [taskId] (e.g. from the Snackbar). */
    fun onUndoComplete(taskId: String) {
        viewModelScope.launch {
            undoTask(taskId)
            syncCoordinator?.requestSync()
        }
    }

    fun onFilterSelected(newFilter: TaskFilter) {
        filter.value = newFilter
    }
}

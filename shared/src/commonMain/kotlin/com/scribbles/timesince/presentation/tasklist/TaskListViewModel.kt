package com.scribbles.timesince.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.domain.usecase.CompleteTaskUseCase
import com.scribbles.timesince.domain.usecase.DeleteTaskUseCase
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
    private val completeTask: CompleteTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val undoTask: UndoTaskUseCase,
    private val syncCoordinator: SyncCoordinator? = null,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = TimeZoneProvider.System,
) : ViewModel() {

    private val showArchived = MutableStateFlow(false)

    val state: StateFlow<TaskListUiState> = combine(getSortedTasks(), showArchived) { tasks, archived ->
        val now = clock.now()
        val tz = timeZoneProvider.current()
        TaskListUiState(
            isLoading = false,
            tasks = tasks.filter { it.archived == archived }.map { it.toListItem(now, tz) },
            showingArchived = archived,
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

    fun onTaskDeleted(taskId: String) {
        viewModelScope.launch {
            deleteTask(taskId)
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

    /** Toggles between the active list and the archived-tasks view. */
    fun onToggleShowArchived() {
        showArchived.value = !showArchived.value
    }
}

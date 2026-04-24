package com.scribbles.timesince.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.domain.usecase.CompleteTaskUseCase
import com.scribbles.timesince.domain.usecase.DeleteTaskUseCase
import com.scribbles.timesince.domain.usecase.GetSortedTasksUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class TaskListViewModel(
    private val getSortedTasks: GetSortedTasksUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val syncCoordinator: SyncCoordinator? = null,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    val state: StateFlow<TaskListUiState> = getSortedTasks()
        .map { tasks ->
            val now = clock.now()
            TaskListUiState(
                isLoading = false,
                tasks = tasks.map { it.toListItem(now) },
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
}

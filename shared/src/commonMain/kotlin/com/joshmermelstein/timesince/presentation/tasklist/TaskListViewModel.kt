package com.joshmermelstein.timesince.presentation.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshmermelstein.timesince.domain.usecase.CompleteTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.DeleteTaskUseCase
import com.joshmermelstein.timesince.domain.usecase.GetSortedTasksUseCase
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

    fun onTaskCompleted(taskId: String) {
        viewModelScope.launch { completeTask(taskId) }
    }

    fun onTaskDeleted(taskId: String) {
        viewModelScope.launch { deleteTask(taskId) }
    }
}

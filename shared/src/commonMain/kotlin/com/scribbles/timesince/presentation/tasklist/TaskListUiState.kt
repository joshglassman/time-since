package com.scribbles.timesince.presentation.tasklist

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.elapsedSinceCompleted
import com.scribbles.timesince.domain.model.status
import kotlin.time.Duration
import kotlin.time.Instant

data class TaskListUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskListItem> = emptyList(),
)

data class TaskListItem(
    val id: String,
    val name: String,
    val status: TaskStatus,
    val elapsed: Duration,
    val frequency: TaskFrequency,
)

internal fun Task.toListItem(now: Instant): TaskListItem = TaskListItem(
    id = id,
    name = name,
    status = status(now),
    elapsed = elapsedSinceCompleted(now),
    frequency = frequency,
)

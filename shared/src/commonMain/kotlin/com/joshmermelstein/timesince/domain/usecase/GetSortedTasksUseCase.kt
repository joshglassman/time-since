package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.model.remainingTime
import com.joshmermelstein.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class GetSortedTasksUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
) {
    operator fun invoke(): Flow<List<Task>> =
        repository.observeAll().map { tasks ->
            tasks.sortedBy { it.remainingTime(clock.now()) }
        }
}

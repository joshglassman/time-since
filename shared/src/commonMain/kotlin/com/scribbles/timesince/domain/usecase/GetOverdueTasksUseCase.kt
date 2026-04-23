package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.status
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class GetOverdueTasksUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(): List<Task> {
        val now = clock.now()
        return repository.observeAll().first().filter {
            it.status(now) == TaskStatus.OVERDUE
        }
    }
}

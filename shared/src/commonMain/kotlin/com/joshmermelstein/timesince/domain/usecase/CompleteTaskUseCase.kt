package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.repository.TaskRepository
import kotlin.time.Clock

class CompleteTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getById(taskId) ?: return
        repository.update(task.copy(lastCompletedAt = clock.now()))
    }
}

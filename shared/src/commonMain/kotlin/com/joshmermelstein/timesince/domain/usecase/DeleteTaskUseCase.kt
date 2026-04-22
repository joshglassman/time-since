package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String) {
        repository.delete(taskId)
    }
}

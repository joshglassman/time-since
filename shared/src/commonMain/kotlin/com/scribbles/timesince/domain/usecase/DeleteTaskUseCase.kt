package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String) {
        repository.delete(taskId)
    }
}

package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task) {
        repository.update(task)
    }
}

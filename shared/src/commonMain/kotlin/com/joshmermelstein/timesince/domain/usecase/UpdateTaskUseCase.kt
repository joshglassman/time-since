package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task) {
        repository.update(task)
    }
}

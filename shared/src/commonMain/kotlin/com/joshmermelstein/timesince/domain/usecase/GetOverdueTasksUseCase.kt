package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.model.TaskStatus
import com.joshmermelstein.timesince.domain.model.status
import com.joshmermelstein.timesince.domain.repository.TaskRepository
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

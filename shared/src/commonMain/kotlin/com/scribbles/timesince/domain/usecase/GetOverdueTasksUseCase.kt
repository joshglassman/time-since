package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.status
import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.time.TimeZoneProvider
import kotlinx.coroutines.flow.first
import kotlin.time.Clock

class GetOverdueTasksUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = TimeZoneProvider.System,
) {
    suspend operator fun invoke(): List<Task> {
        val now = clock.now()
        val tz = timeZoneProvider.current()
        return repository.observeAll().first().filter {
            !it.archived && it.status(now, tz) == TaskStatus.OVERDUE
        }
    }
}

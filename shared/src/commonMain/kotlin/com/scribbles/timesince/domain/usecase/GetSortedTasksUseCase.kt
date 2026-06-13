package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.remainingTime
import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.time.TimeZoneProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class GetSortedTasksUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = TimeZoneProvider.System,
) {
    operator fun invoke(): Flow<List<Task>> =
        repository.observeAll().map { tasks ->
            val now = clock.now()
            val tz = timeZoneProvider.current()
            tasks.sortedBy { it.remainingTime(now, tz) }
        }
}

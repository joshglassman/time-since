package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.model.TaskFrequency
import com.joshmermelstein.timesince.domain.repository.TaskRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(name: String, frequency: TaskFrequency) {
        val now = clock.now()
        val task = Task(
            id = Uuid.random().toString(),
            name = name,
            lastCompletedAt = now,
            frequency = frequency,
            createdAt = now,
        )
        repository.create(task)
    }
}

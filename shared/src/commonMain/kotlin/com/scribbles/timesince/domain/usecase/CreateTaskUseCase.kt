package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(name: String, frequency: TaskFrequency, categoryId: String? = null) {
        val now = clock.now()
        val task = Task(
            id = Uuid.random().toString(),
            name = name,
            lastCompletedAt = now,
            frequency = frequency,
            createdAt = now,
            categoryId = categoryId,
        )
        repository.create(task)
    }
}

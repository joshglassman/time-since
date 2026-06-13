package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.undo.UndoStore
import kotlin.time.Clock
import kotlin.time.Duration

class CompleteTaskUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
    private val undoStore: UndoStore = UndoStore(),
) {
    suspend operator fun invoke(taskId: String) {
        val task = repository.getById(taskId) ?: return
        undoStore.record(task)
        repository.update(
            task.copy(lastCompletedAt = clock.now(), snooze = Duration.ZERO),
        )
    }
}

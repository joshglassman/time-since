package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.TaskRepository
import kotlin.time.Clock

/**
 * Pauses or resumes a task, freezing/unfreezing its clock.
 *
 * - **Pause** sets `pausedAt = now` (no-op if already paused or archived).
 * - **Resume** folds the paused span into `snooze` (`snooze += now - pausedAt`)
 *   and clears `pausedAt`, so the deadline shifts forward by exactly the time
 *   spent paused — `lastCompletedAt` is never touched.
 */
class SetPausedUseCase(
    private val repository: TaskRepository,
    private val clock: Clock = Clock.System,
) {
    suspend operator fun invoke(taskId: String, paused: Boolean) {
        val task = repository.getById(taskId) ?: return
        val now = clock.now()
        if (paused) {
            if (task.pausedAt != null || task.archived) return
            repository.update(task.copy(pausedAt = now))
        } else {
            val pausedAt = task.pausedAt ?: return
            repository.update(
                task.copy(
                    snooze = task.snooze + (now - pausedAt),
                    pausedAt = null,
                ),
            )
        }
    }
}

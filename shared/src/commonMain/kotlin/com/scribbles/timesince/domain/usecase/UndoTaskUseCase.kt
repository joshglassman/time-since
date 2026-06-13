package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.undo.UndoStore

/**
 * Reverts the last undoable action recorded for [taskId], restoring
 * `lastCompletedAt` and `snooze` to their pre-mutation values. The write goes
 * through [TaskRepository.update] so `updatedAt` is freshly stamped — the undo
 * is the user's latest intent and must win sync merge resolution (otherwise a
 * synced intermediate state, e.g. a snooze, could resurrect after undo).
 *
 * Returns `true` if a snapshot existed and was applied.
 */
class UndoTaskUseCase(
    private val repository: TaskRepository,
    private val undoStore: UndoStore,
) {
    suspend operator fun invoke(taskId: String): Boolean {
        val snapshot = undoStore.take(taskId) ?: return false
        val current = repository.getById(taskId) ?: return false
        repository.update(
            current.copy(
                lastCompletedAt = snapshot.prevLastCompletedAt,
                snooze = snapshot.prevSnooze,
            ),
        )
        return true
    }
}

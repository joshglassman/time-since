package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.TaskRepository

/**
 * Archives or unarchives a task.
 *
 * - **Archive** parks the task and **overrides pause**: it clears `pausedAt`
 *   *without* applying the resume snooze-shift, so no time is folded in.
 * - **Unarchive** returns the task with its prior timing untouched
 *   (`lastCompletedAt` / `snooze` unchanged) — it may legitimately be overdue if
 *   time has passed; there is no fresh-completion behavior.
 */
class SetArchivedUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, archived: Boolean) {
        val task = repository.getById(taskId) ?: return
        if (task.archived == archived) return
        repository.update(
            if (archived) {
                task.copy(archived = true, pausedAt = null)
            } else {
                task.copy(archived = false)
            },
        )
    }
}

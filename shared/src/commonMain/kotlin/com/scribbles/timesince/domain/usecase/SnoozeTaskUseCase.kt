package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.TaskFrequency
import com.scribbles.timesince.domain.repository.TaskRepository
import com.scribbles.timesince.domain.time.TimeZoneProvider
import com.scribbles.timesince.domain.undo.UndoStore
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * Snoozes a task by [amount] [unit] from *now*: sets the deadline to `now + N`,
 * floored at the natural deadline. An existing snooze can be reduced (down to
 * zero) but the final snooze is never negative.
 *
 * An [amount] of `0` **clears** the snooze outright (back to no snooze), so the
 * snoozed indicator goes away — it is not treated as "now + 0".
 */
class SnoozeTaskUseCase(
    private val repository: TaskRepository,
    private val undoStore: UndoStore,
    private val clock: Clock = Clock.System,
    private val timeZoneProvider: TimeZoneProvider = TimeZoneProvider.System,
) {
    suspend operator fun invoke(taskId: String, amount: Int, unit: FrequencyUnit) {
        require(amount >= 0) { "Snooze amount must be non-negative" }
        val task = repository.getById(taskId) ?: return
        val newSnooze = if (amount == 0) {
            Duration.ZERO
        } else {
            val now = clock.now()
            val tz = timeZoneProvider.current()
            val base = task.frequency.advance(task.lastCompletedAt, tz) // natural deadline
            val target = TaskFrequency(amount, unit).advance(now, tz) // "now + N", calendar-aware
            maxOf(base, target) - base // always >= 0
        }
        undoStore.record(task)
        repository.update(task.copy(snooze = newSnooze))
    }
}

package com.scribbles.timesince.domain.undo

import com.scribbles.timesince.domain.model.Task
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * The state needed to revert a single undoable mutation on one task: the
 * pre-mutation `lastCompletedAt` and `snooze`.
 *
 * `updatedAt` is intentionally **not** captured — an undo is the user's latest
 * intent and is re-stamped on apply so it wins sync merge resolution (otherwise
 * a synced intermediate state could resurrect, e.g. a snooze reappearing after
 * undo).
 */
data class UndoSnapshot(
    val prevLastCompletedAt: Instant,
    val prevSnooze: Duration,
)

/**
 * App-session-scoped, in-memory record of the last undoable action per task.
 *
 * Shared across the task list and task edit screens (a single Koin singleton),
 * intentionally **not persisted** so it does not survive process death. Each
 * undoable mutation records (overwriting any prior) a snapshot before applying;
 * an undo takes and applies it.
 */
class UndoStore {
    private val snapshots = mutableMapOf<String, UndoSnapshot>()

    /** Record the current state of [task] so the next mutation can be undone. */
    fun record(task: Task) {
        snapshots[task.id] = UndoSnapshot(
            prevLastCompletedAt = task.lastCompletedAt,
            prevSnooze = task.snooze,
        )
    }

    fun hasSnapshot(taskId: String): Boolean = snapshots.containsKey(taskId)

    /** Remove and return the snapshot for [taskId], if any. */
    fun take(taskId: String): UndoSnapshot? = snapshots.remove(taskId)

    fun clear(taskId: String) {
        snapshots.remove(taskId)
    }
}

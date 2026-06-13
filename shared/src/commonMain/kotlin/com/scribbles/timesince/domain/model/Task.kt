package com.scribbles.timesince.domain.model

import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.time.Clock
import kotlin.time.Duration

data class Task(
    val id: String,
    val name: String,
    val lastCompletedAt: Instant,
    val frequency: TaskFrequency,
    val createdAt: Instant,
    val updatedAt: Instant = lastCompletedAt,
    /**
     * Extra time added to the natural deadline until the next completion.
     * Always `>= 0`; reset to [Duration.ZERO] on completion. See
     * `SnoozeTaskUseCase` for how it is computed.
     */
    val snooze: Duration = Duration.ZERO,
    /**
     * When non-null, the task's clock is frozen as of this instant: status,
     * remaining, and elapsed are all evaluated using [pausedAt] as "now".
     * Resume folds the paused span into [snooze]; archive clears it outright.
     */
    val pausedAt: Instant? = null,
    /** Parked tasks: excluded from the active list; overrides/clears pause. */
    val archived: Boolean = false,
)

/** "Now" for time evaluation: frozen at [Task.pausedAt] while paused. */
private fun Task.evalNow(now: Instant): Instant = pausedAt ?: now

fun Task.deadline(tz: TimeZone): Instant = frequency.advance(lastCompletedAt, tz) + snooze

fun Task.remainingTime(now: Instant = Clock.System.now(), tz: TimeZone): Duration =
    deadline(tz) - evalNow(now)

fun Task.elapsedSinceCompleted(now: Instant = Clock.System.now()): Duration =
    evalNow(now) - lastCompletedAt

fun Task.status(now: Instant = Clock.System.now(), tz: TimeZone): TaskStatus {
    val deadline = deadline(tz)
    val remaining = deadline - evalNow(now)
    // Use the concrete current cycle length (calendar-correct, incl. snooze) as the total.
    val total = deadline - lastCompletedAt
    return when {
        remaining < Duration.ZERO -> TaskStatus.OVERDUE
        remaining < total * 0.1 -> TaskStatus.DUE_SOON
        else -> TaskStatus.OK
    }
}

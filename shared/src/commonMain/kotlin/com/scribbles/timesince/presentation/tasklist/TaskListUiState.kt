package com.scribbles.timesince.presentation.tasklist

import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.domain.model.deadline
import com.scribbles.timesince.domain.model.elapsedSinceCompleted
import com.scribbles.timesince.domain.model.status
import com.scribbles.timesince.presentation.format.TimeSinceFormatter
import kotlinx.datetime.TimeZone
import kotlin.time.Duration
import kotlin.time.Instant

data class TaskListUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskListItem> = emptyList(),
    /** When true the list shows archived tasks instead of the active ones. */
    val showingArchived: Boolean = false,
)

data class TaskListItem(
    val id: String,
    val name: String,
    val status: TaskStatus,
    val elapsed: Duration,
    val frequency: TaskFrequency,
    /**
     * Calendar-correct "elapsed / frequency" label (e.g. `"1w 3d / 3m"`),
     * precomputed here so the Compose layer never has to resolve a `TimeZone`.
     */
    val displayText: String,
    /**
     * Fill fraction of the current cycle (0 at just-completed, 1 at the
     * calendar-correct deadline), already clamped to `0f..1f`. Computed here so
     * the Compose layer never touches `frequency` for deadline/fill math.
     */
    val fillFraction: Float,
    /** True when the task carries an active snooze (shows the 💤 indicator). */
    val isSnoozed: Boolean,
    /** True while the task is paused (frozen countdown + "paused" label). */
    val isPaused: Boolean,
)

internal fun Task.toListItem(now: Instant, tz: TimeZone): TaskListItem {
    // While paused the clock is frozen as of pausedAt, so all time-derived
    // display (elapsed, status, fill, and the formatted label) uses that instant.
    val evalNow = pausedAt ?: now
    val deadline = deadline(tz)
    val cycle = deadline - lastCompletedAt
    val elapsed = elapsedSinceCompleted(now)
    val fraction = if (cycle > Duration.ZERO) {
        (elapsed / cycle).toFloat().coerceIn(0f, 1f)
    } else {
        1f
    }
    return TaskListItem(
        id = id,
        name = name,
        status = status(now, tz),
        elapsed = elapsed,
        frequency = frequency,
        displayText = TimeSinceFormatter.format(lastCompletedAt, evalNow, tz, frequency),
        fillFraction = fraction,
        isSnoozed = snooze > Duration.ZERO,
        isPaused = pausedAt != null,
    )
}

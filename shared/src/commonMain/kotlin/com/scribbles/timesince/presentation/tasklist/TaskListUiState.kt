package com.scribbles.timesince.presentation.tasklist

import com.scribbles.timesince.domain.model.Category
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

/** Which slice of tasks the list is showing. Default is [Active]. */
sealed interface TaskFilter {
    /** All non-archived tasks. */
    data object Active : TaskFilter

    /** Non-archived, currently paused tasks. */
    data object Paused : TaskFilter

    /** Archived tasks. */
    data object Archived : TaskFilter

    /** Non-archived tasks with the given [id] (`null` = uncategorized). */
    data class Category(val id: String?) : TaskFilter

    fun matches(task: Task): Boolean = when (this) {
        Active -> !task.archived
        Paused -> !task.archived && task.pausedAt != null
        Archived -> task.archived
        is Category -> !task.archived && task.categoryId == id
    }
}

data class CategoryChip(
    val id: String,
    val name: String,
    val colorHex: String,
    val icon: String,
)

data class TaskListUiState(
    val isLoading: Boolean = true,
    val tasks: List<TaskListItem> = emptyList(),
    val filter: TaskFilter = TaskFilter.Active,
    val categories: List<CategoryChip> = emptyList(),
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
    /** Hex color of the task's category for the corner badge; `null` = uncategorized. */
    val categoryColorHex: String?,
    /** Emoji of the task's category for the corner badge; `""`/`null` = none. */
    val categoryIcon: String?,
)

/**
 * Maps tasks to list items, attaching each task's category color/icon. Shared by
 * the task-list ViewModel and the Glance home-screen widget.
 */
fun List<Task>.toListItems(now: Instant, tz: TimeZone, categories: List<Category>): List<TaskListItem> {
    val byId = categories.associateBy { it.id }
    return map {
        val category = byId[it.categoryId]
        it.toListItem(now, tz, category?.colorHex, category?.icon)
    }
}

fun Task.toListItem(
    now: Instant,
    tz: TimeZone,
    categoryColorHex: String?,
    categoryIcon: String?,
): TaskListItem {
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
        categoryColorHex = categoryColorHex,
        categoryIcon = categoryIcon,
    )
}

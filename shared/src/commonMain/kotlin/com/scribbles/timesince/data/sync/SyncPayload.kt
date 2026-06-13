package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/**
 * Wire format for syncing tasks to/from Google Drive.
 *
 * v2 adds per-task `updatedAt` (merge-resolution basis) and a `deletedTasks`
 * tombstone list so deletions propagate across devices. v3 adds per-task
 * `snoozeMillis`. Older payloads parse with back-compatible defaults: a missing
 * `updatedAt` falls back to `lastCompletedAt`, a missing `snoozeMillis` to `0`.
 */
@Serializable
data class SyncPayload(
    val version: Int = 3,
    val syncedAt: String,
    val tasks: List<TaskDto> = emptyList(),
    val deletedTasks: List<TombstoneDto> = emptyList(),
) {
    companion object {
        fun from(
            tasks: List<Task>,
            tombstones: List<DeletedTaskTombstone>,
            now: Instant,
        ): SyncPayload = SyncPayload(
            syncedAt = now.toString(),
            tasks = tasks.map { TaskDto.from(it) },
            deletedTasks = tombstones.map { TombstoneDto.from(it) },
        )
    }

    fun toTasks(): List<Task> = tasks.mapNotNull { it.toTask() }
    fun toTombstones(): List<DeletedTaskTombstone> = deletedTasks.mapNotNull { it.toTombstone() }
}

@Serializable
data class TaskDto(
    val id: String,
    val name: String,
    val lastCompletedAt: String,
    val frequencyAmount: Int,
    val frequencyUnit: String,
    val createdAt: String,
    val updatedAt: String? = null,
    val snoozeMillis: Long = 0,
) {
    companion object {
        fun from(task: Task): TaskDto = TaskDto(
            id = task.id,
            name = task.name,
            lastCompletedAt = task.lastCompletedAt.toString(),
            frequencyAmount = task.frequency.amount,
            frequencyUnit = task.frequency.unit.name,
            createdAt = task.createdAt.toString(),
            updatedAt = task.updatedAt.toString(),
            snoozeMillis = task.snooze.inWholeMilliseconds,
        )
    }

    fun toTask(): Task? = try {
        val unit = FrequencyUnit.valueOf(frequencyUnit)
        val lastCompleted = Instant.parse(lastCompletedAt)
        Task(
            id = id,
            name = name,
            lastCompletedAt = lastCompleted,
            frequency = TaskFrequency(frequencyAmount, unit),
            createdAt = Instant.parse(createdAt),
            updatedAt = updatedAt?.let { Instant.parse(it) } ?: lastCompleted,
            snooze = snoozeMillis.milliseconds,
        )
    } catch (_: Exception) {
        null
    }
}

@Serializable
data class TombstoneDto(
    val id: String,
    val deletedAt: String,
) {
    companion object {
        fun from(tombstone: DeletedTaskTombstone): TombstoneDto = TombstoneDto(
            id = tombstone.id,
            deletedAt = tombstone.deletedAt.toString(),
        )
    }

    fun toTombstone(): DeletedTaskTombstone? = try {
        DeletedTaskTombstone(id = id, deletedAt = Instant.parse(deletedAt))
    } catch (_: Exception) {
        null
    }
}

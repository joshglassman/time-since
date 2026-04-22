package com.joshmermelstein.timesince.data.sync

import com.joshmermelstein.timesince.domain.model.FrequencyUnit
import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.model.TaskFrequency
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Wire format for syncing tasks to/from Google Drive.
 * Uses a last-write-wins strategy based on the embedded [syncedAt] timestamp.
 */
@Serializable
data class SyncPayload(
    val version: Int = 1,
    val syncedAt: String,
    val tasks: List<TaskDto>,
) {
    companion object {
        fun from(tasks: List<Task>, now: Instant): SyncPayload = SyncPayload(
            syncedAt = now.toString(),
            tasks = tasks.map { TaskDto.from(it) },
        )
    }

    fun toTasks(): List<Task> = tasks.mapNotNull { it.toTask() }
}

@Serializable
data class TaskDto(
    val id: String,
    val name: String,
    val lastCompletedAt: String,
    val frequencyAmount: Int,
    val frequencyUnit: String,
    val createdAt: String,
) {
    companion object {
        fun from(task: Task): TaskDto = TaskDto(
            id = task.id,
            name = task.name,
            lastCompletedAt = task.lastCompletedAt.toString(),
            frequencyAmount = task.frequency.amount,
            frequencyUnit = task.frequency.unit.name,
            createdAt = task.createdAt.toString(),
        )
    }

    fun toTask(): Task? = try {
        val unit = FrequencyUnit.valueOf(frequencyUnit)
        Task(
            id = id,
            name = name,
            lastCompletedAt = Instant.parse(lastCompletedAt),
            frequency = TaskFrequency(frequencyAmount, unit),
            createdAt = Instant.parse(createdAt),
        )
    } catch (_: Exception) {
        null
    }
}

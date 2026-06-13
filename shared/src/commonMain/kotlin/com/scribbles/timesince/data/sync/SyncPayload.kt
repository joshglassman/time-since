package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
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
 * `snoozeMillis`. v4 adds per-task `pausedAtMillis` and `archived`. v5 adds
 * per-task `categoryId` plus `categories` and `deletedCategories` lists. v6 adds
 * `CategoryDto.icon`. Older payloads parse with back-compatible defaults: missing
 * `updatedAt` falls back to `lastCompletedAt`; `snoozeMillis`→`0`,
 * `pausedAtMillis`→`null`, `archived`→`false`, `categoryId`→`null`, the category
 * lists→empty, and a category `icon`→`""`.
 */
@Serializable
data class SyncPayload(
    val version: Int = 6,
    val syncedAt: String,
    val tasks: List<TaskDto> = emptyList(),
    val deletedTasks: List<TombstoneDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val deletedCategories: List<TombstoneDto> = emptyList(),
) {
    companion object {
        fun from(snapshot: SyncSnapshot, now: Instant): SyncPayload = SyncPayload(
            syncedAt = now.toString(),
            tasks = snapshot.tasks.map { TaskDto.from(it) },
            deletedTasks = snapshot.tombstones.map { TombstoneDto.from(it.id, it.deletedAt) },
            categories = snapshot.categories.map { CategoryDto.from(it) },
            deletedCategories = snapshot.categoryTombstones.map { TombstoneDto.from(it.id, it.deletedAt) },
        )
    }

    fun toTasks(): List<Task> = tasks.mapNotNull { it.toTask() }
    fun toTombstones(): List<DeletedTaskTombstone> =
        deletedTasks.mapNotNull { dto -> dto.parse()?.let { DeletedTaskTombstone(it.first, it.second) } }
    fun toCategories(): List<Category> = categories.mapNotNull { it.toCategory() }
    fun toCategoryTombstones(): List<DeletedCategoryTombstone> =
        deletedCategories.mapNotNull { dto -> dto.parse()?.let { DeletedCategoryTombstone(it.first, it.second) } }
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
    val pausedAtMillis: Long? = null,
    val archived: Boolean = false,
    val categoryId: String? = null,
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
            pausedAtMillis = task.pausedAt?.toEpochMilliseconds(),
            archived = task.archived,
            categoryId = task.categoryId,
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
            pausedAt = pausedAtMillis?.let { Instant.fromEpochMilliseconds(it) },
            archived = archived,
            categoryId = categoryId,
        )
    } catch (_: Exception) {
        null
    }
}

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val updatedAt: String,
    val icon: String = "",
) {
    companion object {
        fun from(category: Category): CategoryDto = CategoryDto(
            id = category.id,
            name = category.name,
            colorHex = category.colorHex,
            updatedAt = category.updatedAt.toString(),
            icon = category.icon,
        )
    }

    fun toCategory(): Category? = try {
        Category(
            id = id,
            name = name,
            colorHex = colorHex,
            updatedAt = Instant.parse(updatedAt),
            icon = icon,
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
        fun from(id: String, deletedAt: Instant): TombstoneDto = TombstoneDto(
            id = id,
            deletedAt = deletedAt.toString(),
        )
    }

    /** Parses to `(id, deletedAt)` or `null` if malformed. */
    fun parse(): Pair<String, Instant>? = try {
        id to Instant.parse(deletedAt)
    } catch (_: Exception) {
        null
    }
}

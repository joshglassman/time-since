package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    name = name,
    lastCompletedAt = Instant.fromEpochMilliseconds(lastCompletedAtEpochMillis),
    frequency = TaskFrequency(
        amount = frequencyAmount,
        unit = FrequencyUnit.valueOf(frequencyUnit),
    ),
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
    updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis),
    snooze = snoozeMillis.milliseconds,
    pausedAt = pausedAtEpochMillis?.let { Instant.fromEpochMilliseconds(it) },
    archived = archived,
    categoryId = categoryId,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    lastCompletedAtEpochMillis = lastCompletedAt.toEpochMilliseconds(),
    frequencyAmount = frequency.amount,
    frequencyUnit = frequency.unit.name,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
    snoozeMillis = snooze.inWholeMilliseconds,
    pausedAtEpochMillis = pausedAt?.toEpochMilliseconds(),
    archived = archived,
    categoryId = categoryId,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    colorHex = colorHex,
    updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis),
    icon = icon,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
    icon = icon,
)

fun DeletedCategoryEntity.toDomain(): DeletedCategoryTombstone = DeletedCategoryTombstone(
    id = id,
    deletedAt = Instant.fromEpochMilliseconds(deletedAtEpochMillis),
)

fun DeletedCategoryTombstone.toEntity(): DeletedCategoryEntity = DeletedCategoryEntity(
    id = id,
    deletedAtEpochMillis = deletedAt.toEpochMilliseconds(),
)

fun DeletedTaskEntity.toDomain(): DeletedTaskTombstone = DeletedTaskTombstone(
    id = id,
    deletedAt = Instant.fromEpochMilliseconds(deletedAtEpochMillis),
)

fun DeletedTaskTombstone.toEntity(): DeletedTaskEntity = DeletedTaskEntity(
    id = id,
    deletedAtEpochMillis = deletedAt.toEpochMilliseconds(),
)

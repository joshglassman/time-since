package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
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
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    lastCompletedAtEpochMillis = lastCompletedAt.toEpochMilliseconds(),
    frequencyAmount = frequency.amount,
    frequencyUnit = frequency.unit.name,
    createdAtEpochMillis = createdAt.toEpochMilliseconds(),
)

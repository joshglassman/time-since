package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.FrequencyUnit
import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.model.TaskFrequency
import kotlin.time.Instant

val BASE_TIME: Instant = Instant.parse("2026-04-07T12:00:00Z")

fun taskWith(
    id: String = "task-1",
    name: String = "Test Task",
    lastCompletedAt: Instant = BASE_TIME,
    frequencyAmount: Int = 7,
    frequencyUnit: FrequencyUnit = FrequencyUnit.DAYS,
    createdAt: Instant = BASE_TIME,
): Task = Task(
    id = id,
    name = name,
    lastCompletedAt = lastCompletedAt,
    frequency = TaskFrequency(frequencyAmount, frequencyUnit),
    createdAt = createdAt,
)

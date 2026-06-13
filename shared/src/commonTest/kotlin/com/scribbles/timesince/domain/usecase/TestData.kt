package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.model.TaskFrequency
import com.scribbles.timesince.domain.time.TimeZoneProvider
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

val BASE_TIME: Instant = Instant.parse("2026-04-07T12:00:00Z")

/** Fixed UTC zone provider so calendar arithmetic stays deterministic in tests. */
val UTC_PROVIDER: TimeZoneProvider = TimeZoneProvider { TimeZone.UTC }

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

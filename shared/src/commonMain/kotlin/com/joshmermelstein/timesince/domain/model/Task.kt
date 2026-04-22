package com.joshmermelstein.timesince.domain.model

import kotlin.time.Instant
import kotlin.time.Clock
import kotlin.time.Duration

data class Task(
    val id: String,
    val name: String,
    val lastCompletedAt: Instant,
    val frequency: TaskFrequency,
    val createdAt: Instant,
)

fun Task.deadline(): Instant = lastCompletedAt + frequency.toDuration()

fun Task.remainingTime(now: Instant = Clock.System.now()): Duration =
    deadline() - now

fun Task.status(now: Instant = Clock.System.now()): TaskStatus {
    val remaining = remainingTime(now)
    val total = frequency.toDuration()
    return when {
        remaining < Duration.ZERO -> TaskStatus.OVERDUE
        remaining < total * 0.1 -> TaskStatus.DUE_SOON
        else -> TaskStatus.OK
    }
}

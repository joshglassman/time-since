package com.scribbles.timesince.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lastCompletedAtEpochMillis: Long,
    val frequencyAmount: Int,
    val frequencyUnit: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val snoozeMillis: Long = 0,
)

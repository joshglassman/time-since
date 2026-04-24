package com.scribbles.timesince.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_tasks")
data class DeletedTaskEntity(
    @PrimaryKey val id: String,
    val deletedAtEpochMillis: Long,
)

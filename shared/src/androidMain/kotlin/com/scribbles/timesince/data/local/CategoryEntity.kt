package com.scribbles.timesince.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val updatedAtEpochMillis: Long,
    val icon: String = "",
)

@Entity(tableName = "deleted_categories")
data class DeletedCategoryEntity(
    @PrimaryKey val id: String,
    val deletedAtEpochMillis: Long,
)

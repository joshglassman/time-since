package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import kotlinx.coroutines.flow.Flow

interface LocalCategoryDataSource {
    fun observeAll(): Flow<List<Category>>
    suspend fun getAll(): List<Category>
    suspend fun getById(id: String): Category?
    suspend fun insert(category: Category)
    suspend fun update(category: Category)
    suspend fun delete(id: String)

    suspend fun insertTombstone(tombstone: DeletedCategoryTombstone)
    suspend fun getTombstones(): List<DeletedCategoryTombstone>
    suspend fun trimTombstones(max: Int)

    /** Bulk-replace local categories with a merged sync snapshot (see [LocalTaskDataSource.applyMerge]). */
    suspend fun applyMerge(categories: List<Category>, tombstones: List<DeletedCategoryTombstone>)
}

package com.scribbles.timesince.domain.repository

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun getAll(): List<Category>
    suspend fun getById(id: String): Category?

    /** Create/update stamp `updatedAt = now` (see the updatedAt invariant). */
    suspend fun create(category: Category)
    suspend fun update(category: Category)

    /** Delete the category and record a tombstone so the deletion syncs. */
    suspend fun delete(id: String)

    suspend fun getTombstones(): List<DeletedCategoryTombstone>

    /** Apply a merged sync result verbatim (no `updatedAt` re-stamping). */
    suspend fun applyMerge(categories: List<Category>, tombstones: List<DeletedCategoryTombstone>)
}

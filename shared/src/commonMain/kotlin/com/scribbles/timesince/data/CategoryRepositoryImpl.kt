package com.scribbles.timesince.data

import com.scribbles.timesince.data.local.LocalCategoryDataSource
import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import com.scribbles.timesince.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class CategoryRepositoryImpl(
    private val localDataSource: LocalCategoryDataSource,
    private val clock: Clock = Clock.System,
) : CategoryRepository {

    override fun observeAll(): Flow<List<Category>> = localDataSource.observeAll()

    override suspend fun getAll(): List<Category> = localDataSource.getAll()

    override suspend fun getById(id: String): Category? = localDataSource.getById(id)

    override suspend fun create(category: Category) =
        localDataSource.insert(category.copy(updatedAt = clock.now()))

    override suspend fun update(category: Category) =
        localDataSource.update(category.copy(updatedAt = clock.now()))

    override suspend fun delete(id: String) {
        val now = clock.now()
        localDataSource.delete(id)
        localDataSource.insertTombstone(DeletedCategoryTombstone(id = id, deletedAt = now))
        localDataSource.trimTombstones(MAX_TOMBSTONES)
    }

    override suspend fun getTombstones(): List<DeletedCategoryTombstone> =
        localDataSource.getTombstones()

    override suspend fun applyMerge(
        categories: List<Category>,
        tombstones: List<DeletedCategoryTombstone>,
    ) {
        localDataSource.applyMerge(categories, tombstones)
    }

    companion object {
        const val MAX_TOMBSTONES = 100
    }
}

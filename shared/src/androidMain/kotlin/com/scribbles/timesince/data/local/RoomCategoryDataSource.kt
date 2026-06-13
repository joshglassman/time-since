package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCategoryDataSource(
    private val categoryDao: CategoryDao,
    private val tombstoneDao: DeletedCategoryDao,
) : LocalCategoryDataSource {

    override fun observeAll(): Flow<List<Category>> =
        categoryDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<Category> =
        categoryDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Category? =
        categoryDao.getById(id)?.toDomain()

    override suspend fun insert(category: Category) =
        categoryDao.insert(category.toEntity())

    override suspend fun update(category: Category) =
        categoryDao.update(category.toEntity())

    override suspend fun delete(id: String) =
        categoryDao.delete(id)

    override suspend fun insertTombstone(tombstone: DeletedCategoryTombstone) =
        tombstoneDao.insert(tombstone.toEntity())

    override suspend fun getTombstones(): List<DeletedCategoryTombstone> =
        tombstoneDao.getAll().map { it.toDomain() }

    override suspend fun trimTombstones(max: Int) =
        tombstoneDao.trim(max)

    override suspend fun applyMerge(
        categories: List<Category>,
        tombstones: List<DeletedCategoryTombstone>,
    ) {
        if (categories.isEmpty()) {
            categoryDao.deleteAll()
        } else {
            categoryDao.deleteAllExcept(categories.map { it.id })
            categoryDao.insertAll(categories.map { it.toEntity() })
        }
        tombstoneDao.clear()
        if (tombstones.isNotEmpty()) {
            tombstoneDao.insertAll(tombstones.map { it.toEntity() })
        }
    }
}

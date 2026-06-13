package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import com.scribbles.timesince.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow<List<Category>>(emptyList())
    private val tombstones = mutableListOf<DeletedCategoryTombstone>()

    override fun observeAll(): Flow<List<Category>> = categories

    override suspend fun getAll(): List<Category> = categories.value

    override suspend fun getById(id: String): Category? = categories.value.find { it.id == id }

    override suspend fun create(category: Category) {
        categories.update { it + category }
    }

    override suspend fun update(category: Category) {
        categories.update { list -> list.map { if (it.id == category.id) category else it } }
    }

    override suspend fun delete(id: String) {
        categories.update { list -> list.filter { it.id != id } }
    }

    override suspend fun getTombstones(): List<DeletedCategoryTombstone> =
        tombstones.sortedByDescending { it.deletedAt }

    override suspend fun applyMerge(
        categories: List<Category>,
        tombstones: List<DeletedCategoryTombstone>,
    ) {
        this.categories.value = categories
        this.tombstones.clear()
        this.tombstones += tombstones
    }
}

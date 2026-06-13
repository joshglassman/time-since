package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetCategoriesUseCase(
    private val repository: CategoryRepository,
) {
    operator fun invoke(): Flow<List<Category>> =
        repository.observeAll().map { categories -> categories.sortedBy { it.name.lowercase() } }
}

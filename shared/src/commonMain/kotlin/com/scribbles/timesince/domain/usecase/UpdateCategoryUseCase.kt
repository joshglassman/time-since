package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.CategoryRepository

/** Renames and/or recolors a category. */
class UpdateCategoryUseCase(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(id: String, name: String, colorHex: String, icon: String = "") {
        val existing = repository.getById(id) ?: return
        repository.update(existing.copy(name = name.trim(), colorHex = colorHex, icon = icon))
    }
}

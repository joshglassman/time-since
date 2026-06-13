package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.repository.CategoryRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CreateCategoryUseCase(
    private val repository: CategoryRepository,
    private val clock: Clock = Clock.System,
) {
    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(name: String, colorHex: String, icon: String = "") {
        val now = clock.now()
        repository.create(
            Category(
                id = Uuid.random().toString(),
                name = name.trim(),
                colorHex = colorHex,
                updatedAt = now,
                icon = icon,
            ),
        )
    }
}

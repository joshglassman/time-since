package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.repository.CategoryRepository
import com.scribbles.timesince.domain.repository.TaskRepository

/**
 * Deletes a category. Tasks are **not** deleted: each task referencing the
 * category has its `categoryId` set to `null` (which re-stamps `updatedAt` so
 * the change propagates), then the category is removed (with a tombstone). The
 * sync merge's dangling-categoryId resolution is the cross-device safety net.
 */
class DeleteCategoryUseCase(
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(categoryId: String) {
        taskRepository.getAll()
            .filter { it.categoryId == categoryId }
            .forEach { taskRepository.update(it.copy(categoryId = null)) }
        categoryRepository.delete(categoryId)
    }
}

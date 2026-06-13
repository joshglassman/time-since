package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.Category
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryUseCasesTest {

    private val categoryRepository = FakeCategoryRepository()
    private val taskRepository = FakeTaskRepository()
    private val clock = TestClock(BASE_TIME)

    private val create = CreateCategoryUseCase(categoryRepository, clock)
    private val update = UpdateCategoryUseCase(categoryRepository)
    private val delete = DeleteCategoryUseCase(categoryRepository, taskRepository)

    @Test
    fun createAddsACategory() = runTest {
        create("Work", "#1e66f5")

        val c = categoryRepository.getAll().single()
        assertEquals("Work", c.name)
        assertEquals("#1e66f5", c.colorHex)
        assertEquals(BASE_TIME, c.updatedAt)
    }

    @Test
    fun updateRenamesAndRecolors() = runTest {
        categoryRepository.create(Category("c1", "Old", "#111111", BASE_TIME))

        update("c1", "New", "#222222")

        val c = categoryRepository.getById("c1")!!
        assertEquals("New", c.name)
        assertEquals("#222222", c.colorHex)
    }

    @Test
    fun deleteRemovesCategoryAndNullsItsTasks() = runTest {
        categoryRepository.create(Category("c1", "Work", "#111111", BASE_TIME))
        taskRepository.create(taskWith(id = "t1").copy(categoryId = "c1"))
        taskRepository.create(taskWith(id = "t2").copy(categoryId = "c1"))
        taskRepository.create(taskWith(id = "other").copy(categoryId = "c2"))

        delete("c1")

        assertNull(categoryRepository.getById("c1"))
        assertNull(taskRepository.getById("t1")?.categoryId)
        assertNull(taskRepository.getById("t2")?.categoryId)
        // Tasks themselves are not deleted.
        assertTrue(taskRepository.getAll().map { it.id }.containsAll(listOf("t1", "t2", "other")))
        // Unrelated category reference untouched.
        assertEquals("c2", taskRepository.getById("other")?.categoryId)
    }
}

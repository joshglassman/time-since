package com.scribbles.timesince.presentation.tasklist

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.usecase.taskWith
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskListItemsTest {

    private val utc = TimeZone.UTC

    @Test
    fun attachesCategoryColorAndIconByCategoryId() {
        val categories = listOf(
            Category(id = "c1", name = "Work", colorHex = "#1e66f5", updatedAt = BASE_TIME, icon = "💼"),
        )
        val tasks = listOf(
            taskWith(id = "a").copy(categoryId = "c1"),
            taskWith(id = "b"), // uncategorized
        )

        val items = tasks.toListItems(BASE_TIME, utc, categories).associateBy { it.id }

        assertEquals("#1e66f5", items["a"]?.categoryColorHex)
        assertEquals("💼", items["a"]?.categoryIcon)
        assertNull(items["b"]?.categoryColorHex)
        assertNull(items["b"]?.categoryIcon)
    }

    @Test
    fun unknownCategoryIdResolvesToNoColor() {
        val items = listOf(taskWith(id = "a").copy(categoryId = "missing"))
            .toListItems(BASE_TIME, utc, emptyList())
        assertNull(items.single().categoryColorHex)
    }
}

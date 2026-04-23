package com.scribbles.timesince.data

import app.cash.turbine.test
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.usecase.taskWith
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TaskRepositoryImplTest {

    private val dataSource = FakeLocalTaskDataSource()
    private val repository = TaskRepositoryImpl(dataSource)

    @Test
    fun createAndObserve() = runTest {
        val task = taskWith(id = "1", name = "Test")

        repository.observeAll().test {
            assertEquals(emptyList(), awaitItem())

            repository.create(task)
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Test", list[0].name)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByIdReturnsTask() = runTest {
        val task = taskWith(id = "1")
        repository.create(task)

        val found = repository.getById("1")
        assertNotNull(found)
        assertEquals("1", found.id)
    }

    @Test
    fun getByIdReturnsNullForMissing() = runTest {
        assertNull(repository.getById("nonexistent"))
    }

    @Test
    fun updateModifiesTask() = runTest {
        val task = taskWith(id = "1", name = "Original")
        repository.create(task)

        repository.update(task.copy(name = "Updated"))
        val updated = repository.getById("1")
        assertEquals("Updated", updated?.name)
    }

    @Test
    fun deleteRemovesTask() = runTest {
        repository.create(taskWith(id = "1"))
        repository.create(taskWith(id = "2"))

        repository.delete("1")

        repository.observeAll().test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals("2", remaining[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

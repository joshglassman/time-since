package com.scribbles.timesince.data

import app.cash.turbine.test
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.usecase.TestClock
import com.scribbles.timesince.domain.usecase.taskWith
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class TaskRepositoryImplTest {

    private val dataSource = FakeLocalTaskDataSource()
    private val clock = TestClock(BASE_TIME)
    private val repository = TaskRepositoryImpl(dataSource, clock)

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

    @Test
    fun createStampsUpdatedAtToNow() = runTest {
        clock.now = BASE_TIME + 2.hours
        repository.create(taskWith(id = "1").copy(updatedAt = BASE_TIME))

        val stored = repository.getById("1")!!
        assertEquals(BASE_TIME + 2.hours, stored.updatedAt)
    }

    @Test
    fun updateStampsUpdatedAtToNow() = runTest {
        repository.create(taskWith(id = "1"))
        clock.now = BASE_TIME + 3.hours

        val existing = repository.getById("1")!!
        repository.update(existing.copy(name = "New"))

        val stored = repository.getById("1")!!
        assertEquals(BASE_TIME + 3.hours, stored.updatedAt)
    }

    @Test
    fun deleteRecordsTombstoneWithCurrentTime() = runTest {
        repository.create(taskWith(id = "1"))
        clock.now = BASE_TIME + 1.hours

        repository.delete("1")

        val tombstones = repository.getTombstones()
        assertEquals(1, tombstones.size)
        assertEquals("1", tombstones[0].id)
        assertEquals(BASE_TIME + 1.hours, tombstones[0].deletedAt)
    }

    @Test
    fun deleteTrimsTombstonesToFifoLimit() = runTest {
        // Create 105 tombstones; only the 100 most recent should survive.
        for (i in 1..105) {
            clock.now = BASE_TIME + i.minutes
            repository.create(taskWith(id = "id-$i"))
            repository.delete("id-$i")
        }

        val tombstones = repository.getTombstones()
        assertEquals(100, tombstones.size)
        // Oldest kept should be id-6 (since id-1..id-5 were evicted).
        assertTrue(tombstones.none { it.id in setOf("id-1", "id-2", "id-3", "id-4", "id-5") })
        assertEquals("id-105", tombstones.first().id)
    }

    @Test
    fun applyMergeReplacesTasksAndTombstonesWithoutRestamping() = runTest {
        repository.create(taskWith(id = "old"))

        val preservedUpdatedAt = BASE_TIME + 9.hours
        val mergedTask = taskWith(id = "new").copy(updatedAt = preservedUpdatedAt)
        val mergedTomb = DeletedTaskTombstone(id = "tomb-1", deletedAt = BASE_TIME + 1.hours)

        clock.now = BASE_TIME + 20.hours
        repository.applyMerge(listOf(mergedTask), listOf(mergedTomb))

        val stored = repository.getById("new")!!
        assertEquals(preservedUpdatedAt, stored.updatedAt)
        assertNull(repository.getById("old"))
        assertEquals(listOf(mergedTomb), repository.getTombstones())
    }
}

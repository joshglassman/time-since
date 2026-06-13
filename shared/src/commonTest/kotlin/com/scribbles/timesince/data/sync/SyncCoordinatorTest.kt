package com.scribbles.timesince.data.sync

import com.scribbles.timesince.data.FakeLocalTaskDataSource
import com.scribbles.timesince.data.TaskRepositoryImpl
import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.usecase.FakeCategoryRepository
import com.scribbles.timesince.domain.usecase.TestClock
import com.scribbles.timesince.domain.usecase.taskWith
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SyncCoordinatorTest {

    private val local = FakeLocalTaskDataSource()
    private val clock = TestClock(BASE_TIME)
    private val repository = TaskRepositoryImpl(local, clock)
    private val categoryRepository = FakeCategoryRepository()
    private val remote = FakeSyncDataSource()
    private val coordinator = SyncCoordinator(
        repository = repository,
        categoryRepository = categoryRepository,
        syncDataSource = remote,
        isSignedIn = { true },
    )

    private fun snapshot(
        tasks: List<com.scribbles.timesince.domain.model.Task> = emptyList(),
        tombstones: List<DeletedTaskTombstone> = emptyList(),
        categories: List<Category> = emptyList(),
        categoryTombstones: List<DeletedCategoryTombstone> = emptyList(),
    ) = SyncSnapshot(tasks, tombstones, categories, categoryTombstones)

    @Test
    fun mergePrefersNewerUpdatedAtPerId() {
        val localA = taskWith(id = "a", name = "Local A").copy(updatedAt = BASE_TIME)
        val remoteA = localA.copy(name = "Remote A", updatedAt = BASE_TIME + 1.hours)
        val localB = taskWith(id = "b", name = "Local B").copy(updatedAt = BASE_TIME + 2.hours)
        val remoteB = localB.copy(name = "Stale Remote B", updatedAt = BASE_TIME)

        val merged = coordinator.merge(
            local = snapshot(tasks = listOf(localA, localB)),
            remote = snapshot(tasks = listOf(remoteA, remoteB)),
        )

        val byId = merged.tasks.associateBy { it.id }
        assertEquals("Remote A", byId["a"]?.name)
        assertEquals("Local B", byId["b"]?.name)
    }

    @Test
    fun tombstoneOverridesStaleRemoteTask() {
        val remoteX = taskWith(id = "x").copy(updatedAt = BASE_TIME)
        val tomb = DeletedTaskTombstone(id = "x", deletedAt = BASE_TIME + 1.days)

        val merged = coordinator.merge(
            local = snapshot(tombstones = listOf(tomb)),
            remote = snapshot(tasks = listOf(remoteX)),
        )

        assertTrue(merged.tasks.isEmpty())
        assertEquals(listOf(tomb), merged.tombstones)
    }

    @Test
    fun editAfterDeletionResurrects() {
        val tomb = DeletedTaskTombstone(id = "x", deletedAt = BASE_TIME)
        val remoteX = taskWith(id = "x").copy(updatedAt = BASE_TIME + 1.hours)

        val merged = coordinator.merge(
            local = snapshot(tombstones = listOf(tomb)),
            remote = snapshot(tasks = listOf(remoteX)),
        )

        assertEquals(listOf(remoteX), merged.tasks)
    }

    @Test
    fun tombstoneListTrimsToMaxAndKeepsMostRecent() {
        val ts = (1..150).map { i -> DeletedTaskTombstone(id = "t-$i", deletedAt = BASE_TIME + i.minutes) }
        val merged = coordinator.merge(local = snapshot(tombstones = ts), remote = snapshot())
        assertEquals(SyncCoordinator.MAX_TOMBSTONES, merged.tombstones.size)
        assertEquals("t-150", merged.tombstones.first().id)
        assertEquals("t-51", merged.tombstones.last().id)
    }

    @Test
    fun categoriesMergeByNewerUpdatedAt() {
        val localC = Category(id = "c", name = "Local", colorHex = "#111111", updatedAt = BASE_TIME)
        val remoteC = localC.copy(name = "Remote", colorHex = "#222222", updatedAt = BASE_TIME + 1.hours)

        val merged = coordinator.merge(
            local = snapshot(categories = listOf(localC)),
            remote = snapshot(categories = listOf(remoteC)),
        )

        assertEquals("Remote", merged.categories.single().name)
    }

    @Test
    fun categoryTombstonePropagatesDeletion() {
        val remoteC = Category(id = "c", name = "Gone", colorHex = "#111111", updatedAt = BASE_TIME)
        val tomb = DeletedCategoryTombstone(id = "c", deletedAt = BASE_TIME + 1.days)

        val merged = coordinator.merge(
            local = snapshot(categoryTombstones = listOf(tomb)),
            remote = snapshot(categories = listOf(remoteC)),
        )

        assertTrue(merged.categories.isEmpty())
        assertEquals(listOf(tomb), merged.categoryTombstones)
    }

    @Test
    fun taskReferencingRemovedCategoryResolvesToNull() {
        // Task references category "c", but "c" is deleted (tombstone) in the merge.
        val task = taskWith(id = "t").copy(updatedAt = BASE_TIME, categoryId = "c")
        val remoteC = Category(id = "c", name = "Gone", colorHex = "#111111", updatedAt = BASE_TIME)
        val tomb = DeletedCategoryTombstone(id = "c", deletedAt = BASE_TIME + 1.days)

        val merged = coordinator.merge(
            local = snapshot(tasks = listOf(task), categoryTombstones = listOf(tomb)),
            remote = snapshot(categories = listOf(remoteC)),
        )

        assertNull(merged.tasks.single().categoryId)
    }

    @Test
    fun taskKeepsCategoryIdWhenCategorySurvives() {
        val task = taskWith(id = "t").copy(updatedAt = BASE_TIME, categoryId = "c")
        val cat = Category(id = "c", name = "Work", colorHex = "#111111", updatedAt = BASE_TIME)

        val merged = coordinator.merge(
            local = snapshot(tasks = listOf(task), categories = listOf(cat)),
            remote = snapshot(),
        )

        assertEquals("c", merged.tasks.single().categoryId)
    }

    @Test
    fun uploadsAfterMergeWhenRemoteWasStale() = runTest {
        local.insert(taskWith(id = "a").copy(updatedAt = BASE_TIME))
        remote.snapshot = SyncSnapshot()

        val result = coordinator.sync()
        assertTrue(result is SyncResult.Success)
        assertEquals(1, remote.lastUploaded.tasks.size)
        assertEquals("a", remote.lastUploaded.tasks.first().id)
    }

    @Test
    fun skipsUploadWhenMergedMatchesRemote() = runTest {
        val t = taskWith(id = "a").copy(updatedAt = BASE_TIME)
        local.insert(t)
        remote.snapshot = SyncSnapshot(tasks = listOf(t))

        val result = coordinator.sync()
        assertTrue(result is SyncResult.Success)
        assertEquals(0, remote.uploadCount)
    }
}

private class FakeSyncDataSource : SyncDataSource {
    var snapshot: SyncSnapshot = SyncSnapshot()
    var uploadCount: Int = 0
    var lastUploaded: SyncSnapshot = SyncSnapshot()

    override suspend fun upload(snapshot: SyncSnapshot): SyncResult {
        uploadCount += 1
        lastUploaded = snapshot
        return SyncResult.Success(snapshot.tasks.size)
    }

    override suspend fun download(): Pair<SyncResult, SyncSnapshot> =
        SyncResult.Success(snapshot.tasks.size) to snapshot
}

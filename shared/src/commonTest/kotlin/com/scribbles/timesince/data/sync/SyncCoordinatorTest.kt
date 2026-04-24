package com.scribbles.timesince.data.sync

import com.scribbles.timesince.data.FakeLocalTaskDataSource
import com.scribbles.timesince.data.TaskRepositoryImpl
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.usecase.BASE_TIME
import com.scribbles.timesince.domain.usecase.TestClock
import com.scribbles.timesince.domain.usecase.taskWith
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SyncCoordinatorTest {

    private val local = FakeLocalTaskDataSource()
    private val clock = TestClock(BASE_TIME)
    private val repository = TaskRepositoryImpl(local, clock)
    private val remote = FakeSyncDataSource()
    private val coordinator = SyncCoordinator(
        repository = repository,
        syncDataSource = remote,
        isSignedIn = { true },
    )

    @Test
    fun mergePrefersNewerUpdatedAtPerId() {
        val localA = taskWith(id = "a", name = "Local A")
            .copy(updatedAt = BASE_TIME)
        val remoteA = localA.copy(name = "Remote A", updatedAt = BASE_TIME + 1.hours)
        val localB = taskWith(id = "b", name = "Local B")
            .copy(updatedAt = BASE_TIME + 2.hours)
        val remoteB = localB.copy(name = "Stale Remote B", updatedAt = BASE_TIME)

        val merged = coordinator.merge(
            local = listOf(localA, localB),
            remote = listOf(remoteA, remoteB),
            localTombstones = emptyList(),
            remoteTombstones = emptyList(),
        )

        val byId = merged.tasks.associateBy { it.id }
        assertEquals("Remote A", byId["a"]?.name)
        assertEquals("Local B", byId["b"]?.name)
    }

    @Test
    fun tombstoneOverridesStaleRemoteTask() {
        // Local has deleted id "x" at T+1d; remote still has a copy with updatedAt T.
        val remoteX = taskWith(id = "x").copy(updatedAt = BASE_TIME)
        val tomb = DeletedTaskTombstone(id = "x", deletedAt = BASE_TIME + 1.days)

        val merged = coordinator.merge(
            local = emptyList(),
            remote = listOf(remoteX),
            localTombstones = listOf(tomb),
            remoteTombstones = emptyList(),
        )

        assertTrue(merged.tasks.isEmpty())
        assertEquals(listOf(tomb), merged.tombstones)
    }

    @Test
    fun editAfterDeletionResurrects() {
        // Remote has a fresh edit that's newer than the tombstone — the user
        // re-created/re-edited the task after deleting it.
        val tomb = DeletedTaskTombstone(id = "x", deletedAt = BASE_TIME)
        val remoteX = taskWith(id = "x").copy(updatedAt = BASE_TIME + 1.hours)

        val merged = coordinator.merge(
            local = emptyList(),
            remote = listOf(remoteX),
            localTombstones = listOf(tomb),
            remoteTombstones = emptyList(),
        )

        assertEquals(listOf(remoteX), merged.tasks)
    }

    @Test
    fun tombstoneListTrimsToMaxAndKeepsMostRecent() {
        val ts = (1..150).map { i ->
            DeletedTaskTombstone(id = "t-$i", deletedAt = BASE_TIME + i.minutes)
        }
        val merged = coordinator.merge(
            local = emptyList(),
            remote = emptyList(),
            localTombstones = ts,
            remoteTombstones = emptyList(),
        )
        assertEquals(SyncCoordinator.MAX_TOMBSTONES, merged.tombstones.size)
        // Most recent preserved: t-150 down to t-51
        assertEquals("t-150", merged.tombstones.first().id)
        assertEquals("t-51", merged.tombstones.last().id)
    }

    @Test
    fun uploadsAfterMergeWhenRemoteWasStale() = runTest {
        // Local has an extra task the remote doesn't; sync should upload.
        local.insert(taskWith(id = "a").copy(updatedAt = BASE_TIME))
        remote.snapshot = SyncSnapshot()

        val result = coordinator.sync()
        assertTrue(result is SyncResult.Success)
        assertEquals(1, remote.lastUploadedTasks.size)
        assertEquals("a", remote.lastUploadedTasks.first().id)
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
    var lastUploadedTasks: List<com.scribbles.timesince.domain.model.Task> = emptyList()
    var lastUploadedTombstones: List<DeletedTaskTombstone> = emptyList()

    override suspend fun upload(
        tasks: List<com.scribbles.timesince.domain.model.Task>,
        tombstones: List<DeletedTaskTombstone>,
    ): SyncResult {
        uploadCount += 1
        lastUploadedTasks = tasks
        lastUploadedTombstones = tombstones
        return SyncResult.Success(tasks.size)
    }

    override suspend fun download(): Pair<SyncResult, SyncSnapshot> =
        SyncResult.Success(snapshot.tasks.size) to snapshot
}

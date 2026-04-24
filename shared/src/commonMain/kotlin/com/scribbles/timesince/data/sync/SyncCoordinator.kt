package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates two-way sync with a [SyncDataSource] using `updatedAt`-wins
 * per task UUID and a FIFO list of the most recent [MAX_TOMBSTONES] deletion
 * tombstones so deleted tasks don't get resurrected by the remote.
 *
 * Algorithm (see [sync]):
 *   1. Download remote snapshot.
 *   2. Load local tasks + tombstones.
 *   3. Merge tombstones (max deletedAt per id), trim to [MAX_TOMBSTONES] most recent.
 *   4. For each task in local ∪ remote, keep the one with greater `updatedAt`.
 *   5. Drop any task whose id has a tombstone with `deletedAt >= task.updatedAt`
 *      (intentional deletion beats a stale remote copy; a local edit after the
 *      tombstone's timestamp wins).
 *   6. Apply merged state locally via `repository.applyMerge` (does NOT re-stamp
 *      `updatedAt`).
 *   7. If the merged state differs from the remote snapshot, upload it.
 *
 * [requestSync] is debounced and single-flight: rapid calls coalesce into one run.
 */
class SyncCoordinator(
    private val repository: TaskRepository,
    private val syncDataSource: SyncDataSource,
    private val isSignedIn: suspend () -> Boolean,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MS,
) {
    private val mutex = Mutex()
    private var pendingJob: Job? = null
    private val ownScope: CoroutineScope = scope

    private val _results = MutableSharedFlow<SyncResult>(extraBufferCapacity = 8)
    val results: SharedFlow<SyncResult> = _results

    /**
     * Schedule a sync after [debounceMillis] of quiet. If another request arrives,
     * the previous timer is cancelled and replaced. No-op when not signed in.
     */
    fun requestSync() {
        pendingJob?.cancel()
        pendingJob = ownScope.launch {
            delay(debounceMillis)
            val result = sync()
            _results.emit(result)
        }
    }

    /** Perform a sync immediately. Safe to call from lifecycle hooks. */
    suspend fun sync(): SyncResult = mutex.withLock {
        if (!isSignedIn()) return@withLock SyncResult.Error("Not signed in.")

        val (downloadResult, remote) = syncDataSource.download()
        if (downloadResult is SyncResult.Error) return@withLock downloadResult

        val localTasks = repository.getAll()
        val localTombstones = repository.getTombstones()

        val merged = merge(
            local = localTasks,
            remote = remote.tasks,
            localTombstones = localTombstones,
            remoteTombstones = remote.tombstones,
        )

        repository.applyMerge(merged.tasks, merged.tombstones)

        val remoteDiffers = remote.tasks.toSet() != merged.tasks.toSet() ||
            remote.tombstones.toSet() != merged.tombstones.toSet()
        if (remoteDiffers) {
            return@withLock syncDataSource.upload(merged.tasks, merged.tombstones)
        }
        SyncResult.Success(merged.tasks.size)
    }

    internal data class Merged(
        val tasks: List<Task>,
        val tombstones: List<DeletedTaskTombstone>,
    )

    internal fun merge(
        local: List<Task>,
        remote: List<Task>,
        localTombstones: List<DeletedTaskTombstone>,
        remoteTombstones: List<DeletedTaskTombstone>,
    ): Merged {
        val tombstoneMap = mutableMapOf<String, DeletedTaskTombstone>()
        for (t in localTombstones + remoteTombstones) {
            val existing = tombstoneMap[t.id]
            if (existing == null || t.deletedAt > existing.deletedAt) {
                tombstoneMap[t.id] = t
            }
        }

        val taskMap = mutableMapOf<String, Task>()
        for (t in local + remote) {
            val existing = taskMap[t.id]
            if (existing == null || t.updatedAt > existing.updatedAt) {
                taskMap[t.id] = t
            }
        }

        // Drop tasks overridden by a same-or-newer tombstone.
        val iter = taskMap.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val tomb = tombstoneMap[entry.key] ?: continue
            if (tomb.deletedAt >= entry.value.updatedAt) iter.remove()
        }

        val keptTombstones = tombstoneMap.values
            .sortedByDescending { it.deletedAt }
            .take(MAX_TOMBSTONES)

        return Merged(
            tasks = taskMap.values.toList(),
            tombstones = keptTombstones,
        )
    }

    companion object {
        const val MAX_TOMBSTONES = 100
        private const val DEFAULT_DEBOUNCE_MS = 500L
    }
}

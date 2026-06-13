package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.repository.CategoryRepository
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
import kotlin.time.Instant

/**
 * Orchestrates two-way sync with a [SyncDataSource]. Tasks **and** categories
 * merge by `updatedAt`-wins per id, each with its own FIFO list of the most
 * recent [MAX_TOMBSTONES] deletion tombstones so deleted entities aren't
 * resurrected by the remote. After merging, a task whose `categoryId` points at
 * a category that did not survive the merge is resolved to `null`
 * (uncategorized).
 *
 * Algorithm (see [sync]): download remote → load local → merge tombstones (max
 * deletedAt per id, trimmed) → keep the greater-`updatedAt` entity per id → drop
 * entities overridden by a same-or-newer tombstone → resolve dangling
 * `categoryId` → apply locally (no `updatedAt` re-stamp) → upload if changed.
 *
 * [requestSync] is debounced and single-flight: rapid calls coalesce into one run.
 */
class SyncCoordinator(
    private val repository: TaskRepository,
    private val categoryRepository: CategoryRepository,
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

        val local = SyncSnapshot(
            tasks = repository.getAll(),
            tombstones = repository.getTombstones(),
            categories = categoryRepository.getAll(),
            categoryTombstones = categoryRepository.getTombstones(),
        )

        val merged = merge(local, remote)

        repository.applyMerge(merged.tasks, merged.tombstones)
        categoryRepository.applyMerge(merged.categories, merged.categoryTombstones)

        val remoteDiffers = remote.tasks.toSet() != merged.tasks.toSet() ||
            remote.tombstones.toSet() != merged.tombstones.toSet() ||
            remote.categories.toSet() != merged.categories.toSet() ||
            remote.categoryTombstones.toSet() != merged.categoryTombstones.toSet()
        if (remoteDiffers) {
            return@withLock syncDataSource.upload(merged)
        }
        SyncResult.Success(merged.tasks.size)
    }

    internal fun merge(local: SyncSnapshot, remote: SyncSnapshot): SyncSnapshot {
        val taskTombs = mergeTombstones(local.tombstones.map { it.id to it.deletedAt } + remote.tombstones.map { it.id to it.deletedAt })
        val catTombs = mergeTombstones(local.categoryTombstones.map { it.id to it.deletedAt } + remote.categoryTombstones.map { it.id to it.deletedAt })

        val tasks = mergeEntities(local.tasks, remote.tasks, { it.id }, { it.updatedAt }, taskTombs)
        val categories = mergeEntities(local.categories, remote.categories, { it.id }, { it.updatedAt }, catTombs)

        // Resolve dangling category references to null (uncategorized).
        val surviving = categories.mapTo(HashSet()) { it.id }
        val resolvedTasks = tasks.map {
            if (it.categoryId != null && it.categoryId !in surviving) it.copy(categoryId = null) else it
        }

        return SyncSnapshot(
            tasks = resolvedTasks,
            tombstones = trim(taskTombs).map { DeletedTaskTombstone(it.first, it.second) },
            categories = categories,
            categoryTombstones = trim(catTombs).map { DeletedCategoryTombstone(it.first, it.second) },
        )
    }

    private fun mergeTombstones(pairs: List<Pair<String, Instant>>): Map<String, Instant> {
        val map = mutableMapOf<String, Instant>()
        for ((id, deletedAt) in pairs) {
            val existing = map[id]
            if (existing == null || deletedAt > existing) map[id] = deletedAt
        }
        return map
    }

    private fun <T> mergeEntities(
        local: List<T>,
        remote: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> Instant,
        tombstones: Map<String, Instant>,
    ): List<T> {
        val map = mutableMapOf<String, T>()
        for (e in local + remote) {
            val existing = map[id(e)]
            if (existing == null || updatedAt(e) > updatedAt(existing)) map[id(e)] = e
        }
        // Drop entities overridden by a same-or-newer tombstone.
        val iter = map.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            val deletedAt = tombstones[entry.key] ?: continue
            if (deletedAt >= updatedAt(entry.value)) iter.remove()
        }
        return map.values.toList()
    }

    private fun trim(tombstones: Map<String, Instant>): List<Pair<String, Instant>> =
        tombstones.entries.sortedByDescending { it.value }.take(MAX_TOMBSTONES).map { it.key to it.value }

    companion object {
        const val MAX_TOMBSTONES = 100
        private const val DEFAULT_DEBOUNCE_MS = 500L
    }
}

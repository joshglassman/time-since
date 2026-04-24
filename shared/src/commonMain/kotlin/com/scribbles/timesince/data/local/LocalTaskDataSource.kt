package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface LocalTaskDataSource {
    fun observeAll(): Flow<List<Task>>
    suspend fun getAll(): List<Task>
    suspend fun getById(id: String): Task?
    suspend fun insert(task: Task)
    suspend fun update(task: Task)
    suspend fun delete(id: String)

    suspend fun insertTombstone(tombstone: DeletedTaskTombstone)
    suspend fun getTombstones(): List<DeletedTaskTombstone>
    suspend fun trimTombstones(max: Int)

    /**
     * Bulk-replace local state with the merged result of a sync cycle.
     *
     * Upserts each task with its `updatedAt` preserved (no re-stamping), hard-deletes
     * local tasks whose ids are absent from [tasks], and replaces the tombstone table
     * with [tombstones] verbatim. Callers are responsible for supplying an already-merged,
     * already-trimmed snapshot.
     */
    suspend fun applyMerge(tasks: List<Task>, tombstones: List<DeletedTaskTombstone>)
}

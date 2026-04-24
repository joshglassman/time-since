package com.scribbles.timesince.domain.repository

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAll(): Flow<List<Task>>
    suspend fun getAll(): List<Task>
    suspend fun getById(id: String): Task?
    suspend fun create(task: Task)
    suspend fun update(task: Task)
    suspend fun delete(id: String)

    suspend fun getTombstones(): List<DeletedTaskTombstone>

    /**
     * Apply the merged result of a sync cycle. Preserves `updatedAt` on tasks
     * (no re-stamping) and replaces the tombstone table with [tombstones].
     */
    suspend fun applyMerge(tasks: List<Task>, tombstones: List<DeletedTaskTombstone>)
}

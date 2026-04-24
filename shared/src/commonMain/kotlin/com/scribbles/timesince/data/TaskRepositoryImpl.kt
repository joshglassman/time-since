package com.scribbles.timesince.data

import com.scribbles.timesince.data.local.LocalTaskDataSource
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

class TaskRepositoryImpl(
    private val localDataSource: LocalTaskDataSource,
    private val clock: Clock = Clock.System,
) : TaskRepository {

    override fun observeAll(): Flow<List<Task>> =
        localDataSource.observeAll()

    override suspend fun getAll(): List<Task> =
        localDataSource.getAll()

    override suspend fun getById(id: String): Task? =
        localDataSource.getById(id)

    override suspend fun create(task: Task) =
        localDataSource.insert(task.copy(updatedAt = clock.now()))

    override suspend fun update(task: Task) =
        localDataSource.update(task.copy(updatedAt = clock.now()))

    override suspend fun delete(id: String) {
        val now = clock.now()
        localDataSource.delete(id)
        localDataSource.insertTombstone(DeletedTaskTombstone(id = id, deletedAt = now))
        localDataSource.trimTombstones(MAX_TOMBSTONES)
    }

    override suspend fun getTombstones(): List<DeletedTaskTombstone> =
        localDataSource.getTombstones()

    override suspend fun applyMerge(
        tasks: List<Task>,
        tombstones: List<DeletedTaskTombstone>,
    ) {
        localDataSource.applyMerge(tasks, tombstones)
    }

    companion object {
        const val MAX_TOMBSTONES = 100
    }
}

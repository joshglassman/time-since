package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskDataSource(
    private val taskDao: TaskDao,
    private val tombstoneDao: DeletedTaskDao,
) : LocalTaskDataSource {

    override fun observeAll(): Flow<List<Task>> =
        taskDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<Task> =
        taskDao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Task? =
        taskDao.getById(id)?.toDomain()

    override suspend fun insert(task: Task) =
        taskDao.insert(task.toEntity())

    override suspend fun update(task: Task) =
        taskDao.update(task.toEntity())

    override suspend fun delete(id: String) =
        taskDao.delete(id)

    override suspend fun insertTombstone(tombstone: DeletedTaskTombstone) =
        tombstoneDao.insert(tombstone.toEntity())

    override suspend fun getTombstones(): List<DeletedTaskTombstone> =
        tombstoneDao.getAll().map { it.toDomain() }

    override suspend fun trimTombstones(max: Int) =
        tombstoneDao.trim(max)

    override suspend fun applyMerge(
        tasks: List<Task>,
        tombstones: List<DeletedTaskTombstone>,
    ) {
        if (tasks.isEmpty()) {
            taskDao.deleteAll()
        } else {
            taskDao.deleteAllExcept(tasks.map { it.id })
            taskDao.insertAll(tasks.map { it.toEntity() })
        }
        tombstoneDao.clear()
        if (tombstones.isNotEmpty()) {
            tombstoneDao.insertAll(tombstones.map { it.toEntity() })
        }
    }
}

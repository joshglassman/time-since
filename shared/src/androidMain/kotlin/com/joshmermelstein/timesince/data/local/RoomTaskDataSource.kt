package com.joshmermelstein.timesince.data.local

import com.joshmermelstein.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTaskDataSource(
    private val dao: TaskDao,
) : LocalTaskDataSource {

    override fun observeAll(): Flow<List<Task>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<Task> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Task? =
        dao.getById(id)?.toDomain()

    override suspend fun insert(task: Task) =
        dao.insert(task.toEntity())

    override suspend fun update(task: Task) =
        dao.update(task.toEntity())

    override suspend fun delete(id: String) =
        dao.delete(id)
}

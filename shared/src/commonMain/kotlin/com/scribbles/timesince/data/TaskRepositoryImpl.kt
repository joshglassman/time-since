package com.scribbles.timesince.data

import com.scribbles.timesince.data.local.LocalTaskDataSource
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class TaskRepositoryImpl(
    private val localDataSource: LocalTaskDataSource,
) : TaskRepository {

    override fun observeAll(): Flow<List<Task>> =
        localDataSource.observeAll()

    override suspend fun getById(id: String): Task? =
        localDataSource.getById(id)

    override suspend fun create(task: Task) =
        localDataSource.insert(task)

    override suspend fun update(task: Task) =
        localDataSource.update(task)

    override suspend fun delete(id: String) =
        localDataSource.delete(id)
}

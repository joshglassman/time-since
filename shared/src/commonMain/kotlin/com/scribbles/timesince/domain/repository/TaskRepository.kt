package com.scribbles.timesince.domain.repository

import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAll(): Flow<List<Task>>
    suspend fun getById(id: String): Task?
    suspend fun create(task: Task)
    suspend fun update(task: Task)
    suspend fun delete(id: String)
}

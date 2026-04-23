package com.scribbles.timesince.data.local

import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface LocalTaskDataSource {
    fun observeAll(): Flow<List<Task>>
    suspend fun getAll(): List<Task>
    suspend fun getById(id: String): Task?
    suspend fun insert(task: Task)
    suspend fun update(task: Task)
    suspend fun delete(id: String)
}

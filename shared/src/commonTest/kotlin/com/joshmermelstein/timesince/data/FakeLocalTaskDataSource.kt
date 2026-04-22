package com.joshmermelstein.timesince.data

import com.joshmermelstein.timesince.data.local.LocalTaskDataSource
import com.joshmermelstein.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeLocalTaskDataSource : LocalTaskDataSource {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    override fun observeAll(): Flow<List<Task>> = tasks

    override suspend fun getAll(): List<Task> = tasks.value

    override suspend fun getById(id: String): Task? =
        tasks.value.find { it.id == id }

    override suspend fun insert(task: Task) {
        tasks.update { it + task }
    }

    override suspend fun update(task: Task) {
        tasks.update { list ->
            list.map { if (it.id == task.id) task else it }
        }
    }

    override suspend fun delete(id: String) {
        tasks.update { list -> list.filter { it.id != id } }
    }
}

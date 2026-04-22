package com.joshmermelstein.timesince.domain.usecase

import com.joshmermelstein.timesince.domain.model.Task
import com.joshmermelstein.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    override fun observeAll(): Flow<List<Task>> = tasks

    override suspend fun getById(id: String): Task? =
        tasks.value.find { it.id == id }

    override suspend fun create(task: Task) {
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

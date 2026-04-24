package com.scribbles.timesince.domain.usecase

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeTaskRepository : TaskRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val tombstones = mutableListOf<DeletedTaskTombstone>()

    override fun observeAll(): Flow<List<Task>> = tasks

    override suspend fun getAll(): List<Task> = tasks.value

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

    override suspend fun getTombstones(): List<DeletedTaskTombstone> =
        tombstones.sortedByDescending { it.deletedAt }

    override suspend fun applyMerge(
        tasks: List<Task>,
        tombstones: List<DeletedTaskTombstone>,
    ) {
        this.tasks.value = tasks
        this.tombstones.clear()
        this.tombstones += tombstones
    }
}

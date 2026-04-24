package com.scribbles.timesince.data

import com.scribbles.timesince.data.local.LocalTaskDataSource
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeLocalTaskDataSource : LocalTaskDataSource {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())
    private val tombstones = mutableListOf<DeletedTaskTombstone>()

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

    override suspend fun insertTombstone(tombstone: DeletedTaskTombstone) {
        tombstones.removeAll { it.id == tombstone.id }
        tombstones += tombstone
    }

    override suspend fun getTombstones(): List<DeletedTaskTombstone> =
        tombstones.sortedByDescending { it.deletedAt }

    override suspend fun trimTombstones(max: Int) {
        if (tombstones.size <= max) return
        val keep = tombstones.sortedByDescending { it.deletedAt }.take(max)
        tombstones.clear()
        tombstones += keep
    }

    override suspend fun applyMerge(
        tasks: List<Task>,
        tombstones: List<DeletedTaskTombstone>,
    ) {
        this.tasks.value = tasks
        this.tombstones.clear()
        this.tombstones += tombstones
    }
}

package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task

data class SyncSnapshot(
    val tasks: List<Task> = emptyList(),
    val tombstones: List<DeletedTaskTombstone> = emptyList(),
)

interface SyncDataSource {
    suspend fun upload(tasks: List<Task>, tombstones: List<DeletedTaskTombstone>): SyncResult
    suspend fun download(): Pair<SyncResult, SyncSnapshot>
}

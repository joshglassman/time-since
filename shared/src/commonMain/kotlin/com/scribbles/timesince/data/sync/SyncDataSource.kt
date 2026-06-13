package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.domain.model.DeletedCategoryTombstone
import com.scribbles.timesince.domain.model.DeletedTaskTombstone
import com.scribbles.timesince.domain.model.Task

data class SyncSnapshot(
    val tasks: List<Task> = emptyList(),
    val tombstones: List<DeletedTaskTombstone> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryTombstones: List<DeletedCategoryTombstone> = emptyList(),
)

interface SyncDataSource {
    suspend fun upload(snapshot: SyncSnapshot): SyncResult
    suspend fun download(): Pair<SyncResult, SyncSnapshot>
}

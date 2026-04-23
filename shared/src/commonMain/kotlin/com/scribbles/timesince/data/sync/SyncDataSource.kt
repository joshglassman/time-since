package com.scribbles.timesince.data.sync

import com.scribbles.timesince.domain.model.Task

interface SyncDataSource {
    suspend fun upload(tasks: List<Task>): SyncResult
    suspend fun download(): Pair<SyncResult, List<Task>>
}

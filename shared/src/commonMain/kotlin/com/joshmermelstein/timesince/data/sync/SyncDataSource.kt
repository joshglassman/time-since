package com.joshmermelstein.timesince.data.sync

import com.joshmermelstein.timesince.domain.model.Task

interface SyncDataSource {
    suspend fun upload(tasks: List<Task>): SyncResult
    suspend fun download(): Pair<SyncResult, List<Task>>
}

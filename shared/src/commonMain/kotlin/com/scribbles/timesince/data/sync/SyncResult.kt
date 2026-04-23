package com.scribbles.timesince.data.sync

sealed class SyncResult {
    data class Success(val taskCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

package com.scribbles.timesince.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.export.MarkdownExporter
import com.scribbles.timesince.data.export.MarkdownImporter
import com.scribbles.timesince.data.sync.SyncDataSource
import com.scribbles.timesince.data.sync.SyncResult
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TaskRepository,
    private val syncDataSource: SyncDataSource?,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    fun clearMessage() {
        _message.value = null
    }

    fun exportMarkdown(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = repository.observeAll().first()
            val markdown = MarkdownExporter.export(tasks)
            onResult(markdown)
        }
    }

    fun importMarkdown(markdown: String) {
        viewModelScope.launch {
            val tasks = MarkdownImporter.import(markdown)
            if (tasks.isEmpty()) {
                _message.value = "No valid tasks found in file."
                return@launch
            }
            var imported = 0
            for (task in tasks) {
                val existing = repository.getById(task.id)
                if (existing != null) {
                    repository.update(task)
                } else {
                    repository.create(task)
                }
                imported++
            }
            _message.value = "Imported $imported task${if (imported != 1) "s" else ""}."
        }
    }

    fun syncUpload() {
        val source = syncDataSource ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val tasks = repository.observeAll().first()
            when (val result = source.upload(tasks)) {
                is SyncResult.Success -> _message.value = "Uploaded ${result.taskCount} task${if (result.taskCount != 1) "s" else ""} to Drive."
                is SyncResult.Error -> _message.value = "Sync failed: ${result.message}"
            }
            _isSyncing.value = false
        }
    }

    fun syncDownload() {
        val source = syncDataSource ?: return
        viewModelScope.launch {
            _isSyncing.value = true
            val (result, tasks) = source.download()
            when (result) {
                is SyncResult.Success -> {
                    for (task in tasks) {
                        val existing = repository.getById(task.id)
                        if (existing != null) {
                            repository.update(task)
                        } else {
                            repository.create(task)
                        }
                    }
                    _message.value = "Downloaded ${tasks.size} task${if (tasks.size != 1) "s" else ""} from Drive."
                }
                is SyncResult.Error -> _message.value = "Sync failed: ${result.message}"
            }
            _isSyncing.value = false
        }
    }
}

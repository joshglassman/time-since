package com.scribbles.timesince.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scribbles.timesince.data.export.MarkdownExporter
import com.scribbles.timesince.data.export.MarkdownImporter
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: TaskRepository,
    private val syncCoordinator: SyncCoordinator? = null,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun clearMessage() {
        _message.value = null
    }

    fun exportMarkdown(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val tasks = repository.getAll()
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
            syncCoordinator?.requestSync()
            _message.value = "Imported $imported task${if (imported != 1) "s" else ""}."
        }
    }
}

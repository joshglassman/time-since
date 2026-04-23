package com.scribbles.timesince.ui.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.presentation.format.RemainingTimeFormatter
import com.scribbles.timesince.presentation.tasklist.TaskListItem
import com.scribbles.timesince.presentation.tasklist.TaskListUiState
import com.scribbles.timesince.presentation.tasklist.TaskListViewModel
import com.scribbles.timesince.ui.theme.statusDueSoon
import com.scribbles.timesince.ui.theme.statusOk
import com.scribbles.timesince.ui.theme.statusOverdue
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onSettings: () -> Unit,
    viewModel: TaskListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var taskToDelete by remember { mutableStateOf<TaskListItem?>(null) }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete task?") },
            text = { Text("\"${task.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onTaskDeleted(task.id)
                    taskToDelete = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Since") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
    ) { padding ->
        TaskListContent(
            state = state,
            onCompleteTask = viewModel::onTaskCompleted,
            onEditTask = onEditTask,
            onDeleteTask = { task -> taskToDelete = task },
            contentPadding = padding,
        )
    }
}

@Composable
private fun TaskListContent(
    state: TaskListUiState,
    onCompleteTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    onDeleteTask: (TaskListItem) -> Unit,
    contentPadding: PaddingValues,
) {
    when {
        state.isLoading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        state.tasks.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "\u23F0",
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "No tasks yet",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap + to add your first task",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onComplete = { onCompleteTask(task.id) },
                    onEdit = { onEditTask(task.id) },
                    onDelete = { onDeleteTask(task) },
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: TaskListItem,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusLabel = when (task.status) {
        TaskStatus.OK -> "on track"
        TaskStatus.DUE_SOON -> "due soon"
        TaskStatus.OVERDUE -> "overdue"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${task.name}, ${RemainingTimeFormatter.format(task.remaining)}, $statusLabel. Tap to mark complete."
            },
        onClick = onComplete,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(task.status)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = RemainingTimeFormatter.format(task.remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorForStatus(task.status),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit ${task.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete ${task.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusDot(status: TaskStatus) {
    val label = when (status) {
        TaskStatus.OK -> "On track"
        TaskStatus.DUE_SOON -> "Due soon"
        TaskStatus.OVERDUE -> "Overdue"
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(colorForStatus(status))
            .semantics { contentDescription = label },
    )
}

private fun colorForStatus(status: TaskStatus): Color = when (status) {
    TaskStatus.OK -> statusOk
    TaskStatus.DUE_SOON -> statusDueSoon
    TaskStatus.OVERDUE -> statusOverdue
}

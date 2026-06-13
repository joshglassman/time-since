package com.scribbles.timesince.ui.tasklist

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scribbles.timesince.domain.model.TaskStatus
import com.scribbles.timesince.ui.components.CategoryCornerBadge
import com.scribbles.timesince.ui.components.parseHexColor
import com.scribbles.timesince.presentation.tasklist.CategoryChip
import com.scribbles.timesince.presentation.tasklist.TaskFilter
import com.scribbles.timesince.presentation.tasklist.TaskListItem
import com.scribbles.timesince.presentation.tasklist.TaskListUiState
import com.scribbles.timesince.presentation.tasklist.TaskListViewModel
import com.scribbles.timesince.ui.theme.statusDueSoon
import com.scribbles.timesince.ui.theme.statusOk
import com.scribbles.timesince.ui.theme.statusOverdue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    onAddTask: () -> Unit,
    onEditTask: (String) -> Unit,
    onSettings: () -> Unit,
    scrollToTaskId: String? = null,
    onScrollConsumed: () -> Unit = {},
    viewModel: TaskListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // In a filtered view, back returns to the active tasks view; in the active
    // view it's disabled so the default back behavior (exit the app) applies.
    BackHandler(enabled = state.filter != TaskFilter.Active) {
        viewModel.onFilterSelected(TaskFilter.Active)
    }

    // Widget deep link: switch to the active view, then scroll to the task.
    LaunchedEffect(scrollToTaskId) {
        if (scrollToTaskId != null) viewModel.onFilterSelected(TaskFilter.Active)
    }
    LaunchedEffect(scrollToTaskId, state.tasks) {
        val target = scrollToTaskId ?: return@LaunchedEffect
        val index = state.tasks.indexOfFirst { it.id == target }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            onScrollConsumed()
        }
    }

    var flashTick by remember { mutableStateOf(0) }
    var flashTaskId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.completedTaskEvents.collect { id ->
            flashTaskId = id
            flashTick += 1
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Task completed",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onUndoComplete(id)
                }
            }
        }
    }

    var filterMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(filterTitle(state.filter, state.categories)) },
                actions = {
                    Box {
                        IconButton(onClick = { filterMenuOpen = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                        FilterMenu(
                            expanded = filterMenuOpen,
                            current = state.filter,
                            categories = state.categories,
                            onDismiss = { filterMenuOpen = false },
                            onFilterSelected = {
                                viewModel.onFilterSelected(it)
                                filterMenuOpen = false
                            },
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            listState = listState,
            flashTaskId = flashTaskId,
            flashTick = flashTick,
            contentPadding = padding,
        )
    }
}

@Composable
private fun TaskListContent(
    state: TaskListUiState,
    onCompleteTask: (String) -> Unit,
    onEditTask: (String) -> Unit,
    listState: LazyListState,
    flashTaskId: String?,
    flashTick: Int,
    contentPadding: PaddingValues,
) {
    when {
        state.isLoading -> Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        state.tasks.isEmpty() -> Box(
            modifier = Modifier.fillMaxSize().padding(contentPadding),
        ) {
            EmptyState(state.filter)
        }

        else -> LazyColumn(
            state = listState,
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
                    flashTick = if (flashTaskId == task.id) flashTick else 0,
                )
            }
        }
    }
}

@Composable
private fun FilterMenu(
    expanded: Boolean,
    current: TaskFilter,
    categories: List<CategoryChip>,
    onDismiss: () -> Unit,
    onFilterSelected: (TaskFilter) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        FilterMenuItem("Active", null, null, current == TaskFilter.Active) {
            onFilterSelected(TaskFilter.Active)
        }
        categories.forEach { category ->
            FilterMenuItem(
                label = category.name,
                icon = category.icon,
                background = parseHexColor(category.colorHex),
                selected = current == TaskFilter.Category(category.id),
            ) { onFilterSelected(TaskFilter.Category(category.id)) }
        }
        FilterMenuItem("Paused", null, null, current == TaskFilter.Paused) {
            onFilterSelected(TaskFilter.Paused)
        }
        FilterMenuItem("Archived", null, null, current == TaskFilter.Archived) {
            onFilterSelected(TaskFilter.Archived)
        }
    }
}

@Composable
private fun FilterMenuItem(
    label: String,
    icon: String?,
    background: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent

    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = icon?.takeIf { it.isNotEmpty() }?.let {
            {
                // Only the icon chip carries the category color.
                val chipColor = background ?: MaterialTheme.colorScheme.surfaceVariant
                val onChip = if (chipColor.luminance() > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(chipColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(it, color = onChip)
                }
            }
        },
        modifier = Modifier
            .background(rowBackground)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            ),
    )
}

private fun filterTitle(filter: TaskFilter, categories: List<CategoryChip>): String = when (filter) {
    TaskFilter.Active -> "Time Since"
    TaskFilter.Paused -> "Paused"
    TaskFilter.Archived -> "Archived"
    is TaskFilter.Category -> categories.firstOrNull { it.id == filter.id }
        ?.let { "${it.icon} ${it.name}".trim() } ?: "Category"
}

@Composable
private fun EmptyState(filter: TaskFilter) {
    val (emoji, title, body) = when (filter) {
        TaskFilter.Archived -> Triple("🗄", "No archived tasks", "Tasks you archive will appear here.")
        TaskFilter.Paused -> Triple("⏸", "No paused tasks", "Paused tasks will appear here.")
        is TaskFilter.Category -> Triple("🗂", "No tasks in this category", "Assign a category from a task's edit screen.")
        TaskFilter.Active -> Triple("⏰", "No tasks yet", "Long-press a task to mark it complete.")
    }
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = emoji, style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: TaskListItem,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    flashTick: Int,
) {
    val statusLabel = when (task.status) {
        TaskStatus.OK -> "on track"
        TaskStatus.DUE_SOON -> "due soon"
        TaskStatus.OVERDUE -> "overdue"
    }
    val displayText = task.displayText
    val fraction = task.fillFraction

    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(flashTick) {
        if (flashTick > 0) {
            showCheck = true
            delay(700)
            showCheck = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                val snoozeNote = if (task.isSnoozed) " Snoozed." else ""
                val pauseNote = if (task.isPaused) " Paused." else ""
                contentDescription = "${task.name}, $displayText, $statusLabel.$snoozeNote$pauseNote " +
                    "Tap to edit. Long-press to mark complete."
            }
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onComplete,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(task.status, gradientColorAt(fraction))
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorForStatus(task.status),
                            )
                            if (task.isSnoozed) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "💤",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (task.isPaused) {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "⏸",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                TimeSinceBar(
                    fraction = fraction,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                )
            }

            // Category corner badge in the top-right corner.
            task.categoryColorHex?.let { hex ->
                CategoryCornerBadge(
                    colorHex = hex,
                    icon = task.categoryIcon.orEmpty(),
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }

            CheckFlashOverlay(
                visible = showCheck,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun CheckFlashOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
        exit = scaleOut(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = statusOk,
            modifier = Modifier.size(56.dp),
        )
    }
}

@Composable
private fun StatusDot(status: TaskStatus, color: Color) {
    val label = when (status) {
        TaskStatus.OK -> "On track"
        TaskStatus.DUE_SOON -> "Due soon"
        TaskStatus.OVERDUE -> "Overdue"
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
            .semantics { contentDescription = label },
    )
}

private fun colorForStatus(status: TaskStatus): Color = when (status) {
    TaskStatus.OK -> statusOk
    TaskStatus.DUE_SOON -> statusDueSoon
    TaskStatus.OVERDUE -> statusOverdue
}

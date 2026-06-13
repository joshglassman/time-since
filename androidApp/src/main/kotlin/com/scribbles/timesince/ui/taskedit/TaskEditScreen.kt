package com.scribbles.timesince.ui.taskedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scribbles.timesince.domain.model.FrequencyUnit
import com.scribbles.timesince.presentation.taskedit.TaskEditViewModel
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(
    taskId: String?,
    onBack: () -> Unit,
    viewModel: TaskEditViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(taskId) {
        viewModel.load(taskId)
    }

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "New task" else "Edit task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        TaskEditForm(
            isNew = state.isNew,
            name = state.name,
            frequencyAmount = state.frequencyAmount,
            frequencyUnit = state.frequencyUnit,
            lastCompletedAt = state.lastCompletedAt,
            snoozeAmount = state.snoozeAmount,
            snoozeUnit = state.snoozeUnit,
            canSave = state.canSave,
            canSnooze = state.canSnooze,
            canUndo = state.canUndo,
            isPaused = state.isPaused,
            isArchived = state.isArchived,
            onNameChanged = viewModel::onNameChanged,
            onFrequencyAmountChanged = viewModel::onFrequencyAmountChanged,
            onFrequencyUnitChanged = viewModel::onFrequencyUnitChanged,
            onLastCompletedAtChanged = viewModel::onLastCompletedAtChanged,
            onSnoozeAmountChanged = viewModel::onSnoozeAmountChanged,
            onSnoozeUnitChanged = viewModel::onSnoozeUnitChanged,
            onSnooze = viewModel::onSnooze,
            onUndo = viewModel::onUndo,
            onTogglePause = viewModel::onTogglePause,
            onToggleArchive = viewModel::onToggleArchive,
            onSave = viewModel::onSave,
            contentPadding = padding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditForm(
    isNew: Boolean,
    name: String,
    frequencyAmount: String,
    frequencyUnit: FrequencyUnit,
    lastCompletedAt: Instant?,
    snoozeAmount: String,
    snoozeUnit: FrequencyUnit,
    canSave: Boolean,
    canSnooze: Boolean,
    canUndo: Boolean,
    isPaused: Boolean,
    isArchived: Boolean,
    onNameChanged: (String) -> Unit,
    onFrequencyAmountChanged: (String) -> Unit,
    onFrequencyUnitChanged: (FrequencyUnit) -> Unit,
    onLastCompletedAtChanged: (Instant) -> Unit,
    onSnoozeAmountChanged: (String) -> Unit,
    onSnoozeUnitChanged: (FrequencyUnit) -> Unit,
    onSnooze: () -> Unit,
    onUndo: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleArchive: () -> Unit,
    onSave: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Task name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Repeat every", style = MaterialTheme.typography.labelLarge)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = frequencyAmount,
                onValueChange = onFrequencyAmountChanged,
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier.weight(1f),
            )
            FrequencyUnitDropdown(
                selected = frequencyUnit,
                onSelected = onFrequencyUnitChanged,
                modifier = Modifier.weight(1f),
            )
        }

        if (!isNew && lastCompletedAt != null) {
            LastCompletedRow(
                value = lastCompletedAt,
                onChanged = onLastCompletedAtChanged,
            )
        }

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }

        if (!isNew) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Snooze until later", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = snoozeAmount,
                    onValueChange = onSnoozeAmountChanged,
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.weight(1f),
                )
                FrequencyUnitDropdown(
                    selected = snoozeUnit,
                    onSelected = onSnoozeUnitChanged,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                onClick = onSnooze,
                enabled = canSnooze,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Snooze")
            }
            OutlinedButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Undo last action")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Pause is unavailable on archived tasks (archive overrides pause).
            OutlinedButton(
                onClick = onTogglePause,
                enabled = !isArchived,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isPaused) "Resume" else "Pause")
            }
            OutlinedButton(
                onClick = onToggleArchive,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isArchived) "Unarchive" else "Archive")
            }
        }
    }
}

@Composable
private fun LastCompletedRow(
    value: Instant,
    onChanged: (Instant) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }

    Text("Last completed", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatLastCompleted(value),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(onClick = { editing = true }) {
            Text("Change…")
        }
    }
    if (editing) {
        DateTimePickerDialog(
            initial = value,
            onDismiss = { editing = false },
            onConfirmed = { instant ->
                editing = false
                onChanged(instant)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    initial: Instant,
    onDismiss: () -> Unit,
    onConfirmed: (Instant) -> Unit,
) {
    val initialJavaInstant = java.time.Instant.ofEpochMilli(initial.toEpochMilliseconds())
    val initialLocal: LocalDateTime = LocalDateTime.ofInstant(initialJavaInstant, ZoneId.systemDefault())
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.toEpochMilliseconds(),
    )

    if (pickedDate == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        pickedDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                    },
                ) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        val timePickerState = rememberTimePickerState(
            initialHour = initialLocal.hour,
            initialMinute = initialLocal.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick a time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val date = pickedDate!!
                    val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    val javaInstant = LocalDateTime.of(date, time)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                    onConfirmed(Instant.fromEpochMilliseconds(javaInstant.toEpochMilli()))
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            },
        )
    }
}

private fun formatLastCompleted(instant: Instant): String {
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds()))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyUnitDropdown(
    selected: FrequencyUnit,
    onSelected: (FrequencyUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor(
                    androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true,
                )
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FrequencyUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.label) },
                    onClick = {
                        onSelected(unit)
                        expanded = false
                    },
                )
            }
        }
    }
}

private val FrequencyUnit.label: String
    get() = when (this) {
        FrequencyUnit.HOURS -> "Hours"
        FrequencyUnit.DAYS -> "Days"
        FrequencyUnit.WEEKS -> "Weeks"
        FrequencyUnit.MONTHS -> "Months"
        FrequencyUnit.YEARS -> "Years"
    }

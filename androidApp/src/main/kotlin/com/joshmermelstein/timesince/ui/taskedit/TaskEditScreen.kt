package com.joshmermelstein.timesince.ui.taskedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joshmermelstein.timesince.domain.model.FrequencyUnit
import com.joshmermelstein.timesince.presentation.taskedit.TaskEditViewModel
import org.koin.androidx.compose.koinViewModel

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
            name = state.name,
            frequencyAmount = state.frequencyAmount,
            frequencyUnit = state.frequencyUnit,
            canSave = state.canSave,
            onNameChanged = viewModel::onNameChanged,
            onFrequencyAmountChanged = viewModel::onFrequencyAmountChanged,
            onFrequencyUnitChanged = viewModel::onFrequencyUnitChanged,
            onSave = viewModel::onSave,
            contentPadding = padding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditForm(
    name: String,
    frequencyAmount: String,
    frequencyUnit: FrequencyUnit,
    canSave: Boolean,
    onNameChanged: (String) -> Unit,
    onFrequencyAmountChanged: (String) -> Unit,
    onFrequencyUnitChanged: (FrequencyUnit) -> Unit,
    onSave: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
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

        Text("Repeat every", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)

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

        Button(
            onClick = onSave,
            enabled = canSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
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
        TextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
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
    }

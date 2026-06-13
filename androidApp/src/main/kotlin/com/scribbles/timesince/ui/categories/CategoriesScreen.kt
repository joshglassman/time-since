package com.scribbles.timesince.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.Surface
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.emoji2.emojipicker.EmojiPickerView
import com.scribbles.timesince.domain.model.CATEGORY_PALETTE
import com.scribbles.timesince.domain.model.Category
import com.scribbles.timesince.presentation.categories.CategoriesViewModel
import com.scribbles.timesince.ui.components.parseHexColor
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = koinViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        },
    ) { padding ->
        if (categories.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), Alignment.Center) {
                Text(
                    "No categories yet. Tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(categories, key = { it.id }) { category ->
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(category.colorHex)),
                            )
                        },
                        headlineContent = { Text("${category.icon} ${category.name}".trim()) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { editing = category }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit ${category.name}")
                                }
                                IconButton(onClick = { viewModel.onDelete(category.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete ${category.name}")
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    if (creating) {
        CategoryEditDialog(
            initial = null,
            onDismiss = { creating = false },
            onConfirm = { name, color, icon ->
                viewModel.onCreate(name, color, icon)
                creating = false
            },
        )
    }
    editing?.let { category ->
        CategoryEditDialog(
            initial = category,
            onDismiss = { editing = null },
            onConfirm = { name, color, icon ->
                viewModel.onUpdate(category.id, name, color, icon)
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String, icon: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.colorHex ?: CATEGORY_PALETTE.first()) }
    var icon by remember { mutableStateOf(initial?.icon ?: "") }
    var pickingEmoji by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New category" else "Edit category") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (icon.isEmpty()) "No icon" else icon,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { pickingEmoji = true }) { Text("Choose icon") }
                    if (icon.isNotEmpty()) {
                        TextButton(onClick = { icon = "" }) { Text("Clear") }
                    }
                }
                Spacer(Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATEGORY_PALETTE.forEach { swatch ->
                        val selected = swatch == color
                        Box(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(swatch))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { color = swatch },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, color, icon) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickingEmoji) {
        EmojiPickerDialog(
            onDismiss = { pickingEmoji = false },
            onPicked = {
                icon = it
                pickingEmoji = false
            },
        )
    }
}

@Composable
private fun EmojiPickerDialog(
    onDismiss: () -> Unit,
    onPicked: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            AndroidView(
                factory = { context ->
                    EmojiPickerView(context).apply {
                        setOnEmojiPickedListener { item -> onPicked(item.emoji) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
            )
        }
    }
}

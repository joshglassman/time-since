package com.joshmermelstein.timesince.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joshmermelstein.timesince.presentation.settings.SettingsViewModel
import com.joshmermelstein.timesince.sync.GoogleAuthHelper
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    authHelper: GoogleAuthHelper = koinInject(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var isSignedIn by remember { mutableStateOf(authHelper.isSignedIn) }
    var accountEmail by remember { mutableStateOf(authHelper.accountEmail) }

    LaunchedEffect(message) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.exportMarkdown { markdown ->
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(markdown.toByteArray())
            }
            Toast.makeText(context, "Tasks exported.", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val markdown = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().readText()
        } ?: return@rememberLauncherForActivityResult
        viewModel.importMarkdown(markdown)
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        scope.launch {
            val account = authHelper.handleSignInResult(result.data)
            isSignedIn = account != null
            accountEmail = account?.email
            if (account != null) {
                Toast.makeText(context, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Sign-in failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // --- Markdown Export/Import ---
            Text(
                text = "Data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { exportLauncher.launch("time-since-tasks.md") },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export tasks to Markdown")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { importLauncher.launch(arrayOf("text/markdown", "text/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import tasks from Markdown")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            // --- Google Drive Sync ---
            Text(
                text = "Google Drive Sync",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            if (isSignedIn) {
                Text(
                    text = "Signed in as $accountEmail",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.syncUpload() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing,
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Upload to Drive")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.syncDownload() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSyncing,
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Download from Drive")
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            authHelper.signOut()
                            isSignedIn = false
                            accountEmail = null
                            Toast.makeText(context, "Signed out.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign out")
                }
            } else {
                Text(
                    text = "Sign in with Google to back up your tasks to Drive.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { signInLauncher.launch(authHelper.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}

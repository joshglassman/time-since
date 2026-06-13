package com.scribbles.timesince

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.scribbles.timesince.ui.navigation.AppNavGraph
import com.scribbles.timesince.ui.theme.TimeSinceTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result handled implicitly — worker will simply skip posting if denied */ }

    /** Task id to scroll the list to, delivered by the home-screen widget. */
    private var scrollToTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        scrollToTaskId = intent?.getStringExtra(EXTRA_TASK_ID)
        setContent {
            TimeSinceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        scrollToTaskId = scrollToTaskId,
                        onScrollConsumed = { scrollToTaskId = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_TASK_ID)?.let { scrollToTaskId = it }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(permission)
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "com.scribbles.timesince.extra.TASK_ID"
    }
}

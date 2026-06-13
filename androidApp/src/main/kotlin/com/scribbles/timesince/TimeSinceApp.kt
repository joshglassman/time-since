package com.scribbles.timesince

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.scribbles.timesince.data.sync.SyncCoordinator
import com.scribbles.timesince.data.sync.SyncResult
import com.scribbles.timesince.di.appModule
import com.scribbles.timesince.di.sharedModule
import com.scribbles.timesince.notification.NotificationHelper
import com.scribbles.timesince.notification.OverdueCheckWorker
import com.scribbles.timesince.sync.GoogleAuthHelper
import androidx.glance.appwidget.updateAll
import com.scribbles.timesince.widget.TimeSinceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class TimeSinceApp : Application() {

    private val notificationHelper: NotificationHelper by inject()
    private val syncCoordinator: SyncCoordinator by inject()
    private val authHelper: GoogleAuthHelper by inject()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@TimeSinceApp)
            modules(sharedModule, appModule)
        }
        notificationHelper.ensureChannel()
        OverdueCheckWorker.enqueue(this)

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                refreshWidget()
                if (!authHelper.isSignedIn) return
                appScope.launch {
                    val result = syncCoordinator.sync()
                    if (result is SyncResult.Error) {
                        Toast.makeText(
                            this@TimeSinceApp,
                            "Sync failed: ${result.message}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }

            // Refresh the home-screen widget when the app goes to the background.
            override fun onStop(owner: LifecycleOwner) = refreshWidget()
        })

        appScope.launch {
            syncCoordinator.results.collect { result ->
                if (result is SyncResult.Error) {
                    Toast.makeText(
                        this@TimeSinceApp,
                        "Sync failed: ${result.message}",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    private fun refreshWidget() {
        appScope.launch { TimeSinceWidget().updateAll(this@TimeSinceApp) }
    }
}

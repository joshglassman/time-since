package com.scribbles.timesince

import android.app.Application
import com.scribbles.timesince.di.appModule
import com.scribbles.timesince.di.sharedModule
import com.scribbles.timesince.notification.NotificationHelper
import com.scribbles.timesince.notification.OverdueCheckWorker
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class TimeSinceApp : Application() {

    private val notificationHelper: NotificationHelper by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@TimeSinceApp)
            modules(sharedModule, appModule)
        }
        notificationHelper.ensureChannel()
        OverdueCheckWorker.enqueue(this)
    }
}

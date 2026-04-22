package com.joshmermelstein.timesince.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.joshmermelstein.timesince.domain.usecase.GetOverdueTasksUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class OverdueCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params), KoinComponent {

    private val getOverdueTasks: GetOverdueTasksUseCase by inject()
    private val notificationHelper: NotificationHelper by inject()

    override suspend fun doWork(): Result {
        return try {
            val overdue = getOverdueTasks()
            if (overdue.isEmpty()) {
                notificationHelper.clearOverdue()
            } else {
                notificationHelper.showOverdue(overdue)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "overdue_check_periodic"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<OverdueCheckWorker>(
                15, TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}

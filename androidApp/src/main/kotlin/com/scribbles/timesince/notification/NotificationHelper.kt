package com.scribbles.timesince.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.scribbles.timesince.MainActivity
import com.scribbles.timesince.R
import com.scribbles.timesince.domain.model.Task

class NotificationHelper(private val context: Context) {

    fun ensureChannel() {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_overdue_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_overdue_description)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun showOverdue(tasks: List<Task>) {
        ensureChannel()
        val manager = NotificationManagerCompat.from(context)
        if (tasks.isEmpty()) {
            manager.cancel(NOTIFICATION_ID)
            return
        }

        val title = if (tasks.size == 1) {
            context.getString(R.string.notification_overdue_title_one)
        } else {
            context.getString(R.string.notification_overdue_title_many, tasks.size)
        }

        val visibleNames = tasks.take(MAX_LINES).joinToString(separator = "\n") { "• ${it.name}" }
        val expandedText = if (tasks.size > MAX_LINES) {
            "$visibleNames\n+${tasks.size - MAX_LINES} more"
        } else {
            visibleNames
        }
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(expandedText)

        val contentText = tasks.joinToString(separator = ", ") { it.name }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(bigTextStyle)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try {
                manager.notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Permission revoked between check and post — ignore.
            }
        }
    }

    fun clearOverdue() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "overdue_tasks"
        const val NOTIFICATION_ID = 1001
        private const val MAX_LINES = 5
    }
}

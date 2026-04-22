package com.joshmermelstein.timesince.data.local

import android.content.Context
import androidx.room.Room

object TaskDatabaseFactory {
    fun create(context: Context): TaskDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            TaskDatabase::class.java,
            "time-since.db",
        ).build()
}

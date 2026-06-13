package com.scribbles.timesince.data.local

import android.content.Context
import androidx.room.Room

object TaskDatabaseFactory {
    fun create(context: Context): TaskDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            TaskDatabase::class.java,
            "time-since.db",
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
}

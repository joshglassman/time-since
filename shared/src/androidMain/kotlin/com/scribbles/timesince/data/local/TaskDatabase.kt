package com.scribbles.timesince.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        DeletedTaskEntity::class,
        CategoryEntity::class,
        DeletedCategoryEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun deletedTaskDao(): DeletedTaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun deletedCategoryDao(): DeletedCategoryDao
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "UPDATE tasks SET updatedAtEpochMillis = lastCompletedAtEpochMillis",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deleted_tasks (
                id TEXT NOT NULL PRIMARY KEY,
                deletedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE tasks ADD COLUMN snoozeMillis INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN pausedAtEpochMillis INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tasks ADD COLUMN categoryId TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                updatedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS deleted_categories (
                id TEXT NOT NULL PRIMARY KEY,
                deletedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE categories ADD COLUMN icon TEXT NOT NULL DEFAULT ''")
    }
}

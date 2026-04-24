package com.scribbles.timesince.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeletedTaskDao {
    @Query("SELECT * FROM deleted_tasks ORDER BY deletedAtEpochMillis DESC")
    suspend fun getAll(): List<DeletedTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeletedTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DeletedTaskEntity>)

    @Query("DELETE FROM deleted_tasks")
    suspend fun clear()

    /**
     * Keeps the [max] most recent tombstones (by deletedAt) and discards the rest.
     */
    @Query(
        """
        DELETE FROM deleted_tasks
        WHERE id NOT IN (
            SELECT id FROM deleted_tasks
            ORDER BY deletedAtEpochMillis DESC
            LIMIT :max
        )
        """,
    )
    suspend fun trim(max: Int)
}

package com.scribbles.timesince.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Update
    suspend fun update(entity: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM categories WHERE id NOT IN (:idsToKeep)")
    suspend fun deleteAllExcept(idsToKeep: List<String>)

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}

@Dao
interface DeletedCategoryDao {
    @Query("SELECT * FROM deleted_categories ORDER BY deletedAtEpochMillis DESC")
    suspend fun getAll(): List<DeletedCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DeletedCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DeletedCategoryEntity>)

    @Query("DELETE FROM deleted_categories")
    suspend fun clear()

    @Query(
        """
        DELETE FROM deleted_categories
        WHERE id NOT IN (
            SELECT id FROM deleted_categories
            ORDER BY deletedAtEpochMillis DESC
            LIMIT :max
        )
        """,
    )
    suspend fun trim(max: Int)
}

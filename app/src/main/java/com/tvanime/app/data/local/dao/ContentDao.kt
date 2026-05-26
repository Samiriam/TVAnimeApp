package com.tvanime.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tvanime.app.data.local.entity.ContentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {

    @Query("SELECT * FROM content ORDER BY syncedAt DESC")
    fun observeAll(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContentEntity?

    @Query("SELECT * FROM content WHERE mediaType = :type ORDER BY title")
    fun getByType(type: String): Flow<List<ContentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ContentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ContentEntity)

    @Delete
    suspend fun delete(item: ContentEntity)

    @Query("DELETE FROM content")
    suspend fun clearAll()

    @Query("SELECT EXISTS(SELECT 1 FROM content WHERE id = :id)")
    fun exists(id: String): Flow<Boolean>
}

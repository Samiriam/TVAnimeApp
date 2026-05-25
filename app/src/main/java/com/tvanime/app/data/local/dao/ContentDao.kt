package com.tvanime.app.data.local.dao

import androidx.room.*
import com.tvanime.app.data.local.entity.ContentEntity

@Query("SELECT * FROM content ORDER BY syncedAt DESC")
fun observeAll(): Flow<List<ContentEntity>>

@Query("SELECT * FROM content WHERE id = :id LIMIT 1")
fun getById(id: String): Flow<ContentEntity>

@Query("SELECT * FROM content WHERE mediaType = :type ORDER BY title")
fun getByType(type: String): Flow<List<ContentEntity>>

@Insert(onConflict = OnConflictStrategy.REPLACE)
fun insertAll(items: List<ContentEntity>)

@Insert(onConflict = OnConflictStrategy.REPLACE)
fun insert(item: ContentEntity)

@Delete
fun delete(item: ContentEntity)

@Query("DELETE FROM content")
fun clearAll()

@Query("SELECT EXISTS(SELECT 1 FROM content WHERE id = :id)")
fun exists(id: String): Flow<Boolean>

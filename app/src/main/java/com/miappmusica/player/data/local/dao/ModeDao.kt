package com.miappmusica.player.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.miappmusica.player.data.local.entity.ModeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {

    @Query("SELECT * FROM modes ORDER BY sortOrder ASC")
    fun observeModes(): Flow<List<ModeEntity>>

    @Query("SELECT COUNT(*) FROM modes")
    suspend fun count(): Int

    @Query("SELECT * FROM modes WHERE id = :id")
    suspend fun getById(id: String): ModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mode: ModeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(modes: List<ModeEntity>)

    @Query("DELETE FROM modes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM modes")
    suspend fun nextSortOrder(): Int
}

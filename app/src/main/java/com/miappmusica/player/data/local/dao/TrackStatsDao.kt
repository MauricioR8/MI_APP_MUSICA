package com.miappmusica.player.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackStatsDao {
    @Query("INSERT OR IGNORE INTO track_stats(trackId, favorite, playCount, lastPlayedAt) VALUES(:id, 0, 0, 0)")
    suspend fun ensure(id: Long)

    @Query("UPDATE track_stats SET playCount = playCount + 1, lastPlayedAt = :t WHERE trackId = :id")
    suspend fun incrementPlay(id: Long, t: Long)

    @Query("UPDATE track_stats SET favorite = :fav WHERE trackId = :id")
    suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("SELECT favorite FROM track_stats WHERE trackId = :id")
    fun observeFavorite(id: Long): Flow<Boolean?>

    @Query("SELECT trackId FROM track_stats WHERE favorite = 1")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Query("SELECT trackId FROM track_stats WHERE playCount > 0 ORDER BY playCount DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int): Flow<List<Long>>

    @Query("SELECT trackId FROM track_stats WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int): Flow<List<Long>>
}

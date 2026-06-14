package com.miappmusica.player.data.repository

import com.miappmusica.player.data.local.dao.TrackStatsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Favorites + play statistics backed by the track_stats table. */
@Singleton
class StatsRepository @Inject constructor(private val dao: TrackStatsDao) {

    fun observeFavorite(id: Long): Flow<Boolean> = dao.observeFavorite(id).map { it == true }

    fun observeFavoriteIds(): Flow<List<Long>> = dao.observeFavoriteIds()

    fun observeMostPlayed(limit: Int = 100): Flow<List<Long>> = dao.observeMostPlayed(limit)

    fun observeRecentlyPlayed(limit: Int = 100): Flow<List<Long>> = dao.observeRecentlyPlayed(limit)

    suspend fun recordPlay(id: Long) {
        dao.ensure(id)
        dao.incrementPlay(id, System.currentTimeMillis())
    }

    suspend fun toggleFavorite(id: Long, makeFavorite: Boolean) {
        dao.ensure(id)
        dao.setFavorite(id, makeFavorite)
    }
}

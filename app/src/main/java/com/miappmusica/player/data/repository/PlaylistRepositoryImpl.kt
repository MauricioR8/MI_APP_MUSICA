package com.miappmusica.player.data.repository

import com.miappmusica.player.data.local.dao.PlaylistDao
import com.miappmusica.player.data.local.entity.PlaylistEntity
import com.miappmusica.player.data.local.entity.PlaylistTrackCrossRef
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val dao: PlaylistDao
) : PlaylistRepository {

    override fun observePlaylists(): Flow<List<Playlist>> =
        combine(dao.observePlaylists(), dao.observeAllCrossRefs()) { playlists, refs ->
            val byPlaylist: Map<Long, List<PlaylistTrackCrossRef>> = refs.groupBy { it.playlistId }
            playlists.map { entity ->
                val trackIds = byPlaylist[entity.id]
                    ?.sortedBy { it.position }
                    ?.map { it.trackId }
                    .orEmpty()
                entity.toDomain(trackIds)
            }
        }

    override suspend fun create(name: String, description: String): Long =
        dao.insert(PlaylistEntity(name = name, description = description, sortOrder = dao.nextSortOrder()))

    override suspend fun rename(id: Long, name: String) = dao.rename(id, name)

    override suspend fun setCover(id: Long, coverUri: String?) = dao.setCover(id, coverUri)

    override suspend fun delete(id: Long) = dao.deletePlaylist(id)

    override suspend fun setTracks(playlistId: Long, trackIds: List<Long>) =
        dao.replaceTracks(playlistId, trackIds)

    override suspend fun addTrack(playlistId: Long, trackId: Long) {
        val position = dao.nextPosition(playlistId)
        dao.insertCrossRefs(listOf(PlaylistTrackCrossRef(playlistId, trackId, position)))
    }

    override suspend fun removeTrack(playlistId: Long, trackId: Long) =
        dao.removeTrack(playlistId, trackId)

    override suspend fun getPlaylist(id: Long): Playlist? {
        val entity = dao.getPlaylist(id) ?: return null
        return entity.toDomain(dao.getTrackIds(id))
    }

    override suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> dao.setSortOrder(id, index) }
    }
}

private fun PlaylistEntity.toDomain(trackIds: List<Long>): Playlist = Playlist(
    id = id,
    name = name,
    description = description,
    trackIds = trackIds,
    coverUri = coverUri,
    isSystem = isSystem
)

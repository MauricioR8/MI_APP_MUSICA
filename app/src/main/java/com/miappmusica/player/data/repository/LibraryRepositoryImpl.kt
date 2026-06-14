package com.miappmusica.player.data.repository

import com.miappmusica.player.data.mediastore.MediaStoreSource
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val mediaStore: MediaStoreSource
) : LibraryRepository {

    private val tracksFlow = MutableStateFlow<List<Track>>(emptyList())

    override fun observeTracks(): Flow<List<Track>> = tracksFlow.asStateFlow()

    override fun observeDownloads(): Flow<List<Track>> =
        tracksFlow.map { list -> list.filter { it.isDownloaded } }

    override suspend fun refresh() {
        tracksFlow.value = mediaStore.queryTracks()
    }

    override suspend fun getTrack(id: Long): Track? {
        if (tracksFlow.value.isEmpty()) refresh()
        return tracksFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun getTracks(ids: List<Long>): List<Track> {
        if (tracksFlow.value.isEmpty()) refresh()
        val byId = tracksFlow.value.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    override fun buildDeleteRequest(uris: List<String>): android.content.IntentSender? =
        mediaStore.buildDeleteRequest(uris.map { android.net.Uri.parse(it) })

    override suspend fun deleteDirect(uris: List<String>): Int {
        val deleted = mediaStore.deleteDirect(uris.map { android.net.Uri.parse(it) })
        refresh()
        return deleted
    }
}

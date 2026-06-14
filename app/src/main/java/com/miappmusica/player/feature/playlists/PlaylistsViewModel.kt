package com.miappmusica.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.domain.repository.TransferRepository
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val recentlyAdded: List<Track> = emptyList(),
    val downloads: List<Track> = emptyList(),
    val totalTracks: Int = 0,
    /** playlistId -> cover image (manual cover, else first track artwork). */
    val covers: Map<Long, String?> = emptyMap()
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val transferRepository: TransferRepository,
    private val libraryRepository: LibraryRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    val state: StateFlow<PlaylistsUiState> = combine(
        playlistRepository.observePlaylists(),
        libraryRepository.observeTracks()
    ) { playlists, tracks ->
        val artworkByTrack = tracks.associate { it.id to it.artworkUri }
        val covers = playlists.associate { p ->
            p.id to (p.coverUri ?: p.trackIds.firstNotNullOfOrNull { artworkByTrack[it] })
        }
        PlaylistsUiState(
            playlists = playlists,
            recentlyAdded = tracks.sortedByDescending { it.dateAdded }.take(50),
            downloads = tracks.filter { it.isDownloaded },
            totalTracks = tracks.size,
            covers = covers
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistsUiState())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.create(name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    fun rename(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.rename(id, name) }
    }

    /** Sets a manual cover image (gallery content:// URI) for a playlist/album. */
    fun setCover(id: Long, coverUri: String) {
        viewModelScope.launch { playlistRepository.setCover(id, coverUri) }
    }

    fun playTracks(tracks: List<Track>) {
        if (tracks.isNotEmpty()) playbackConnection.playTracks(tracks)
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val tracks = libraryRepository.getTracks(playlist.trackIds)
            if (tracks.isNotEmpty()) playbackConnection.playTracks(tracks)
        }
    }

    fun export(treeUri: String) {
        viewModelScope.launch {
            _message.value = transferRepository.export(treeUri).fold(
                onSuccess = { "Exportadas $it listas" },
                onFailure = { "Error al exportar: ${it.message}" }
            )
        }
    }

    fun import(sourceUri: String) {
        viewModelScope.launch {
            _message.value = transferRepository.import(sourceUri).fold(
                onSuccess = { "Importadas $it pistas" },
                onFailure = { "Error al importar: ${it.message}" }
            )
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

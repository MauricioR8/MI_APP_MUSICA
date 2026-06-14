package com.miappmusica.player.feature.playlists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    /** Tracks belonging to the playlist, in playlist order. */
    val tracks: List<Track> = emptyList(),
    /** Library tracks NOT already in the playlist (for the "add songs" picker). */
    val availableTracks: List<Track> = emptyList()
) {
    val title: String get() = playlist?.name ?: ""
    val cover: String? get() = playlist?.coverUri ?: tracks.firstOrNull()?.artworkUri
    val isEmpty: Boolean get() = tracks.isEmpty()
}

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val playbackConnection: PlaybackConnection,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: 0L

    val state: StateFlow<PlaylistDetailUiState> = combine(
        playlistRepository.observePlaylists(),
        libraryRepository.observeTracks()
    ) { playlists, tracks ->
        val playlist = playlists.firstOrNull { it.id == playlistId }
        if (playlist == null) {
            PlaylistDetailUiState()
        } else {
            val byId = tracks.associateBy { it.id }
            val ordered = playlist.trackIds.mapNotNull { byId[it] }
            val inPlaylist = playlist.trackIds.toSet()
            val available = tracks.filter { it.id !in inPlaylist }
            PlaylistDetailUiState(
                playlist = playlist,
                tracks = ordered,
                availableTracks = available
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaylistDetailUiState())

    init {
        playbackConnection.connect()
        viewModelScope.launch { libraryRepository.refresh() }
    }

    fun play(startIndex: Int) {
        val tracks = state.value.tracks
        if (tracks.isNotEmpty()) {
            playbackConnection.playTracks(tracks, startIndex.coerceIn(0, tracks.size - 1))
        }
    }

    fun addTracks(ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { trackId -> playlistRepository.addTrack(playlistId, trackId) }
        }
    }

    fun removeTrack(trackId: Long) {
        viewModelScope.launch { playlistRepository.removeTrack(playlistId, trackId) }
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.rename(playlistId, name) }
    }
}

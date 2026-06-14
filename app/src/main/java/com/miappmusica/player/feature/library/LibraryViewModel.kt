package com.miappmusica.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.AppMode
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.ModeRepository
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val isolatedTitle: String? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    modeRepository: ModeRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    /**
     * The visible track list. When a Mode isolates a playlist, the library is filtered down to
     * just that playlist's tracks and everything else is hidden — the core "isolation" behavior.
     */
    val state: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeTracks(),
        modeRepository.observeActiveMode(),
        playlistRepository.observePlaylists()
    ) { tracks, activeMode, playlists ->
        val isolatedPlaylist: Playlist? = activeMode
            .takeIf { !it.isNormal }
            ?.isolatedPlaylistId
            ?.let { id -> playlists.firstOrNull { it.id == id } }

        if (isolatedPlaylist != null) {
            val order = isolatedPlaylist.trackIds.withIndex().associate { (i, id) -> id to i }
            val filtered = tracks
                .filter { order.containsKey(it.id) }
                .sortedBy { order[it.id] ?: Int.MAX_VALUE }
            LibraryUiState(filtered, isLoading = false, isolatedTitle = isolatedPlaylist.name)
        } else {
            LibraryUiState(tracks, isLoading = false, isolatedTitle = null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        playbackConnection.connect()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { libraryRepository.refresh() }
    }

    fun play(startIndex: Int) {
        playbackConnection.playTracks(state.value.tracks, startIndex)
    }
}

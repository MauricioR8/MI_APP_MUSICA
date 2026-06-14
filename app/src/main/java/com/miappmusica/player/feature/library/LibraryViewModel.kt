package com.miappmusica.player.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.ModeRepository
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
    val isolatedTitle: String? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showCreatePlaylist: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    modeRepository: ModeRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    private val _uiExtras = MutableStateFlow(LibraryUiExtras())

    /**
     * The visible track list. When a Mode isolates a playlist, the library is filtered down to
     * just that playlist's tracks and everything else is hidden — the core "isolation" behavior.
     */
    val state: StateFlow<LibraryUiState> = combine(
        libraryRepository.observeTracks(),
        modeRepository.observeActiveMode(),
        playlistRepository.observePlaylists(),
        _uiExtras
    ) { tracks, activeMode, playlists, extras ->
        val isolatedPlaylist: Playlist? = activeMode
            .takeIf { !it.isNormal }
            ?.isolatedPlaylistId
            ?.let { id -> playlists.firstOrNull { it.id == id } }

        val base = if (isolatedPlaylist != null) {
            val order = isolatedPlaylist.trackIds.withIndex().associate { (i, id) -> id to i }
            val filtered = tracks
                .filter { order.containsKey(it.id) }
                .sortedBy { order[it.id] ?: Int.MAX_VALUE }
            LibraryUiState(filtered, isLoading = false, isolatedTitle = isolatedPlaylist.name)
        } else {
            LibraryUiState(tracks, isLoading = false, isolatedTitle = null)
        }
        base.copy(
            selectionMode = extras.selectionMode,
            selectedIds = extras.selectedIds,
            showCreatePlaylist = extras.showCreatePlaylist
        )
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

    // --- Selection mode for creating playlists ---

    fun enterSelectionMode() {
        _uiExtras.value = _uiExtras.value.copy(selectionMode = true, selectedIds = emptySet())
    }

    fun exitSelectionMode() {
        _uiExtras.value = _uiExtras.value.copy(selectionMode = false, selectedIds = emptySet())
    }

    fun toggleTrackSelection(id: Long) {
        val current = _uiExtras.value.selectedIds
        val updated = if (current.contains(id)) current - id else current + id
        _uiExtras.value = _uiExtras.value.copy(selectedIds = updated)
    }

    fun showCreatePlaylistDialog() {
        _uiExtras.value = _uiExtras.value.copy(showCreatePlaylist = true)
    }

    fun dismissCreatePlaylistDialog() {
        _uiExtras.value = _uiExtras.value.copy(showCreatePlaylist = false)
    }

    fun createPlaylistWithSelected(name: String) {
        if (name.isBlank()) return
        val ids = _uiExtras.value.selectedIds.toList()
        viewModelScope.launch {
            val playlistId = playlistRepository.create(name)
            if (ids.isNotEmpty()) {
                playlistRepository.setTracks(playlistId, ids)
            }
            _uiExtras.value = LibraryUiExtras() // Reset selection
        }
    }

    fun createEmptyPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            playlistRepository.create(name)
            _uiExtras.value = _uiExtras.value.copy(showCreatePlaylist = false)
        }
    }
}

private data class LibraryUiExtras(
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val showCreatePlaylist: Boolean = false
)

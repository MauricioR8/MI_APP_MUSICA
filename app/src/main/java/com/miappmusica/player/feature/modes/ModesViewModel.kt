package com.miappmusica.player.feature.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.AppMode
import com.miappmusica.player.domain.model.Playlist
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

data class ModesUiState(
    val modes: List<AppMode> = emptyList(),
    val activeMode: AppMode = AppMode.NORMAL,
    val expanded: Boolean = false
) {
    val isIsolated: Boolean get() = !activeMode.isNormal && activeMode.isolatedPlaylistId != null
}

@HiltViewModel
class ModesViewModel @Inject constructor(
    private val modeRepository: ModeRepository,
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    val state: StateFlow<ModesUiState> = combine(
        modeRepository.observeModes(),
        modeRepository.observeActiveMode()
    ) { modes, active ->
        ModesUiState(modes = modes, activeMode = active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModesUiState())

    /** User playlists, used by the mode editor to pick an isolated playlist. */
    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Activates [modeId]. Side effects: persists the active mode (which drives UI theming and
     * playlist isolation reactively) and, when the mode opts in, auto-plays its isolated playlist.
     */
    fun activate(modeId: String) {
        viewModelScope.launch {
            var mode = state.value.modes.firstOrNull { it.id == modeId } ?: return@launch
            // Lazily give each non-Normal mode its OWN private playlist the first time it is
            // selected. This guarantees independent memory: e.g. Tristeza and Concentración keep
            // distinct song lists instead of both showing the full library.
            if (!mode.isNormal && mode.isolatedPlaylistId == null) {
                val pid = playlistRepository.create("Modo: ${mode.label}")
                mode = mode.copy(isolatedPlaylistId = pid)
                modeRepository.upsert(mode) // preserves sortOrder
            }
            modeRepository.activate(modeId)
            val playlistId = mode.isolatedPlaylistId
            if (mode.autoPlay && playlistId != null) {
                val playlist = playlistRepository.getPlaylist(playlistId) ?: return@launch
                val tracks = libraryRepository.getTracks(playlist.trackIds)
                if (tracks.isNotEmpty()) playbackConnection.playTracks(tracks)
            }
        }
    }

    fun exitToNormal() = activate(AppMode.NORMAL_ID)

    fun upsert(mode: AppMode) {
        viewModelScope.launch { modeRepository.upsert(mode) }
    }

    /**
     * Saves a mode from the editor. A brand-new non-Normal mode that doesn't reuse an existing
     * playlist gets its OWN private backing playlist, giving each mode independent memory: edits
     * made while in that mode only affect that mode's songs.
     */
    fun saveMode(
        existingId: String?,
        label: String,
        iconKey: String,
        colorArgb: Long,
        isolatedPlaylistId: Long?,
        autoPlay: Boolean
    ) {
        viewModelScope.launch {
            val id = existingId ?: ("mode_" + System.currentTimeMillis())
            var playlistId = isolatedPlaylistId
            if (existingId == null && playlistId == null) {
                // Give the new mode its own private playlist (independent memory).
                playlistId = playlistRepository.create("Modo: $label")
            }
            modeRepository.upsert(
                AppMode(
                    id = id,
                    label = label,
                    iconKey = iconKey,
                    accentColorArgb = colorArgb,
                    isolatedPlaylistId = playlistId,
                    autoPlay = autoPlay,
                    isBuiltIn = false
                )
            )
        }
    }

    fun delete(modeId: String) {
        viewModelScope.launch { modeRepository.delete(modeId) }
    }
}

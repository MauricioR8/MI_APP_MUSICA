package com.miappmusica.player.feature.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.AppMode
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

    /**
     * Activates [modeId]. Side effects: persists the active mode (which drives UI theming and
     * playlist isolation reactively) and, when the mode opts in, auto-plays its isolated playlist.
     */
    fun activate(modeId: String) {
        viewModelScope.launch {
            modeRepository.activate(modeId)
            val mode = state.value.modes.firstOrNull { it.id == modeId } ?: return@launch
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

    fun delete(modeId: String) {
        viewModelScope.launch { modeRepository.delete(modeId) }
    }
}

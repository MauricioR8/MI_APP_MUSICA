package com.miappmusica.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.playback.NowPlayingState
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-wide player state holder. Lives at the navigation root so the mini-player and the full
 * NowPlaying screen share the same playback connection and state.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackConnection: PlaybackConnection,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingState> = playbackConnection.nowPlaying

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        playbackConnection.connect()
    }

    fun togglePlayPause() = playbackConnection.togglePlayPause()
    fun next() = playbackConnection.next()
    fun previous() = playbackConnection.previous()
    fun seekTo(positionMs: Long) = playbackConnection.seekTo(positionMs)

    /** Re-scans MediaStore so edits made by the automatic metadata tool show up immediately. */
    fun refreshLibrary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            libraryRepository.refresh()
            _isRefreshing.value = false
        }
    }
}

package com.miappmusica.player.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.data.repository.LyricsRepository
import com.miappmusica.player.data.repository.LyricsResult
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.playback.NowPlayingState
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-wide player state holder. Lives at the navigation root so the mini-player and the full
 * NowPlaying screen share the same playback connection and state.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackConnection: PlaybackConnection,
    private val libraryRepository: LibraryRepository,
    private val lyricsRepository: LyricsRepository
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlayingState> = playbackConnection.nowPlaying

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** The full domain Track for the currently playing media item (for the info dialog + lyrics). */
    val currentTrack: StateFlow<Track?> = playbackConnection.nowPlaying
        .map { it.mediaId.toLongOrNull() }
        .distinctUntilChanged()
        .map { id -> id?.let { libraryRepository.getTrack(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _lyrics = MutableStateFlow<LyricsResult>(LyricsResult.Loading)
    val lyrics: StateFlow<LyricsResult> = _lyrics.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val isLyricsDownloaded: StateFlow<Boolean> = currentTrack.flatMapLatest { t ->
        if (t == null) flowOf(false) else lyricsRepository.observeDownloaded(t.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        playbackConnection.connect()
    }

    fun togglePlayPause() = playbackConnection.togglePlayPause()
    fun next() = playbackConnection.next()
    fun previous() = playbackConnection.previous()
    fun seekTo(positionMs: Long) = playbackConnection.seekTo(positionMs)

    /** Loads lyrics for the current track (cache first, then online unless offline-only). */
    fun loadLyrics() {
        val t = currentTrack.value ?: return
        viewModelScope.launch {
            _lyrics.value = LyricsResult.Loading
            _lyrics.value = lyricsRepository.fetch(t)
        }
    }

    /** Downloads lyrics for offline use, then refreshes the displayed lyrics. */
    fun downloadLyrics() {
        val t = currentTrack.value ?: return
        viewModelScope.launch {
            lyricsRepository.download(t)
            _lyrics.value = lyricsRepository.fetch(t)
        }
    }

    /** Re-scans MediaStore so edits made by the automatic metadata tool show up immediately. */
    fun refreshLibrary() {
        viewModelScope.launch {
            _isRefreshing.value = true
            libraryRepository.refresh()
            _isRefreshing.value = false
        }
    }
}

package com.miappmusica.player.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
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

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playbackConnection: PlaybackConnection
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<Track>> = combine(
        libraryRepository.observeTracks(),
        _query
    ) { tracks, q ->
        if (q.isBlank()) {
            emptyList()
        } else {
            tracks.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.displayArtist.contains(q, ignoreCase = true) ||
                    it.displayAlbum.contains(q, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        playbackConnection.connect()
        viewModelScope.launch { libraryRepository.refresh() }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun play(track: Track) {
        playbackConnection.playTracks(listOf(track))
    }
}

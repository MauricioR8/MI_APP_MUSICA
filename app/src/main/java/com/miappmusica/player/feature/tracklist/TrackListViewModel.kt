package com.miappmusica.player.feature.tracklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.data.repository.StatsRepository
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.playback.PlaybackConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrackListUiState(
    val title: String = "",
    val tracks: List<Track> = emptyList()
) {
    val isEmpty: Boolean get() = tracks.isEmpty()
}

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val statsRepository: StatsRepository,
    private val playbackConnection: PlaybackConnection,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val type: String = savedStateHandle.get<String>("type") ?: "recently_added"

    val state: StateFlow<TrackListUiState> = buildFlow(type)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackListUiState())

    private fun buildFlow(type: String): Flow<TrackListUiState> = when (type) {
        "most_played" -> combine(
            libraryRepository.observeTracks(),
            statsRepository.observeMostPlayed(200)
        ) { tracks, ids ->
            TrackListUiState("Más escuchadas", orderByIds(tracks, ids))
        }
        "recently_played" -> combine(
            libraryRepository.observeTracks(),
            statsRepository.observeRecentlyPlayed(200)
        ) { tracks, ids ->
            TrackListUiState("Últimas escuchadas", orderByIds(tracks, ids))
        }
        "favorites" -> combine(
            libraryRepository.observeTracks(),
            statsRepository.observeFavoriteIds()
        ) { tracks, ids ->
            TrackListUiState("Pistas favoritas", orderByIds(tracks, ids))
        }
        else -> combine(
            libraryRepository.observeTracks(),
            statsRepository.observeFavoriteIds() // unused; keeps a single combine shape
        ) { tracks, _ ->
            TrackListUiState(
                "Recién añadidas",
                tracks.sortedByDescending { it.dateAdded }
            )
        }
    }

    private fun orderByIds(tracks: List<Track>, ids: List<Long>): List<Track> {
        val byId = tracks.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    init {
        playbackConnection.connect()
        viewModelScope.launch { libraryRepository.refresh() }
    }

    fun play(index: Int) {
        val tracks = state.value.tracks
        if (tracks.isNotEmpty()) {
            playbackConnection.playTracks(tracks, index.coerceIn(0, tracks.size - 1))
        }
    }

    fun playAll() = play(0)
}

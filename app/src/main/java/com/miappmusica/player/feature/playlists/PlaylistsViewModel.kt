package com.miappmusica.player.feature.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val transferRepository: TransferRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun create(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.create(name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { playlistRepository.delete(id) }
    }

    /** Export all playlists to a user-picked document tree (Samsung/Pixel-compatible .m3u8). */
    fun export(treeUri: String) {
        viewModelScope.launch {
            val result = transferRepository.export(treeUri)
            _message.value = result.fold(
                onSuccess = { "Exportadas $it listas" },
                onFailure = { "Error al exportar: ${it.message}" }
            )
        }
    }

    /** Import a playlist file (.m3u/.m3u8) coming from Samsung Music / Pixel Player. */
    fun import(sourceUri: String) {
        viewModelScope.launch {
            val result = transferRepository.import(sourceUri)
            _message.value = result.fold(
                onSuccess = { "Importadas $it pistas" },
                onFailure = { "Error al importar: ${it.message}" }
            )
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

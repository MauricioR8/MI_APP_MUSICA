package com.miappmusica.player.feature.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.data.metadata.ContextDetector
import com.miappmusica.player.domain.model.ContextDetection
import com.miappmusica.player.domain.model.MetadataDiff
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.model.TrackMetadata
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.MetadataRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MetadataPhase { SELECTION, PREVIEW, DONE }
enum class CleanMode { MANUAL, AUTO }
enum class MetadataSource { ALL, DOWNLOADS }

data class MetadataUiState(
    val phase: MetadataPhase = MetadataPhase.SELECTION,
    val allTracks: List<Track> = emptyList(),
    val downloads: List<Track> = emptyList(),
    val source: MetadataSource = MetadataSource.ALL,
    val selectedIds: Set<Long> = emptySet(),
    val mode: CleanMode = CleanMode.AUTO,
    val context: ContextDetection? = null,
    val diffs: List<MetadataDiff> = emptyList(),
    val isProcessing: Boolean = false,
    val resultMessage: String? = null,
    val pendingWriteRequest: androidx.activity.result.IntentSenderRequest? = null
) {
    val candidates: List<Track> get() = if (source == MetadataSource.ALL) allTracks else downloads
    val selectedCount: Int get() = selectedIds.size
    val acceptedChanges: Int get() = diffs.count { it.accepted && it.hasChanges }
}

@HiltViewModel
class MetadataViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val metadataRepository: MetadataRepository,
    private val contextDetector: ContextDetector
) : ViewModel() {

    private val _state = MutableStateFlow(MetadataUiState())
    val state: StateFlow<MetadataUiState> = _state.asStateFlow()

    init {
        // Observe both the full library and downloads; the user can toggle which set to clean.
        libraryRepository.observeTracks()
            .onEach { tracks -> _state.value = _state.value.copy(allTracks = tracks) }
            .launchIn(viewModelScope)
        libraryRepository.observeDownloads()
            .onEach { downloads -> _state.value = _state.value.copy(downloads = downloads) }
            .launchIn(viewModelScope)
        viewModelScope.launch { libraryRepository.refresh() }
    }

    fun setSource(src: MetadataSource) {
        _state.value = _state.value.copy(source = src, selectedIds = emptySet(), context = null)
    }

    fun toggleSelect(id: Long) {
        val current = _state.value.selectedIds
        val updated = if (current.contains(id)) current - id else current + id
        _state.value = _state.value.copy(
            selectedIds = updated,
            context = detectContext(updated)
        )
    }

    fun selectAll() {
        val all = _state.value.candidates.map { it.id }.toSet()
        _state.value = _state.value.copy(selectedIds = all, context = detectContext(all))
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedIds = emptySet(), context = null)
    }

    fun setMode(mode: CleanMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    private fun detectContext(ids: Set<Long>): ContextDetection? {
        if (ids.isEmpty()) return null
        val selected = _state.value.candidates.filter { ids.contains(it.id) }
        return contextDetector.analyze(selected)
    }

    /**
     * Builds the diff set for the current selection. AUTO derives proposals from the engine
     * (heuristics + online lookup). MANUAL seeds identity diffs the user then edits inline.
     * Each track is processed independently so one failure does not abort the whole batch.
     */
    fun analyze() {
        val selected = _state.value.candidates.filter { _state.value.selectedIds.contains(it.id) }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true)
            val ctx = contextDetector.analyze(selected)
            val diffs = when (_state.value.mode) {
                CleanMode.AUTO -> selected.mapNotNull { track ->
                    try {
                        val diff = metadataRepository.buildAutoProposal(track)
                        // When the engine is confident about a single album, propagate it across the batch.
                        if (ctx.isSingleAlbum && ctx.dominantAlbum != null) {
                            diff.copy(proposed = diff.proposed.copy(album = ctx.dominantAlbum))
                        } else diff
                    } catch (_: Exception) {
                        // Graceful degradation: if a track fails, build a no-change identity diff.
                        try {
                            val original = metadataRepository.read(track)
                            MetadataDiff(track.id, track.uri, original, original)
                        } catch (_: Exception) {
                            null // Skip completely broken tracks
                        }
                    }
                }
                CleanMode.MANUAL -> selected.mapNotNull { track ->
                    try {
                        val original = metadataRepository.read(track)
                        MetadataDiff(track.id, track.uri, original, original)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            _state.value = _state.value.copy(
                isProcessing = false,
                phase = MetadataPhase.PREVIEW,
                context = ctx,
                diffs = diffs
            )
        }
    }

    /** Inline edit of a single track inside the preview (used by both Manual and Auto). */
    fun updateProposed(trackId: Long, transform: (TrackMetadata) -> TrackMetadata) {
        _state.value = _state.value.copy(
            diffs = _state.value.diffs.map { diff ->
                if (diff.trackId == trackId) diff.copy(proposed = transform(diff.proposed)) else diff
            }
        )
    }

    fun setManualArtwork(trackId: Long, contentUri: String) {
        updateProposed(trackId) { it.copy(artworkSource = contentUri) }
    }

    fun toggleAccept(trackId: Long) {
        _state.value = _state.value.copy(
            diffs = _state.value.diffs.map { diff ->
                if (diff.trackId == trackId) diff.copy(accepted = !diff.accepted) else diff
            }
        )
    }

    /** Applies every accepted diff to the underlying files (batch operation). */
    fun applyAll() {
        val accepted = _state.value.diffs.filter { it.accepted }
        if (accepted.isEmpty()) return
        val sender = metadataRepository.buildWriteRequest(accepted)
        if (sender != null) {
            _state.value = _state.value.copy(
                pendingWriteRequest = androidx.activity.result.IntentSenderRequest.Builder(sender).build()
            )
        } else {
            performWrites() // API < 30 path
        }
    }

    fun consumeWriteRequest() {
        _state.value = _state.value.copy(pendingWriteRequest = null)
    }

    fun onWritePermissionResult(granted: Boolean) {
        if (granted) {
            performWrites()
        } else {
            _state.value = _state.value.copy(
                phase = MetadataPhase.DONE,
                resultMessage = "Permiso de escritura denegado. No se aplicaron cambios."
            )
        }
    }

    private fun performWrites() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isProcessing = true)
            var ok = 0
            val failures = mutableListOf<String>()
            _state.value.diffs.filter { it.accepted }.forEach { diff ->
                metadataRepository.apply(diff).fold(
                    onSuccess = { ok++ },
                    onFailure = { e -> failures.add("• ${diff.proposed.title}: ${e.message ?: "error desconocido"}") }
                )
            }
            libraryRepository.refresh()
            val msg = buildString {
                append("Aplicados $ok")
                if (failures.isNotEmpty()) {
                    append(" • Fallidos ${failures.size}\n\nDetalle del fallo:\n")
                    append(failures.take(8).joinToString("\n"))
                }
            }
            _state.value = _state.value.copy(
                isProcessing = false,
                phase = MetadataPhase.DONE,
                resultMessage = msg
            )
        }
    }

    fun backToSelection() {
        _state.value = _state.value.copy(
            phase = MetadataPhase.SELECTION,
            diffs = emptyList(),
            resultMessage = null
        )
    }
}

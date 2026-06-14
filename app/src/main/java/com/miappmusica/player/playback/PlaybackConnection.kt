package com.miappmusica.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.miappmusica.player.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Snapshot of the currently playing item, observed by the mini-player and the full player. */
data class NowPlayingState(
    val hasItem: Boolean = false,
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

/**
 * Owns the [MediaController] lifecycle and exposes play commands + a reactive [nowPlaying] state.
 * The controller is only touched on the main thread (all callers are Compose/ViewModel code).
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickerJob: Job? = null

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _nowPlaying = MutableStateFlow(NowPlayingState())
    val nowPlaying: StateFlow<NowPlayingState> = _nowPlaying.asStateFlow()

    private val _queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    val queue: StateFlow<List<QueueEntry>> = _queue.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateNowPlaying()
        }
    }

    fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = runCatching { future.get() }.getOrNull()
                _connected.value = controller != null
                controller?.addListener(listener)
                updateNowPlaying()
                startTicker()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun release() {
        tickerJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _connected.value = false
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        val c = controller ?: return
        val items = tracks.map { it.toMediaItem() }
        c.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        updateNowPlaying()
    }

    /** Jumps to the given queue index and starts playback. */
    fun playQueueIndex(index: Int) {
        controller?.seekToDefaultPosition(index)
        controller?.play()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (true) {
                if (controller?.isPlaying == true) updateNowPlaying()
                delay(500)
            }
        }
    }

    private fun updateNowPlaying() {
        val c = controller
        if (c == null || c.mediaItemCount == 0) {
            _nowPlaying.value = NowPlayingState()
            _queue.value = emptyList()
            return
        }
        val meta = c.mediaMetadata
        _nowPlaying.value = NowPlayingState(
            hasItem = true,
            mediaId = c.currentMediaItem?.mediaId.orEmpty(),
            title = meta.title?.toString().orEmpty(),
            artist = meta.artist?.toString().orEmpty(),
            artworkUri = meta.artworkUri?.toString(),
            isPlaying = c.isPlaying,
            positionMs = c.currentPosition.coerceAtLeast(0),
            durationMs = c.duration.coerceAtLeast(0),
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem()
        )

        // Rebuild the queue snapshot for the player's "cola" overlay.
        val list = ArrayList<QueueEntry>()
        val count = c.mediaItemCount
        val curIndex = c.currentMediaItemIndex
        for (i in 0 until count) {
            val mi = c.getMediaItemAt(i)
            list.add(
                QueueEntry(
                    mediaId = mi.mediaId,
                    title = mi.mediaMetadata.title?.toString().orEmpty(),
                    artist = mi.mediaMetadata.artist?.toString().orEmpty(),
                    isCurrent = i == curIndex
                )
            )
        }
        _queue.value = list
    }

    /** A single item in the playback queue, surfaced to the "cola" overlay in the player. */
    data class QueueEntry(
        val mediaId: String,
        val title: String,
        val artist: String,
        val isCurrent: Boolean
    )

    private fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(Uri.parse(uri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(displayArtist)
                .setAlbumTitle(displayAlbum)
                .setArtworkUri(artworkUri?.let { Uri.parse(it) })
                .build()
        )
        .build()
}

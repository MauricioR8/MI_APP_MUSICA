package com.miappmusica.player.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.miappmusica.player.data.repository.LyricsResult
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.playback.NowPlayingState
import com.miappmusica.player.playback.PlaybackConnection
import java.util.Locale

fun formatTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0)
    val totalSeconds = safe / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/** Bottom mini-player bar (Samsung Music style). Tapping it opens the full player. */
@Composable
fun MiniPlayer(
    state: NowPlayingState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasItem) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExpand)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = state.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    state.title.ifBlank { "Reproduciendo" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    state.artist,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onTogglePlay, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Reproducir/Pausar",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.SkipNext, "Siguiente", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Full-screen player with large artwork, seek bar with times, and transport controls. */
@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    currentTrack: Track? = null,
    lyrics: LyricsResult = LyricsResult.Loading,
    lyricsDownloaded: Boolean = false,
    onLoadLyrics: () -> Unit = {},
    onDownloadLyrics: () -> Unit = {},
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    queue: List<PlaybackConnection.QueueEntry> = emptyList(),
    onPlayQueueIndex: (Int) -> Unit = {},
    playlists: List<Playlist> = emptyList(),
    onAddToPlaylist: (Long) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    backgroundColor: Color? = null
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    var showInfo by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    val duration = state.durationMs.coerceAtLeast(1L)
    val sliderValue = if (dragging) dragValue else state.positionMs.toFloat().coerceIn(0f, duration.toFloat())

    Box(modifier = modifier.fillMaxSize().background(backgroundColor ?: MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCollapse) {
                    Icon(Icons.Filled.KeyboardArrowDown, "Minimizar")
                }
                Text(
                    "Reproduciendo ahora",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Lyrics ("ver letras") + song info ("hoja con líneas") actions
                IconButton(onClick = {
                    showLyrics = true
                    onLoadLyrics()
                }) {
                    Icon(Icons.Filled.Lyrics, "Ver letras")
                }
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Filled.Description, "Información de la canción")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (state.artworkUri != null) {
                    AsyncImage(
                        model = state.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, null, modifier = Modifier.size(96.dp))
                }
            }

            Text(
                state.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                state.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Slider(
                value = sliderValue,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    dragging = false
                    onSeek(dragValue.toLong())
                },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier.padding(top = 16.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(sliderValue.toLong()), style = MaterialTheme.typography.labelSmall)
                Text(formatTime(state.durationMs), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.weight(1f))

            // Secondary actions row: queue (left) + favorite heart (center) + add-to-playlist (right)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showQueue = true }) {
                    Icon(Icons.Filled.QueueMusic, "Cola de reproducción")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Quitar de favoritas" else "Añadir a favoritas",
                            tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showAddToPlaylist = true }) {
                        Icon(Icons.Filled.PlaylistAdd, "Añadir a una lista")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipPrevious, "Anterior", modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(16.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp).clickable(onClick = onTogglePlay)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Reproducir/Pausar",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.SkipNext, "Siguiente", modifier = Modifier.size(40.dp))
                }
            }
        }

        // Lyrics overlay: dims the player and shows the lyrics on top. Playback keeps running.
        if (showLyrics) {
            LyricsOverlay(
                lyrics = lyrics,
                downloaded = lyricsDownloaded,
                state = state,
                onDownload = onDownloadLyrics,
                onClose = { showLyrics = false }
            )
        }

        // Queue overlay: shows what's playing now and what comes next.
        if (showQueue) {
            QueueOverlay(
                queue = queue,
                onPlayIndex = { index ->
                    onPlayQueueIndex(index)
                    showQueue = false
                },
                onClose = { showQueue = false }
            )
        }
    }

    if (showInfo) {
        currentTrack?.let { track ->
            TrackInfoDialog(track = track, onDismiss = { showInfo = false })
        }
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            playlists = playlists,
            onSelect = { id ->
                onAddToPlaylist(id)
                showAddToPlaylist = false
            },
            onCreate = { name ->
                onCreatePlaylist(name)
                showAddToPlaylist = false
            },
            onDismiss = { showAddToPlaylist = false }
        )
    }
}

@Composable
private fun LyricsOverlay(
    lyrics: LyricsResult,
    downloaded: Boolean,
    state: NowPlayingState,
    onDownload: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(enabled = true) { onClose() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Letras",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                when (lyrics) {
                    LyricsResult.Loading -> CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                    is LyricsResult.Available -> SyncedLyrics(
                        plain = lyrics.plain,
                        synced = lyrics.synced,
                        state = state
                    )
                    LyricsResult.NotFound -> Text(
                        "No se encontraron letras",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    LyricsResult.Disabled -> Text(
                        "Activa las letras en Ajustes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                    is LyricsResult.Error -> Text(
                        lyrics.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }

            Button(
                onClick = onDownload,
                enabled = !downloaded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (downloaded) "Descargada" else "Descargar")
            }
        }
    }
}

/**
 * Lyrics display. When timestamped LRC ([synced]) is available it highlights the exact current
 * line based on real playback position and auto-scrolls to it (accurate, no desync). Otherwise it
 * falls back to a static, scrollable plain-text view (no misleading position-based highlight).
 */
@Composable
private fun SyncedLyrics(plain: String, synced: String?, state: NowPlayingState) {
    val listState = rememberLazyListState()

    // Parse LRC lines into (timeMs, text) pairs, sorted by time.
    val regex = remember { Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\](.*)""") }
    val parsed = remember(synced) {
        if (synced.isNullOrBlank()) {
            emptyList()
        } else {
            synced.lineSequence().mapNotNull { line ->
                val m = regex.find(line) ?: return@mapNotNull null
                val min = m.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val sec = m.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                val freq = m.groupValues[3]
                val frac = if (freq.isBlank()) 0 else (freq.padEnd(3, '0').take(3)).toIntOrNull() ?: 0
                val ms = (min * 60 + sec) * 1000L + frac
                ms to m.groupValues[4].trim()
            }.toList().sortedBy { it.first }
        }
    }

    if (parsed.isNotEmpty()) {
        val activeIndex = remember(state.positionMs, parsed) {
            parsed.indexOfLast { it.first <= state.positionMs }.coerceAtLeast(0)
        }
        LaunchedEffect(activeIndex) {
            runCatching { listState.animateScrollToItem(activeIndex.coerceAtLeast(0)) }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            itemsIndexed(parsed) { index, pair ->
                val text = pair.second
                if (text.isBlank()) {
                    Spacer(Modifier.size(10.dp))
                } else {
                    val isActive = index == activeIndex
                    Text(
                        text = text,
                        style = if (isActive) MaterialTheme.typography.titleMedium
                        else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    )
                }
            }
        }
        return
    }

    // Fallback: plain text (static, scrollable). No fake highlight, so it can't be "way off".
    val lines = remember(plain) { plain.split("\n") }
    val hasContent = remember(lines) { lines.any { it.isNotBlank() } }
    if (plain.isBlank() || !hasContent) {
        Text(
            "No se encontraron letras",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        return
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(lines) { line ->
            if (line.isBlank()) {
                Spacer(Modifier.size(10.dp))
            } else {
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QueueOverlay(
    queue: List<PlaybackConnection.QueueEntry>,
    onPlayIndex: (Int) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(enabled = true) { onClose() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(onClick = onClose)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "En reproducción / A continuación",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, "Cerrar", tint = Color.White)
                }
            }

            if (queue.isEmpty()) {
                Text(
                    "La cola está vacía",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                    itemsIndexed(queue) { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayIndex(index) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.title.ifBlank { "Pista" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    color = if (entry.isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (entry.isCurrent) "Reproduciendo" else entry.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isCurrent) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (entry.isCurrent) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onSelect: (Long) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir a una lista") },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(
                        "No tienes listas todavía. Crea una nueva abajo.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                        items(playlists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(playlist.id) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.QueueMusic, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    playlist.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nueva lista") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (newName.isNotBlank()) onCreate(newName) },
                        enabled = newName.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, "Crear lista")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

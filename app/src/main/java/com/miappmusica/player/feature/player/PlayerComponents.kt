package com.miappmusica.player.feature.player

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.playback.NowPlayingState
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
    onDownloadLyrics: () -> Unit = {}
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(0f) }
    var showInfo by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    val duration = state.durationMs.coerceAtLeast(1L)
    val sliderValue = if (dragging) dragValue else state.positionMs.toFloat().coerceIn(0f, duration.toFloat())

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                onDownload = onDownloadLyrics,
                onClose = { showLyrics = false }
            )
        }
    }

    if (showInfo) {
        currentTrack?.let { track ->
            TrackInfoDialog(track = track, onDismiss = { showInfo = false })
        }
    }
}

@Composable
private fun LyricsOverlay(
    lyrics: LyricsResult,
    downloaded: Boolean,
    onDownload: () -> Unit,
    onClose: () -> Unit
) {
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
                    is LyricsResult.Available -> Text(
                        text = lyrics.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
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

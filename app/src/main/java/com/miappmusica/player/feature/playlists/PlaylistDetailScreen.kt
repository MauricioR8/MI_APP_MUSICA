package com.miappmusica.player.feature.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.ui.components.TrackRow

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize()) {
        // Header row: back + title + play all
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (state.cover != null) {
                    AsyncImage(state.cover, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.LibraryMusic, null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${state.tracks.size} pistas", style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.play(0) },
                enabled = state.tracks.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(4.dp))
                Text("Reproducir todo")
            }
            Button(onClick = { showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PlaylistAdd, null)
                Spacer(Modifier.width(4.dp))
                Text("Agregar")
            }
        }

        if (state.isEmpty) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.LibraryMusic,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    "Esta lista está vacía. Agrega canciones.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.size(16.dp))
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.PlaylistAdd, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar canciones")
                }
            }
        } else {
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(state.tracks, key = { it.id }) { track ->
                    val index = state.tracks.indexOf(track)
                    TrackRow(
                        track = track,
                        onClick = { viewModel.play(index) },
                        trailing = {
                            IconButton(onClick = { viewModel.removeTrack(track.id) }) {
                                Icon(Icons.Filled.RemoveCircle, contentDescription = "Quitar")
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddTracksDialog(
            available = state.availableTracks,
            onConfirm = { ids ->
                viewModel.addTracks(ids)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AddTracksDialog(
    available: List<Track>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<Set<Long>>(emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar canciones") },
        text = {
            if (available.isEmpty()) {
                Text("No hay más canciones disponibles para agregar.")
            } else {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(available, key = { it.id }) { track ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    selected = if (selected.contains(track.id)) {
                                        selected - track.id
                                    } else {
                                        selected + track.id
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.contains(track.id),
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + track.id else selected - track.id
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    track.displayArtist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty()
            ) {
                Icon(Icons.Filled.Check, null)
                Spacer(Modifier.width(4.dp))
                Text("Agregar (${selected.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

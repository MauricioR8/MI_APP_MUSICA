package com.miappmusica.player.feature.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onDeleteResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    LaunchedEffect(state.pendingDeleteRequest) {
        state.pendingDeleteRequest?.let {
            deleteLauncher.launch(it)
            viewModel.consumeDeleteRequest()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.tracks.isEmpty() -> Text(
                text = "No se encontraron audios.\nConcede permisos o agrega musica al dispositivo.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
            else -> {
                Column(Modifier.fillMaxSize()) {
                    // Selection mode header
                    if (state.selectionMode) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.exitSelectionMode() }) {
                                Icon(Icons.Filled.Close, "Cancelar")
                            }
                            Text(
                                "${state.selectedIds.size} seleccionadas",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { viewModel.showCreatePlaylistDialog() },
                                enabled = state.selectedIds.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.PlaylistAdd, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Crear lista")
                            }
                            TextButton(
                                onClick = { viewModel.requestDeleteSelected() },
                                enabled = state.selectedIds.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Eliminar")
                            }
                        }
                    }

                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        state.isolatedTitle?.let { title ->
                            item {
                                Text(
                                    text = "Aislado en: $title",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                        itemsIndexed(state.tracks, key = { _, t -> t.id }) { index, track ->
                            if (state.selectionMode) {
                                SelectableTrackRow(
                                    track = track,
                                    isSelected = state.selectedIds.contains(track.id),
                                    onClick = { viewModel.toggleTrackSelection(track.id) }
                                )
                            } else {
                                LongPressTrackRow(
                                    track = track,
                                    onClick = { viewModel.play(index) },
                                    onLongClick = {
                                        viewModel.enterSelectionMode()
                                        viewModel.toggleTrackSelection(track.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to create a new playlist (visible when NOT in selection mode)
        if (!state.isLoading && state.tracks.isNotEmpty() && !state.selectionMode) {
            FloatingActionButton(
                onClick = { viewModel.showCreatePlaylistDialog() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Crear lista de reproduccion")
            }
        }
    }

    // Create playlist dialog
    if (state.showCreatePlaylist) {
        CreatePlaylistDialog(
            hasSelection = state.selectedIds.isNotEmpty(),
            selectionCount = state.selectedIds.size,
            onConfirm = { name ->
                if (state.selectedIds.isNotEmpty()) {
                    viewModel.createPlaylistWithSelected(name)
                } else {
                    viewModel.createEmptyPlaylist(name)
                }
            },
            onDismiss = { viewModel.dismissCreatePlaylistDialog() }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressTrackRow(
    track: com.miappmusica.player.domain.model.Track,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.displayArtist} \u2022 ${track.displayAlbum}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableTrackRow(
    track: com.miappmusica.player.domain.model.Track,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.displayArtist} \u2022 ${track.displayAlbum}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = if (isSelected) "Seleccionada" else "No seleccionada",
            tint = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    hasSelection: Boolean,
    selectionCount: Int,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva lista de reproduccion") },
        text = {
            Column {
                if (hasSelection) {
                    Text(
                        "Se agregaran $selectionCount canciones a la nueva lista.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Crear")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

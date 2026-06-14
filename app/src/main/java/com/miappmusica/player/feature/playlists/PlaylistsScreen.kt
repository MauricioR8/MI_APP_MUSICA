package com.miappmusica.player.feature.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.miappmusica.player.domain.model.Playlist
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenTrackList: (String) -> Unit = {},
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Local, live-editable copy used during drag-to-reorder. Resets whenever the persisted order
    // changes (e.g. after reorder() round-trips through the DB).
    var orderedPlaylists by remember(state.playlists) { mutableStateOf(state.playlists) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragAccumulator by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val rowThresholdPx = with(density) { 64.dp.toPx() }

    fun moveItem(id: Long, delta: Int) {
        val idx = orderedPlaylists.indexOfFirst { it.id == id }
        val target = idx + delta
        if (idx < 0 || target < 0 || target > orderedPlaylists.lastIndex) return
        val updated = orderedPlaylists.toMutableList()
        val moved = updated.removeAt(idx)
        updated.add(target, moved)
        orderedPlaylists = updated
        viewModel.reorder(updated.map { it.id })
    }

    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var pendingCoverId by remember { mutableLongStateOf(-1L) }

    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pendingCoverId >= 0) viewModel.setCover(pendingCoverId, uri.toString())
        pendingCoverId = -1L
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // Header (Samsung Music style)
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Listas de reproducción",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showCreate = true }) { Icon(Icons.Filled.Add, "Crear") }
            }

            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
                // Smart cards rail
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            SmartCard(
                                title = "Recién añadidas",
                                count = state.recentlyAdded.size,
                                cover = lastCover(state.recentlyAdded),
                                onClick = { onOpenTrackList("recently_added") }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Más escuchadas",
                                count = state.mostPlayed.size,
                                cover = lastCover(state.mostPlayed),
                                onClick = { onOpenTrackList("most_played") }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Últimas escuchadas",
                                count = state.recentlyPlayed.size,
                                cover = lastCover(state.recentlyPlayed),
                                onClick = { onOpenTrackList("recently_played") }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Pistas favoritas",
                                count = state.favorites.size,
                                cover = lastCover(state.favorites),
                                onClick = { onOpenTrackList("favorites") }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Biblioteca",
                                count = state.totalTracks,
                                cover = null,
                                onClick = onOpenLibrary
                            )
                        }
                    }
                }

                items(orderedPlaylists, key = { it.id }) { playlist ->
                    val dragModifier = Modifier.pointerInput(playlist.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingId = playlist.id
                                dragAccumulator = 0f
                            },
                            onDragEnd = {
                                viewModel.reorder(orderedPlaylists.map { it.id })
                                draggingId = null
                                dragAccumulator = 0f
                            },
                            onDragCancel = {
                                draggingId = null
                                dragAccumulator = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulator += dragAmount.y
                                val curIdx = orderedPlaylists.indexOfFirst { it.id == playlist.id }
                                if (curIdx < 0) return@detectDragGesturesAfterLongPress
                                if (dragAccumulator >= rowThresholdPx && curIdx < orderedPlaylists.lastIndex) {
                                    val updated = orderedPlaylists.toMutableList()
                                    val moved = updated.removeAt(curIdx)
                                    updated.add(curIdx + 1, moved)
                                    orderedPlaylists = updated
                                    dragAccumulator = 0f
                                } else if (dragAccumulator <= -rowThresholdPx && curIdx > 0) {
                                    val updated = orderedPlaylists.toMutableList()
                                    val moved = updated.removeAt(curIdx)
                                    updated.add(curIdx - 1, moved)
                                    orderedPlaylists = updated
                                    dragAccumulator = 0f
                                }
                            }
                        )
                    }
                    PlaylistRow(
                        playlist = playlist,
                        cover = state.covers[playlist.id],
                        dragging = draggingId == playlist.id,
                        dragModifier = dragModifier,
                        onOpen = { onOpenPlaylist(playlist.id) },
                        onPlay = { viewModel.playPlaylist(playlist) },
                        onChangeCover = {
                            pendingCoverId = playlist.id
                            coverPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRename = { renameTarget = playlist },
                        onDelete = { viewModel.delete(playlist.id) },
                        onMoveUp = { moveItem(playlist.id, -1) },
                        onMoveDown = { moveItem(playlist.id, 1) }
                    )
                }
            }
        }

        // A-Z fast index (right edge)
        AlphabetIndex(
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetter = { letter ->
                val headerOffset = 1 // only the smart cards rail precedes the playlist items
                val idx = orderedPlaylists.indexOfFirst { it.name.uppercase().startsWith(letter) }
                if (idx >= 0) scope.launch { listState.animateScrollToItem(idx + headerOffset) }
            }
        )
    }

    if (showCreate) {
        TextPromptDialog(
            title = "Nueva lista",
            initial = "",
            onConfirm = { viewModel.create(it); showCreate = false },
            onDismiss = { showCreate = false }
        )
    }
    renameTarget?.let { target ->
        TextPromptDialog(
            title = "Renombrar lista",
            initial = target.name,
            onConfirm = { viewModel.rename(target.id, it); renameTarget = null },
            onDismiss = { renameTarget = null }
        )
    }
}

@Composable
private fun SmartCard(title: String, count: Int, cover: String?, onClick: () -> Unit) {
    Column(modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        Box(
            Modifier.size(150.dp).clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                AsyncImage(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.LibraryMusic, null, modifier = Modifier.size(48.dp))
            }
        }
        Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp))
        Text("$count pistas", style = MaterialTheme.typography.labelSmall)
    }
}

/** Cover for a smart-card: the artwork of the LAST song in that card (newest/bottom entry). */
private fun lastCover(tracks: List<com.miappmusica.player.domain.model.Track>): String? =
    tracks.lastOrNull { it.artworkUri != null }?.artworkUri ?: tracks.lastOrNull()?.artworkUri

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    cover: String?,
    dragging: Boolean,
    dragModifier: Modifier,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onChangeCover: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val rowBackground = if (dragging) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    Row(
        dragModifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.DragHandle,
            contentDescription = "Mantén presionado para reordenar",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                AsyncImage(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Filled.LibraryMusic, null)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text("${playlist.size} pistas", style = MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick = onPlay) { Icon(Icons.Filled.PlayArrow, "Reproducir") }
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Más") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Subir") },
                    leadingIcon = { Icon(Icons.Filled.ArrowUpward, null) },
                    onClick = { menu = false; onMoveUp() }
                )
                DropdownMenuItem(
                    text = { Text("Bajar") },
                    leadingIcon = { Icon(Icons.Filled.ArrowDownward, null) },
                    onClick = { menu = false; onMoveDown() }
                )
                DropdownMenuItem(
                    text = { Text("Cambiar portada") },
                    leadingIcon = { Icon(Icons.Filled.Image, null) },
                    onClick = { menu = false; onChangeCover() }
                )
                DropdownMenuItem(text = { Text("Renombrar") }, onClick = { menu = false; onRename() })
                if (!playlist.isSystem) {
                    DropdownMenuItem(text = { Text("Eliminar") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun AlphabetIndex(modifier: Modifier = Modifier, onLetter: (String) -> Unit) {
    val letters = remember { ('A'..'Z').map { it.toString() } }
    Column(
        modifier = modifier.fillMaxHeight().width(18.dp).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        letters.forEach { letter ->
            Text(
                text = letter,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.clickable { onLetter(letter) }.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun TextPromptDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true)
        },
        confirmButton = { TextButton(onClick = { onConfirm(value) }) { Text("Aceptar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

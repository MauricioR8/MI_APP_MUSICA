package com.miappmusica.player.feature.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.layout.ContentScale
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
    viewModel: PlaylistsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sorted = remember(state.playlists) { state.playlists.sortedBy { it.name.uppercase() } }

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
                                cover = state.recentlyAdded.firstOrNull()?.artworkUri,
                                onClick = { viewModel.playTracks(state.recentlyAdded) }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Más escuchadas",
                                count = state.mostPlayed.size,
                                cover = state.mostPlayed.firstOrNull()?.artworkUri,
                                onClick = { viewModel.playTracks(state.mostPlayed) }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Últimas escuchadas",
                                count = state.recentlyPlayed.size,
                                cover = state.recentlyPlayed.firstOrNull()?.artworkUri,
                                onClick = { viewModel.playTracks(state.recentlyPlayed) }
                            )
                        }
                        item {
                            SmartCard(
                                title = "Pistas favoritas",
                                count = state.favorites.size,
                                cover = state.favorites.firstOrNull()?.artworkUri,
                                onClick = { viewModel.playTracks(state.favorites) }
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

                // Import / Export row
                item {
                    ImportExportRow(
                        onImport = viewModel::import,
                        onExport = viewModel::export
                    )
                }

                items(sorted, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        cover = state.covers[playlist.id],
                        onOpen = { onOpenPlaylist(playlist.id) },
                        onPlay = { viewModel.playPlaylist(playlist) },
                        onChangeCover = {
                            pendingCoverId = playlist.id
                            coverPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRename = { renameTarget = playlist },
                        onDelete = { viewModel.delete(playlist.id) }
                    )
                }
            }
        }

        // A-Z fast index (right edge)
        AlphabetIndex(
            modifier = Modifier.align(Alignment.CenterEnd),
            onLetter = { letter ->
                val headerOffset = 2 // smart cards + import/export items
                val idx = sorted.indexOfFirst { it.name.uppercase().startsWith(letter) }
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

@Composable
private fun ImportExportRow(onImport: (String) -> Unit, onExport: (String) -> Unit) {
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { onImport(it.toString()) } }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { onExport(it.toString()) } }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
            Icon(Icons.Filled.Download, null); Spacer(Modifier.width(4.dp)); Text("Importar")
        }
        TextButton(onClick = { exportLauncher.launch(null) }) {
            Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(4.dp)); Text("Exportar")
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    cover: String?,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onChangeCover: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

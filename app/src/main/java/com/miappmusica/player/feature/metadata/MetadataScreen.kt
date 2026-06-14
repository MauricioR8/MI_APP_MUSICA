package com.miappmusica.player.feature.metadata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.miappmusica.player.domain.model.FieldChange
import com.miappmusica.player.domain.model.MetadataDiff

@Composable
fun MetadataScreen(
    modifier: Modifier = Modifier,
    viewModel: MetadataViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val writeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.onWritePermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    androidx.compose.runtime.LaunchedEffect(state.pendingWriteRequest) {
        state.pendingWriteRequest?.let {
            writeLauncher.launch(it)
            viewModel.consumeWriteRequest()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (state.phase) {
            MetadataPhase.SELECTION -> SelectionPhase(state, viewModel)
            MetadataPhase.PREVIEW -> PreviewPhase(state, viewModel)
            MetadataPhase.DONE -> DonePhase(state, viewModel)
        }
        if (state.isProcessing) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SelectionPhase(state: MetadataUiState, vm: MetadataViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Limpieza de metadatos", style = MaterialTheme.typography.titleLarge)

        // Source toggle: all songs vs only downloaded
        Row(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.source == MetadataSource.ALL,
                onClick = { vm.setSource(MetadataSource.ALL) },
                label = { Text("Todas") }
            )
            FilterChip(
                selected = state.source == MetadataSource.DOWNLOADS,
                onClick = { vm.setSource(MetadataSource.DOWNLOADS) },
                label = { Text("Descargados") }
            )
        }

        // Context detection banner
        state.context?.let { ctx ->
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = ctx.summary,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // Mode toggle
        Row(
            Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.mode == CleanMode.AUTO,
                onClick = { vm.setMode(CleanMode.AUTO) },
                label = { Text("Automático") },
                leadingIcon = { Icon(Icons.Filled.AutoFixHigh, null) }
            )
            FilterChip(
                selected = state.mode == CleanMode.MANUAL,
                onClick = { vm.setMode(CleanMode.MANUAL) },
                label = { Text("Manual") },
                leadingIcon = { Icon(Icons.Filled.Edit, null) }
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { vm.selectAll() }) { Text("Seleccionar todo") }
            OutlinedButton(onClick = { vm.clearSelection() }) { Text("Limpiar") }
        }

        LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
            items(state.candidates, key = { it.id }) { track ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.selectedIds.contains(track.id),
                        onCheckedChange = { vm.toggleSelect(track.id) }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(track.title, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                        Text(track.displayArtist, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Button(
            onClick = { vm.analyze() },
            enabled = state.selectedCount > 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Previsualizar cambios (${state.selectedCount})")
        }
    }
}

@Composable
private fun PreviewPhase(state: MetadataUiState, vm: MetadataViewModel) {
    var pendingArtworkTrackId by remember { mutableStateOf<Long?>(null) }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val id = pendingArtworkTrackId
        if (uri != null && id != null) vm.setManualArtwork(id, uri.toString())
        pendingArtworkTrackId = null
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Previsualización (antes / después)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.diffs, key = { it.trackId }) { diff ->
                DiffCard(
                    diff = diff,
                    onToggleAccept = { vm.toggleAccept(diff.trackId) },
                    onEditField = { transform -> vm.updateProposed(diff.trackId, transform) },
                    onPickImage = {
                        pendingArtworkTrackId = diff.trackId
                        pickImage.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.backToSelection() }, modifier = Modifier.weight(1f)) {
                Text("Atrás")
            }
            Button(
                onClick = { vm.applyAll() },
                enabled = state.acceptedChanges > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text("Aplicar (${state.acceptedChanges})")
            }
        }
    }
}

@Composable
private fun DiffCard(
    diff: MetadataDiff,
    onToggleAccept: () -> Unit,
    onEditField: ((com.miappmusica.player.domain.model.TrackMetadata) -> com.miappmusica.player.domain.model.TrackMetadata) -> Unit,
    onPickImage: () -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Artwork before/after
                AsyncImage(
                    model = diff.proposed.artworkSource ?: diff.proposedArtworkUrl
                        ?: diff.original.artworkSource,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(diff.proposed.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (diff.hasChanges) "${diff.changedCount} cambios propuestos"
                        else "Sin cambios",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Switch(checked = diff.accepted, onCheckedChange = { onToggleAccept() })
            }

            Spacer(Modifier.size(8.dp))

            // Field-level before/after rows
            diff.fieldChanges.filter { it.changed }.forEach { change ->
                DiffRow(change)
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { editing = !editing }) {
                    Icon(Icons.Filled.Edit, null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (editing) "Cerrar" else "Editar")
                }
                OutlinedButton(onClick = onPickImage) {
                    Icon(Icons.Filled.Image, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Imagen")
                }
            }

            if (editing) {
                InlineEditor(diff, onEditField)
            }
        }
    }
}

@Composable
private fun DiffRow(change: FieldChange) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = change.field.displayName + ": ",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = change.before.ifBlank { "—" },
            style = MaterialTheme.typography.labelSmall,
            textDecoration = TextDecoration.LineThrough,
            color = Color(0xFFB00020),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = change.after.ifBlank { "—" },
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF1B7F3B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InlineEditor(
    diff: MetadataDiff,
    onEditField: ((com.miappmusica.player.domain.model.TrackMetadata) -> com.miappmusica.player.domain.model.TrackMetadata) -> Unit
) {
    Column(Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = diff.proposed.title,
            onValueChange = { v -> onEditField { it.copy(title = v) } },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = diff.proposed.artist,
            onValueChange = { v -> onEditField { it.copy(artist = v) } },
            label = { Text("Artista") },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
        OutlinedTextField(
            value = diff.proposed.album,
            onValueChange = { v -> onEditField { it.copy(album = v) } },
            label = { Text("Álbum") },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

@Composable
private fun DonePhase(state: MetadataUiState, vm: MetadataViewModel) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Proceso completado", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            text = state.resultMessage ?: "",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = { vm.backToSelection() }) { Text("Volver") }
    }
}

package com.miappmusica.player.feature.modes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miappmusica.player.domain.model.AppMode
import com.miappmusica.player.domain.model.Playlist

private val ModePalette: List<Long> = listOf(
    0xFF7C4DFF,
    0xFFFF5252,
    0xFF2962FF,
    0xFF00BFA5,
    0xFFFF6D00,
    0xFFEC407A,
    0xFF00ACC1,
    0xFFFFB300
)

private val ModeIconKeys: List<String> = listOf("home", "fitness", "focus", "sad", "music")

@Composable
fun ModesManagerScreen(
    onBack: () -> Unit,
    onOpenModePlaylist: (Long) -> Unit = {},
    viewModel: ModesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    var editorVisible by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<AppMode?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
            }
            Text(
                "Modos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                editing = null
                editorVisible = true
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear modo")
            }
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(state.modes, key = { it.id }) { mode ->
                ModeRow(
                    mode = mode,
                    onOpen = { mode.isolatedPlaylistId?.let(onOpenModePlaylist) },
                    onEdit = {
                        editing = mode
                        editorVisible = true
                    },
                    onDelete = { viewModel.delete(mode.id) }
                )
            }
        }

        Button(
            onClick = {
                editing = null
                editorVisible = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Crear modo")
        }
    }

    if (editorVisible) {
        ModeEditorDialog(
            existing = editing,
            playlists = playlists,
            onSave = { existingId, label, iconKey, colorArgb, isolatedPlaylistId, autoPlay ->
                viewModel.saveMode(existingId, label, iconKey, colorArgb, isolatedPlaylistId, autoPlay)
                editorVisible = false
            },
            onDismiss = { editorVisible = false }
        )
    }
}

@Composable
private fun ModeRow(
    mode: AppMode,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = mode.isolatedPlaylistId != null, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(iconForKey(mode.iconKey), contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                mode.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                when {
                    mode.isNormal -> "Modo base (biblioteca completa)"
                    mode.isolatedPlaylistId != null -> "Lista propia · toca para editar sus canciones"
                    else -> "Modo personalizado"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(mode.accentColor())
        )
        if (!mode.isNormal) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        }
    }
}

@Composable
private fun ModeEditorDialog(
    existing: AppMode?,
    playlists: List<Playlist>,
    onSave: (
        existingId: String?,
        label: String,
        iconKey: String,
        colorArgb: Long,
        isolatedPlaylistId: Long?,
        autoPlay: Boolean
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf(existing?.label ?: "") }
    var selectedColor by remember { mutableStateOf(existing?.accentColorArgb ?: ModePalette.first()) }
    var selectedIcon by remember { mutableStateOf(existing?.iconKey ?: ModeIconKeys.first()) }
    var selectedPlaylist by remember { mutableStateOf(existing?.isolatedPlaylistId) }
    var autoPlay by remember { mutableStateOf(existing?.autoPlay ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Crear modo" else "Editar modo") },
        text = {
            Column {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModePalette.forEach { argb ->
                        val isSelected = selectedColor == argb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = argb },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    "Icono",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeIconKeys.forEach { key ->
                        val isSelected = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                iconForKey(key),
                                contentDescription = key,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text(
                    "Playlist del modo",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 160.dp)) {
                    item {
                        PlaylistChoiceRow(
                            name = if (existing == null) "Crear lista propia (memoria independiente)" else "Ninguna",
                            selected = selectedPlaylist == null,
                            onClick = { selectedPlaylist = null }
                        )
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistChoiceRow(
                            name = playlist.name,
                            selected = selectedPlaylist == playlist.id,
                            onClick = { selectedPlaylist = playlist.id }
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-reproducir", Modifier.weight(1f))
                    Switch(checked = autoPlay, onCheckedChange = { autoPlay = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        existing?.id,
                        label.trim().ifBlank { "Modo" },
                        selectedIcon,
                        selectedColor,
                        selectedPlaylist,
                        autoPlay
                    )
                },
                enabled = label.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun PlaylistChoiceRow(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

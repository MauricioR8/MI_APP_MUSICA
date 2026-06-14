package com.miappmusica.player.feature.player

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miappmusica.player.domain.model.Track

/** Shows every available metadata field for a track in a simple label/value list. */
@Composable
fun TrackInfoDialog(track: Track, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Información de la canción") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                InfoRow("Título", track.title.ifBlank { "—" })
                InfoRow("Artista", track.displayArtist)
                InfoRow("Álbum", track.displayAlbum)
                InfoRow("Año", if (track.year > 0) track.year.toString() else "—")
                InfoRow("Género", track.genre?.ifBlank { null } ?: "—")
                InfoRow("Pista #", if (track.trackNumber > 0) track.trackNumber.toString() else "—")
                InfoRow("Duración", formatTime(track.durationMs))
                InfoRow("Ruta", track.data ?: "—")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(96.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

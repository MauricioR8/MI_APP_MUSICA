package com.miappmusica.player.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miappmusica.player.domain.model.DarkMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)

        SectionTitle("Apariencia")
        SwitchRow(
            title = "Color dinámico (Material You)",
            subtitle = "Usa los colores del fondo de pantalla (Android 12+)",
            checked = settings.dynamicColor,
            onCheckedChange = viewModel::setDynamicColor
        )

        Text("Tema", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        DarkMode.entries.forEach { mode ->
            Row(
                Modifier.fillMaxWidth()
                    .selectable(
                        selected = settings.darkMode == mode,
                        onClick = { viewModel.setDarkMode(mode) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = settings.darkMode == mode, onClick = { viewModel.setDarkMode(mode) })
                Spacer(Modifier.width(8.dp))
                Text(
                    when (mode) {
                        DarkMode.SYSTEM -> "Según el sistema"
                        DarkMode.LIGHT -> "Claro"
                        DarkMode.DARK -> "Oscuro"
                    }
                )
            }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Inicio (Modo Normal)")
        SwitchRow(
            title = "Inicio estilo Samsung Music",
            subtitle = "Listas con portada, tarjetas destacadas e índice A-Z",
            checked = settings.samsungHome,
            onCheckedChange = viewModel::setSamsungHome
        )

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Color del reproductor")
        Text(
            "Elige el color de acento para el reproductor y la app.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        AccentColorPicker(
            selected = settings.accentColorArgb,
            onSelect = viewModel::setAccentColor
        )

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Letras")
        SwitchRow(
            title = "Mostrar letras",
            subtitle = "Activa el botón de letras en el reproductor",
            checked = settings.lyricsEnabled,
            onCheckedChange = viewModel::setLyricsEnabled
        )
        SwitchRow(
            title = "Solo letras descargadas (offline)",
            subtitle = "Si se activa, no busca en línea; usa únicamente las letras que descargaste",
            checked = settings.lyricsOfflineOnly,
            onCheckedChange = viewModel::setLyricsOfflineOnly
        )

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Metadatos")
        SwitchRow(
            title = "Buscar portada en línea (modo automático)",
            subtitle = "Descarga carátula y datos canónicos desde internet",
            checked = settings.autoArtworkOnline,
            onCheckedChange = viewModel::setAutoArtwork
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

private val AccentPalette: List<Long> = listOf(
    0L,            // Predeterminado (rendered with the brand purple)
    0xFFFF5252,
    0xFF2962FF,
    0xFF00BFA5,
    0xFFFF6D00,
    0xFFEC407A,
    0xFF00ACC1,
    0xFFFFB300,
    0xFF66BB6A,
    0xFFAB47BC
)

private const val BrandPurpleArgb: Long = 0xFF7C4DFF

@Composable
private fun AccentColorPicker(
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccentPalette.forEach { argb ->
            val displayArgb = if (argb == 0L) BrandPurpleArgb else argb
            val isSelected = selected == argb
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(displayArgb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Seleccionado",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

package com.miappmusica.player.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Column(modifier.fillMaxSize().padding(16.dp)) {
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

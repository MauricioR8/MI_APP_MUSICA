package com.miappmusica.player.feature.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miappmusica.player.domain.model.DarkMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onOpenMetadata: () -> Unit = {},
    onOpenModes: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Import / Export document pickers
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.import(it.toString()) } }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> uri?.let { viewModel.export(it.toString()) } }

    // Audio read permission
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var audioGranted by remember { mutableStateOf(false) }
    var allFilesGranted by remember { mutableStateOf(false) }

    fun recheck() {
        audioGranted = ContextCompat.checkSelfPermission(context, audioPermission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        allFilesGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.os.Environment.isExternalStorageManager()
        } else {
            audioGranted
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> audioGranted = granted; recheck() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) recheck()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        recheck()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

        Text(
            "Color de fondo",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            "Fondo del reproductor a pantalla completa.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        BackgroundColorPicker(
            selected = settings.playerBackgroundArgb,
            onSelect = viewModel::setPlayerBackground
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
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenMetadata).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CleaningServices,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Herramienta de limpieza de metadatos", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Revisa y corrige títulos, artistas y carátulas de tu biblioteca",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Modos")
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenModes).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Gestionar modos", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Crea, edita o elimina modos. Cada modo guarda su propia configuración",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Importar / Exportar")
        Text(
            "Importa listas (.m3u / .json) o exporta tu biblioteca y listas a una carpeta.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Importar")
            }
            OutlinedButton(
                onClick = { exportLauncher.launch(null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Upload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportar")
            }
        }
        message?.let { msg ->
            Text(
                msg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Divider(Modifier.padding(vertical = 12.dp))

        SectionTitle("Permisos")
        Text(
            "Concede acceso a tus archivos de música para reproducir, editar etiquetas y eliminar canciones.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Acceso a la música", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Permite leer las canciones de tu dispositivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (audioGranted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Concedido",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Button(onClick = { permLauncher.launch(audioPermission) }) { Text("Conceder") }
        }

        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Acceso a todos los archivos (editar/eliminar)", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Permite editar etiquetas y eliminar canciones sin pedir confirmación cada vez",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (allFilesGranted) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Concedido",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runCatching {
                        context.startActivity(
                            Intent(
                                android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                android.net.Uri.parse("package:" + context.packageName)
                            )
                        )
                    }.onFailure {
                        context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else {
                    context.startActivity(
                        Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.parse("package:" + context.packageName)
                        )
                    )
                }
            }) { Text("Abrir ajustes") }
        }

        Row(
            Modifier.fillMaxWidth().clickable {
                context.startActivity(
                    Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + context.packageName)
                    )
                )
            }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Abrir información de la app", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Gestiona todos los permisos desde los ajustes del sistema",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
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

private val PlayerBackgroundPalette: List<Long> = listOf(
    0L,            // Predeterminado (usa el fondo del tema)
    0xFF000000,
    0xFF101014,
    0xFF1B1430,
    0xFF0D1B2A,
    0xFF1A1A2E,
    0xFF2C1810,
    0xFFFFFFFF
)

@Composable
private fun BackgroundColorPicker(
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerBackgroundPalette.forEach { argb ->
            val isSelected = selected == argb
            val isDefault = argb == 0L
            // Default swatch shows the theme background so the user knows it means "no custom color".
            val swatchColor = if (isDefault) MaterialTheme.colorScheme.surfaceVariant else Color(argb)
            // Pick a contrasting check tint for very light backgrounds.
            val checkTint = if (argb == 0xFFFFFFFF || isDefault) {
                MaterialTheme.colorScheme.onSurface
            } else {
                Color.White
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isSelected -> Icon(
                        Icons.Filled.Check,
                        contentDescription = "Seleccionado",
                        tint = checkTint,
                        modifier = Modifier.size(20.dp)
                    )
                    isDefault -> Text(
                        "Auto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

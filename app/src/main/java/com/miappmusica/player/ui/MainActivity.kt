package com.miappmusica.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miappmusica.player.feature.modes.ModesViewModel
import com.miappmusica.player.feature.modes.accentColor
import com.miappmusica.player.feature.settings.SettingsViewModel
import com.miappmusica.player.domain.model.DarkMode
import com.miappmusica.player.ui.theme.MiAppMusicaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val modesViewModel: ModesViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val modesState by modesViewModel.state.collectAsStateWithLifecycle()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            val accent: Color? = modesState.activeMode
                .takeIf { !it.isNormal }
                ?.accentColor()

            val userAccent: Color? = settings.accentColorArgb.takeIf { it != 0L }?.let { Color(it) }

            val darkTheme = when (settings.darkMode) {
                DarkMode.SYSTEM -> isSystemInDarkTheme()
                DarkMode.LIGHT -> false
                DarkMode.DARK -> true
            }

            MiAppMusicaTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor && settings.accentColorArgb == 0L,
                modeAccent = accent,
                userAccent = userAccent
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AudioPermissionGate {
                        AppRoot(modesViewModel = modesViewModel)
                    }
                }
            }
        }
    }
}

private val audioPermission: String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@Composable
private fun AudioPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> granted = isGranted }

    if (granted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MI APP MUSICA necesita acceso a tu música para construir la biblioteca.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = { launcher.launch(audioPermission) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Conceder acceso")
            }
        }
    }
}

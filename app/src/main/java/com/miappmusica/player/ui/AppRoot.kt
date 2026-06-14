package com.miappmusica.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.miappmusica.player.feature.library.LibraryScreen
import com.miappmusica.player.feature.metadata.MetadataScreen
import com.miappmusica.player.feature.modes.ModeBar
import com.miappmusica.player.feature.modes.ModesViewModel
import com.miappmusica.player.feature.playlists.PlaylistsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Biblioteca", Icons.Filled.LibraryMusic),
    PLAYLISTS("playlists", "Listas", Icons.Filled.QueueMusic),
    DOWNLOADS("metadata", "Descargados", Icons.Filled.Download)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(modesViewModel: ModesViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val modesState by modesViewModel.state.collectAsStateWithLifecycle()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                ModeBar(
                    state = modesState,
                    onActivate = modesViewModel::activate,
                    onExit = modesViewModel::exitToNormal
                )
                TopAppBar(
                    title = { Text("MI APP MUSICA") },
                    actions = {
                        // The "lupa" entry point to the smart metadata tool.
                        IconButton(onClick = { navController.navigateSingleTop(Dest.DOWNLOADS.route) }) {
                            Icon(Icons.Filled.Search, contentDescription = "Herramienta de metadatos")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                Dest.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { navController.navigateSingleTop(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.LIBRARY.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.LIBRARY.route) { LibraryScreen() }
            composable(Dest.PLAYLISTS.route) { PlaylistsScreen() }
            composable(Dest.DOWNLOADS.route) { MetadataScreen() }
        }
    }
}

private fun androidx.navigation.NavController.navigateSingleTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

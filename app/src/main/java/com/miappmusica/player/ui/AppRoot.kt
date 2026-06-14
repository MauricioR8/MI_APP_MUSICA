package com.miappmusica.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miappmusica.player.feature.library.LibraryScreen
import com.miappmusica.player.feature.metadata.MetadataScreen
import com.miappmusica.player.feature.modes.ModeBar
import com.miappmusica.player.feature.modes.ModesViewModel
import com.miappmusica.player.feature.player.MiniPlayer
import com.miappmusica.player.feature.player.NowPlayingScreen
import com.miappmusica.player.feature.player.PlayerViewModel
import com.miappmusica.player.feature.playlists.PlaylistDetailScreen
import com.miappmusica.player.feature.playlists.PlaylistsScreen
import com.miappmusica.player.feature.settings.SettingsScreen

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    LIBRARY("library", "Biblioteca", Icons.Filled.LibraryMusic),
    PLAYLISTS("playlists", "Listas", Icons.Filled.QueueMusic),
    DOWNLOADS("metadata", "Descargados", Icons.Filled.Download)
}

private const val ROUTE_SETTINGS = "settings"
const val ROUTE_PLAYLIST_DETAIL = "playlist/{playlistId}"
fun playlistDetailRoute(id: Long) = "playlist/$id"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    modesViewModel: ModesViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val modesState by modesViewModel.state.collectAsStateWithLifecycle()
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()

    var showPlayer by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    Box(Modifier.fillMaxSize()) {
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
                            IconButton(onClick = { playerViewModel.refreshLibrary() }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refrescar biblioteca")
                            }
                            IconButton(onClick = { navController.navigateSingleTop(Dest.DOWNLOADS.route) }) {
                                Icon(Icons.Filled.Search, contentDescription = "Herramienta de metadatos")
                            }
                            IconButton(onClick = { navController.navigateSingleTop(ROUTE_SETTINGS) }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                Column {
                    MiniPlayer(
                        state = nowPlaying,
                        onTogglePlay = playerViewModel::togglePlayPause,
                        onNext = playerViewModel::next,
                        onPrevious = playerViewModel::previous,
                        onExpand = { showPlayer = true }
                    )
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
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Dest.PLAYLISTS.route,
                modifier = Modifier.padding(padding)
            ) {
                composable(Dest.LIBRARY.route) { LibraryScreen() }
                composable(Dest.PLAYLISTS.route) {
                    PlaylistsScreen(onOpenPlaylist = { id -> navController.navigate(playlistDetailRoute(id)) })
                }
                composable(Dest.DOWNLOADS.route) { MetadataScreen() }
                composable(ROUTE_SETTINGS) { SettingsScreen() }
                composable(
                    route = ROUTE_PLAYLIST_DETAIL,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("playlistId") ?: 0L
                    PlaylistDetailScreen(playlistId = id, onBack = { navController.popBackStack() })
                }
            }
        }

        // Full-screen Now Playing overlay
        AnimatedVisibility(
            visible = showPlayer && nowPlaying.hasItem,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NowPlayingScreen(
                    state = nowPlaying,
                    onTogglePlay = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onSeek = playerViewModel::seekTo,
                    onCollapse = { showPlayer = false }
                )
            }
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

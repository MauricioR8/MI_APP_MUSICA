package com.miappmusica.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.miappmusica.player.feature.library.LibraryScreen
import com.miappmusica.player.feature.metadata.MetadataScreen
import com.miappmusica.player.feature.modes.ModeBar
import com.miappmusica.player.feature.modes.ModesManagerScreen
import com.miappmusica.player.feature.modes.ModesViewModel
import com.miappmusica.player.feature.player.MiniPlayer
import com.miappmusica.player.feature.player.NowPlayingScreen
import com.miappmusica.player.feature.player.PlayerViewModel
import com.miappmusica.player.feature.playlists.PlaylistDetailScreen
import com.miappmusica.player.feature.playlists.PlaylistsScreen
import com.miappmusica.player.feature.search.SearchScreen
import com.miappmusica.player.feature.settings.SettingsScreen
import com.miappmusica.player.feature.tracklist.TrackListScreen

private const val ROUTE_PLAYLISTS = "playlists"
private const val ROUTE_LIBRARY = "library"
private const val ROUTE_METADATA = "metadata"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_SEARCH = "search"
private const val ROUTE_MODES = "modes"
const val ROUTE_PLAYLIST_DETAIL = "playlist/{playlistId}"
fun playlistDetailRoute(id: Long) = "playlist/$id"
const val ROUTE_TRACK_LIST = "tracklist/{type}"
fun trackListRoute(type: String) = "tracklist/$type"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    modesViewModel: ModesViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val modesState by modesViewModel.state.collectAsStateWithLifecycle()
    val nowPlaying by playerViewModel.nowPlaying.collectAsStateWithLifecycle()
    val currentTrack by playerViewModel.currentTrack.collectAsStateWithLifecycle()
    val lyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsDownloaded by playerViewModel.isLyricsDownloaded.collectAsStateWithLifecycle()
    val isFavorite by playerViewModel.isCurrentFavorite.collectAsStateWithLifecycle()
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    val playlists by playerViewModel.playlists.collectAsStateWithLifecycle()

    var showPlayer by remember { mutableStateOf(false) }

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
                            IconButton(onClick = { navController.navigate(ROUTE_SEARCH) }) {
                                Icon(Icons.Filled.Search, contentDescription = "Buscar canción")
                            }
                            IconButton(onClick = { navController.navigate(ROUTE_SETTINGS) }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Ajustes")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                MiniPlayer(
                    state = nowPlaying,
                    onTogglePlay = playerViewModel::togglePlayPause,
                    onNext = playerViewModel::next,
                    onPrevious = playerViewModel::previous,
                    onExpand = { showPlayer = true }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = ROUTE_PLAYLISTS,
                modifier = Modifier.padding(padding)
            ) {
                composable(ROUTE_PLAYLISTS) {
                    PlaylistsScreen(
                        onOpenPlaylist = { id -> navController.navigate(playlistDetailRoute(id)) },
                        onOpenLibrary = { navController.navigate(ROUTE_LIBRARY) },
                        onOpenTrackList = { type -> navController.navigate(trackListRoute(type)) }
                    )
                }
                composable(ROUTE_LIBRARY) {
                    LibraryScreen(onBack = { navController.popBackStack() })
                }
                composable(ROUTE_SEARCH) {
                    SearchScreen(onBack = { navController.popBackStack() })
                }
                composable(ROUTE_METADATA) { MetadataScreen() }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        onOpenMetadata = { navController.navigate(ROUTE_METADATA) },
                        onOpenModes = { navController.navigate(ROUTE_MODES) }
                    )
                }
                composable(ROUTE_MODES) {
                    ModesManagerScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = ROUTE_TRACK_LIST,
                    arguments = listOf(navArgument("type") { type = NavType.StringType })
                ) { entry ->
                    val type = entry.arguments?.getString("type") ?: "recently_added"
                    TrackListScreen(type = type, onBack = { navController.popBackStack() })
                }
                composable(
                    route = ROUTE_PLAYLIST_DETAIL,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { entry ->
                    val id = entry.arguments?.getLong("playlistId") ?: 0L
                    PlaylistDetailScreen(playlistId = id, onBack = { navController.popBackStack() })
                }
            }
        }

        // Closing the full player with the back gesture should not exit the app.
        BackHandler(enabled = showPlayer) { showPlayer = false }

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
                    onCollapse = { showPlayer = false },
                    currentTrack = currentTrack,
                    lyrics = lyrics,
                    lyricsDownloaded = lyricsDownloaded,
                    onLoadLyrics = playerViewModel::loadLyrics,
                    onDownloadLyrics = playerViewModel::downloadLyrics,
                    isFavorite = isFavorite,
                    onToggleFavorite = playerViewModel::toggleFavorite,
                    queue = queue,
                    onPlayQueueIndex = playerViewModel::playQueueIndex,
                    playlists = playlists,
                    onAddToPlaylist = playerViewModel::addCurrentToPlaylist,
                    onCreatePlaylist = playerViewModel::createPlaylistWithCurrent
                )
            }
        }
    }
}

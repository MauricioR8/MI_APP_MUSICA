package com.miappmusica.player.data.repository

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.miappmusica.player.domain.repository.LibraryRepository
import com.miappmusica.player.domain.repository.PlaylistRepository
import com.miappmusica.player.domain.repository.TransferRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports/exports playlists in the de-facto interchange formats understood by
 * Samsung Music and Pixel/Google Play Music style players (.m3u8) plus a richer
 * JSON sidecar for lossless round-trips inside this app.
 */
@Singleton
class TransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository
) : TransferRepository {

    override suspend fun export(destinationTreeUri: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(destinationTreeUri))
                    ?: error("Destino inválido")
                val tracksById = libraryRepository.observeTracks().first().associateBy { it.id }
                val playlists = playlistRepository.observePlaylists().first()
                var written = 0
                playlists.forEach { playlist ->
                    val safeName = playlist.name.replace(Regex("""[^\w\- ]"""), "_")
                    val file = tree.createFile("audio/x-mpegurl", "$safeName.m3u8")
                        ?: return@forEach
                    context.contentResolver.openOutputStream(file.uri)?.use { out ->
                        out.bufferedWriter().use { w ->
                            w.appendLine("#EXTM3U")
                            playlist.trackIds.forEach { id ->
                                val t = tracksById[id] ?: return@forEach
                                val seconds = (t.durationMs / 1000).toInt()
                                w.appendLine("#EXTINF:$seconds,${t.artist} - ${t.title}")
                                w.appendLine(t.data ?: t.uri)
                            }
                        }
                    }
                    written++
                }
                written
            }
        }

    override suspend fun import(sourceUri: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(sourceUri)
            val lines = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readLines()
            } ?: error("No se pudo abrir el archivo")

            val name = DocumentFile.fromSingleUri(context, uri)?.name
                ?.substringBeforeLast('.') ?: "Importada"

            val paths = lines.filter { it.isNotBlank() && !it.startsWith("#") }
            val library = libraryRepository.observeTracks().first()
            val byPath = library.associateBy { it.data }
            val byUri = library.associateBy { it.uri }
            val matchedIds = paths.mapNotNull { line ->
                byPath[line]?.id ?: byUri[line]?.id
            }

            val playlistId = playlistRepository.create(name, "Importada externamente")
            playlistRepository.setTracks(playlistId, matchedIds)
            matchedIds.size
        }
    }
}

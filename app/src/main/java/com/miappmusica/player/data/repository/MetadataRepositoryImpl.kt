package com.miappmusica.player.data.repository

import android.content.Context
import android.net.Uri
import com.miappmusica.player.data.mediastore.MediaStoreSource
import com.miappmusica.player.data.metadata.ArtworkProvider
import com.miappmusica.player.data.metadata.MetadataFormatter
import com.miappmusica.player.data.metadata.TagIo
import com.miappmusica.player.data.prefs.UserPreferences
import com.miappmusica.player.domain.model.MetadataDiff
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.model.TrackMetadata
import com.miappmusica.player.domain.repository.MetadataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetadataRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tagIo: TagIo,
    private val formatter: MetadataFormatter,
    private val artworkProvider: ArtworkProvider,
    private val mediaStore: MediaStoreSource,
    private val httpClient: OkHttpClient,
    private val userPreferences: UserPreferences
) : MetadataRepository {

    override suspend fun read(track: Track): TrackMetadata {
        val path = track.data
        return if (path != null) {
            runCatching { tagIo.read(path) }.getOrElse { TrackMetadata.from(track) }
        } else {
            TrackMetadata.from(track)
        }
    }

    override suspend fun buildAutoProposal(track: Track): MetadataDiff {
        val original = read(track)

        // 1) Local heuristic cleanup from the (often dirty) title field.
        val (splitArtist, splitTitle) = formatter.splitArtistTitle(original.title)
        val baseArtist = original.artist.ifBlank { splitArtist.orEmpty() }
        val cleanedArtist = formatter.cleanString(baseArtist)
        val cleanedTitle = formatter.cleanString(splitTitle)

        // 2) Online enrichment (canonical names + high-res artwork), if enabled in Settings.
        //    Wrapped in try/catch so network failures degrade gracefully to local-only heuristics.
        val online = userPreferences.settings.first().autoArtworkOnline
        val lookup = if (online) {
            try {
                artworkProvider.lookup(cleanedArtist, cleanedTitle)
            } catch (_: Exception) {
                null
            }
        } else null

        val proposed = TrackMetadata(
            title = cleanedTitle,
            artist = lookup?.canonicalArtist?.takeIf { it.isNotBlank() } ?: cleanedArtist,
            album = lookup?.canonicalAlbum?.takeIf { it.isNotBlank() } ?: original.album,
            albumArtist = lookup?.canonicalArtist?.takeIf { it.isNotBlank() } ?: cleanedArtist,
            trackNumber = original.trackNumber,
            year = lookup?.year ?: original.year,
            genre = lookup?.genre?.takeIf { it.isNotBlank() } ?: original.genre,
            artworkSource = original.artworkSource
        )

        return MetadataDiff(
            trackId = track.id,
            trackUri = track.uri,
            original = original,
            proposed = proposed,
            proposedArtworkUrl = lookup?.artworkUrl
        )
    }

    override suspend fun apply(diff: MetadataDiff): Result<Unit> {
        if (!diff.accepted) return Result.success(Unit)
        return withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(diff.trackUri)
                val ext = (pathFor(diff)?.substringAfterLast('.', "mp3") ?: "mp3").take(5)
                val tmp = java.io.File(context.cacheDir, "edit_${diff.trackId}.$ext")
                // 1) copy original -> cache
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { input.copyTo(it) }
                } ?: error("No se pudo leer el archivo de audio")
                // 2) edit cache copy (always writable)
                val artworkBytes = resolveArtworkBytes(diff)
                tagIo.write(tmp.absolutePath, diff.proposed, artworkBytes).getOrThrow()
                // 3) write cache copy back through the resolver (needs the write grant on API 29+)
                context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    tmp.inputStream().use { it.copyTo(out) }
                } ?: error("No se pudo escribir en el archivo (permiso denegado)")
                tmp.delete()
                mediaStore.notifyChanged(uri)
            }
        }
    }

    override fun buildWriteRequest(diffs: List<MetadataDiff>): android.content.IntentSender? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null
        val uris = diffs.filter { it.accepted }.map { Uri.parse(it.trackUri) }
        if (uris.isEmpty()) return null
        return android.provider.MediaStore.createWriteRequest(context.contentResolver, uris).intentSender
    }

    private fun pathFor(diff: MetadataDiff): String? {
        // The proposed/original metadata don't carry the data path; resolve via MediaStore uri.
        return runCatching {
            context.contentResolver.query(
                Uri.parse(diff.trackUri),
                arrayOf(android.provider.MediaStore.Audio.Media.DATA),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
    }

    private suspend fun resolveArtworkBytes(diff: MetadataDiff): ByteArray? = withContext(Dispatchers.IO) {
        // Priority: explicit gallery selection (content://) > online artwork URL.
        val source = diff.proposed.artworkSource
        when {
            source != null && source.startsWith("content://") -> runCatching {
                context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
            }.getOrNull()

            diff.proposedArtworkUrl != null -> runCatching {
                val request = Request.Builder().url(diff.proposedArtworkUrl).build()
                httpClient.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.bytes() else null
                }
            }.getOrNull()

            else -> null
        }
    }
}

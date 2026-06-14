package com.miappmusica.player.data.repository

import android.content.Context
import android.net.Uri
import com.miappmusica.player.data.mediastore.MediaStoreSource
import com.miappmusica.player.data.metadata.ArtworkProvider
import com.miappmusica.player.data.metadata.MetadataFormatter
import com.miappmusica.player.data.metadata.TagIo
import com.miappmusica.player.domain.model.MetadataDiff
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.model.TrackMetadata
import com.miappmusica.player.domain.repository.MetadataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    private val httpClient: OkHttpClient
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

        // 2) Online enrichment (canonical names + high-res artwork).
        val lookup = artworkProvider.lookup(cleanedArtist, cleanedTitle)

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
        val path = pathFor(diff) ?: return Result.failure(
            IllegalStateException("No se encontró la ruta del archivo para ${diff.trackUri}")
        )

        val artworkBytes = resolveArtworkBytes(diff)
        val result = tagIo.write(path, diff.proposed, artworkBytes)
        if (result.isSuccess) {
            mediaStore.notifyChanged(Uri.parse(diff.trackUri))
        }
        return result
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

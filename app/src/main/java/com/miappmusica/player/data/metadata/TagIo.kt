package com.miappmusica.player.data.metadata

import com.miappmusica.player.domain.model.TrackMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes ID3 tags on the physical file using jaudiotagger.
 *
 * NOTE on scoped storage (API 29+): in-place edits require write access to the file. The
 * app obtains it for files it owns or via MediaStore `createWriteRequest`; the caller is
 * responsible for having secured that grant before invoking [write].
 */
@Singleton
class TagIo @Inject constructor() {

    init {
        // jaudiotagger is extremely chatty; silence it on device.
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    suspend fun read(path: String): TrackMetadata = withContext(Dispatchers.IO) {
        val file = File(path)
        val audio = AudioFileIO.read(file)
        val tag = audio.tag
        if (tag == null) {
            TrackMetadata(title = file.nameWithoutExtension, artist = "", album = "")
        } else {
            TrackMetadata(
                title = tag.getFirst(FieldKey.TITLE).orEmpty().ifBlank { file.nameWithoutExtension },
                artist = tag.getFirst(FieldKey.ARTIST).orEmpty(),
                album = tag.getFirst(FieldKey.ALBUM).orEmpty(),
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST).orEmpty(),
                trackNumber = tag.getFirst(FieldKey.TRACK).toIntOrNull() ?: 0,
                year = tag.getFirst(FieldKey.YEAR).take(4).toIntOrNull() ?: 0,
                genre = tag.getFirst(FieldKey.GENRE).orEmpty()
            )
        }
    }

    /**
     * Writes [metadata] to the file at [path]. When [artworkBytes] is provided the embedded
     * cover art is replaced. Returns Result so the caller can surface failures in the UI.
     */
    suspend fun write(
        path: String,
        metadata: TrackMetadata,
        artworkBytes: ByteArray? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            require(file.canWrite()) { "Sin permiso de escritura: $path" }

            val audio = AudioFileIO.read(file)
            val tag = audio.tagOrCreateAndSetDefault

            fun setIfPresent(key: FieldKey, value: String) {
                if (value.isNotBlank()) tag.setField(key, value)
            }

            setIfPresent(FieldKey.TITLE, metadata.title)
            setIfPresent(FieldKey.ARTIST, metadata.artist)
            setIfPresent(FieldKey.ALBUM, metadata.album)
            setIfPresent(FieldKey.ALBUM_ARTIST, metadata.albumArtist)
            setIfPresent(FieldKey.GENRE, metadata.genre)
            if (metadata.trackNumber > 0) tag.setField(FieldKey.TRACK, metadata.trackNumber.toString())
            if (metadata.year > 0) tag.setField(FieldKey.YEAR, metadata.year.toString())

            if (artworkBytes != null && artworkBytes.isNotEmpty()) {
                // ArtworkFactory.getNew() returns the platform-appropriate Artwork impl.
                // We only set raw bytes/metadata and never invoke the AWT/ImageIO-backed
                // helpers, so this stays Android-safe.
                val artwork = ArtworkFactory.getNew().apply {
                    binaryData = artworkBytes
                    mimeType = "image/jpeg"
                    description = ""
                    pictureType = 3 // Cover (front)
                }
                tag.deleteArtworkField()
                tag.setField(artwork)
            }

            audio.commit()
        }
    }
}

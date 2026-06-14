package com.miappmusica.player.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.miappmusica.player.domain.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Reads audio tracks from MediaStore. This is the canonical on-device library. */
@Singleton
class MediaStoreSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val albumArtBase: Uri = Uri.parse("content://media/external/audio/albumart")

    suspend fun queryTracks(): List<Track> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val result = ArrayList<Track>()
        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val relPathCol = c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val albumId = c.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val artworkUri = ContentUris.withAppendedId(albumArtBase, albumId)
                val relPath = if (relPathCol >= 0) c.getString(relPathCol).orEmpty() else ""
                val data = c.getString(dataCol)

                result += Track(
                    id = id,
                    uri = contentUri.toString(),
                    data = data,
                    title = c.getString(titleCol).orEmpty(),
                    artist = c.getString(artistCol).orEmpty(),
                    album = c.getString(albumCol).orEmpty(),
                    albumId = albumId,
                    durationMs = c.getLong(durationCol),
                    trackNumber = c.getInt(trackCol) % 1000,
                    year = c.getInt(yearCol),
                    artworkUri = artworkUri.toString(),
                    dateAdded = c.getLong(dateCol),
                    isDownloaded = isDownloadPath(relPath, data)
                )
            }
        }
        result
    }

    private fun isDownloadPath(relativePath: String, data: String?): Boolean {
        val haystack = (relativePath.ifBlank { data.orEmpty() }).lowercase()
        return haystack.contains("download") || haystack.contains("telegram") ||
            haystack.contains("whatsapp")
    }

    /** Notifies MediaStore that a file changed so its cached metadata is refreshed. */
    fun notifyChanged(uri: Uri) {
        runCatching { context.contentResolver.notifyChange(uri, null) }
    }

    /**
     * Builds a system delete-confirmation request (API 30+). The OS shows a dialog and performs
     * the deletion itself when the user approves. Returns null on API < 30 or for an empty list.
     */
    fun buildDeleteRequest(uris: List<Uri>): android.content.IntentSender? {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return null
        if (uris.isEmpty()) return null
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    /** Direct deletion path for API < 30. Returns the number of rows deleted. */
    suspend fun deleteDirect(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        var n = 0
        uris.forEach { uri ->
            runCatching { context.contentResolver.delete(uri, null, null) }.onSuccess { n += it }
        }
        n
    }
}

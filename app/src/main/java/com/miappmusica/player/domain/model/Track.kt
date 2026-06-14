package com.miappmusica.player.domain.model

/**
 * A single audio track in the user's library.
 *
 * @param uri content:// or file:// URI string used by the player and the tag editor.
 * @param data absolute file-system path when known (needed by jaudiotagger for in-place edits).
 */
data class Track(
    val id: Long,
    val uri: String,
    val data: String?,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String? = null,
    val artworkUri: String? = null,
    val dateAdded: Long = 0L,
    val isDownloaded: Boolean = false
) {
    val displayArtist: String get() = artist.ifBlank { "Artista desconocido" }
    val displayAlbum: String get() = album.ifBlank { "Álbum desconocido" }
}

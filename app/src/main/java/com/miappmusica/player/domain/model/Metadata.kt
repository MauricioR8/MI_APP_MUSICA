package com.miappmusica.player.domain.model

/**
 * The editable ID3 fields of a track. [artworkSource] points at either a remote URL,
 * a local gallery `content://` URI, or null when unchanged.
 */
data class TrackMetadata(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = "",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String = "",
    val artworkSource: String? = null
) {
    companion object {
        fun from(track: Track): TrackMetadata = TrackMetadata(
            title = track.title,
            artist = track.artist,
            album = track.album,
            albumArtist = track.artist,
            trackNumber = track.trackNumber,
            year = track.year,
            genre = track.genre.orEmpty(),
            artworkSource = track.artworkUri
        )
    }
}

/** Result of analyzing a selection to decide single-artist vs. mixed. */
data class ContextDetection(
    val isSingleArtist: Boolean,
    val isSingleAlbum: Boolean,
    val dominantArtist: String?,
    val dominantAlbum: String?,
    /** 0f..1f confidence of the dominant-artist hypothesis. */
    val confidence: Float
) {
    val summary: String
        get() = when {
            isSingleAlbum && dominantAlbum != null ->
                "Álbum detectado: \"$dominantAlbum\" de ${dominantArtist ?: "?"}"
            isSingleArtist && dominantArtist != null ->
                "Artista único detectado: $dominantArtist"
            else -> "Mezcla de varios artistas"
        }
}

/** A single before/after field change shown in the diff preview. */
data class FieldChange(
    val field: MetadataField,
    val before: String,
    val after: String
) {
    val changed: Boolean get() = before.trim() != after.trim()
}

enum class MetadataField(val displayName: String) {
    TITLE("Título"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    ALBUM_ARTIST("Artista del álbum"),
    TRACK_NUMBER("Pista #"),
    YEAR("Año"),
    GENRE("Género"),
    ARTWORK("Portada")
}

/**
 * The per-track diff produced by the engine and rendered in the comparison screen.
 * The user may override [proposed] inline before applying, or toggle [accepted] off.
 */
data class MetadataDiff(
    val trackId: Long,
    val trackUri: String,
    val original: TrackMetadata,
    val proposed: TrackMetadata,
    val accepted: Boolean = true,
    val proposedArtworkUrl: String? = null
) {
    val fieldChanges: List<FieldChange> = buildList {
        add(FieldChange(MetadataField.TITLE, original.title, proposed.title))
        add(FieldChange(MetadataField.ARTIST, original.artist, proposed.artist))
        add(FieldChange(MetadataField.ALBUM, original.album, proposed.album))
        add(FieldChange(MetadataField.ALBUM_ARTIST, original.albumArtist, proposed.albumArtist))
        add(FieldChange(MetadataField.TRACK_NUMBER, original.trackNumber.toString(), proposed.trackNumber.toString()))
        add(FieldChange(MetadataField.YEAR, original.year.toString(), proposed.year.toString()))
        add(FieldChange(MetadataField.GENRE, original.genre, proposed.genre))
        if (proposedArtworkUrl != null) {
            add(FieldChange(MetadataField.ARTWORK, original.artworkSource ?: "—", "Nueva portada"))
        }
    }

    val hasChanges: Boolean get() = fieldChanges.any { it.changed }
    val changedCount: Int get() = fieldChanges.count { it.changed }
}

/** Scope of a batch metadata operation. */
enum class BatchScope { SINGLE, ALBUM, PLAYLIST, SELECTION }

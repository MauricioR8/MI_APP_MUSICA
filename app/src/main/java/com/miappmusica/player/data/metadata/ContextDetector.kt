package com.miappmusica.player.data.metadata

import com.miappmusica.player.domain.model.ContextDetection
import com.miappmusica.player.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decides whether a selection of audios belongs to a single artist / single album
 * (e.g. a full Linkin Park or Three Days Grace album) or is a random mix.
 * Drives whether the Auto engine fills the album field across the whole batch.
 */
@Singleton
class ContextDetector @Inject constructor() {

    fun analyze(tracks: List<Track>): ContextDetection {
        if (tracks.isEmpty()) {
            return ContextDetection(false, false, null, null, 0f)
        }

        val artistCounts = tracks
            .map { normalize(it.artist) }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

        val albumCounts = tracks
            .map { normalize(it.album) }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

        val dominantArtistEntry = artistCounts.maxByOrNull { it.value }
        val dominantAlbumEntry = albumCounts.maxByOrNull { it.value }

        val total = tracks.size.toFloat()
        val artistConfidence = (dominantArtistEntry?.value ?: 0) / total
        val albumConfidence = (dominantAlbumEntry?.value ?: 0) / total

        // A selection is "single artist" if >=80% share the dominant artist.
        val isSingleArtist = artistConfidence >= 0.8f
        val isSingleAlbum = albumConfidence >= 0.8f && isSingleArtist

        return ContextDetection(
            isSingleArtist = isSingleArtist,
            isSingleAlbum = isSingleAlbum,
            dominantArtist = dominantArtistEntry?.key?.takeIf { isSingleArtist }
                ?.let { displayCase(it) },
            dominantAlbum = dominantAlbumEntry?.key?.takeIf { isSingleAlbum }
                ?.let { displayCase(it) },
            confidence = artistConfidence
        )
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(Regex("""\s+"""), " ")

    private fun displayCase(value: String): String =
        value.split(" ").joinToString(" ") { w ->
            w.replaceFirstChar { it.uppercase() }
        }
}

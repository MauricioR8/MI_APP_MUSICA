package com.miappmusica.player.domain.repository

import com.miappmusica.player.domain.model.AppMode
import com.miappmusica.player.domain.model.MetadataDiff
import com.miappmusica.player.domain.model.Playlist
import com.miappmusica.player.domain.model.Track
import com.miappmusica.player.domain.model.TrackMetadata
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    /** Streams the full on-device audio library (MediaStore-backed). */
    fun observeTracks(): Flow<List<Track>>
    fun observeDownloads(): Flow<List<Track>>
    suspend fun refresh()
    suspend fun getTrack(id: Long): Track?
    suspend fun getTracks(ids: List<Long>): List<Track>

    /** Builds a system delete-confirmation request (API 30+) for the given track URIs, or null. */
    fun buildDeleteRequest(uris: List<String>): android.content.IntentSender?

    /** Deletes the given track URIs directly (API < 30) and refreshes the library. Returns rows deleted. */
    suspend fun deleteDirect(uris: List<String>): Int
}

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<Playlist>>
    suspend fun create(name: String, description: String = ""): Long
    suspend fun rename(id: Long, name: String)
    suspend fun setCover(id: Long, coverUri: String?)
    suspend fun delete(id: Long)
    suspend fun setTracks(playlistId: Long, trackIds: List<Long>)
    suspend fun addTrack(playlistId: Long, trackId: Long)
    suspend fun removeTrack(playlistId: Long, trackId: Long)
    suspend fun getPlaylist(id: Long): Playlist?
    suspend fun reorder(orderedIds: List<Long>)
}

interface ModeRepository {
    fun observeModes(): Flow<List<AppMode>>
    fun observeActiveMode(): Flow<AppMode>
    suspend fun activate(modeId: String)
    suspend fun upsert(mode: AppMode)
    suspend fun delete(modeId: String)
}

interface MetadataRepository {
    /** Reads current ID3 tags directly from the file. */
    suspend fun read(track: Track): TrackMetadata

    /**
     * Builds an automatic proposal for a track (clean title/artist + remote artwork lookup).
     * Pure analysis: does not touch the file.
     */
    suspend fun buildAutoProposal(track: Track): MetadataDiff

    /** Persists accepted changes to the underlying audio file (and refreshes MediaStore). */
    suspend fun apply(diff: MetadataDiff): Result<Unit>

    /** Builds a MediaStore write-permission request (API 30+) for the accepted diffs, or null if not needed. */
    fun buildWriteRequest(diffs: List<MetadataDiff>): android.content.IntentSender?
}

interface TransferRepository {
    /** Exports the library + playlists to a document tree (e.g. for Samsung Music / Pixel). */
    suspend fun export(destinationTreeUri: String): Result<Int>
    /** Imports playlists from an external source file (.m3u / .json / Samsung backup). */
    suspend fun import(sourceUri: String): Result<Int>
}

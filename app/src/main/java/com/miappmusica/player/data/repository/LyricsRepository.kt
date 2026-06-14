package com.miappmusica.player.data.repository

import com.miappmusica.player.data.local.dao.LyricsDao
import com.miappmusica.player.data.local.entity.LyricsEntity
import com.miappmusica.player.data.prefs.UserPreferences
import com.miappmusica.player.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a lyrics lookup, consumed by the player overlay. */
sealed interface LyricsResult {
    data object Disabled : LyricsResult
    data object Loading : LyricsResult

    /**
     * @param plain plain-text lyrics (always present when found; derived from [synced] if needed).
     * @param synced timestamped LRC lyrics ("[mm:ss.xx] line"), or null when unavailable.
     */
    data class Available(val plain: String, val synced: String?, val fromCache: Boolean) : LyricsResult
    data object NotFound : LyricsResult
    data class Error(val message: String) : LyricsResult
}

/**
 * Fetches song lyrics from the public lrclib.net service (which returns timestamped LRC lyrics)
 * with an offline Room cache. Online lookups are skipped when the user enables "offline only".
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val dao: LyricsDao,
    private val client: OkHttpClient,
    private val userPreferences: UserPreferences
) {

    suspend fun fetch(track: Track): LyricsResult {
        val settings = userPreferences.settings.first()
        if (!settings.lyricsEnabled) return LyricsResult.Disabled

        val cached = dao.get(track.id)
        if (cached != null) {
            return LyricsResult.Available(
                plain = cached.text,
                synced = cached.synced.ifBlank { null },
                fromCache = true
            )
        }

        if (settings.lyricsOfflineOnly) return LyricsResult.NotFound

        return when (val online = fetchOnline(track)) {
            is OnlineResult.Found ->
                LyricsResult.Available(online.plain, online.synced?.ifBlank { null }, fromCache = false)
            OnlineResult.NotFound -> LyricsResult.NotFound
            is OnlineResult.Error -> LyricsResult.Error(online.message)
        }
    }

    /** Downloads and stores lyrics for offline use, regardless of the offline-only flag. */
    suspend fun download(track: Track): Result<Unit> {
        return when (val online = fetchOnline(track)) {
            is OnlineResult.Found -> {
                dao.upsert(
                    LyricsEntity(
                        trackId = track.id,
                        artist = track.artist,
                        title = track.title,
                        text = online.plain,
                        synced = online.synced ?: ""
                    )
                )
                Result.success(Unit)
            }
            OnlineResult.NotFound -> Result.failure(IllegalStateException("No se encontraron letras"))
            is OnlineResult.Error -> Result.failure(IllegalStateException(online.message))
        }
    }

    fun observeDownloaded(trackId: Long): Flow<Boolean> =
        dao.observeDownloaded(trackId).map { it != null }

    suspend fun removeDownload(trackId: Long) = dao.delete(trackId)

    private sealed interface OnlineResult {
        data class Found(val plain: String, val synced: String?) : OnlineResult
        data object NotFound : OnlineResult
        data class Error(val message: String) : OnlineResult
    }

    private suspend fun fetchOnline(track: Track): OnlineResult = withContext(Dispatchers.IO) {
        val artist = track.artist.trim()
        val title = track.title.trim()
        if (artist.isBlank() || title.isBlank()) return@withContext OnlineResult.NotFound

        runCatching {
            // 1) Exact lookup (artist + title + duration when known).
            val exact = lrclibGet(artist, title, track.durationMs)
            if (exact != null) return@runCatching exact

            // 2) Fallback: fuzzy search, take the first match.
            lrclibSearch(artist, title) ?: OnlineResult.NotFound
        }.getOrElse { e -> OnlineResult.Error(e.message ?: "Error desconocido") }
    }

    /** GET https://lrclib.net/api/get?artist_name=&track_name=&duration= */
    private fun lrclibGet(artist: String, title: String, durationMs: Long): OnlineResult? {
        val builder = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("artist_name", artist)
            .addQueryParameter("track_name", title)
        if (durationMs > 0) builder.addQueryParameter("duration", (durationMs / 1000).toString())

        val request = Request.Builder()
            .url(builder.build())
            .header("User-Agent", "MiAppMusica")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            return parseObject(JSONObject(body))
        }
    }

    /** GET https://lrclib.net/api/search?track_name=&artist_name= (returns a JSON array). */
    private fun lrclibSearch(artist: String, title: String): OnlineResult? {
        val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", title)
            .addQueryParameter("artist_name", artist)
            .build()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MiAppMusica")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null
            val array = JSONArray(body)
            if (array.length() == 0) return null
            return parseObject(array.getJSONObject(0))
        }
    }

    private fun parseObject(obj: JSONObject): OnlineResult {
        val syncedRaw = if (obj.isNull("syncedLyrics")) "" else obj.optString("syncedLyrics").trim()
        val plainRaw = if (obj.isNull("plainLyrics")) "" else obj.optString("plainLyrics").trim()
        val plain = when {
            plainRaw.isNotBlank() -> plainRaw
            syncedRaw.isNotBlank() -> stripTimestamps(syncedRaw)
            else -> ""
        }
        return if (plain.isBlank() && syncedRaw.isBlank()) {
            OnlineResult.NotFound
        } else {
            OnlineResult.Found(plain = plain, synced = syncedRaw.ifBlank { null })
        }
    }

    /** Removes "[mm:ss.xx]" timestamp prefixes from an LRC body to derive plain text. */
    private fun stripTimestamps(lrc: String): String =
        lrc.lineSequence()
            .map { it.replace(Regex("""\[\d{1,2}:\d{2}(?:[.:]\d{1,3})?\]"""), "").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
}

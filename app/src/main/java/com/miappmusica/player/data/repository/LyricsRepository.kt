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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** Result of a lyrics lookup, consumed by the player overlay. */
sealed interface LyricsResult {
    data object Disabled : LyricsResult
    data object Loading : LyricsResult
    data class Available(val text: String, val fromCache: Boolean) : LyricsResult
    data object NotFound : LyricsResult
    data class Error(val message: String) : LyricsResult
}

/**
 * Fetches song lyrics from the public api.lyrics.ovh service with an offline Room cache.
 * Online lookups are skipped when the user enables "offline only".
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
        if (cached != null) return LyricsResult.Available(cached.text, fromCache = true)

        if (settings.lyricsOfflineOnly) return LyricsResult.NotFound

        return when (val online = fetchOnline(track)) {
            is OnlineResult.Found -> LyricsResult.Available(online.text, fromCache = false)
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
                        text = online.text
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
        data class Found(val text: String) : OnlineResult
        data object NotFound : OnlineResult
        data class Error(val message: String) : OnlineResult
    }

    private suspend fun fetchOnline(track: Track): OnlineResult = withContext(Dispatchers.IO) {
        val artist = track.artist.trim()
        val title = track.title.trim()
        if (artist.isBlank() || title.isBlank()) return@withContext OnlineResult.NotFound

        val url = "https://api.lyrics.ovh/v1/${encode(artist)}/${encode(title)}"
        val request = Request.Builder().url(url).header("User-Agent", "MiAppMusica/1.0").build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use if (response.code == 404) OnlineResult.NotFound
                    else OnlineResult.Error("Error de red (${response.code})")
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use OnlineResult.NotFound
                val text = JSONObject(body).optString("lyrics").trim()
                if (text.isBlank()) OnlineResult.NotFound else OnlineResult.Found(text)
            }
        }.getOrElse { e -> OnlineResult.Error(e.message ?: "Error desconocido") }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

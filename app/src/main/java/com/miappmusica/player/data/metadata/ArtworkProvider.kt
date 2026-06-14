package com.miappmusica.player.data.metadata

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/** Online lookup of canonical track info + album art via the public iTunes Search API. */
@Singleton
class ArtworkProvider @Inject constructor(
    private val client: OkHttpClient
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(ItunesResponse::class.java)

    data class Lookup(
        val artworkUrl: String?,
        val canonicalArtist: String?,
        val canonicalAlbum: String?,
        val year: Int?,
        val genre: String?
    )

    suspend fun lookup(artist: String, title: String): Lookup? = withContext(Dispatchers.IO) {
        val term = listOf(artist, title).filter { it.isNotBlank() }.joinToString(" ")
        if (term.isBlank()) return@withContext null

        val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("entity", "song")
            .addQueryParameter("limit", "1")
            .build()

        val request = Request.Builder().url(url).header("User-Agent", "MiAppMusica/1.0").build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val parsed = adapter.fromJson(body)
                val item = parsed?.results?.firstOrNull() ?: return@use null
                Lookup(
                    // iTunes returns 100x100; request a high-res variant instead.
                    artworkUrl = item.artworkUrl100?.replace("100x100bb", "600x600bb"),
                    canonicalArtist = item.artistName,
                    canonicalAlbum = item.collectionName,
                    year = item.releaseDate?.take(4)?.toIntOrNull(),
                    genre = item.primaryGenreName
                )
            }
        }.getOrNull()
    }

    @JsonClass(generateAdapter = false)
    data class ItunesResponse(val resultCount: Int = 0, val results: List<ItunesItem> = emptyList())

    @JsonClass(generateAdapter = false)
    data class ItunesItem(
        val artistName: String? = null,
        val collectionName: String? = null,
        val trackName: String? = null,
        val artworkUrl100: String? = null,
        val releaseDate: String? = null,
        val primaryGenreName: String? = null
    )
}

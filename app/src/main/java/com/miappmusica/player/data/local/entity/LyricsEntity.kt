package com.miappmusica.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: Long,
    val artist: String,
    val title: String,
    val text: String,
    /** Timestamped LRC lyrics ("[mm:ss.xx] line"); empty when only plain lyrics are available. */
    val synced: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

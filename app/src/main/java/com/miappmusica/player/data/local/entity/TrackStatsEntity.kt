package com.miappmusica.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Per-track favorite flag + play statistics, used for the smart "home" cards. */
@Entity(tableName = "track_stats")
data class TrackStatsEntity(
    @PrimaryKey val trackId: Long,
    val favorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L
)

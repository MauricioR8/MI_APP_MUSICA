package com.miappmusica.player.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.miappmusica.player.data.local.dao.ModeDao
import com.miappmusica.player.data.local.dao.PlaylistDao
import com.miappmusica.player.data.local.entity.ModeEntity
import com.miappmusica.player.data.local.entity.PlaylistEntity
import com.miappmusica.player.data.local.entity.PlaylistTrackCrossRef

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        ModeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun modeDao(): ModeDao

    companion object {
        const val NAME = "mi_app_musica.db"
    }
}

package com.miappmusica.player.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.miappmusica.player.data.local.AppDatabase
import com.miappmusica.player.data.local.dao.LyricsDao
import com.miappmusica.player.data.local.dao.ModeDao
import com.miappmusica.player.data.local.dao.PlaylistDao
import com.miappmusica.player.data.local.dao.TrackStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS lyrics (" +
                    "trackId INTEGER NOT NULL, artist TEXT NOT NULL, title TEXT NOT NULL, " +
                    "text TEXT NOT NULL, savedAt INTEGER NOT NULL, PRIMARY KEY(trackId))"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS track_stats (" +
                    "trackId INTEGER NOT NULL, favorite INTEGER NOT NULL, playCount INTEGER NOT NULL, " +
                    "lastPlayedAt INTEGER NOT NULL, PRIMARY KEY(trackId))"
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE lyrics ADD COLUMN synced TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE playlists ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun provideModeDao(db: AppDatabase): ModeDao = db.modeDao()

    @Provides
    fun provideLyricsDao(db: AppDatabase): LyricsDao = db.lyricsDao()

    @Provides
    fun provideTrackStatsDao(db: AppDatabase): TrackStatsDao = db.trackStatsDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}

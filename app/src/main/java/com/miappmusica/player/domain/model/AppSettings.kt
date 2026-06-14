package com.miappmusica.player.domain.model

enum class DarkMode { SYSTEM, LIGHT, DARK }

/**
 * User-configurable app settings (persisted in DataStore, edited from the Settings module).
 *
 * @param samsungHome when true the Normal mode opens the Samsung-Music-style playlist home.
 */
data class AppSettings(
    val dynamicColor: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val samsungHome: Boolean = true,
    val autoArtworkOnline: Boolean = true,
    val accentColorArgb: Long = 0L,
    val lyricsEnabled: Boolean = true,
    val lyricsOfflineOnly: Boolean = false
)

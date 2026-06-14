package com.miappmusica.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miappmusica.player.domain.model.AppMode
import com.miappmusica.player.domain.model.AppSettings
import com.miappmusica.player.domain.model.DarkMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activeModeKey = stringPreferencesKey("active_mode_id")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val darkModeKey = stringPreferencesKey("dark_mode")
    private val samsungHomeKey = booleanPreferencesKey("samsung_home")
    private val autoArtworkKey = booleanPreferencesKey("auto_artwork_online")

    val activeModeId: Flow<String> = context.dataStore.data
        .map { it[activeModeKey] ?: AppMode.NORMAL_ID }

    suspend fun setActiveModeId(id: String) {
        context.dataStore.edit { it[activeModeKey] = id }
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            dynamicColor = prefs[dynamicColorKey] ?: true,
            darkMode = prefs[darkModeKey]?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() }
                ?: DarkMode.SYSTEM,
            samsungHome = prefs[samsungHomeKey] ?: true,
            autoArtworkOnline = prefs[autoArtworkKey] ?: true
        )
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[dynamicColorKey] = enabled }
    }

    suspend fun setDarkMode(mode: DarkMode) {
        context.dataStore.edit { it[darkModeKey] = mode.name }
    }

    suspend fun setSamsungHome(enabled: Boolean) {
        context.dataStore.edit { it[samsungHomeKey] = enabled }
    }

    suspend fun setAutoArtworkOnline(enabled: Boolean) {
        context.dataStore.edit { it[autoArtworkKey] = enabled }
    }
}

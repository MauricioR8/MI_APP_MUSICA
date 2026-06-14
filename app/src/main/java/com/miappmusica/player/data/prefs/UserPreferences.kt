package com.miappmusica.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miappmusica.player.domain.model.AppMode
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

    val activeModeId: Flow<String> = context.dataStore.data
        .map { it[activeModeKey] ?: AppMode.NORMAL_ID }

    suspend fun setActiveModeId(id: String) {
        context.dataStore.edit { it[activeModeKey] = id }
    }
}

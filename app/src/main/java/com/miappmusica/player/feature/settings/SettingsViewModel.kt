package com.miappmusica.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.data.prefs.UserPreferences
import com.miappmusica.player.domain.model.AppSettings
import com.miappmusica.player.domain.model.DarkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setDynamicColor(enabled) }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch { prefs.setDarkMode(mode) }
    }

    fun setSamsungHome(enabled: Boolean) {
        viewModelScope.launch { prefs.setSamsungHome(enabled) }
    }

    fun setAutoArtwork(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoArtworkOnline(enabled) }
    }
}

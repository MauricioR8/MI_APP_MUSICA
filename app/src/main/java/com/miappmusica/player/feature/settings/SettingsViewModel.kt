package com.miappmusica.player.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miappmusica.player.data.prefs.UserPreferences
import com.miappmusica.player.domain.model.AppSettings
import com.miappmusica.player.domain.model.DarkMode
import com.miappmusica.player.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val transferRepository: TransferRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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

    fun setAccentColor(argb: Long) {
        viewModelScope.launch { prefs.setAccentColor(argb) }
    }

    fun setPlayerBackground(argb: Long) {
        viewModelScope.launch { prefs.setPlayerBackground(argb) }
    }

    fun setLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setLyricsEnabled(enabled) }
    }

    fun setLyricsOfflineOnly(enabled: Boolean) {
        viewModelScope.launch { prefs.setLyricsOfflineOnly(enabled) }
    }

    fun export(treeUri: String) {
        viewModelScope.launch {
            _message.value = transferRepository.export(treeUri).fold(
                onSuccess = { "Exportadas $it listas" },
                onFailure = { "Error: ${it.message}" }
            )
        }
    }

    fun import(uri: String) {
        viewModelScope.launch {
            _message.value = transferRepository.import(uri).fold(
                onSuccess = { "Importadas $it pistas" },
                onFailure = { "Error: ${it.message}" }
            )
        }
    }

    fun consumeMessage() {
        _message.value = null
    }
}

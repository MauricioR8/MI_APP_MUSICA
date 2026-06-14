package com.miappmusica.player.feature.modes

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.miappmusica.player.domain.model.AppMode

fun AppMode.accentColor(): Color = Color(accentColorArgb)

fun iconForKey(key: String): ImageVector = when (key) {
    "home" -> Icons.Filled.Home
    "fitness" -> Icons.Filled.FitnessCenter
    "focus" -> Icons.Filled.CenterFocusStrong
    "sad" -> Icons.Filled.SentimentDissatisfied
    else -> Icons.Filled.MusicNote
}

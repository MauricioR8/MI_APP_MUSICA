package com.miappmusica.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    background = DarkSurface,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface
)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandSecondary,
    background = LightSurface,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface
)

/**
 * Application theme.
 *
 * @param modeAccent optional accent color injected by the active "Mode". When non-null, the
 * primary color is replaced and the background is tinted toward the accent, which is how the
 * Dynamic-Island mode bar visually transforms the whole UI.
 */
@Composable
fun MiAppMusicaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    modeAccent: Color? = null,
    userAccent: Color? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val scheme = when {
        modeAccent != null -> {
            val tintTarget = if (darkTheme) Color.Black else Color.White
            base.copy(
                primary = modeAccent,
                secondary = modeAccent,
                background = lerp(modeAccent, tintTarget, if (darkTheme) 0.82f else 0.90f),
                surface = lerp(modeAccent, tintTarget, if (darkTheme) 0.75f else 0.86f)
            )
        }
        userAccent != null -> base.copy(primary = userAccent, secondary = userAccent)
        else -> base
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}

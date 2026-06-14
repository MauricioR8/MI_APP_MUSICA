package com.miappmusica.player.feature.modes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miappmusica.player.domain.model.AppMode

/**
 * The interactive "Dynamic Island" style mode selector pinned to the top of the app.
 * Collapsed it shows the active mode; tapping expands a horizontal rail of selectable modes.
 * Activating a non-Normal mode recolors the surface; the explicit close button returns to Normal.
 */
@Composable
fun ModeBar(
    state: ModesUiState,
    onActivate: (String) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val active = state.activeMode
    val accent by animateColorAsState(
        targetValue = active.accentColor(),
        animationSpec = tween(450),
        label = "accent"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 28.dp else 32.dp,
        label = "corner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = accent,
        contentColor = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            // ---- Collapsed header ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = iconForKey(active.iconKey),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (active.isNormal) "Modo Normal" else "Modo ${active.label}",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))

                if (!active.isNormal) {
                    HeaderIcon(Icons.Filled.Close, "Salir del modo") {
                        expanded = false
                        onExit()
                    }
                    Spacer(Modifier.width(6.dp))
                }
                HeaderIcon(
                    icon = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expandir"
                ) { expanded = !expanded }
            }

            // ---- Expanded mode rail ----
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.modes, key = { it.id }) { mode ->
                        ModeChip(
                            mode = mode,
                            selected = mode.id == active.id,
                            onClick = {
                                onActivate(mode.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(6.dp)
            .size(20.dp)
    )
}

@Composable
private fun ModeChip(
    mode: AppMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Color.White else Color.White.copy(alpha = 0.16f)
    val fg = if (selected) mode.accentColor() else Color.White
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        contentColor = fg,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconForKey(mode.iconKey),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = mode.label, fontWeight = FontWeight.Medium)
        }
    }
}

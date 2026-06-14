package com.miappmusica.player.domain.model

/**
 * A user-selectable context "Mode" surfaced by the Dynamic-Island style bar.
 *
 * Modes are fully customizable (add / edit / delete). Activating one:
 *  - recolors the UI using [accentColorArgb],
 *  - isolates the view to [isolatedPlaylistId] (hiding everything else),
 *  - optionally auto-plays that playlist.
 *
 * The [NORMAL] mode is the always-present escape hatch and isolates nothing.
 */
data class AppMode(
    val id: String,
    val label: String,
    val iconKey: String,
    val accentColorArgb: Long,
    val isolatedPlaylistId: Long? = null,
    val autoPlay: Boolean = false,
    val isBuiltIn: Boolean = false
) {
    val isNormal: Boolean get() = id == NORMAL_ID

    companion object {
        const val NORMAL_ID = "normal"

        val NORMAL = AppMode(
            id = NORMAL_ID,
            label = "Normal",
            iconKey = "home",
            accentColorArgb = 0xFF7C4DFF,
            isolatedPlaylistId = null,
            autoPlay = false,
            isBuiltIn = true
        )

        /** Seed modes shipped with the app; the user may edit or remove them. */
        fun defaults(): List<AppMode> = listOf(
            NORMAL,
            AppMode("gym", "Gym", "fitness", 0xFFFF5252, autoPlay = true, isBuiltIn = true),
            AppMode("focus", "Enfoque", "focus", 0xFF2962FF, autoPlay = false, isBuiltIn = true),
            AppMode("sadness", "Tristeza", "sad", 0xFF455A64, autoPlay = false, isBuiltIn = true)
        )
    }
}

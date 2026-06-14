package com.miappmusica.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.miappmusica.player.domain.model.AppMode

@Entity(tableName = "modes")
data class ModeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val iconKey: String,
    val accentColorArgb: Long,
    val isolatedPlaylistId: Long?,
    val autoPlay: Boolean,
    val isBuiltIn: Boolean,
    val sortOrder: Int = 0
) {
    fun toDomain(): AppMode = AppMode(
        id = id,
        label = label,
        iconKey = iconKey,
        accentColorArgb = accentColorArgb,
        isolatedPlaylistId = isolatedPlaylistId,
        autoPlay = autoPlay,
        isBuiltIn = isBuiltIn
    )

    companion object {
        fun from(mode: AppMode, sortOrder: Int): ModeEntity = ModeEntity(
            id = mode.id,
            label = mode.label,
            iconKey = mode.iconKey,
            accentColorArgb = mode.accentColorArgb,
            isolatedPlaylistId = mode.isolatedPlaylistId,
            autoPlay = mode.autoPlay,
            isBuiltIn = mode.isBuiltIn,
            sortOrder = sortOrder
        )
    }
}

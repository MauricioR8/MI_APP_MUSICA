package com.miappmusica.player.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val trackIds: List<Long> = emptyList(),
    val coverUri: String? = null,
    val isSystem: Boolean = false
) {
    val size: Int get() = trackIds.size
}

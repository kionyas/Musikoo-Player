package com.reikoonya.musikoo.data

data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList(),
    val customCoverPath: String? = null,
    val isPinned: Boolean = false,
    val orderIndex: Int = 0
)

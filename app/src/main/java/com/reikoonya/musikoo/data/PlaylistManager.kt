package com.reikoonya.musikoo.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PlaylistManager {
    private const val PREFS_NAME = "musikoo_playlists_prefs"
    private const val KEY_PLAYLISTS = "playlists_json"
    private const val KEY_PINNED_ALBUMS = "pinned_albums_json"
    private const val KEY_TIP_DISMISSED = "tip_dismissed"
    private const val KEY_FAVORITES = "favorites_json"

    const val FAVORITES_PLAYLIST_ID = "favorites_playlist_id"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ---------- FAVORITES ----------

    fun getFavorites(context: Context): List<Long> {
        val jsonStr = getPrefs(context).getString(KEY_FAVORITES, null) ?: return emptyList()
        val list = mutableListOf<Long>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                list.add(array.getLong(i))
            }
        } catch (_: Exception) {}
        return list
    }

    fun isFavorite(context: Context, songId: Long): Boolean {
        return getFavorites(context).contains(songId)
    }

    fun toggleFavorite(context: Context, songId: Long): Boolean {
        val favs = getFavorites(context).toMutableList()
        val nowFav = if (favs.contains(songId)) {
            favs.remove(songId)
            false
        } else {
            favs.add(songId)
            true
        }
        val array = JSONArray()
        favs.forEach { array.put(it) }
        getPrefs(context).edit().putString(KEY_FAVORITES, array.toString()).apply()

        // Also sync Favorites playlist in user list if pinned
        return nowFav
    }

    // ---------- PLAYLISTS ----------

    fun getPlaylists(context: Context): List<Playlist> {
        val userPlaylists = getUserPlaylists(context)
        val favSongIds = getFavorites(context).toList()

        val list = userPlaylists.toMutableList()
        if (favSongIds.isNotEmpty()) {
            val isFavPinned = getPrefs(context).getBoolean("favorites_is_pinned", false)
            val favPlaylist = Playlist(
                id = FAVORITES_PLAYLIST_ID,
                name = "Favorites",
                songIds = favSongIds,
                customCoverPath = null,
                isPinned = isFavPinned,
                orderIndex = -1
            )
            list.add(0, favPlaylist)
        }
        return list.sortedBy { it.orderIndex }
    }

    private fun getUserPlaylists(context: Context): List<Playlist> {
        val jsonStr = getPrefs(context).getString(KEY_PLAYLISTS, null) ?: return emptyList()
        val list = mutableListOf<Playlist>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                val name = obj.getString("name")
                val isPinned = obj.optBoolean("isPinned", false)
                val orderIndex = obj.optInt("orderIndex", i)
                val customCover = obj.optString("customCoverPath", "").ifEmpty { null }

                val songIdsArray = obj.getJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                for (j in 0 until songIdsArray.length()) {
                    songIds.add(songIdsArray.getLong(j))
                }

                list.add(
                    Playlist(
                        id = id,
                        name = name,
                        songIds = songIds,
                        customCoverPath = customCover,
                        isPinned = isPinned,
                        orderIndex = orderIndex
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        try {
            val userPlaylists = playlists.filter { it.id != FAVORITES_PLAYLIST_ID }
            val array = JSONArray()
            userPlaylists.forEachIndexed { index, playlist ->
                val obj = JSONObject()
                obj.put("id", playlist.id)
                obj.put("name", playlist.name)
                obj.put("isPinned", playlist.isPinned)
                obj.put("orderIndex", index)
                obj.put("customCoverPath", playlist.customCoverPath ?: "")

                val songIdsArray = JSONArray()
                playlist.songIds.forEach { songIdsArray.put(it) }
                obj.put("songIds", songIdsArray)

                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_PLAYLISTS, array.toString()).apply()

            // Save favorites pinned state
            playlists.firstOrNull { it.id == FAVORITES_PLAYLIST_ID }?.let { favPl ->
                getPrefs(context).edit().putBoolean("favorites_is_pinned", favPl.isPinned).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createPlaylist(context: Context, name: String, songIds: List<Long> = emptyList()): Playlist {
        val playlists = getUserPlaylists(context).toMutableList()
        val newPlaylist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name,
            songIds = songIds,
            orderIndex = playlists.size
        )
        playlists.add(newPlaylist)
        savePlaylists(context, playlists)
        return newPlaylist
    }

    fun addSongsToPlaylist(context: Context, playlistId: String, songIds: List<Long>) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            songIds.forEach { toggleFavorite(context, it) }
            return
        }
        val playlists = getPlaylists(context).map { playlist ->
            if (playlist.id == playlistId) {
                val updatedIds = (playlist.songIds + songIds).distinct()
                playlist.copy(songIds = updatedIds)
            } else playlist
        }
        savePlaylists(context, playlists)
    }

    fun removeSongFromPlaylist(context: Context, playlistId: String, songId: Long) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            toggleFavorite(context, songId)
            return
        }
        val playlists = getPlaylists(context).map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds.filter { it != songId })
            } else playlist
        }
        savePlaylists(context, playlists)
    }

    fun updatePlaylistSongs(context: Context, playlistId: String, songIds: List<Long>) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            val array = JSONArray()
            songIds.forEach { array.put(it) }
            getPrefs(context).edit().putString(KEY_FAVORITES, array.toString()).apply()
            return
        }
        val playlists = getPlaylists(context).map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = songIds)
            } else playlist
        }
        savePlaylists(context, playlists)
    }

    fun togglePinPlaylist(context: Context, playlistId: String) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            val curPinned = getPrefs(context).getBoolean("favorites_is_pinned", false)
            getPrefs(context).edit().putBoolean("favorites_is_pinned", !curPinned).apply()
            return
        }
        val playlists = getPlaylists(context).map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(isPinned = !playlist.isPinned)
            } else playlist
        }
        savePlaylists(context, playlists)
    }

    fun renamePlaylist(context: Context, playlistId: String, newName: String) {
        if (playlistId == FAVORITES_PLAYLIST_ID) return
        val playlists = getPlaylists(context).map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(name = newName)
            } else playlist
        }
        savePlaylists(context, playlists)
    }

    fun setCustomCover(context: Context, playlistId: String, coverPath: String?) {
        val playlists = getPlaylists(context).map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(customCoverPath = coverPath)
            } else playlist
        }
        savePlaylists(context, playlists)
    }

    fun deletePlaylist(context: Context, playlistId: String) {
        if (playlistId == FAVORITES_PLAYLIST_ID) {
            getPrefs(context).edit().remove(KEY_FAVORITES).apply()
            return
        }
        val playlists = getPlaylists(context).filter { it.id != playlistId }
        savePlaylists(context, playlists)
    }

    // ---------- PINNED ALBUMS ----------

    fun getPinnedAlbums(context: Context): Set<String> {
        val jsonStr = getPrefs(context).getString(KEY_PINNED_ALBUMS, null) ?: return emptySet()
        val set = mutableSetOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return set
    }

    fun togglePinAlbum(context: Context, albumName: String) {
        val pinned = getPinnedAlbums(context).toMutableSet()
        if (pinned.contains(albumName)) {
            pinned.remove(albumName)
        } else {
            pinned.add(albumName)
        }
        val array = JSONArray()
        pinned.forEach { array.put(it) }
        getPrefs(context).edit().putString(KEY_PINNED_ALBUMS, array.toString()).apply()
    }

    // ---------- SECTION COLLAPSE ----------

    fun isSectionCollapsed(context: Context, sectionKey: String): Boolean {
        return getPrefs(context).getBoolean("collapsed_$sectionKey", false)
    }

    fun toggleSectionCollapse(context: Context, sectionKey: String): Boolean {
        val current = isSectionCollapsed(context, sectionKey)
        val next = !current
        getPrefs(context).edit().putBoolean("collapsed_$sectionKey", next).apply()
        return next
    }

    // ---------- TIP DISMISSED ----------

    fun isTipDismissed(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_TIP_DISMISSED, false)
    }

    fun setTipDismissed(context: Context, dismissed: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_TIP_DISMISSED, dismissed).apply()
    }
}

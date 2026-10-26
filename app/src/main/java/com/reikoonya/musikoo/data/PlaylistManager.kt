package com.reikoonya.musikoo.data

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Плейлисты, избранное и закреплённые альбомы. Данные лежат в [MusikooDatabase].
 * Публичный API не менялся, поэтому экраны работают как раньше.
 *
 * В SharedPreferences остались только настройки интерфейса: состояние свёрнутых секций,
 * подсказка и закреплено ли «Избранное» в списке.
 */
object PlaylistManager {
    private const val TAG = "PlaylistManager"
    private val lock = Any()
    private const val PREFS_NAME = "musikoo_playlists_prefs"
    private const val KEY_TIP_DISMISSED = "tip_dismissed"
    private const val KEY_FAVORITES_PINNED = "favorites_is_pinned"

    const val FAVORITES_PLAYLIST_ID = "favorites_playlist_id"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun writeDb(context: Context): SQLiteDatabase = MusikooDatabase.get(context).writableDatabase
    private fun readDb(context: Context): SQLiteDatabase = MusikooDatabase.get(context).readableDatabase

    // ---------- FAVORITES ----------

    /** Избранное в памяти. Обновляется при каждом переключении, поэтому строки списка не ходят в базу. */
    private val favoriteSet = MutableStateFlow<Set<Long>>(emptySet())
    val favorites: StateFlow<Set<Long>> = favoriteSet.asStateFlow()

    @Volatile
    private var favoritesLoaded = false

    /** Читает избранное из базы один раз. Вызывать в фоне; повторные вызовы ничего не делают. */
    fun warmFavorites(context: Context) {
        if (favoritesLoaded) return
        favoriteSet.value = getFavorites(context).toSet()
        favoritesLoaded = true
    }

    fun getFavorites(context: Context): List<Long> {
        return readDb(context)
            .query("favorites", arrayOf("song_id"), null, null, null, null, "position ASC")
            .use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }
    }

    fun isFavorite(context: Context, songId: Long): Boolean {
        if (favoritesLoaded) return songId in favoriteSet.value
        return readDb(context)
            .query("favorites", arrayOf("song_id"), "song_id = ?", arrayOf(songId.toString()), null, null, null)
            .use { it.moveToFirst() }
    }

    fun toggleFavorite(context: Context, songId: Long): Boolean {
        warmFavorites(context)
        val db = writeDb(context)
        var nowFav = false
        db.beginTransaction()
        try {
            val exists = db
                .query("favorites", arrayOf("song_id"), "song_id = ?", arrayOf(songId.toString()), null, null, null)
                .use { it.moveToFirst() }
            if (exists) {
                db.delete("favorites", "song_id = ?", arrayOf(songId.toString()))
            } else {
                val next = DatabaseUtils.longForQuery(db, "SELECT COALESCE(MAX(position), -1) + 1 FROM favorites", null)
                db.insertOrThrow("favorites", null, ContentValues().apply {
                    put("song_id", songId)
                    put("position", next)
                })
                nowFav = true
            }
            db.setTransactionSuccessful()
            favoriteSet.value = if (nowFav) favoriteSet.value + songId else favoriteSet.value - songId
        } catch (e: Exception) {
            Log.e(TAG, "toggleFavorite failed", e)
        } finally {
            db.endTransaction()
        }
        return nowFav
    }

    private fun replaceFavorites(context: Context, songIds: List<Long>) {
        val db = writeDb(context)
        db.beginTransaction()
        try {
            db.delete("favorites", null, null)
            songIds.forEachIndexed { index, songId ->
                db.insertWithOnConflict("favorites", null, ContentValues().apply {
                    put("song_id", songId)
                    put("position", index)
                }, SQLiteDatabase.CONFLICT_IGNORE)
            }
            db.setTransactionSuccessful()
            favoriteSet.value = songIds.toSet()
        } catch (e: Exception) {
            Log.e(TAG, "replaceFavorites failed", e)
        } finally {
            db.endTransaction()
        }
    }

    // ---------- ASYNC READS ----------

    /** Плейлисты в фоновом потоке: экраны вызывают это из корутины, а не из композиции. */
    suspend fun loadPlaylists(context: Context): List<Playlist> =
        withContext(Dispatchers.IO) { getPlaylists(context) }

    /** Закреплённые альбомы в фоновом потоке. */
    suspend fun loadPinnedAlbums(context: Context): Set<String> =
        withContext(Dispatchers.IO) { getPinnedAlbums(context) }

    // ---------- PLAYLISTS ----------

    fun getPlaylists(context: Context): List<Playlist> {
        val list = getUserPlaylists(context).toMutableList()
        val favSongIds = getFavorites(context)
        if (favSongIds.isNotEmpty()) {
            list.add(
                0,
                Playlist(
                    id = FAVORITES_PLAYLIST_ID,
                    name = "Favorites",
                    songIds = favSongIds,
                    customCoverPath = null,
                    isPinned = prefs(context).getBoolean(KEY_FAVORITES_PINNED, false),
                    orderIndex = -1
                )
            )
        }
        return list.sortedBy { it.orderIndex }
    }

    private fun getUserPlaylists(context: Context): List<Playlist> {
        val db = readDb(context)
        val result = mutableListOf<Playlist>()
        try {
            db.query(
                "playlists",
                arrayOf("id", "name", "custom_cover", "pinned", "position"),
                null, null, null, null, "position ASC"
            ).use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    result.add(
                        Playlist(
                            id = id,
                            name = c.getString(1),
                            songIds = songsOf(db, id),
                            customCoverPath = c.getString(2)?.takeIf { it.isNotEmpty() },
                            isPinned = c.getInt(3) == 1,
                            orderIndex = c.getInt(4)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getUserPlaylists failed", e)
        }
        return result
    }

    private fun songsOf(db: SQLiteDatabase, playlistId: String): List<Long> {
        return db.query(
            "playlist_songs",
            arrayOf("song_id"),
            "playlist_id = ?", arrayOf(playlistId),
            null, null, "position ASC"
        ).use { c -> buildList { while (c.moveToNext()) add(c.getLong(0)) } }
    }

    /**
     * Сохраняет весь список пользовательских плейлистов одной транзакцией.
     * Избранное не трогает, кроме флага «закреплено».
     */
    fun savePlaylists(context: Context, playlists: List<Playlist>) {
        val db = writeDb(context)
        db.beginTransaction()
        try {
            db.delete("playlist_songs", null, null)
            db.delete("playlists", null, null)
            playlists.filter { it.id != FAVORITES_PLAYLIST_ID }.forEachIndexed { index, playlist ->
                db.insertOrThrow("playlists", null, ContentValues().apply {
                    put("id", playlist.id)
                    put("name", playlist.name)
                    put("custom_cover", playlist.customCoverPath)
                    put("pinned", if (playlist.isPinned) 1 else 0)
                    put("position", index)
                })
                playlist.songIds.forEachIndexed { position, songId ->
                    db.insertOrThrow("playlist_songs", null, ContentValues().apply {
                        put("playlist_id", playlist.id)
                        put("position", position)
                        put("song_id", songId)
                    })
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "savePlaylists failed", e)
        } finally {
            db.endTransaction()
        }

        playlists.firstOrNull { it.id == FAVORITES_PLAYLIST_ID }?.let { fav ->
            prefs(context).edit().putBoolean(KEY_FAVORITES_PINNED, fav.isPinned).apply()
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
                playlist.copy(songIds = (playlist.songIds + songIds).distinct())
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
            replaceFavorites(context, songIds)
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
            val curPinned = prefs(context).getBoolean(KEY_FAVORITES_PINNED, false)
            prefs(context).edit().putBoolean(KEY_FAVORITES_PINNED, !curPinned).apply()
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
        synchronized(lock) {
            if (playlistId == FAVORITES_PLAYLIST_ID) {
                writeDb(context).delete("favorites", null, null)
                favoriteSet.value = emptySet()
                return
            }
            val playlists = getPlaylists(context).filter { it.id != playlistId }
            savePlaylists(context, playlists)
        }
    }

    fun onSongDeleted(context: Context, songId: Long) {
        synchronized(lock) {
            val db = writeDb(context)
            db.beginTransaction()
            try {
                db.delete("favorites", "song_id = ?", arrayOf(songId.toString()))
                db.delete("playlist_songs", "song_id = ?", arrayOf(songId.toString()))
                db.setTransactionSuccessful()
                favoriteSet.value = favoriteSet.value - songId
            } catch (e: Exception) {
                Log.e(TAG, "onSongDeleted failed", e)
            } finally {
                db.endTransaction()
            }
        }
    }

    // ---------- PINNED ALBUMS ----------

    fun getPinnedAlbums(context: Context): Set<String> {
        return readDb(context)
            .query("pinned_albums", arrayOf("name"), null, null, null, null, null)
            .use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
    }

    fun togglePinAlbum(context: Context, albumName: String) {
        val db = writeDb(context)
        val pinned = db
            .query("pinned_albums", arrayOf("name"), "name = ?", arrayOf(albumName), null, null, null)
            .use { it.moveToFirst() }
        if (pinned) {
            db.delete("pinned_albums", "name = ?", arrayOf(albumName))
        } else {
            db.insertOrThrow("pinned_albums", null, ContentValues().apply { put("name", albumName) })
        }
    }

    // ---------- SECTION COLLAPSE ----------

    fun isSectionCollapsed(context: Context, sectionKey: String): Boolean {
        return prefs(context).getBoolean("collapsed_$sectionKey", false)
    }

    fun toggleSectionCollapse(context: Context, sectionKey: String): Boolean {
        val next = !isSectionCollapsed(context, sectionKey)
        prefs(context).edit().putBoolean("collapsed_$sectionKey", next).apply()
        return next
    }

    // ---------- TIP DISMISSED ----------

    fun isTipDismissed(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_TIP_DISMISSED, false)
    }

    fun setTipDismissed(context: Context, dismissed: Boolean) {
        prefs(context).edit().putBoolean(KEY_TIP_DISMISSED, dismissed).apply()
    }
}

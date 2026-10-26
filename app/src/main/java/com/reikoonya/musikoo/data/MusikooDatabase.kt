package com.reikoonya.musikoo.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.io.File

/**
 * Единая SQLite-база приложения: плейлисты, избранное, закреплённые альбомы и журнал прослушиваний.
 *
 * Схема создаётся при первом открытии. В той же операции импортируются старые данные из
 * SharedPreferences и play_log.json (они остаются на диске как резервная копия и больше не читаются).
 */
class MusikooDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, NAME, null, VERSION) {

    private val appContext: Context = context.applicationContext

    companion object {
        private const val NAME = "musikoo.db"
        private const val VERSION = 1

        // Старые места хранения (до перехода на базу)
        private const val LEGACY_PLAYLIST_PREFS = "musikoo_playlists_prefs"
        private const val LEGACY_KEY_PLAYLISTS = "playlists_json"
        private const val LEGACY_KEY_PINNED_ALBUMS = "pinned_albums_json"
        private const val LEGACY_KEY_FAVORITES = "favorites_json"
        private const val LEGACY_PLAY_LOG_FILE = "play_log.json"

        @Volatile
        private var instance: MusikooDatabase? = null

        fun get(context: Context): MusikooDatabase =
            instance ?: synchronized(this) {
                instance ?: MusikooDatabase(context).also { instance = it }
            }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        // Без этого ON DELETE CASCADE для playlist_songs не работает
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE playlists (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                custom_cover TEXT,
                pinned INTEGER NOT NULL DEFAULT 0,
                position INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE playlist_songs (
                playlist_id TEXT NOT NULL REFERENCES playlists(id) ON DELETE CASCADE,
                position INTEGER NOT NULL,
                song_id INTEGER NOT NULL,
                PRIMARY KEY (playlist_id, position)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE favorites (song_id INTEGER PRIMARY KEY, position INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE pinned_albums (name TEXT PRIMARY KEY)")
        db.execSQL(
            """
            CREATE TABLE plays (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                artist TEXT NOT NULL,
                title TEXT NOT NULL,
                album TEXT NOT NULL,
                listened_ms INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_plays_ts ON plays(ts)")

        importLegacy(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Версия 1 — первая. Миграции между версиями появятся здесь.
    }

    // ---------- импорт старых данных (однократно, при создании базы) ----------

    private fun importLegacy(db: SQLiteDatabase) {
        val prefs = appContext.getSharedPreferences(LEGACY_PLAYLIST_PREFS, Context.MODE_PRIVATE)

        // Каждый блок в runCatching: повреждённые старые данные не должны мешать созданию базы
        runCatching {
            val raw = prefs.getString(LEGACY_KEY_PLAYLISTS, null) ?: return@runCatching
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.getString("id")
                db.insertOrThrow("playlists", null, ContentValues().apply {
                    put("id", id)
                    put("name", obj.getString("name"))
                    put("custom_cover", obj.optString("customCoverPath", "").ifEmpty { null })
                    put("pinned", if (obj.optBoolean("isPinned", false)) 1 else 0)
                    put("position", obj.optInt("orderIndex", i))
                })
                insertSongs(db, "playlist_songs", "playlist_id", id, obj.getJSONArray("songIds"))
            }
        }

        runCatching {
            val raw = prefs.getString(LEGACY_KEY_FAVORITES, null) ?: return@runCatching
            insertSongs(db, "favorites", null, null, JSONArray(raw))
        }

        runCatching {
            val raw = prefs.getString(LEGACY_KEY_PINNED_ALBUMS, null) ?: return@runCatching
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                db.insertOrThrow("pinned_albums", null, ContentValues().apply {
                    put("name", array.getString(i))
                })
            }
        }

        runCatching {
            val file = File(appContext.filesDir, LEGACY_PLAY_LOG_FILE)
            if (!file.exists()) return@runCatching
            val array = JSONArray(file.readText())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                db.insertOrThrow("plays", null, ContentValues().apply {
                    put("ts", o.getLong("ts"))
                    put("artist", o.getString("a"))
                    put("title", o.getString("t"))
                    put("album", o.optString("al"))
                    put("listened_ms", o.optLong("ms"))
                })
            }
        }
    }

    /**
     * Вставляет список id песен с порядковым номером. Дубликаты в старых данных пропускаются,
     * а не обрывают весь импорт. Для избранного ownerColumn и ownerId равны null.
     */
    private fun insertSongs(
        db: SQLiteDatabase,
        table: String,
        ownerColumn: String?,
        ownerId: String?,
        songIds: JSONArray,
    ) {
        for (j in 0 until songIds.length()) {
            db.insertWithOnConflict(table, null, ContentValues().apply {
                if (ownerColumn != null) put(ownerColumn, ownerId)
                put("song_id", songIds.getLong(j))
                put("position", j)
            }, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }
}

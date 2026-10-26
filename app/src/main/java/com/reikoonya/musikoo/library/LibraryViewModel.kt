package com.reikoonya.musikoo.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reikoonya.musikoo.data.AlbumArtCache
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.data.scanLibrary
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.util.MusicPlayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Список песен устройства. Переживает поворот экрана и пересоздание Activity,
 * поэтому библиотека не сканируется заново при каждом повороте.
 *
 * Сканирование идёт в фоне и занимает только запрос к MediaStore: обложки грузятся позже,
 * по мере появления строк на экране (см. AlbumArtCache).
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    /** true, пока не закончилось первое сканирование: экран показывает загрузку. */
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Избранное читаем из базы один раз в фоне, чтобы строки списка не ходили в базу сами
        viewModelScope.launch(Dispatchers.IO) {
            PlaylistManager.warmFavorites(getApplication<Application>())
        }
    }

    /** Замена списка (например, после редактирования тегов или удаления файла). */
    fun setSongs(newSongs: List<Song>) {
        _songs.value = newSongs
    }

    /**
     * Запускает сканирование. Повторный вызов отменяет незаконченное сканирование.
     * [debounce] = true при изменениях MediaStore: несколько подряд идущих изменений дают одно сканирование.
     * Если библиотека не изменилась, список не пересоздаётся, и экраны не перерисовываются.
     */
    fun rescan(debounce: Boolean = false) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            if (debounce) delay(400)
            val context = getApplication<Application>()
            val fresh = withContext(Dispatchers.IO) { scanLibrary(context) }
            if (fresh != _songs.value) _songs.value = fresh
            _loading.value = false
            AlbumArtCache.forgetMissing()

            MusicPlayerManager.initPlayer(context)
            MusicPlayerManager.restoreQueue(fresh)
        }
    }
}

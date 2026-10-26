package com.reikoonya.musikoo.scrobble

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Вход и выход из Last.fm. Состояние для экрана настроек и три операции.
 * Тосты и открытие браузера остаются в UI, сюда они не попадают.
 */
class LastFmSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Имя пользователя, если вход выполнен. */
    var user by mutableStateOf(Scrobbler.username())
        private set

    /** URL аватарки пользователя. */
    var avatarUrl by mutableStateOf(Scrobbler.avatarUrl())
        private set

    /** Токен, пока пользователь подтверждает вход в браузере. */
    var pendingToken by mutableStateOf<String?>(null)
        private set

    /** true, пока идёт обмен токена на сессию. */
    var busy by mutableStateOf(false)
        private set

    /** true, пока запрашивается токен перед открытием страницы входа. */
    var starting by mutableStateOf(false)
        private set

    init {
        val u = user
        if (u != null && avatarUrl == null) {
            scope.launch { fetchAvatar(u) }
        }
    }

    private suspend fun fetchAvatar(username: String) {
        val url = withContext(Dispatchers.IO) { LastFmClient.getUserAvatarUrl(username) }
        if (url != null) {
            Scrobbler.saveAvatarUrl(url)
            avatarUrl = url
        }
    }

    /** Запрашивает токен и возвращает адрес страницы входа. Вызывать из корутины. */
    suspend fun beginLogin(): String {
        starting = true
        try {
            val token = withContext(Dispatchers.IO) { LastFmClient.getToken() }
            pendingToken = token
            return LastFmClient.authUrl(token)
        } finally {
            starting = false
        }
    }

    /** Обменивает подтверждённый токен на сессию и сохраняет её. Возвращает имя пользователя. */
    suspend fun confirm(token: String): String {
        busy = true
        try {
            val (key, name) = withContext(Dispatchers.IO) { LastFmClient.getSession(token) }
            val avatar = withContext(Dispatchers.IO) { LastFmClient.getUserAvatarUrl(name) }
            Scrobbler.saveSession(key, name, avatar)
            user = name
            avatarUrl = avatar
            pendingToken = null
            return name
        } finally {
            busy = false
        }
    }

    /** Пользователь закрыл окно подтверждения. */
    fun cancelPending() {
        pendingToken = null
    }

    fun disconnect() {
        Scrobbler.logout()
        user = null
        avatarUrl = null
    }
}

package com.reikoonya.musikoo.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** Where the releases live: https://github.com/OWNER/REPO/releases (the repo has to be public). */
object UpdateConfig {
    const val OWNER = "kionyas"
    const val REPO = "Musikoo-Player"
    const val REPO_URL = "https://github.com/$OWNER/$REPO"
    const val LATEST_RELEASE_URL = "$REPO_URL/releases/latest"
}

class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
)

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    object UpToDate : UpdateState
    class Available(val info: UpdateInfo) : UpdateState
    class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    class Ready(val info: UpdateInfo, val file: File) : UpdateState

    /** [info] is set when the download failed (so it can be retried), null when the check itself failed. */
    class Failed(val message: String, val info: UpdateInfo? = null) : UpdateState
}

/**
 * Over-the-air updates through GitHub Releases.
 * Flow: the latest release is read from the GitHub API, its .apk asset is downloaded into the cache
 * and handed to the system installer. The state is Compose state, so any screen can show it.
 */
object UpdateManager {
    var state: UpdateState by mutableStateOf<UpdateState>(UpdateState.Idle)
        private set

    /** The update dialog is shown while this is true. */
    var dialogVisible: Boolean by mutableStateOf(false)
        private set

    private const val PREFS = "musikoo_updates"
    private const val KEY_LAST_CHECK = "last_check"
    private const val AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    private const val APK_MIME = "application/vnd.android.package-archive"
    private const val USER_AGENT = "Musikoo-Updater (https://github.com/${UpdateConfig.OWNER}/${UpdateConfig.REPO})"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var lastInfo: UpdateInfo? = null

    fun dismissDialog() {
        dialogVisible = false
    }

    @Suppress("DEPRECATION")
    fun currentVersion(context: Context): String = runCatching {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "0"
    }.getOrDefault("0")

    /** Silent check on app start, at most once every few hours. Shows the dialog only if there is an update. */
    fun autoCheck(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (System.currentTimeMillis() - last < AUTO_CHECK_INTERVAL_MS) return
        check(app, manual = false)
    }

    /** [manual] = the user tapped "check": errors are reported and the dialog reopens for a known update.
     *  [showDialog] = false when the caller shows the state itself (the App updates page). */
    fun check(context: Context, manual: Boolean = true, showDialog: Boolean = true) {
        when (val current = state) {
            is UpdateState.Checking -> return
            is UpdateState.Downloading, is UpdateState.Available, is UpdateState.Ready -> {
                if (manual && showDialog) dialogVisible = true
                return
            }
            is UpdateState.Failed -> if (current.info != null) {
                if (manual && showDialog) dialogVisible = true
                return
            }
            else -> Unit
        }

        val app = context.applicationContext
        job = scope.launch {
            state = UpdateState.Checking
            val result = withContext(Dispatchers.IO) { runCatching { fetchLatest() } }
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply()

            result.onSuccess { info ->
                if (info != null && isNewer(info.version, currentVersion(app))) {
                    lastInfo = info
                    state = UpdateState.Available(info)
                    dialogVisible = showDialog
                } else {
                    state = UpdateState.UpToDate
                }
            }.onFailure { error ->
                state = if (manual) UpdateState.Failed(friendly(error)) else UpdateState.Idle
            }
        }
    }

    fun startDownload(context: Context) {
        val info = lastInfo ?: return
        if (state is UpdateState.Downloading) return
        val app = context.applicationContext
        job = scope.launch {
            state = UpdateState.Downloading(info, 0f)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    download(app, info) { progress -> state = UpdateState.Downloading(info, progress) }
                }
            }
            result.onSuccess { file ->
                state = UpdateState.Ready(info, file)
            }.onFailure { error ->
                state = UpdateState.Failed(friendly(error), info)
            }
        }
    }

    /** Opens the system installer. If installs from this app are not allowed yet, opens that setting first. */
    fun install(context: Context) {
        val ready = state as? UpdateState.Ready ?: return
        val app = context.applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            app.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", ready.file)
        app.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    // ---- network ----

    /** Latest non-draft, non-prerelease release that carries an .apk. Null if there is none yet. */
    private fun fetchLatest(): UpdateInfo? {
        val url = "https://api.github.com/repos/${UpdateConfig.OWNER}/${UpdateConfig.REPO}/releases/latest"
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/vnd.github+json")

            val code = connection.responseCode
            if (code == 404) return null
            if (code !in 200..299) error("GitHub answered $code")

            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val assets = json.optJSONArray("assets") ?: return null
            var apk: JSONObject? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apk = asset
                    break
                }
            }
            if (apk == null) return null

            return UpdateInfo(
                version = json.optString("tag_name").removePrefix("v").removePrefix("V"),
                notes = json.optString("body"),
                apkUrl = apk.getString("browser_download_url"),
                apkSize = apk.optLong("size"),
                pageUrl = json.optString("html_url"),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "update.apk.part")
        val target = File(dir, "musikoo-${info.version}.apk")

        val connection = URL(info.apkUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode !in 200..299) error("Download failed (${connection.responseCode})")

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: info.apkSize
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    var lastPercent = -1
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        if (!part.renameTo(target)) error("Could not save the update file")
        verify(context, target)
        return target
    }

    /** The file must be a real APK of this very app. Android checks the signature when installing. */
    @Suppress("DEPRECATION")
    private fun verify(context: Context, file: File) {
        val pm = context.packageManager
        val archive = if (Build.VERSION.SDK_INT >= 33) {
            pm.getPackageArchiveInfo(file.path, PackageManager.PackageInfoFlags.of(0))
        } else {
            pm.getPackageArchiveInfo(file.path, 0)
        }
        if (archive == null || archive.packageName != context.packageName) {
            file.delete()
            error("The downloaded file is not a Musikoo update")
        }
    }

    // ---- helpers ----

    /** Логика сравнения вынесена в AppVersion.kt и покрыта тестами. */
    fun isNewer(remote: String, local: String): Boolean = isNewerVersion(remote, local)

    private fun friendly(error: Throwable): String = when (error) {
        is UnknownHostException, is SocketTimeoutException -> "Can't reach GitHub. Check your connection"
        else -> error.message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
    }
}
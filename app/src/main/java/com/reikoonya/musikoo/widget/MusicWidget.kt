package com.reikoonya.musikoo.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
// The day/night overload lives in androidx.glance.color, not androidx.glance.unit
import androidx.glance.color.ColorProvider as colorProviderOf
import com.reikoonya.musikoo.MainActivity
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.ui.theme.ColorThemeMode
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.UiStylePrefs
import com.reikoonya.musikoo.util.accentFromBitmap
import com.reikoonya.musikoo.util.readableOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Widget state. Metadata goes to SharedPreferences (instant), artwork to a small file (written separately),
 * so a track change shows up right away and the cover follows a moment later.
 */
object MusicWidgetStore {
    private const val PREFS = "music_widget"
    private const val ART_FILE = "widget_art.jpg"

    data class State(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val artPath: String?,
        val artId: Long,
    )

    fun read(context: Context): State {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val art = File(context.filesDir, ART_FILE)
        return State(
            title = prefs.getString("title", null) ?: "Nothing playing",
            artist = prefs.getString("artist", null) ?: "Musikoo",
            isPlaying = prefs.getBoolean("playing", false),
            artPath = if (art.exists()) art.absolutePath else null,
            artId = prefs.getLong("art_id", -1L),
        )
    }

    fun writeMeta(context: Context, title: String, artist: String, isPlaying: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("title", title)
            .putString("artist", artist)
            .putBoolean("playing", isPlaying)
            .apply()
    }

    fun writeArt(context: Context, artId: Long, art: Bitmap?) {
        val file = File(context.filesDir, ART_FILE)
        if (art == null) {
            file.delete()
        } else {
            // 160px is plenty for a 64dp cover on any density
            val scaled = Bitmap.createScaledBitmap(art, 160, 160, true)
            file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("art_id", artId).apply()
    }
}

/** Compact Spotify-style widget: cover on the left, title and artist, small controls on the right. */
class MusicWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = MusicWidgetStore.read(context)
        val art = state.artPath?.let { BitmapFactory.decodeFile(it) }
        val palette = paletteFor(context, art)
        provideContent {
            WidgetContent(state, art, palette)
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Call when the track or play state changes. Metadata is saved and the widget redrawn right away,
         * the cover is written afterwards only if the track changed.
         */
        fun refresh(context: Context) {
            val app = context.applicationContext
            val song = MusicPlayerManager.songs.getOrNull(MusicPlayerManager.currentSongIndex)
            val playing = MusicPlayerManager.exoPlayer?.isPlaying == true
            MusicWidgetStore.writeMeta(
                app,
                title = song?.title ?: "Nothing playing",
                artist = song?.artist ?: "Musikoo",
                isPlaying = playing,
            )
            scope.launch { updateAllWidgets(app) }

            val artId = song?.id ?: -1L
            if (artId != MusicWidgetStore.read(app).artId) {
                val art = song?.albumArt
                scope.launch {
                    MusicWidgetStore.writeArt(app, artId, art)
                    updateAllWidgets(app)
                }
            }
        }

        /** Call when the app's color setting changes, so the widget restyles without being re-added. */
        fun updateAppearance(context: Context) {
            val app = context.applicationContext
            scope.launch { updateAllWidgets(app) }
        }
    }
}

class MusicWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MusicWidget()
}

suspend fun updateAllWidgets(context: Context) {
    MusicWidget().updateAll(context)
}

/** Colors for each widget element, day and night, following the app's color setting. */
private class Palette(
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val onSurfaceVariant: ColorProvider,
    val primary: ColorProvider,
    val onPrimary: ColorProvider,
    val primaryContainer: ColorProvider,
    val secondaryContainer: ColorProvider,
    val onSecondaryContainer: ColorProvider,
)

private fun paletteFor(context: Context, art: Bitmap?): Palette {
    val (light, dark) = when (UiStylePrefs.loadColorMode(context)) {
        ColorThemeMode.DYNAMIC ->
            dynamicLightColorScheme(context) to dynamicDarkColorScheme(context)
        ColorThemeMode.CLASSIC ->
            lightColorScheme() to darkColorScheme()
        ColorThemeMode.COVER -> {
            val accentLight = art?.let { accentFromBitmap(it, dark = false) }
            val accentDark = art?.let { accentFromBitmap(it, dark = true) }
            (accentLight?.let { lightColorScheme(primary = it, onPrimary = readableOn(it)) } ?: lightColorScheme()) to
                (accentDark?.let { darkColorScheme(primary = it, onPrimary = readableOn(it)) } ?: darkColorScheme())
        }
    }
    fun pick(light: Color, dark: Color): ColorProvider = colorProviderOf(light, dark)
    return Palette(
        surface = pick(light.surface, dark.surface),
        onSurface = pick(light.onSurface, dark.onSurface),
        onSurfaceVariant = pick(light.onSurfaceVariant, dark.onSurfaceVariant),
        primary = pick(light.primary, dark.primary),
        onPrimary = pick(light.onPrimary, dark.onPrimary),
        primaryContainer = pick(light.primaryContainer, dark.primaryContainer),
        secondaryContainer = pick(light.secondaryContainer, dark.secondaryContainer),
        onSecondaryContainer = pick(light.onSecondaryContainer, dark.onSecondaryContainer),
    )
}

@Composable
private fun WidgetContent(state: MusicWidgetStore.State, art: Bitmap?, p: Palette) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(p.surface)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(LocalContext.current, MainActivity::class.java)))
    ) {
        // Center-left: cover
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Cover(art, p, 130.dp)
        }
        // Top-right: small brand text "Musikoo :3" in primary (brightest accent) color
        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
            Text(
                text = "Musikoo :3",
                style = TextStyle(
                    color = p.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            )
        }
        // Right side: Title & Artist above controls, aligned bottom-end
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(start = 112.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = state.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = p.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = state.artist,
                    maxLines = 1,
                    style = TextStyle(
                        color = p.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                )
                Spacer(modifier = GlanceModifier.height(6.dp))
                Controls(state.isPlaying, p)
            }
        }
    }
}

@Composable
private fun Cover(art: Bitmap?, p: Palette, size: Dp) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(size / 4)
            .background(p.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(
                provider = ImageProvider(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Controls(isPlaying: Boolean, p: Palette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ExpressiveButton(
            icon = R.drawable.ic_widget_previous,
            size = 48.dp,
            corner = 24.dp,
            action = actionRunCallback<PreviousAction>(),
            background = p.secondaryContainer,
            tint = p.onSecondaryContainer,
            iconSize = 24.dp
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        ExpressiveButton(
            icon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            size = 64.dp,
            corner = if (isPlaying) 20.dp else 32.dp,
            action = actionRunCallback<PlayPauseAction>(),
            background = p.primary,
            tint = p.onPrimary,
            iconSize = 32.dp,
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        ExpressiveButton(
            icon = R.drawable.ic_widget_next,
            size = 48.dp,
            corner = 24.dp,
            action = actionRunCallback<NextAction>(),
            background = p.secondaryContainer,
            tint = p.onSecondaryContainer,
            iconSize = 24.dp
        )
    }
}

@Composable
private fun ExpressiveButton(
    icon: Int,
    size: Dp,
    corner: Dp,
    action: Action,
    background: ColorProvider,
    tint: ColorProvider,
    iconSize: Dp,
) {
    Box(
        modifier = GlanceModifier
            .size(size)
            .cornerRadius(corner)
            .background(background)
            .clickable(action),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(iconSize)
        )
    }
}

// ---------- actions ----------

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runOnPlayer(context) { 
            MusicPlayerManager.togglePlayPause() 
            MusicWidget.refresh(context)
        }
    }
}

class PreviousAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runOnPlayer(context) { 
            MusicPlayerManager.skipPrevious() 
            MusicWidget.refresh(context)
        }
    }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runOnPlayer(context) { 
            MusicPlayerManager.skipNext() 
            MusicWidget.refresh(context)
        }
    }
}

/** Controls need the player, which lives in the app process. If the app is not running, open it instead. */
private suspend fun runOnPlayer(context: Context, block: () -> Unit) {
    if (MusicPlayerManager.exoPlayer == null) {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }
    withContext(Dispatchers.Main) { block() }
}

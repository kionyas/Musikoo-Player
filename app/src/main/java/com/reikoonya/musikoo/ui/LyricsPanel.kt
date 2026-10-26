package com.reikoonya.musikoo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.lyrics.LyricLine
import com.reikoonya.musikoo.lyrics.LyricsResult
import com.reikoonya.musikoo.ui.components.LoadingScreen
import com.reikoonya.musikoo.ui.theme.LocalUiStyle
import com.reikoonya.musikoo.ui.theme.UiStyle
import com.reikoonya.musikoo.util.MusicPlayerManager
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LyricsPanel(lyrics: LyricsResult?, loaded: Boolean, visible: Boolean, accent: Color, modifier: Modifier = Modifier) {
    if (visible) {
        Column(modifier = modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                val state = Triple(loaded, lyrics, lyrics?.lines?.isEmpty() == true)
                AnimatedContent(
                    targetState = state,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val duration = 500
                        (fadeIn(tween(duration, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.96f)) togetherWith
                            (fadeOut(tween(220, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 0.96f)) using
                            SizeTransform(clip = false)
                    },
                    label = "lyricsState"
                ) { (isLoaded, currentLyrics, isEmpty) ->
                    val duration = 500
                    val blur by transition.animateDp(
                        transitionSpec = { tween(duration, easing = FastOutSlowInEasing) },
                        label = "lyricsBlur"
                    ) { s -> if (s == EnterExitState.Visible) 0.dp else 18.dp }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(blur, BlurredEdgeTreatment.Unbounded)
                    ) {
                        when {
                            !isLoaded -> LoadingScreen(label = "Loading lyrics…")
                            currentLyrics == null || isEmpty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Text(
                                    "No lyrics for this track",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            else -> {
                                key(currentLyrics) {
                                    LyricsLines(currentLyrics, accent)
                                }
                            }
                        }
                    }
                }

                // Edge fades only for the plain (Classic) style; Expensive shows lyrics without them
                if (LocalUiStyle.current == UiStyle.CLASSIC) {
                // Top blur gradient
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
                                )
                            )
                        )
                )
                // Bottom blur gradient
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
                }
            }
            if (lyrics != null && lyrics.lines.isNotEmpty()) {
                val kind = when (lyrics.level) {
                    "word" -> "Word-synced lyrics"
                    "line" -> "Synced lyrics"
                    else -> "Lyrics"
                }
                val label = if (lyrics.source.isNotBlank()) "$kind · ${lyrics.source}" else kind
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricsLines(lyrics: LyricsResult, accent: Color) {
    val isSynced = lyrics.level != "none"
    val initialPos = remember { MusicPlayerManager.controller?.currentPosition ?: 0L }
    var positionMs by remember { mutableLongStateOf(initialPos) }

    val initialActive = remember(lyrics, isSynced) {
        if (isSynced) lyrics.lines.indexOfLast { it.startMs <= initialPos } else -1
    }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (initialActive - 1).coerceAtLeast(0)
    )

    if (isSynced) {
        LaunchedEffect(lyrics) {
            while (true) {
                positionMs = MusicPlayerManager.controller?.currentPosition ?: 0L
                delay(50.milliseconds)
            }
        }
    }

    val active = remember(positionMs, lyrics, isSynced) { if (isSynced) lyrics.lines.indexOfLast { it.startMs <= positionMs } else -1 }
    LaunchedEffect(active) {
        if (isSynced && active >= 0 && active != initialActive) {
            listState.animateScrollToItem((active - 1).coerceAtLeast(0))
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(if (isSynced) 20.dp else 14.dp)) {
        itemsIndexed(lyrics.lines) { index, line ->
            LyricRow(line, index, active, positionMs, accent, isSynced)
        }
    }
}

@Composable
private fun LyricRow(line: LyricLine, index: Int, active: Int, positionMs: Long, accent: Color, isSynced: Boolean) {
    val isActive = isSynced && index == active
    val distance = if (isSynced) abs(index - active) else 0

    // Blur grows with distance from the active line (capped). Only rows currently composed by the
    // LazyColumn get a layer, so the cost stays bounded by the number of visible rows.
    val targetBlur = if (isSynced && !isActive) (distance * 1.5f).coerceAtMost(6f).dp else 0.dp
    val animatedBlur by animateDpAsState(targetBlur, tween(200, easing = FastOutSlowInEasing), label = "lyricBlur")

    val targetAlpha = when { !isSynced -> 0.90f; isActive -> 1f; distance == 1 -> 0.52f; distance == 2 -> 0.32f; else -> 0.16f }
    val animatedAlpha by animateFloatAsState(targetAlpha, tween(200, easing = FastOutSlowInEasing), label = "lyricAlpha")
    val animatedScale by animateFloatAsState(if (isActive) 1.03f else 1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium), label = "lyricScale")

    val base = MaterialTheme.colorScheme.onSurface
    val text: AnnotatedString = remember(line, isActive, positionMs, isSynced) {
        if (!isActive || line.words.isNullOrEmpty()) {
            AnnotatedString(line.text)
        } else {
            buildAnnotatedString {
                val lastIdx = line.words.lastIndex
                line.words.forEachIndexed { i, w ->
                    val sang = positionMs >= w.startMs
                    withStyle(SpanStyle(color = if (sang) accent else base.copy(alpha = 0.55f))) { append(w.text) }
                    if (i < lastIdx && w.text.lastOrNull()?.isWhitespace() != true) append(" ")
                }
            }
        }
    }

    Text(
        text = text,
        style = if (isSynced) {
            MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp, lineHeight = 33.sp)
        } else {
            MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, lineHeight = 27.sp)
        },
        fontWeight = if (isSynced) FontWeight.SemiBold else FontWeight.Bold,
        color = base,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0.5f)
                alpha = animatedAlpha
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .blur(animatedBlur)
    )
}

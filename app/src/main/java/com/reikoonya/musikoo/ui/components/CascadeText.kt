package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.UiStylePrefs
import kotlinx.coroutines.delay

/** Включён ли каскадный текст (настройка Interface and interaction). */
@Composable
fun cascadeTextEnabled(): Boolean {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    return UiStylePrefs.cascadeText
}

/** Сколько букв подряд находятся в «полупроявленном» состоянии: чем больше, тем плавнее волна. */
private const val CASCADE_SPREAD = 7f

/**
 * Текст, который проявляется каскадом: буквы одна за другой выходят из прозрачности слева направо,
 * а вся строка при этом чуть поднимается на место. Размер и перенос строк не меняются: анимируется
 * только прозрачность букв, поэтому соседние элементы не прыгают.
 *
 * Если каскадный текст выключен в настройках, это обычный [Text].
 *
 * @param delayMillis задержка перед стартом (для ступенчатого появления нескольких строк).
 * @param stepMillis сколько миллисекунд проходит между началом проявления соседних букв.
 * @param playOnce не повторять анимацию для того же текста, когда элемент снова попадает в
 *  композицию (например, при прокрутке списка назад).
 */
@Composable
fun CascadeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    style: TextStyle = LocalTextStyle.current,
    delayMillis: Int = 0,
    stepMillis: Int = 26,
    playOnce: Boolean = true,
) {
    val enabled = cascadeTextEnabled()
    var played by rememberSaveable(text) { mutableStateOf(false) }
    // Решение «играть или нет» принимается один раз для этого текста, а не на каждой перерисовке
    val replay = remember(text) { !(playOnce && played) }

    if (!enabled || !replay || text.isBlank()) {
        Text(
            text = text,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            style = style
        )
        return
    }

    val progress = remember(text) { Animatable(0f) }
    val total = text.length + CASCADE_SPREAD
    LaunchedEffect(text) {
        progress.snapTo(0f)
        if (delayMillis > 0) delay(delayMillis.toLong())
        val duration = (total * stepMillis).toInt().coerceIn(380, 1400)
        progress.animateTo(total, tween(duration, easing = LinearEasing))
        played = true
    }

    val base = when {
        color != Color.Unspecified -> color
        style.color != Color.Unspecified -> style.color
        else -> LocalContentColor.current
    }
    val liftPx = with(LocalDensity.current) { 3.dp.toPx() }
    val p = progress.value
    val annotated = buildAnnotatedString {
        append(text)
        // Буквы, которые уже проявились целиком, остаются без стиля: после конца волны текст обычный
        for (i in text.indices) {
            val t = ((p - i) / CASCADE_SPREAD).coerceIn(0f, 1f)
            if (t < 1f) {
                val eased = FastOutSlowInEasing.transform(t)
                addStyle(SpanStyle(color = base.copy(alpha = base.alpha * eased)), i, i + 1)
            }
        }
    }

    Text(
        text = annotated,
        modifier = modifier.graphicsLayer {
            val whole = (progress.value / total).coerceIn(0f, 1f)
            translationY = (1f - FastOutSlowInEasing.transform(whole)) * liftPx
        },
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = style
    )
}

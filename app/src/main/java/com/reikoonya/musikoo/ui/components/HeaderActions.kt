package com.reikoonya.musikoo.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R

/**
 * The button row of a header card (album, artist, playlist, genre): Play, Shuffle and, when
 * [onQueue] is given, a round queue button at the end.
 *
 * The queue button lives here, in the header next to the cover, instead of the top bar:
 * the top bar's right side is for search.
 */
@Composable
fun PlayShuffleQueueRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    onQueue: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val playSource = remember { MutableInteractionSource() }
        Button(
            onClick = onPlay,
            modifier = Modifier
                .weight(1f)
                .pressScale(playSource),
            shape = CircleShape,
            interactionSource = playSource
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Play", fontWeight = FontWeight.Bold)
        }

        val shuffleSource = remember { MutableInteractionSource() }
        FilledTonalButton(
            onClick = onShuffle,
            modifier = Modifier
                .weight(1f)
                .pressScale(shuffleSource),
            shape = CircleShape,
            interactionSource = shuffleSource
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Shuffle", fontWeight = FontWeight.Bold)
        }

        if (onQueue != null) {
            val queueSource = remember { MutableInteractionSource() }
            FilledTonalIconButton(
                onClick = onQueue,
                modifier = Modifier
                    .size(48.dp)
                    .pressScale(queueSource),
                interactionSource = queueSource
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = stringResource(R.string.queue)
                )
            }
        }
    }
}

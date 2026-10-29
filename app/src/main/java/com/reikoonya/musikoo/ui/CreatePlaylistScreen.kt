package com.reikoonya.musikoo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.ScreenTopBar
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.shapeOf

/**
 * Создание плейлиста как «шторка» поверх экрана, как и остальные экраны списков.
 * После создания вызывается [onCreated] с id нового плейлиста.
 */
@Composable
fun CreatePlaylistScreen(
    onCreated: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var name by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focus.requestFocus() }
    }

    fun submit() {
        val clean = name.trim()
        if (clean.isEmpty()) return
        keyboard?.hide()
        val created = PlaylistManager.createPlaylist(context, clean)
        onCreated(created.id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(top = 92.dp, start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                shape = shapeOf(20.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
            Button(
                onClick = { submit() },
                enabled = name.isNotBlank(),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create), fontWeight = FontWeight.Bold)
            }
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
        ScreenTopBar(
            title = stringResource(R.string.create_playlist),
            onBack = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

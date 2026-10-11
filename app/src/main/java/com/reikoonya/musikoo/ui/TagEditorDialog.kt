package com.reikoonya.musikoo.ui

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.GlassIconButton
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.util.CoverChange
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.TagField
import com.reikoonya.musikoo.util.TagWriter
import com.reikoonya.musikoo.util.WriteResult
import kotlinx.coroutines.launch

@Composable
fun TagEditorDialog(
    songs: List<Song>,
    onDismiss: () -> Unit,
    onSaved: (List<Song>) -> Unit,
) {
    val targets = remember { songs.toList() }
    if (targets.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val single = targets.size == 1

    val fields = remember(single) {
        if (single) {
            listOf(
                TagField.TITLE, TagField.ARTIST, TagField.ALBUM, TagField.ALBUM_ARTIST,
                TagField.COMPOSER, TagField.LYRICIST, TagField.GENRE, TagField.YEAR,
                TagField.TRACK, TagField.DISC, TagField.COMMENT
            )
        } else {
            listOf(
                TagField.ARTIST, TagField.ALBUM, TagField.ALBUM_ARTIST, TagField.COMPOSER,
                TagField.LYRICIST, TagField.GENRE, TagField.YEAR, TagField.DISC
            )
        }
    }

    var loading by remember { mutableStateOf(true) }
    var initial by remember { mutableStateOf<Map<TagField, String?>>(emptyMap()) }
    val values = remember { mutableStateMapOf<TagField, String>() }
    var cover by remember { mutableStateOf<CoverChange>(CoverChange.Keep) }

    var saving by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var pendingIndex by remember { mutableIntStateOf(0) }
    var pendingChanges by remember { mutableStateOf<Map<TagField, String>>(emptyMap()) }
    var resumeSignal by remember { mutableIntStateOf(0) }
    val updated = remember { mutableListOf<Song>() }

    val strTagsSaved = stringResource(R.string.tags_saved)
    val strSaveFailed = stringResource(R.string.saving_tags_failed, "")
    val strPermissionDenied = stringResource(R.string.write_permission_denied)
    val strImageFailed = stringResource(R.string.image_open_failed)

    LaunchedEffect(Unit) {
        val loaded = TagWriter.loadInitialValues(context, targets)
        initial = loaded
        fields.forEach { field -> values[field] = loaded[field].orEmpty() }
        loading = false
    }

    fun collectChanges(): Map<TagField, String> {
        val result = mutableMapOf<TagField, String>()
        for (field in fields) {
            val value = values[field].orEmpty()
            val start = initial[field]
            val changed = if (start == null) value.isNotEmpty() else value.trim() != start.trim()
            if (changed) result[field] = value
        }
        return result
    }

    fun completeSaving(failure: String?) {
        saving = false
        if (updated.isNotEmpty()) {
            val done = updated.toList()
            MusicPlayerManager.applyEditedSongs(done)
            onSaved(done)
        } else {
            MusicPlayerManager.cancelTagEdit()
        }
        val message = if (failure == null) strTagsSaved else "$strSaveFailed $failure"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        onDismiss()
    }

    fun cancelSaving() {
        if (updated.isNotEmpty()) {
            completeSaving(strPermissionDenied)
        } else {
            saving = false
            MusicPlayerManager.cancelTagEdit()
            Toast.makeText(context, strPermissionDenied, Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            resumeSignal += 1
        } else {
            cancelSaving()
        }
    }

    suspend fun runWriting(from: Int) {
        var index = from
        while (index < targets.size) {
            when (val result = TagWriter.writeSong(context, targets[index], pendingChanges, cover)) {
                is WriteResult.Updated -> {
                    updated.add(result.song)
                    index++
                    progress = index
                }
                is WriteResult.NeedsPermission -> {
                    pendingIndex = index
                    permissionLauncher.launch(IntentSenderRequest.Builder(result.intentSender).build())
                    return
                }
                is WriteResult.Failed -> {
                    completeSaving("«${targets[index].title}»: ${result.reason}")
                    return
                }
            }
        }
        completeSaving(null)
    }

    LaunchedEffect(resumeSignal) {
        if (resumeSignal > 0) runWriting(pendingIndex)
    }

    fun onSaveClick() {
        val changes = collectChanges()
        if (changes.isEmpty() && cover is CoverChange.Keep) {
            onDismiss()
            return
        }
        pendingChanges = changes
        updated.clear()
        progress = 0
        pendingIndex = 0
        saving = true
        MusicPlayerManager.beginTagEdit(targets.map { it.id }.toSet())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val request = MediaStore.createWriteRequest(
                    context.contentResolver,
                    targets.map { it.uri }
                )
                permissionLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } catch (e: Exception) {
                completeSaving(e.message ?: strPermissionDenied)
            }
        } else {
            resumeSignal += 1
        }
    }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val picked = TagWriter.loadCover(context, uri)
                if (picked != null) {
                    cover = picked
                } else {
                    Toast.makeText(context, strImageFailed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val shownCover: Bitmap? = when (val change = cover) {
        is CoverChange.Replace -> change.preview
        CoverChange.Remove -> null
        CoverChange.Keep -> targets.firstOrNull { it.albumArt != null }?.albumArt
    }
    val changes = collectChanges()
    val canSave = !loading && !saving && (changes.isNotEmpty() || cover !is CoverChange.Keep)

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxSize()) {
                ArtBackdrop(art = shownCover, topScrim = 0.5f, midScrim = 0.85f)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = stringResource(R.string.collapse),
                            onClick = { if (!saving) onDismiss() },
                            width = 44.dp,
                            height = 44.dp,
                            iconSize = 22.dp,
                            shape = CircleShape
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(stringResource(R.string.tag_editor), style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = if (single) targets[0].title else stringResource(R.string.tracks_count, targets.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = { onSaveClick() },
                            enabled = canSave,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }

                    // Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Cover
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(132.dp),
                                shape = RoundedCornerShape(28.dp),
                                color = cardFill(),
                                border = glassStroke()
                            ) {
                                if (shownCover != null) {
                                    Image(
                                        bitmap = shownCover.asImageBitmap(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(28.dp))
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.padding(start = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { pickCover.launch("image/*") },
                                    enabled = !saving && !loading
                                ) {
                                    Text(stringResource(R.string.replace_cover))
                                }
                                TextButton(
                                    onClick = { cover = CoverChange.Remove },
                                    enabled = !saving && !loading && shownCover != null
                                ) {
                                    Text(stringResource(R.string.remove_cover))
                                }
                            }
                        }

                        if (!single) {
                            Text(
                                text = stringResource(R.string.batch_edit_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fields
                        fields.forEach { field ->
                            OutlinedTextField(
                                value = values[field].orEmpty(),
                                onValueChange = { values[field] = it },
                                label = { Text(labelFor(field)) },
                                placeholder = if (!loading && initial[field] == null) {
                                    { Text(stringResource(R.string.different_values)) }
                                } else null,
                                enabled = !saving && !loading,
                                singleLine = field != TagField.COMMENT,
                                minLines = if (field == TagField.COMMENT) 2 else 1,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = if (field == TagField.YEAR) KeyboardType.Number else KeyboardType.Text
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (saving) {
                            LinearProgressIndicator(
                                progress = { progress.toFloat() / targets.size },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = stringResource(R.string.saving_progress, progress, targets.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun labelFor(field: TagField): String = when (field) {
    TagField.TITLE -> stringResource(R.string.tag_title)
    TagField.ARTIST -> stringResource(R.string.tag_artist)
    TagField.ALBUM -> stringResource(R.string.tag_album)
    TagField.ALBUM_ARTIST -> stringResource(R.string.tag_album_artist)
    TagField.COMPOSER -> stringResource(R.string.tag_composer)
    TagField.LYRICIST -> stringResource(R.string.tag_lyricist)
    TagField.GENRE -> stringResource(R.string.tag_genre)
    TagField.YEAR -> stringResource(R.string.tag_year)
    TagField.TRACK -> stringResource(R.string.tag_track_num)
    TagField.DISC -> stringResource(R.string.tag_disc_num)
    TagField.COMMENT -> stringResource(R.string.tag_comment)
}

package com.reikoonya.musikoo.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.statusBarsPadding
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.*
import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.BottomFadeScrim
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
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
    val strDifferentValues = stringResource(R.string.different_values)
    val strBatchEditHint = stringResource(R.string.batch_edit_hint)

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
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        )
    ) {
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // One text field, used by every group
        @Composable
        fun TagInput(f: TagField, modifier: Modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = values[f].orEmpty(),
                onValueChange = { values[f] = it },
                label = { Text(labelFor(f)) },
                placeholder = if (!loading && initial[f] == null) {
                    { Text("Different values") }
                } else null,
                enabled = !saving && !loading,
                singleLine = f != TagField.COMMENT,
                minLines = if (f == TagField.COMMENT) 2 else 1,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (f == TagField.YEAR || f == TagField.TRACK || f == TagField.DISC) {
                        KeyboardType.Number
                    } else {
                        KeyboardType.Text
                    }
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = modifier
            )
        }

        fun has(vararg f: TagField) = f.any { it in fields }

        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed background, same as the other screens
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))

            // Scrolling content runs underneath the floating header
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.ime),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusTop + 96.dp,
                    bottom = navBottom + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TagGroup(title = "Cover") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(230.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shownCover != null) {
                                Image(
                                    bitmap = shownCover.asImageBitmap(),
                                    contentDescription = "Cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { pickCover.launch("image/*") },
                                enabled = !saving && !loading,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Change", maxLines = 1)
                            }
                            FilledTonalButton(
                                onClick = { cover = CoverChange.Remove },
                                enabled = !saving && !loading && shownCover != null,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Remove", maxLines = 1)
                            }
                        }
                    }
                }

                if (!single) {
                    item {
                        Text(
                            text = "Changes apply to all ${targets.size} tracks. Fields marked \"Different values\" stay as they are until you fill them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                if (has(TagField.TITLE, TagField.ARTIST, TagField.ALBUM)) {
                    item {
                        TagGroup(title = "Track") {
                            if (TagField.TITLE in fields) TagInput(TagField.TITLE)
                            if (TagField.ARTIST in fields) TagInput(TagField.ARTIST)
                            if (TagField.ALBUM in fields) TagInput(TagField.ALBUM)
                        }
                    }
                }

                if (has(TagField.ALBUM_ARTIST, TagField.COMPOSER, TagField.LYRICIST)) {
                    item {
                        TagGroup(title = "Credits") {
                            if (TagField.ALBUM_ARTIST in fields) TagInput(TagField.ALBUM_ARTIST)
                            if (TagField.COMPOSER in fields) TagInput(TagField.COMPOSER)
                            if (TagField.LYRICIST in fields) TagInput(TagField.LYRICIST)
                        }
                    }
                }

                if (has(TagField.GENRE, TagField.YEAR, TagField.TRACK, TagField.DISC, TagField.COMMENT)) {
                    item {
                        TagGroup(title = "Details") {
                            if (TagField.GENRE in fields) TagInput(TagField.GENRE)
                            val numbers = listOf(TagField.YEAR, TagField.TRACK, TagField.DISC).filter { it in fields }
                            if (numbers.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    numbers.forEach { TagInput(it, Modifier.weight(1f)) }
                                }
                            }
                            if (TagField.COMMENT in fields) TagInput(TagField.COMMENT)
                        }
                    }
                }
            }

            // Edge-to-edge scrim under the floating header, like the other screens
            TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))

            // Floating header: close, title pill with progress, save
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .bounceClickable { if (!saving) onDismiss() },
                    shape = CircleShape,
                    color = glassFill(),
                    border = glassStroke(),
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = glassFill(),
                    border = glassStroke()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tag Editor",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (single) targets[0].title else "${targets.size} tracks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = { onSaveClick() },
                    enabled = canSave,
                    shape = RoundedCornerShape(32.dp),
                    color = if (canSave) MaterialTheme.colorScheme.primaryContainer else glassFill(),
                    border = glassStroke()
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (canSave) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                    )
                }
            }

            if (saving) {
                LinearProgressIndicator(
                    progress = { progress.toFloat() / targets.size },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
            } else if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                )
            }
        }
    }
}

/** A rounded card that groups related tag fields, matching the other screens. */
@Composable
private fun TagGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = cardFill(),
        border = glassStroke()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
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

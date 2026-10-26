package com.reikoonya.musikoo.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.UpdateInfo
import com.reikoonya.musikoo.util.UpdateManager
import com.reikoonya.musikoo.util.UpdateState

/** "Update available" dialog: release notes, download progress, install. Put it once near the root of the UI. */
@Composable
fun UpdateDialog() {
    if (!UpdateManager.dialogVisible) return
    val context = LocalContext.current
    val state = UpdateManager.state

    val info: UpdateInfo = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Ready -> state.info
        is UpdateState.Failed -> state.info
        else -> null
    } ?: return

    val current = UpdateManager.currentVersion(context)
    val later = { UpdateManager.dismissDialog() }

    AlertDialog(
        onDismissRequest = later,
        shape = shapeOf(36.dp),
        icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
        title = {
            Text(
                text = when (state) {
                    is UpdateState.Downloading -> "Downloading update"
                    is UpdateState.Ready -> "Ready to install"
                    is UpdateState.Failed -> "Update failed"
                    else -> "Update available"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val size = if (info.apkSize > 0) " · ${Formatter.formatShortFileSize(context, info.apkSize)}" else ""
                Text(
                    text = "$current  →  ${info.version}$size",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                when (state) {
                    is UpdateState.Downloading -> {
                        WavyProgress(
                            progress = state.progress,
                            isPlaying = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is UpdateState.Ready -> {
                        if (!UpdateManager.canInstall(context)) {
                            Text(
                                text = "Android will first ask you to allow installs from Musikoo. " +
                                        "Allow it, come back and tap Install again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is UpdateState.Failed -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> Unit
                }

                val notes = plainNotes(info.notes)
                if (notes.isNotEmpty() && state !is UpdateState.Downloading) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Downloading -> TextButton(onClick = later, shape = CircleShape) { Text("Hide") }
                is UpdateState.Ready -> Button(
                    onClick = { UpdateManager.install(context) },
                    shape = CircleShape
                ) { Text("Install") }
                is UpdateState.Failed -> Button(
                    onClick = { UpdateManager.startDownload(context) },
                    shape = CircleShape
                ) { Text("Retry") }
                else -> Button(
                    onClick = { UpdateManager.startDownload(context) },
                    shape = CircleShape
                ) { Text("Download") }
            }
        },
        dismissButton = {
            if (state !is UpdateState.Downloading) {
                TextButton(onClick = later, shape = CircleShape) { Text("Later") }
            }
        }
    )
}

/** GitHub release notes are Markdown: drop the markup, keep headings and bullets readable. */
internal fun plainNotes(raw: String): String =
    raw.lines().joinToString("\n") { line ->
        line.trim()
            .replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^[-*]\\s+"), "• ")
            .replace("**", "")
            .replace("`", "")
    }.replace(Regex("\n{3,}"), "\n\n").trim()
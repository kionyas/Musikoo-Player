package com.reikoonya.musikoo.ui

import com.reikoonya.musikoo.ui.components.AppLoadingIndicator
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Second login step: the user has confirmed access in the browser and presses "Done". */
@Composable
fun LastFmAuthDialog(
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Connect Last.fm") },
        text = {
            Text(
                "The Last.fm website will open. Allow the app access to your account, " +
                        "come back here and press \"Done\".",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy) {
                if (busy) {
                    AppLoadingIndicator(size = 22.dp)
                } else {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        },
    )
}

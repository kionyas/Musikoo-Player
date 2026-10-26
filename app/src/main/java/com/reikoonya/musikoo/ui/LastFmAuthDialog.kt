package com.reikoonya.musikoo.ui

import com.reikoonya.musikoo.ui.components.AppLoadingIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import com.reikoonya.musikoo.ui.components.ShapeStyle
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Second login step: the user has confirmed access in the browser and presses "Done". */
@Composable
fun LastFmAuthDialog(
    starting: Boolean = false,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!busy && !starting) onDismiss() },
        title = { Text("Connect Last.fm") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // The app is waiting for the user to confirm access in the browser
                AppLoadingIndicator(
                    size = 56.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    containerColor = if (ShapeStyle.family == ShapeFamily.MD3E) MaterialTheme.colorScheme.primaryContainer else null
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (starting) "Contacting Last.fm…" else if (busy) "Connecting to Last.fm…" else
                        "The Last.fm website will open. Allow the app access to your account, " +
                                "come back here and press \"Done\".",
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !busy && !starting) {
                if (busy || starting) {
                    AppLoadingIndicator(size = 22.dp)
                } else {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy && !starting) { Text("Cancel") }
        },
    )
}
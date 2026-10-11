package com.reikoonya.musikoo.util

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.reikoonya.musikoo.model.Song

sealed interface DeleteResult {
    /** File deleted. */
    object Deleted : DeleteResult

    /**
     * The user must confirm in the system dialog.
     * [retryAfterApproval] = true (Android 10): after approval, the deletion must be retried.
     * [retryAfterApproval] = false (Android 11+): the system deletes the file itself.
     */
    class NeedsConfirmation(
        val intentSender: IntentSender,
        val retryAfterApproval: Boolean,
    ) : DeleteResult

    class Failed(val reason: String? = null) : DeleteResult
}

object SongDeleter {

    /** Starts the deletion. Works on Android 8–15 without MANAGE_EXTERNAL_STORAGE. */
    fun requestDelete(context: Context, song: Song): DeleteResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val request = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    listOf(song.uri),
                )
                DeleteResult.NeedsConfirmation(request.intentSender, retryAfterApproval = false)
            } catch (e: Exception) {
                DeleteResult.Failed(e.message)
            }
        } else {
            deleteDirect(context, song)
        }
    }

    /** Direct deletion via ContentResolver (Android 10 and below, and the retry after approval). */
    fun deleteDirect(context: Context, song: Song): DeleteResult {
        return try {
            // 0 rows means the record is already gone from MediaStore, so count it as deleted
            context.contentResolver.delete(song.uri, null, null)
            DeleteResult.Deleted
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                DeleteResult.NeedsConfirmation(
                    e.userAction.actionIntent.intentSender,
                    retryAfterApproval = true,
                )
            } else {
                DeleteResult.Failed(e.message)
            }
        } catch (e: Exception) {
            DeleteResult.Failed(e.message)
        }
    }
}

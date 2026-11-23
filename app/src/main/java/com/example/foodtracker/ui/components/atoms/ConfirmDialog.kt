package com.example.foodtracker.ui.components.atoms

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.foodtracker.ui.theme.AppText

/**
 * "Are you sure" for the handful of actions that cannot be undone.
 *
 * Lifted out of `DataBackupScreen`, where it was private, when the plan editor
 * needed the same two buttons: a second copy would have been the fourth
 * hand-rolled dialog in the app and the first one to drift. The destructive
 * verb is the confirm button and `Cancel` is the dismiss, never the other way
 * round — a dialog that puts the safe choice under the thumb is a dialog that
 * gets dismissed without being read.
 */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(title, style = AppText.titleL) },
        text = {
            Text(body, style = AppText.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        confirmButton = {
            AppTextButton(text = confirmLabel, onClick = onConfirm, destructive = true)
        },
        dismissButton = { AppTextButton(text = "Cancel", onClick = onDismiss) },
    )
}

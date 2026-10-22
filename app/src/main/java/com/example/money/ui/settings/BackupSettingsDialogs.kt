package com.example.money.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.components.DialogImeEffect
import com.example.money.ui.components.bringIntoViewOnFocus
import com.example.money.ui.components.scrollAboveIme
import com.example.money.domain.backup.BackupRemoteFile

@Composable
internal fun BackupPasswordDialog(
    title: String,
    requireConfirmation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val valid = password.isNotEmpty() && (!requireConfirmation || password == confirmation)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DialogImeEffect()
            Column(
                modifier = Modifier.scrollAboveIme(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.backup_password_hint))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
                if (requireConfirmation) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it },
                        label = { Text(stringResource(R.string.backup_password_repeat)) },
                        supportingText = if (confirmation.isNotEmpty() && password != confirmation) {
                            { Text(stringResource(R.string.backup_password_mismatch)) }
                        } else {
                            null
                        },
                        isError = confirmation.isNotEmpty() && password != confirmation,
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewOnFocus(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val chars = password.toCharArray()
                    password = ""
                    confirmation = ""
                    onConfirm(chars)
                },
            ) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun BackupRestorePreviewDialog(
    preview: BackupRestorePreviewState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val counts = preview.preview.recordCounts
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_restore_preview_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(preview.fileName)
                Text(
                    stringResource(
                        R.string.backup_restore_preview_counts,
                        counts.goals,
                        counts.debtors,
                        counts.debtPayments,
                        counts.transfers,
                        counts.expenses,
                        counts.luckDiaryEntries,
                    ),
                )
                Text(stringResource(R.string.backup_restore_replace_warning))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.backup_restore_replace_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun DeleteRemoteBackupDialog(
    file: BackupRemoteFile,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_delete_title)) },
        text = { Text(stringResource(R.string.backup_delete_message, file.name)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun DisconnectGoogleBackupDialog(
    onDismiss: () -> Unit,
    onKeepRemote: () -> Unit,
    onDeleteRemote: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_disconnect_title)) },
        text = { Text(stringResource(R.string.backup_disconnect_message)) },
        confirmButton = {
            TextButton(onClick = onKeepRemote) {
                Text(stringResource(R.string.backup_disconnect_keep))
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onDeleteRemote) {
                    Text(stringResource(R.string.backup_disconnect_delete))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}

@Composable
internal fun DeleteAllLocalDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_delete_local_dialog_title)) },
        text = {
            DialogImeEffect()
            Column(
                modifier = Modifier.scrollAboveIme(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.backup_delete_local_dialog_message))
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text(stringResource(R.string.backup_delete_local_confirmation_label)) },
                    supportingText = {
                        Text(stringResource(R.string.backup_delete_local_confirmation_help))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewOnFocus(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = confirmation == DELETE_CONFIRMATION,
                onClick = { onConfirm(confirmation) },
            ) {
                Text(stringResource(R.string.backup_delete_local_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

private const val DELETE_CONFIRMATION = "DELETE"

package com.example.money.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.data.backup.BackupErrorCode
import com.example.money.data.backup.BackupProvider
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.ui.util.withLightHaptic
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.OverdueRed
import java.text.DateFormat
import java.util.Date

@Composable
internal fun GoogleConnectionCard(
    state: BackupSettingsUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val connected = state.connection == GoogleBackupConnection.CONNECTED
    val reconnect = state.connection == GoogleBackupConnection.RECONNECT_REQUIRED
    BackupSectionCard(
        title = stringResource(R.string.backup_google_title),
        body = stringResource(R.string.backup_google_hint),
    ) {
        Text(
            text = stringResource(
                when {
                    connected -> R.string.backup_google_connected
                    reconnect -> R.string.backup_google_reconnect
                    else -> R.string.backup_google_disconnected
                },
            ),
            color = if (connected) FinancePositive else if (reconnect) OverdueRed else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (connected) {
            OutlinedButton(
                onClick = onDisconnect,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
            ) {
                Text(stringResource(R.string.backup_disconnect_action))
            }
        } else {
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy,
            ) {
                Text(
                    stringResource(
                        if (reconnect) R.string.backup_reconnect_action else R.string.backup_connect_action,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun LastBackupCard(state: BackupSettingsUiState) {
    val device = state.deviceState
    BackupSectionCard(
        title = stringResource(R.string.backup_status_title),
        body = if (device.lastSuccessAtEpochMillis == null) {
            stringResource(R.string.backup_status_never)
        } else {
            stringResource(
                R.string.backup_status_last_success,
                DateFormat.getDateTimeInstance().format(Date(device.lastSuccessAtEpochMillis)),
                stringResource(
                    when (device.lastSuccessProvider) {
                        BackupProvider.LOCAL_DOCUMENT -> R.string.backup_provider_local
                        BackupProvider.GOOGLE_DRIVE -> R.string.backup_provider_google
                        null -> R.string.backup_provider_unknown
                    },
                ),
                device.lastSuccessFileName.orEmpty(),
            )
        },
    ) {
        if (device.reconnectRequired) {
            Text(stringResource(R.string.backup_google_reconnect), color = OverdueRed)
        }
        device.lastError?.let { error ->
            Text(
                stringResource(R.string.backup_status_error, deviceErrorMessage(error)),
                color = OverdueRed,
            )
        }
    }
}

@Composable
internal fun RemoteBackupCard(
    file: BackupRemoteFile,
    enabled: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(file.name, fontWeight = FontWeight.SemiBold)
            Text(
                text = file.sizeBytes?.let { stringResource(R.string.backup_file_size, it / 1024) }
                    ?: stringResource(R.string.backup_file_size_unknown),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRestore, enabled = enabled) {
                    Text(stringResource(R.string.backup_restore_action))
                }
                TextButton(onClick = onDelete, enabled = enabled) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }
}

@Composable
internal fun BackupSectionCard(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

@Composable
internal fun BackupSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = withLightHaptic(LocalContext.current, enabled, onCheckedChange),
        )
    }
}

@Composable
private fun deviceErrorMessage(error: BackupErrorCode): String =
    stringResource(
        when (error) {
            BackupErrorCode.NETWORK -> R.string.backup_error_network
            BackupErrorCode.AUTHORIZATION -> R.string.backup_error_authorization
            BackupErrorCode.CREDENTIAL -> R.string.backup_error_credential
            BackupErrorCode.STORAGE -> R.string.backup_error_storage
            BackupErrorCode.ENCRYPTION -> R.string.backup_error_password
            BackupErrorCode.RESTORE -> R.string.backup_error_restore
            BackupErrorCode.UNKNOWN -> R.string.backup_error_unknown
        },
    )

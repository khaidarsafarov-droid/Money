package com.example.money.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.data.backup.AutomaticBackupExecutor
import com.example.money.data.locale.AppLanguageManager
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.ui.util.findActivity

private sealed interface BackupPasswordAction {
    data object LocalExport : BackupPasswordAction
    data object GoogleBackup : BackupPasswordAction
    data object EnableAutomatic : BackupPasswordAction
    data class LocalRestore(val uri: Uri) : BackupPasswordAction
    data class GoogleRestore(val file: BackupRemoteFile) : BackupPasswordAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: BackupSettingsViewModel = viewModel(
        factory = BackupSettingsViewModelFactory(app),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordAction by remember { mutableStateOf<BackupPasswordAction?>(null) }
    var localExportPassword by remember { mutableStateOf<CharArray?>(null) }
    var restorePassword by remember { mutableStateOf<CharArray?>(null) }
    var deleteFile by remember { mutableStateOf<BackupRemoteFile?>(null) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showDeleteAllLocalDataDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            localExportPassword?.fill('\u0000')
            restorePassword?.fill('\u0000')
        }
    }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri ->
        val password = localExportPassword
        localExportPassword = null
        if (uri != null && password != null) {
            viewModel.exportToLocalDocument(uri, password)
        }
        password?.fill('\u0000')
    }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) passwordAction = BackupPasswordAction.LocalRestore(uri)
    }
    val authorizationResolution = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.completeGoogleAuthorization(result.data)
    }

    LaunchedEffect(state.pendingAuthorization) {
        val pendingIntent = state.pendingAuthorization ?: return@LaunchedEffect
        authorizationResolution.launch(
            IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
        )
        viewModel.clearAuthorizationRequest()
    }

    LaunchedEffect(state.localResetCompleted) {
        if (state.localResetCompleted) {
            AppLanguageManager.applyStored(context)
            context.findActivity()?.recreate() ?: onBack()
        }
    }

    val feedback = state.error?.let { backupErrorMessage(it) }
        ?: state.message?.let { backupMessage(it) }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            if (state.error != null && state.restorePreview == null) {
                restorePassword?.fill('\u0000')
                restorePassword = null
            }
            snackbarHostState.showSnackbar(feedback)
            viewModel.consumeFeedback()
        }
    }

    passwordAction?.let { action ->
        BackupPasswordDialog(
            title = passwordDialogTitle(action),
            requireConfirmation = action is BackupPasswordAction.EnableAutomatic,
            onDismiss = { passwordAction = null },
            onConfirm = { password ->
                passwordAction = null
                when (action) {
                    BackupPasswordAction.LocalExport -> {
                        localExportPassword?.fill('\u0000')
                        localExportPassword = password
                        createDocument.launch(AutomaticBackupExecutor.backupFileName(System.currentTimeMillis()))
                    }
                    BackupPasswordAction.GoogleBackup ->
                        viewModel.backupToGoogle(password)
                    BackupPasswordAction.EnableAutomatic ->
                        viewModel.setAutomaticBackupEnabled(true, password)
                    is BackupPasswordAction.LocalRestore -> {
                        restorePassword?.fill('\u0000')
                        restorePassword = password.copyOf()
                        viewModel.previewLocalDocument(action.uri, password)
                    }
                    is BackupPasswordAction.GoogleRestore -> {
                        restorePassword?.fill('\u0000')
                        restorePassword = password.copyOf()
                        viewModel.previewGoogleBackup(action.file, password)
                    }
                }
            },
        )
    }

    state.restorePreview?.let { preview ->
        BackupRestorePreviewDialog(
            preview = preview,
            onDismiss = {
                restorePassword?.fill('\u0000')
                restorePassword = null
                viewModel.cancelRestore()
            },
            onConfirm = {
                val password = restorePassword
                restorePassword = null
                if (password != null) viewModel.confirmRestore(password)
                password?.fill('\u0000')
            },
        )
    }

    deleteFile?.let { file ->
        DeleteRemoteBackupDialog(
            file = file,
            onDismiss = { deleteFile = null },
            onConfirm = {
                deleteFile = null
                viewModel.deleteGoogleBackup(file)
            },
        )
    }

    if (showDisconnectDialog) {
        DisconnectGoogleBackupDialog(
            onDismiss = { showDisconnectDialog = false },
            onKeepRemote = {
                showDisconnectDialog = false
                viewModel.disconnectGoogle(deleteRemoteBackups = false)
            },
            onDeleteRemote = {
                showDisconnectDialog = false
                viewModel.disconnectGoogle(deleteRemoteBackups = true)
            },
        )
    }

    if (showDeleteAllLocalDataDialog) {
        DeleteAllLocalDataDialog(
            onDismiss = { showDeleteAllLocalDataDialog = false },
            onConfirm = { confirmation ->
                showDeleteAllLocalDataDialog = false
                viewModel.deleteAllLocalData(confirmation)
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_up),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isBusy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            item {
                BackupSectionCard(
                    title = stringResource(R.string.backup_local_title),
                    body = stringResource(R.string.backup_local_hint),
                ) {
                    Button(
                        onClick = { passwordAction = BackupPasswordAction.LocalExport },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy,
                    ) {
                        Text(stringResource(R.string.backup_local_export))
                    }
                    OutlinedButton(
                        onClick = { openDocument.launch(arrayOf(BACKUP_MIME_TYPE, "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy,
                    ) {
                        Text(stringResource(R.string.backup_local_import))
                    }
                }
            }
            item {
                GoogleConnectionCard(
                    state = state,
                    onConnect = viewModel::startGoogleAuthorization,
                    onDisconnect = { showDisconnectDialog = true },
                )
            }
            item {
                BackupSectionCard(
                    title = stringResource(R.string.backup_automatic_title),
                    body = stringResource(R.string.backup_automatic_hint),
                ) {
                    BackupSwitchRow(
                        label = stringResource(R.string.backup_automatic_enabled),
                        checked = state.deviceState.automaticBackupEnabled,
                        enabled = !state.isBusy &&
                            state.connection == GoogleBackupConnection.CONNECTED,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                passwordAction = BackupPasswordAction.EnableAutomatic
                            } else {
                                viewModel.setAutomaticBackupEnabled(false)
                            }
                        },
                    )
                    BackupSwitchRow(
                        label = stringResource(R.string.backup_wifi_only),
                        checked = state.deviceState.wifiOnly,
                        enabled = !state.isBusy && state.deviceState.automaticBackupEnabled,
                        onCheckedChange = viewModel::setWifiOnly,
                    )
                    Button(
                        onClick = { passwordAction = BackupPasswordAction.GoogleBackup },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy &&
                            state.connection == GoogleBackupConnection.CONNECTED,
                    ) {
                        Text(stringResource(R.string.backup_now))
                    }
                }
            }
            item {
                LastBackupCard(state)
            }
            if (state.remoteFiles.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.backup_remote_list_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                items(state.remoteFiles, key = BackupRemoteFile::id) { file ->
                    RemoteBackupCard(
                        file = file,
                        enabled = !state.isBusy,
                        onRestore = {
                            passwordAction = BackupPasswordAction.GoogleRestore(file)
                        },
                        onDelete = { deleteFile = file },
                    )
                }
            } else if (state.connection == GoogleBackupConnection.CONNECTED) {
                item {
                    Text(
                        stringResource(R.string.backup_remote_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                BackupSectionCard(
                    title = stringResource(R.string.backup_delete_local_title),
                    body = stringResource(R.string.backup_delete_local_hint),
                ) {
                    Button(
                        onClick = { showDeleteAllLocalDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text(stringResource(R.string.backup_delete_local_action))
                    }
                }
            }
        }
    }
}

@Composable
private fun passwordDialogTitle(action: BackupPasswordAction): String =
    stringResource(
        when (action) {
            BackupPasswordAction.LocalExport -> R.string.backup_password_export_title
            BackupPasswordAction.GoogleBackup -> R.string.backup_password_google_title
            BackupPasswordAction.EnableAutomatic -> R.string.backup_password_automatic_title
            is BackupPasswordAction.LocalRestore,
            is BackupPasswordAction.GoogleRestore,
            -> R.string.backup_password_restore_title
        },
    )

@Composable
private fun backupErrorMessage(error: BackupSettingsError): String =
    stringResource(
        when (error) {
            BackupSettingsError.AUTHORIZATION -> R.string.backup_error_authorization
            BackupSettingsError.RECONNECT_REQUIRED -> R.string.backup_error_reconnect
            BackupSettingsError.NETWORK -> R.string.backup_error_network
            BackupSettingsError.INVALID_PASSWORD -> R.string.backup_error_password
            BackupSettingsError.CREDENTIAL -> R.string.backup_error_credential
            BackupSettingsError.STORAGE -> R.string.backup_error_storage
            BackupSettingsError.RESTORE -> R.string.backup_error_restore
            BackupSettingsError.DELETE_CONFIRMATION -> R.string.backup_error_delete_confirmation
            BackupSettingsError.LOCAL_RESET -> R.string.backup_error_local_reset
            BackupSettingsError.UNKNOWN -> R.string.backup_error_unknown
        },
    )

@Composable
private fun backupMessage(message: BackupSettingsMessage): String =
    stringResource(
        when (message) {
            BackupSettingsMessage.LOCAL_EXPORT_COMPLETE -> R.string.backup_message_local_export
            BackupSettingsMessage.GOOGLE_BACKUP_COMPLETE -> R.string.backup_message_google_backup
            BackupSettingsMessage.RESTORE_COMPLETE -> R.string.backup_message_restore
            BackupSettingsMessage.REMOTE_BACKUP_DELETED -> R.string.backup_message_deleted
            BackupSettingsMessage.DISCONNECTED -> R.string.backup_message_disconnected
            BackupSettingsMessage.AUTOMATIC_BACKUP_ENABLED -> R.string.backup_message_auto_enabled
            BackupSettingsMessage.AUTOMATIC_BACKUP_DISABLED -> R.string.backup_message_auto_disabled
            BackupSettingsMessage.LOCAL_DATA_DELETED -> R.string.backup_message_local_data_deleted
            BackupSettingsMessage.LOCAL_DATA_DELETED_WITH_WARNINGS ->
                R.string.backup_message_local_data_deleted_with_warnings
        },
    )

private const val BACKUP_MIME_TYPE = "application/vnd.money.backup"

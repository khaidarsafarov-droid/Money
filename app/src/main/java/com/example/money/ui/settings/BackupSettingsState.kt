package com.example.money.ui.settings

import android.app.PendingIntent
import com.example.money.data.backup.BackupCredentialException
import com.example.money.data.backup.BackupDeviceState
import com.example.money.data.backup.BackupErrorCode
import com.example.money.data.backup.LocalDataResetWarning
import com.example.money.domain.backup.BackupAuthenticationException
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageHttpException
import com.example.money.domain.backup.BackupStorageNetworkException
import com.example.money.domain.backup.BackupStorageUnauthorizedException
import java.io.IOException

enum class GoogleBackupConnection {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECT_REQUIRED,
}

enum class BackupSettingsOperation {
    IDLE,
    LOADING,
    AUTHORIZING,
    LOCAL_EXPORT,
    GOOGLE_BACKUP,
    PREVIEW,
    RESTORE,
    DELETE,
    DISCONNECT,
    LOCAL_RESET,
}

enum class BackupSettingsError {
    AUTHORIZATION,
    RECONNECT_REQUIRED,
    NETWORK,
    INVALID_PASSWORD,
    CREDENTIAL,
    STORAGE,
    RESTORE,
    DELETE_CONFIRMATION,
    LOCAL_RESET,
    UNKNOWN,
}

enum class BackupSettingsMessage {
    LOCAL_EXPORT_COMPLETE,
    GOOGLE_BACKUP_COMPLETE,
    RESTORE_COMPLETE,
    REMOTE_BACKUP_DELETED,
    DISCONNECTED,
    AUTOMATIC_BACKUP_ENABLED,
    AUTOMATIC_BACKUP_DISABLED,
    LOCAL_DATA_DELETED,
    LOCAL_DATA_DELETED_WITH_WARNINGS,
}

enum class BackupRestoreSource {
    LOCAL_DOCUMENT,
    GOOGLE_DRIVE,
}

data class BackupRestorePreviewState(
    val source: BackupRestoreSource,
    val fileName: String,
    val preview: BackupPreview,
)

data class BackupSettingsUiState(
    val deviceState: BackupDeviceState = BackupDeviceState(),
    val connection: GoogleBackupConnection = GoogleBackupConnection.DISCONNECTED,
    val operation: BackupSettingsOperation = BackupSettingsOperation.IDLE,
    val remoteFiles: List<BackupRemoteFile> = emptyList(),
    val pendingAuthorization: PendingIntent? = null,
    val restorePreview: BackupRestorePreviewState? = null,
    val error: BackupSettingsError? = null,
    val message: BackupSettingsMessage? = null,
    val localResetCompleted: Boolean = false,
    val localResetWarnings: List<LocalDataResetWarning> = emptyList(),
) {
    val isBusy: Boolean
        get() = operation != BackupSettingsOperation.IDLE
}

internal fun mapBackupSettingsFailure(error: Exception): BackupSettingsError =
    when (error) {
        is BackupAuthenticationException -> BackupSettingsError.INVALID_PASSWORD
        is BackupCredentialException -> BackupSettingsError.CREDENTIAL
        is BackupStorageUnauthorizedException -> BackupSettingsError.RECONNECT_REQUIRED
        is BackupStorageNetworkException -> BackupSettingsError.NETWORK
        is BackupStorageHttpException ->
            if (error.statusCode >= 500 || error.statusCode == 429) {
                BackupSettingsError.NETWORK
            } else {
                BackupSettingsError.STORAGE
            }
        else -> BackupSettingsError.UNKNOWN
    }

internal fun BackupSettingsError.toDeviceError(): BackupErrorCode =
    when (this) {
        BackupSettingsError.AUTHORIZATION,
        BackupSettingsError.RECONNECT_REQUIRED,
        -> BackupErrorCode.AUTHORIZATION
        BackupSettingsError.NETWORK -> BackupErrorCode.NETWORK
        BackupSettingsError.INVALID_PASSWORD -> BackupErrorCode.ENCRYPTION
        BackupSettingsError.CREDENTIAL -> BackupErrorCode.CREDENTIAL
        BackupSettingsError.STORAGE -> BackupErrorCode.STORAGE
        BackupSettingsError.RESTORE -> BackupErrorCode.RESTORE
        BackupSettingsError.DELETE_CONFIRMATION,
        BackupSettingsError.LOCAL_RESET,
        BackupSettingsError.UNKNOWN -> BackupErrorCode.UNKNOWN
    }

internal fun Throwable.isBackupNetworkFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is IOException || current is BackupStorageNetworkException) return true
        current = current.cause
    }
    return false
}

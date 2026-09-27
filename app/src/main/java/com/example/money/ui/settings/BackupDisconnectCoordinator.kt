package com.example.money.ui.settings

import com.example.money.data.backup.AutomaticBackupExecutor
import com.example.money.data.backup.AutomaticBackupScheduler
import com.example.money.data.backup.BackupCredentialCache
import com.example.money.data.backup.BackupStateStore
import com.example.money.data.backup.GoogleDriveAuthorizationResult
import com.example.money.data.backup.GoogleDriveAuthorizer
import com.example.money.data.backup.GoogleDriveRevocationResult
import com.example.money.data.backup.GoogleDriveStorageProviderFactory
import kotlinx.coroutines.CancellationException

internal class BackupDisconnectCoordinator(
    private val authorizer: GoogleDriveAuthorizer,
    private val providerFactory: GoogleDriveStorageProviderFactory,
    private val stateStore: BackupStateStore,
    private val credentialCache: BackupCredentialCache,
    private val scheduler: AutomaticBackupScheduler,
) {
    suspend fun disconnect(
        deleteRemoteBackups: Boolean,
        currentAccessToken: String?,
        wifiOnly: Boolean,
    ): BackupSettingsError? {
        var error: BackupSettingsError? = null
        if (deleteRemoteBackups) {
            error = deleteRemoteBackups(currentAccessToken)
        }
        try {
            if (authorizer.revokeAccess() is GoogleDriveRevocationResult.Failure) {
                error = error ?: BackupSettingsError.AUTHORIZATION
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            error = error ?: BackupSettingsError.AUTHORIZATION
        }
        try {
            credentialCache.clear()
        } catch (_: Exception) {
            error = error ?: BackupSettingsError.CREDENTIAL
        }
        try {
            stateStore.update {
                it.copy(
                    automaticBackupEnabled = false,
                    reconnectRequired = false,
                )
            }
            scheduler.syncSchedule(enabled = false, wifiOnly = wifiOnly)
        } catch (_: Exception) {
            error = error ?: BackupSettingsError.UNKNOWN
        }
        return error
    }

    private suspend fun deleteRemoteBackups(currentAccessToken: String?): BackupSettingsError? {
        return try {
            val token = currentAccessToken ?: when (val result = authorizer.authorize()) {
                is GoogleDriveAuthorizationResult.Authorized -> result.accessToken
                is GoogleDriveAuthorizationResult.ResolutionRequired -> null
                is GoogleDriveAuthorizationResult.Failure -> null
            }
            if (token == null) {
                BackupSettingsError.RECONNECT_REQUIRED
            } else {
                val provider = providerFactory.create(token)
                provider.list()
                    .filter { it.name.startsWith(AutomaticBackupExecutor.BACKUP_FILE_PREFIX) }
                    .forEach { provider.delete(it.id) }
                null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            mapBackupSettingsFailure(error)
        }
    }
}

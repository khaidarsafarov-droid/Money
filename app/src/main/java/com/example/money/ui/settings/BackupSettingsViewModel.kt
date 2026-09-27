package com.example.money.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.money.data.backup.AutomaticBackupExecutor
import com.example.money.data.backup.AutomaticBackupScheduler
import com.example.money.data.backup.BackupCredentialCache
import com.example.money.data.backup.BackupDocumentGateway
import com.example.money.data.backup.BackupErrorCode
import com.example.money.data.backup.BackupProvider
import com.example.money.data.backup.BackupStateStore
import com.example.money.data.backup.GoogleDriveAuthorizationResult
import com.example.money.data.backup.GoogleDriveAuthorizer
import com.example.money.data.backup.GoogleDriveStorageProviderFactory
import com.example.money.data.backup.LocalDataResetter
import com.example.money.data.backup.recordBackupError
import com.example.money.data.backup.recordBackupSuccess
import com.example.money.data.backup.requireGoogleReconnect
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.repository.BackupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackupSettingsViewModel(
    private val repository: BackupRepository,
    private val documentGateway: BackupDocumentGateway,
    private val authorizer: GoogleDriveAuthorizer,
    private val providerFactory: GoogleDriveStorageProviderFactory,
    private val stateStore: BackupStateStore,
    private val credentialCache: BackupCredentialCache,
    private val scheduler: AutomaticBackupScheduler,
    private val localDataResetter: LocalDataResetter,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    operationScope: CoroutineScope? = null,
    loadRemoteFilesOnStart: Boolean = true,
) : ViewModel() {

    private val scope = operationScope ?: viewModelScope
    private val mutableState = MutableStateFlow(BackupSettingsUiState())
    val state: StateFlow<BackupSettingsUiState> = mutableState.asStateFlow()

    private var accessToken: String? = null
    private var restoreBytes: ByteArray? = null
    private val disconnectCoordinator = BackupDisconnectCoordinator(
        authorizer = authorizer,
        providerFactory = providerFactory,
        stateStore = stateStore,
        credentialCache = credentialCache,
        scheduler = scheduler,
    )

    init {
        scope.launch {
            stateStore.settings.collect { settings ->
                mutableState.update { current ->
                    current.copy(
                        deviceState = settings,
                        connection = if (settings.reconnectRequired) {
                            GoogleBackupConnection.RECONNECT_REQUIRED
                        } else {
                            current.connection
                        },
                    )
                }
            }
        }
        if (loadRemoteFilesOnStart) refreshRemoteFiles()
    }

    fun refreshRemoteFiles() {
        scope.launch {
            startOperation(BackupSettingsOperation.LOADING)
            try {
                val token = accessToken ?: authorize(silent = true)
                if (token != null) loadRemoteFiles(token)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                finishOperation()
            }
        }
    }

    fun startGoogleAuthorization() {
        scope.launch {
            startOperation(BackupSettingsOperation.AUTHORIZING)
            try {
                authorize(silent = false)?.let { loadRemoteFiles(it) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                finishOperation()
            }
        }
    }

    fun completeGoogleAuthorization(intent: Intent?) {
        scope.launch {
            startOperation(BackupSettingsOperation.AUTHORIZING)
            try {
                when (val result = authorizer.completeAuthorization(intent)) {
                    is GoogleDriveAuthorizationResult.Authorized -> {
                        acceptAuthorization(result.accessToken)
                        loadRemoteFiles(result.accessToken)
                    }
                    is GoogleDriveAuthorizationResult.ResolutionRequired ->
                        requireReconnect(result)
                    is GoogleDriveAuthorizationResult.Failure -> {
                        stateStore.recordBackupError(
                            BackupErrorCode.AUTHORIZATION,
                            currentTimeMillis(),
                        )
                        fail(BackupSettingsError.AUTHORIZATION)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                finishOperation()
            }
        }
    }

    fun clearAuthorizationRequest() {
        mutableState.update { it.copy(pendingAuthorization = null) }
    }

    fun exportToLocalDocument(uri: Uri, password: CharArray) {
        launchWithPassword(password, BackupSettingsOperation.LOCAL_EXPORT) { ownedPassword ->
            val encrypted = repository.exportEncrypted(ownedPassword)
            documentGateway.write(uri, encrypted)
            val fileName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf(String::isNotBlank)
                ?: AutomaticBackupExecutor.backupFileName(currentTimeMillis())
            stateStore.recordBackupSuccess(
                BackupProvider.LOCAL_DOCUMENT,
                fileName,
                currentTimeMillis(),
            )
            mutableState.update {
                it.copy(message = BackupSettingsMessage.LOCAL_EXPORT_COMPLETE)
            }
        }
    }

    fun backupToGoogle(password: CharArray) {
        launchWithPassword(password, BackupSettingsOperation.GOOGLE_BACKUP) { ownedPassword ->
            val token = accessToken ?: authorize(silent = false) ?: return@launchWithPassword
            val fileName = AutomaticBackupExecutor.backupFileName(currentTimeMillis())
            val provider = providerFactory.create(token)
            val encrypted = repository.exportEncrypted(ownedPassword)
            val uploaded = provider.upload(fileName, encrypted)
            stateStore.recordBackupSuccess(
                BackupProvider.GOOGLE_DRIVE,
                uploaded.name,
                currentTimeMillis(),
            )
            mutableState.update {
                it.copy(message = BackupSettingsMessage.GOOGLE_BACKUP_COMPLETE)
            }
            loadRemoteFiles(token)
        }
    }

    fun previewLocalDocument(uri: Uri, password: CharArray) {
        launchWithPassword(password, BackupSettingsOperation.PREVIEW) { ownedPassword ->
            val encrypted = documentGateway.read(uri)
            setRestorePreview(
                encrypted = encrypted,
                password = ownedPassword,
                source = BackupRestoreSource.LOCAL_DOCUMENT,
                fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "local.moneybackup",
            )
        }
    }

    fun previewGoogleBackup(file: BackupRemoteFile, password: CharArray) {
        launchWithPassword(password, BackupSettingsOperation.PREVIEW) { ownedPassword ->
            if (!file.name.startsWith(AutomaticBackupExecutor.BACKUP_FILE_PREFIX)) {
                fail(BackupSettingsError.STORAGE)
                return@launchWithPassword
            }
            val token = accessToken ?: authorize(silent = false) ?: return@launchWithPassword
            val encrypted = providerFactory.create(token).download(file.id)
            setRestorePreview(
                encrypted = encrypted,
                password = ownedPassword,
                source = BackupRestoreSource.GOOGLE_DRIVE,
                fileName = file.name,
            )
        }
    }

    fun confirmRestore(password: CharArray) {
        launchWithPassword(password, BackupSettingsOperation.RESTORE) { ownedPassword ->
            val encrypted = restoreBytes
            if (encrypted == null) {
                fail(BackupSettingsError.RESTORE)
                return@launchWithPassword
            }
            repository.importEncrypted(encrypted, ownedPassword)
            clearRestoreBytes()
            mutableState.update {
                it.copy(
                    restorePreview = null,
                    message = BackupSettingsMessage.RESTORE_COMPLETE,
                )
            }
        }
    }

    fun cancelRestore() {
        clearRestoreBytes()
        mutableState.update { it.copy(restorePreview = null) }
    }

    fun deleteGoogleBackup(file: BackupRemoteFile) {
        scope.launch {
            startOperation(BackupSettingsOperation.DELETE)
            try {
                if (!file.name.startsWith(AutomaticBackupExecutor.BACKUP_FILE_PREFIX)) {
                    fail(BackupSettingsError.STORAGE)
                    return@launch
                }
                val token = accessToken ?: authorize(silent = false) ?: return@launch
                providerFactory.create(token).delete(file.id)
                mutableState.update {
                    it.copy(
                        remoteFiles = it.remoteFiles.filterNot { remote -> remote.id == file.id },
                        message = BackupSettingsMessage.REMOTE_BACKUP_DELETED,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                finishOperation()
            }
        }
    }

    fun disconnectGoogle(deleteRemoteBackups: Boolean) {
        scope.launch {
            startOperation(BackupSettingsOperation.DISCONNECT)
            try {
                val disconnectError = disconnectCoordinator.disconnect(
                    deleteRemoteBackups = deleteRemoteBackups,
                    currentAccessToken = accessToken,
                    wifiOnly = state.value.deviceState.wifiOnly,
                )
                accessToken = null
                clearRestoreBytes()
                mutableState.update {
                    it.copy(
                        connection = GoogleBackupConnection.DISCONNECTED,
                        remoteFiles = emptyList(),
                        pendingAuthorization = null,
                        restorePreview = null,
                        error = disconnectError,
                        message = if (disconnectError == null) {
                            BackupSettingsMessage.DISCONNECTED
                        } else {
                            null
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                finishOperation()
            }
        }
    }

    fun deleteAllLocalData(confirmation: String) {
        if (confirmation != DELETE_CONFIRMATION) {
            fail(BackupSettingsError.DELETE_CONFIRMATION)
            return
        }
        scope.launch {
            startOperation(BackupSettingsOperation.LOCAL_RESET)
            try {
                val result = localDataResetter.reset()
                accessToken = null
                clearRestoreBytes()
                mutableState.update {
                    it.copy(
                        connection = GoogleBackupConnection.DISCONNECTED,
                        remoteFiles = emptyList(),
                        pendingAuthorization = null,
                        restorePreview = null,
                        localResetCompleted = true,
                        localResetWarnings = result.warnings,
                        message = if (result.warnings.isEmpty()) {
                            BackupSettingsMessage.LOCAL_DATA_DELETED
                        } else {
                            BackupSettingsMessage.LOCAL_DATA_DELETED_WITH_WARNINGS
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                fail(BackupSettingsError.LOCAL_RESET)
            } finally {
                finishOperation()
            }
        }
    }

    fun setAutomaticBackupEnabled(enabled: Boolean, password: CharArray? = null) {
        val ownedPassword = password?.copyOf()
        password?.fill('\u0000')
        scope.launch {
            startOperation(BackupSettingsOperation.GOOGLE_BACKUP)
            try {
                if (enabled) {
                    if (ownedPassword == null || ownedPassword.isEmpty()) {
                        fail(BackupSettingsError.CREDENTIAL)
                        return@launch
                    }
                    if (accessToken == null) {
                        fail(BackupSettingsError.RECONNECT_REQUIRED)
                        return@launch
                    }
                    credentialCache.save(ownedPassword)
                } else {
                    credentialCache.clear()
                }
                val wifiOnly = state.value.deviceState.wifiOnly
                stateStore.update { it.copy(automaticBackupEnabled = enabled) }
                scheduler.syncSchedule(enabled, wifiOnly)
                mutableState.update {
                    it.copy(
                        message = if (enabled) {
                            BackupSettingsMessage.AUTOMATIC_BACKUP_ENABLED
                        } else {
                            BackupSettingsMessage.AUTOMATIC_BACKUP_DISABLED
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                ownedPassword?.fill('\u0000')
                finishOperation()
            }
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        scope.launch {
            try {
                stateStore.update { it.copy(wifiOnly = wifiOnly) }
                scheduler.syncSchedule(state.value.deviceState.automaticBackupEnabled, wifiOnly)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            }
        }
    }

    fun consumeFeedback() {
        mutableState.update { it.copy(error = null, message = null) }
    }

    private fun launchWithPassword(
        password: CharArray,
        operation: BackupSettingsOperation,
        block: suspend (CharArray) -> Unit,
    ) {
        val ownedPassword = password.copyOf()
        password.fill('\u0000')
        scope.launch {
            startOperation(operation)
            try {
                if (ownedPassword.isEmpty()) {
                    fail(BackupSettingsError.CREDENTIAL)
                    return@launch
                }
                block(ownedPassword)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            } finally {
                ownedPassword.fill('\u0000')
                finishOperation()
            }
        }
    }

    private suspend fun authorize(silent: Boolean): String? =
        when (val result = authorizer.authorize()) {
            is GoogleDriveAuthorizationResult.Authorized -> {
                acceptAuthorization(result.accessToken)
                result.accessToken
            }
            is GoogleDriveAuthorizationResult.ResolutionRequired -> {
                if (silent) {
                    mutableState.update {
                        it.copy(
                            connection = if (it.deviceState.reconnectRequired) {
                                GoogleBackupConnection.RECONNECT_REQUIRED
                            } else {
                                GoogleBackupConnection.DISCONNECTED
                            },
                            error = if (it.deviceState.reconnectRequired) {
                                BackupSettingsError.RECONNECT_REQUIRED
                            } else {
                                null
                            },
                        )
                    }
                } else {
                    requireReconnect(result)
                }
                null
            }
            is GoogleDriveAuthorizationResult.Failure -> {
                val error = if (result.cause.isBackupNetworkFailure()) {
                    BackupSettingsError.NETWORK
                } else {
                    BackupSettingsError.AUTHORIZATION
                }
                stateStore.recordBackupError(
                    error.toDeviceError(),
                    currentTimeMillis(),
                )
                fail(error)
                null
            }
        }

    private suspend fun acceptAuthorization(token: String) {
        accessToken = token
        stateStore.update {
            it.copy(
                reconnectRequired = false,
                lastError = if (it.lastError == BackupErrorCode.AUTHORIZATION) null else it.lastError,
                lastErrorAtEpochMillis = if (it.lastError == BackupErrorCode.AUTHORIZATION) {
                    null
                } else {
                    it.lastErrorAtEpochMillis
                },
            )
        }
        mutableState.update {
            it.copy(
                connection = GoogleBackupConnection.CONNECTED,
                pendingAuthorization = null,
                error = null,
            )
        }
    }

    private suspend fun requireReconnect(
        result: GoogleDriveAuthorizationResult.ResolutionRequired,
    ) {
        stateStore.requireGoogleReconnect(currentTimeMillis())
        mutableState.update {
            it.copy(
                connection = GoogleBackupConnection.RECONNECT_REQUIRED,
                pendingAuthorization = result.pendingIntent,
                error = BackupSettingsError.RECONNECT_REQUIRED,
            )
        }
    }

    private suspend fun loadRemoteFiles(token: String) {
        try {
            val files = providerFactory.create(token).list()
                .filter { it.name.startsWith(AutomaticBackupExecutor.BACKUP_FILE_PREFIX) }
                .sortedWith(
                    compareByDescending<BackupRemoteFile> {
                        it.createdTime ?: it.modifiedTime.orEmpty()
                    }.thenByDescending { it.name },
                )
            mutableState.update {
                it.copy(
                    connection = GoogleBackupConnection.CONNECTED,
                    remoteFiles = files,
                    error = null,
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFailure(error)
        }
    }

    private fun setRestorePreview(
        encrypted: ByteArray,
        password: CharArray,
        source: BackupRestoreSource,
        fileName: String,
    ) {
        val preview = repository.previewEncrypted(encrypted, password)
        clearRestoreBytes()
        restoreBytes = encrypted
        mutableState.update {
            it.copy(
                restorePreview = BackupRestorePreviewState(source, fileName, preview),
                error = null,
            )
        }
    }

    private suspend fun handleFailure(error: Exception) {
        val mapped = mapBackupSettingsFailure(error)
        try {
            if (mapped == BackupSettingsError.RECONNECT_REQUIRED) {
                stateStore.requireGoogleReconnect(currentTimeMillis())
                accessToken = null
                mutableState.update {
                    it.copy(connection = GoogleBackupConnection.RECONNECT_REQUIRED)
                }
            } else {
                stateStore.recordBackupError(mapped.toDeviceError(), currentTimeMillis())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The typed UI error remains available even if status persistence fails.
        }
        fail(mapped)
    }

    private fun fail(error: BackupSettingsError) {
        mutableState.update { it.copy(error = error, message = null) }
    }

    private fun startOperation(operation: BackupSettingsOperation) {
        mutableState.update { it.copy(operation = operation, error = null, message = null) }
    }

    private fun finishOperation() {
        mutableState.update { it.copy(operation = BackupSettingsOperation.IDLE) }
    }

    private fun clearRestoreBytes() {
        restoreBytes?.fill(0)
        restoreBytes = null
    }

    override fun onCleared() {
        clearRestoreBytes()
        accessToken = null
        super.onCleared()
    }

    private companion object {
        const val DELETE_CONFIRMATION = "DELETE"
    }
}

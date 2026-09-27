package com.example.money.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageHttpException
import com.example.money.domain.backup.BackupStorageNetworkException
import com.example.money.domain.backup.BackupStorageProvider
import com.example.money.domain.backup.BackupStorageUnauthorizedException
import com.example.money.domain.repository.BackupRepository
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

enum class AutomaticBackupOutcome {
    SUCCESS,
    RETRY,
}

class AutomaticBackupExecutor(
    private val backupRepository: BackupRepository,
    private val stateStore: BackupStateStore,
    private val credentialCache: BackupCredentialCache,
    private val authorizer: GoogleDriveAuthorizer,
    private val providerFactory: GoogleDriveStorageProviderFactory,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    suspend fun execute(): AutomaticBackupOutcome {
        if (!stateStore.settings.first().automaticBackupEnabled) {
            return AutomaticBackupOutcome.SUCCESS
        }
        val password = try {
            credentialCache.load()
        } catch (_: BackupCredentialException) {
            stateStore.recordBackupError(BackupErrorCode.CREDENTIAL, currentTimeMillis())
            return AutomaticBackupOutcome.SUCCESS
        }
        if (password == null) {
            stateStore.recordBackupError(BackupErrorCode.CREDENTIAL, currentTimeMillis())
            return AutomaticBackupOutcome.SUCCESS
        }

        return try {
            when (val authorization = authorizer.authorize()) {
                is GoogleDriveAuthorizationResult.Authorized ->
                    runAuthorized(authorization.accessToken, password)
                is GoogleDriveAuthorizationResult.ResolutionRequired -> {
                    stateStore.requireGoogleReconnect(currentTimeMillis())
                    AutomaticBackupOutcome.SUCCESS
                }
                is GoogleDriveAuthorizationResult.Failure -> {
                    if (authorization.cause.isNetworkFailure()) {
                        stateStore.recordBackupError(BackupErrorCode.NETWORK, currentTimeMillis())
                        AutomaticBackupOutcome.RETRY
                    } else {
                        stateStore.requireGoogleReconnect(currentTimeMillis())
                        AutomaticBackupOutcome.SUCCESS
                    }
                }
            }
        } finally {
            password.fill('\u0000')
        }
    }

    private suspend fun runAuthorized(
        accessToken: String,
        password: CharArray,
    ): AutomaticBackupOutcome {
        val provider = providerFactory.create(accessToken)
        val now = currentTimeMillis()
        val fileName = backupFileName(now)
        return try {
            val encrypted = backupRepository.exportEncrypted(password)
            val uploaded = provider.upload(fileName, encrypted)
            retainNewestBackups(provider, uploaded)
            stateStore.recordBackupSuccess(
                provider = BackupProvider.GOOGLE_DRIVE,
                fileName = uploaded.name,
                atEpochMillis = now,
            )
            AutomaticBackupOutcome.SUCCESS
        } catch (error: CancellationException) {
            throw error
        } catch (_: BackupStorageUnauthorizedException) {
            stateStore.requireGoogleReconnect(currentTimeMillis())
            AutomaticBackupOutcome.SUCCESS
        } catch (_: BackupStorageNetworkException) {
            stateStore.recordBackupError(BackupErrorCode.NETWORK, currentTimeMillis())
            AutomaticBackupOutcome.RETRY
        } catch (error: BackupStorageHttpException) {
            val retry = error.statusCode == HTTP_TOO_MANY_REQUESTS || error.statusCode >= 500
            stateStore.recordBackupError(
                if (retry) BackupErrorCode.NETWORK else BackupErrorCode.STORAGE,
                currentTimeMillis(),
            )
            if (retry) AutomaticBackupOutcome.RETRY else AutomaticBackupOutcome.SUCCESS
        } catch (_: Exception) {
            stateStore.recordBackupError(BackupErrorCode.UNKNOWN, currentTimeMillis())
            AutomaticBackupOutcome.SUCCESS
        }
    }

    private suspend fun retainNewestBackups(
        provider: BackupStorageProvider,
        uploaded: BackupRemoteFile,
    ) {
        val listed = provider.list()
        val appBackups = (listed + uploaded)
            .distinctBy { it.id }
            .filter { it.name.startsWith(BACKUP_FILE_PREFIX) }
            .sortedWith(
                compareByDescending<BackupRemoteFile> {
                    it.createdTime ?: it.modifiedTime.orEmpty()
                }.thenByDescending { it.name },
            )
        appBackups.drop(MAX_REMOTE_BACKUPS).forEach { provider.delete(it.id) }
    }

    companion object {
        const val BACKUP_FILE_PREFIX = "money-backup-"
        const val BACKUP_FILE_EXTENSION = ".moneybackup"
        const val MAX_REMOTE_BACKUPS = 5

        fun backupFileName(atEpochMillis: Long): String {
            val formatter = SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return "$BACKUP_FILE_PREFIX${formatter.format(Date(atEpochMillis))}$BACKUP_FILE_EXTENSION"
        }

        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}

class AutomaticBackupWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        return when (app.container.automaticBackupExecutor.execute()) {
            AutomaticBackupOutcome.SUCCESS -> Result.success()
            AutomaticBackupOutcome.RETRY -> Result.retry()
        }
    }
}

private fun Throwable.isNetworkFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is IOException || current is BackupStorageNetworkException) return true
        current = current.cause
    }
    return false
}

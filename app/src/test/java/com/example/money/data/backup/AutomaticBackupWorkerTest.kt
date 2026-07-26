package com.example.money.data.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupRecordCounts
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageHttpException
import com.example.money.domain.backup.BackupStorageNetworkException
import com.example.money.domain.backup.BackupStorageProvider
import com.example.money.domain.backup.BackupStorageUnauthorizedException
import com.example.money.domain.backup.ImportResult
import com.example.money.domain.repository.BackupRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AutomaticBackupWorkerTest {

    @Test
    fun happyPathUploadsEncryptedBackupAndRetainsNewestFiveAppFiles() = runBlocking {
        val stateStore = FakeStateStore(enabledState())
        val credentialCache = FakeCredentialCache("secret".toCharArray())
        val provider = FakeProvider(
            files = (1..6).map { index ->
                remote(
                    id = "old-$index",
                    name = "${AutomaticBackupExecutor.BACKUP_FILE_PREFIX}$index.moneybackup",
                    createdTime = "2026-07-0${index}T00:00:00Z",
                )
            } + remote("other", "another-app.json", "2020-01-01T00:00:00Z"),
        )
        val executor = executor(stateStore, credentialCache, provider)

        val result = executor.execute()

        assertEquals(AutomaticBackupOutcome.SUCCESS, result)
        assertEquals(byteArrayOf(4, 2).toList(), provider.uploadedContent!!.toList())
        assertTrue(provider.uploadedName!!.startsWith(AutomaticBackupExecutor.BACKUP_FILE_PREFIX))
        assertEquals(listOf("old-1", "old-2"), provider.deletedIds.sorted())
        assertFalse(provider.deletedIds.contains("other"))
        assertTrue(credentialCache.loadedPassword!!.all { it == '\u0000' })
        val state = stateStore.value
        assertEquals(BackupProvider.GOOGLE_DRIVE, state.lastSuccessProvider)
        assertEquals(provider.uploadedName, state.lastSuccessFileName)
        assertFalse(state.reconnectRequired)
    }

    @Test
    fun authorizationResolutionMarksReconnectRequiredWithoutRetry() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, javaClass),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stateStore = FakeStateStore(enabledState())
        val executor = executor(
            stateStore = stateStore,
            credentialCache = FakeCredentialCache("secret".toCharArray()),
            provider = FakeProvider(),
            authorizationResult = GoogleDriveAuthorizationResult.ResolutionRequired(pendingIntent),
        )

        assertEquals(AutomaticBackupOutcome.SUCCESS, executor.execute())
        assertTrue(stateStore.value.reconnectRequired)
        assertEquals(BackupErrorCode.AUTHORIZATION, stateStore.value.lastError)
    }

    @Test
    fun networkAndServerFailuresRequestBackoffRetry() = runBlocking {
        val networkState = FakeStateStore(enabledState())
        val networkProvider = FakeProvider(
            uploadFailure = BackupStorageNetworkException("upload", IOException("offline")),
        )
        val serverState = FakeStateStore(enabledState())
        val serverProvider = FakeProvider(
            uploadFailure = BackupStorageHttpException("upload", 503, "unavailable"),
        )

        assertEquals(
            AutomaticBackupOutcome.RETRY,
            executor(networkState, FakeCredentialCache("one".toCharArray()), networkProvider).execute(),
        )
        assertEquals(
            AutomaticBackupOutcome.RETRY,
            executor(serverState, FakeCredentialCache("two".toCharArray()), serverProvider).execute(),
        )
        assertEquals(BackupErrorCode.NETWORK, networkState.value.lastError)
        assertEquals(BackupErrorCode.NETWORK, serverState.value.lastError)
    }

    @Test
    fun unauthorizedUploadMarksReconnectRequiredWithoutRetryLoop() = runBlocking {
        val stateStore = FakeStateStore(enabledState())
        val provider = FakeProvider(
            uploadFailure = BackupStorageUnauthorizedException("upload"),
        )

        val result = executor(
            stateStore,
            FakeCredentialCache("secret".toCharArray()),
            provider,
        ).execute()

        assertEquals(AutomaticBackupOutcome.SUCCESS, result)
        assertTrue(stateStore.value.reconnectRequired)
    }

    private fun executor(
        stateStore: FakeStateStore,
        credentialCache: FakeCredentialCache,
        provider: FakeProvider,
        authorizationResult: GoogleDriveAuthorizationResult =
            GoogleDriveAuthorizationResult.Authorized("ephemeral-token"),
    ) = AutomaticBackupExecutor(
        backupRepository = FakeBackupRepository(),
        stateStore = stateStore,
        credentialCache = credentialCache,
        authorizer = FakeAuthorizer(authorizationResult),
        providerFactory = GoogleDriveStorageProviderFactory { provider },
        currentTimeMillis = { NOW },
    )

    private fun enabledState() = BackupDeviceState(automaticBackupEnabled = true)

    private fun remote(id: String, name: String, createdTime: String) =
        BackupRemoteFile(id, name, 2, createdTime, createdTime)

    private class FakeStateStore(initial: BackupDeviceState) : BackupStateStore {
        private val mutableState = MutableStateFlow(initial)
        override val settings: Flow<BackupDeviceState> = mutableState
        val value: BackupDeviceState get() = mutableState.value

        override suspend fun update(transform: (BackupDeviceState) -> BackupDeviceState) {
            mutableState.value = transform(mutableState.value)
        }
    }

    private class FakeCredentialCache(
        private val password: CharArray?,
    ) : BackupCredentialCache {
        var loadedPassword: CharArray? = null

        override suspend fun save(password: CharArray) = Unit

        override suspend fun load(): CharArray? =
            password?.copyOf()?.also { loadedPassword = it }

        override suspend fun clear() = Unit
    }

    private class FakeAuthorizer(
        private val result: GoogleDriveAuthorizationResult,
    ) : GoogleDriveAuthorizer {
        override suspend fun authorize(): GoogleDriveAuthorizationResult = result

        override fun completeAuthorization(intent: Intent?): GoogleDriveAuthorizationResult = result

        override suspend fun revokeAccess(): GoogleDriveRevocationResult =
            GoogleDriveRevocationResult.Revoked
    }

    private class FakeBackupRepository : BackupRepository {
        override suspend fun exportEncrypted(password: CharArray): ByteArray = byteArrayOf(4, 2)

        override fun preview(encrypted: ByteArray, password: CharArray): BackupPreview =
            BackupPreview(1, 0, "test", BackupRecordCounts.EMPTY, "", false)

        override suspend fun importEncrypted(encrypted: ByteArray, password: CharArray): ImportResult =
            ImportResult()

        override suspend fun exportBundle(): BackupBundle = error("Not used")

        override suspend fun importBundle(bundle: BackupBundle): ImportResult = error("Not used")
    }

    private class FakeProvider(
        private val files: List<BackupRemoteFile> = emptyList(),
        private val uploadFailure: Exception? = null,
    ) : BackupStorageProvider {
        var uploadedName: String? = null
        var uploadedContent: ByteArray? = null
        val deletedIds = mutableListOf<String>()

        override suspend fun list(): List<BackupRemoteFile> = files

        override suspend fun upload(fileName: String, content: ByteArray): BackupRemoteFile {
            uploadFailure?.let { throw it }
            uploadedName = fileName
            uploadedContent = content.copyOf()
            return remote("uploaded", fileName, "2026-07-07T00:00:00Z")
        }

        override suspend fun download(fileId: String): ByteArray = error("Not used")

        override suspend fun delete(fileId: String) {
            deletedIds += fileId
        }

        private fun remote(id: String, name: String, createdTime: String) =
            BackupRemoteFile(id, name, 2, createdTime, createdTime)
    }

    private companion object {
        const val NOW = 1_753_488_000_000L
    }
}

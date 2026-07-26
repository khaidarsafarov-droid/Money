package com.example.money.ui.settings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.money.data.backup.AutomaticBackupExecutor
import com.example.money.data.backup.AutomaticBackupScheduler
import com.example.money.data.backup.BackupCredentialCache
import com.example.money.data.backup.BackupDocumentGateway
import com.example.money.data.backup.BackupDeviceState
import com.example.money.data.backup.BackupStateStore
import com.example.money.data.backup.GoogleDriveAuthorizationResult
import com.example.money.data.backup.GoogleDriveAuthorizer
import com.example.money.data.backup.GoogleDriveRevocationResult
import com.example.money.data.backup.GoogleDriveStorageProviderFactory
import com.example.money.data.backup.LocalDataResetResult
import com.example.money.data.backup.LocalDataResetWarning
import com.example.money.data.backup.LocalDataResetter
import com.example.money.domain.backup.BackupAuthenticationException
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupRecordCounts
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageNetworkException
import com.example.money.domain.backup.BackupStorageProvider
import com.example.money.domain.backup.ImportResult
import com.example.money.domain.repository.BackupRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackupSettingsViewModelTest {

    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun startsDisconnectedWithoutGoogleAuthorization() {
        val viewModel = viewModel(FakeRepository(), FakeDocumentGateway(), FakeAuthorizer())

        assertEquals(GoogleBackupConnection.DISCONNECTED, viewModel.state.value.connection)
        assertEquals(BackupSettingsOperation.IDLE, viewModel.state.value.operation)
        assertTrue(viewModel.state.value.remoteFiles.isEmpty())
        assertNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.message)
        assertNull(viewModel.state.value.restorePreview)
    }

    @Test
    fun successfulAuthorizationSetsConnectedAndListsRemoteFiles() {
        val remote = BackupRemoteFile(
            id = "drive-1",
            name = "${AutomaticBackupExecutor.BACKUP_FILE_PREFIX}manual.moneybackup",
            sizeBytes = 12L,
            createdTime = "2026-07-26T00:00:00.000Z",
            modifiedTime = "2026-07-26T00:00:00.000Z",
        )
        val provider = FakeProvider(initialFiles = listOf(remote))
        val viewModel = viewModel(
            FakeRepository(),
            FakeDocumentGateway(),
            FakeAuthorizer(),
            provider = provider,
        )

        viewModel.startGoogleAuthorization()

        assertEquals(GoogleBackupConnection.CONNECTED, viewModel.state.value.connection)
        assertEquals(listOf(remote), viewModel.state.value.remoteFiles)
        assertEquals(BackupSettingsOperation.IDLE, viewModel.state.value.operation)
        assertNull(viewModel.state.value.error)
        assertTrue(provider.operations.contains("list"))
    }

    @Test
    fun googleBackupReportsSuccessAfterUpload() {
        val provider = FakeProvider()
        val viewModel = viewModel(
            FakeRepository(),
            FakeDocumentGateway(),
            FakeAuthorizer(),
            provider = provider,
        )

        viewModel.startGoogleAuthorization()
        viewModel.backupToGoogle("backup-pass".toCharArray())

        assertEquals(GoogleBackupConnection.CONNECTED, viewModel.state.value.connection)
        assertEquals(BackupSettingsMessage.GOOGLE_BACKUP_COMPLETE, viewModel.state.value.message)
        assertEquals(BackupSettingsOperation.IDLE, viewModel.state.value.operation)
        assertTrue(provider.operations.contains("upload"))
        assertEquals(1, viewModel.state.value.remoteFiles.size)
        assertTrue(
            viewModel.state.value.remoteFiles.first().name
                .startsWith(AutomaticBackupExecutor.BACKUP_FILE_PREFIX),
        )
    }

    @Test
    fun localExportReadPreviewAndConfirmedRestoreUseDocumentGateway() {
        val repository = FakeRepository()
        val documents = FakeDocumentGateway()
        val viewModel = viewModel(repository, documents, FakeAuthorizer())
        val uri = Uri.parse("content://documents/backup.moneybackup")

        viewModel.exportToLocalDocument(uri, "export-pass".toCharArray())

        assertArrayEquals(repository.encryptedExport, documents.content)
        assertEquals(
            BackupSettingsMessage.LOCAL_EXPORT_COMPLETE,
            viewModel.state.value.message,
        )

        viewModel.previewLocalDocument(uri, "restore-pass".toCharArray())

        val preview = viewModel.state.value.restorePreview
        assertNotNull(preview)
        assertEquals(BackupRestoreSource.LOCAL_DOCUMENT, preview!!.source)
        assertEquals(6, preview.preview.recordCounts.total)
        assertEquals(BackupSettingsOperation.IDLE, viewModel.state.value.operation)

        viewModel.confirmRestore("restore-pass".toCharArray())

        assertArrayEquals(repository.encryptedExport, repository.imported)
        assertEquals(BackupSettingsMessage.RESTORE_COMPLETE, viewModel.state.value.message)
        assertEquals(null, viewModel.state.value.restorePreview)
    }

    @Test
    fun googleRestorePreviewAndWrongPasswordSurfaceTypedFailure() {
        val remote = BackupRemoteFile(
            id = "drive-2",
            name = "${AutomaticBackupExecutor.BACKUP_FILE_PREFIX}cloud.moneybackup",
            sizeBytes = 9L,
            createdTime = "2026-07-26T01:00:00.000Z",
            modifiedTime = "2026-07-26T01:00:00.000Z",
        )
        val repository = FakeRepository(previewError = BackupAuthenticationException())
        val provider = FakeProvider(
            initialFiles = listOf(remote),
            downloadBytes = byteArrayOf(1, 2, 3),
        )
        val viewModel = viewModel(
            repository,
            FakeDocumentGateway(),
            FakeAuthorizer(),
            provider = provider,
        )

        viewModel.startGoogleAuthorization()
        viewModel.previewGoogleBackup(remote, "bad-pass".toCharArray())

        assertEquals(BackupSettingsError.INVALID_PASSWORD, viewModel.state.value.error)
        assertNull(viewModel.state.value.restorePreview)
        assertNull(viewModel.state.value.message)
        assertEquals(BackupSettingsOperation.IDLE, viewModel.state.value.operation)
        assertTrue(provider.operations.contains("download"))
    }

    @Test
    fun authorizationResolutionSurfacesPendingIntentAsTypedReconnectState() {
        val context: Context = RuntimeEnvironment.getApplication()
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, javaClass),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val viewModel = viewModel(
            FakeRepository(),
            FakeDocumentGateway(),
            FakeAuthorizer(
                result = GoogleDriveAuthorizationResult.ResolutionRequired(pendingIntent),
            ),
        )

        viewModel.startGoogleAuthorization()

        assertEquals(
            GoogleBackupConnection.RECONNECT_REQUIRED,
            viewModel.state.value.connection,
        )
        assertEquals(BackupSettingsError.RECONNECT_REQUIRED, viewModel.state.value.error)
        assertEquals(pendingIntent, viewModel.state.value.pendingAuthorization)
    }

    @Test
    fun networkFailureDuringRemoteListMapsToNetworkError() {
        val provider = FakeProvider(
            listError = BackupStorageNetworkException("list", IOException("offline")),
        )
        val viewModel = viewModel(
            FakeRepository(),
            FakeDocumentGateway(),
            FakeAuthorizer(),
            provider = provider,
        )

        viewModel.startGoogleAuthorization()

        assertEquals(BackupSettingsError.NETWORK, viewModel.state.value.error)
        assertNull(viewModel.state.value.message)
        assertEquals(BackupSettingsOperation.IDLE, viewModel.state.value.operation)
    }

    @Test
    fun localResetRequiresLiteralDeleteAndNeverTouchesRemoteBackups() {
        val resetter = RecordingLocalDataResetter(
            LocalDataResetResult(
                warnings = listOf(LocalDataResetWarning("calendar", "permission revoked")),
            ),
        )
        val provider = FakeProvider()
        val viewModel = viewModel(
            FakeRepository(),
            FakeDocumentGateway(),
            FakeAuthorizer(),
            localDataResetter = resetter,
            provider = provider,
        )

        viewModel.deleteAllLocalData("delete")

        assertEquals(0, resetter.callCount)
        assertEquals(BackupSettingsError.DELETE_CONFIRMATION, viewModel.state.value.error)

        viewModel.deleteAllLocalData("DELETE")

        assertEquals(1, resetter.callCount)
        assertTrue(provider.operations.isEmpty())
        assertTrue(provider.deletedFileIds.isEmpty())
        assertTrue(viewModel.state.value.localResetCompleted)
        assertEquals(
            BackupSettingsMessage.LOCAL_DATA_DELETED_WITH_WARNINGS,
            viewModel.state.value.message,
        )
        assertEquals(1, viewModel.state.value.localResetWarnings.size)
    }

    private fun viewModel(
        repository: FakeRepository,
        documents: FakeDocumentGateway,
        authorizer: FakeAuthorizer,
        localDataResetter: LocalDataResetter = LocalDataResetter { LocalDataResetResult() },
        provider: FakeProvider = FakeProvider(),
    ) = BackupSettingsViewModel(
        repository = repository,
        documentGateway = documents,
        authorizer = authorizer,
        providerFactory = GoogleDriveStorageProviderFactory { provider },
        stateStore = FakeStateStore(),
        credentialCache = FakeCredentialCache(),
        scheduler = AutomaticBackupScheduler { _, _ -> },
        localDataResetter = localDataResetter,
        operationScope = scope,
        loadRemoteFilesOnStart = false,
    )

    private class FakeRepository(
        private val previewError: Exception? = null,
    ) : BackupRepository {
        val encryptedExport = byteArrayOf(7, 8, 9)
        var imported: ByteArray? = null

        override suspend fun exportEncrypted(password: CharArray): ByteArray =
            encryptedExport.copyOf()

        override fun preview(encrypted: ByteArray, password: CharArray): BackupPreview {
            previewError?.let { throw it }
            return BackupPreview(
                formatVersion = 1,
                createdAtEpochMillis = 100,
                appVersionName = "test",
                recordCounts = BackupRecordCounts(1, 1, 1, 1, 1, 1),
                contentSha256 = "hash",
                hasVisionImage = true,
            )
        }

        override suspend fun importEncrypted(
            encrypted: ByteArray,
            password: CharArray,
        ): ImportResult {
            imported = encrypted.copyOf()
            return ImportResult()
        }

        override suspend fun exportBundle(): BackupBundle = error("Not used")

        override suspend fun importBundle(bundle: BackupBundle): ImportResult = error("Not used")
    }

    private class FakeDocumentGateway : BackupDocumentGateway {
        var content = ByteArray(0)

        override suspend fun read(uri: Uri): ByteArray = content.copyOf()

        override suspend fun write(uri: Uri, content: ByteArray) {
            this.content = content.copyOf()
        }
    }

    private class FakeAuthorizer(
        private val result: GoogleDriveAuthorizationResult =
            GoogleDriveAuthorizationResult.Authorized("temporary-token"),
    ) : GoogleDriveAuthorizer {
        override suspend fun authorize(): GoogleDriveAuthorizationResult = result

        override fun completeAuthorization(intent: Intent?): GoogleDriveAuthorizationResult = result

        override suspend fun revokeAccess(): GoogleDriveRevocationResult =
            GoogleDriveRevocationResult.Revoked
    }

    private class FakeProvider(
        private val initialFiles: List<BackupRemoteFile> = emptyList(),
        private val downloadBytes: ByteArray = byteArrayOf(7, 8, 9),
        private val listError: Exception? = null,
    ) : BackupStorageProvider {
        val deletedFileIds = mutableListOf<String>()
        val operations = mutableListOf<String>()
        private val files = initialFiles.toMutableList()

        override suspend fun list(): List<BackupRemoteFile> {
            operations += "list"
            listError?.let { throw it }
            return files.toList()
        }

        override suspend fun upload(fileName: String, content: ByteArray): BackupRemoteFile {
            operations += "upload"
            val uploaded = BackupRemoteFile(
                id = "uploaded-${files.size + 1}",
                name = fileName,
                sizeBytes = content.size.toLong(),
                createdTime = "now",
                modifiedTime = "now",
            )
            files += uploaded
            return uploaded
        }

        override suspend fun download(fileId: String): ByteArray {
            operations += "download"
            return downloadBytes.copyOf()
        }

        override suspend fun delete(fileId: String) {
            operations += "delete"
            deletedFileIds += fileId
            files.removeAll { it.id == fileId }
        }
    }

    private class RecordingLocalDataResetter(
        private val result: LocalDataResetResult,
    ) : LocalDataResetter {
        var callCount = 0

        override suspend fun reset(): LocalDataResetResult {
            callCount += 1
            return result
        }
    }

    private class FakeStateStore : BackupStateStore {
        private val mutableState = MutableStateFlow(BackupDeviceState())
        override val settings: Flow<BackupDeviceState> = mutableState

        override suspend fun update(transform: (BackupDeviceState) -> BackupDeviceState) {
            mutableState.value = transform(mutableState.value)
        }
    }

    private class FakeCredentialCache : BackupCredentialCache {
        private var password: CharArray? = null

        override suspend fun save(password: CharArray) {
            this.password = password.copyOf()
        }

        override suspend fun load(): CharArray? = password?.copyOf()

        override suspend fun clear() {
            password?.fill('\u0000')
            password = null
        }
    }
}

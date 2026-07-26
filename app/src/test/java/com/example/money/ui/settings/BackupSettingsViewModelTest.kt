package com.example.money.ui.settings

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.money.data.backup.AutomaticBackupScheduler
import com.example.money.data.backup.BackupCredentialCache
import com.example.money.data.backup.BackupDocumentGateway
import com.example.money.data.backup.BackupDeviceState
import com.example.money.data.backup.BackupStateStore
import com.example.money.data.backup.GoogleDriveAuthorizationResult
import com.example.money.data.backup.GoogleDriveAuthorizer
import com.example.money.data.backup.GoogleDriveRevocationResult
import com.example.money.data.backup.GoogleDriveStorageProviderFactory
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupRecordCounts
import com.example.money.domain.backup.BackupRemoteFile
import com.example.money.domain.backup.BackupStorageProvider
import com.example.money.domain.backup.ImportResult
import com.example.money.domain.repository.BackupRepository
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

        viewModel.confirmRestore("restore-pass".toCharArray())

        assertArrayEquals(repository.encryptedExport, repository.imported)
        assertEquals(BackupSettingsMessage.RESTORE_COMPLETE, viewModel.state.value.message)
        assertEquals(null, viewModel.state.value.restorePreview)
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

    private fun viewModel(
        repository: FakeRepository,
        documents: FakeDocumentGateway,
        authorizer: FakeAuthorizer,
    ) = BackupSettingsViewModel(
        repository = repository,
        documentGateway = documents,
        authorizer = authorizer,
        providerFactory = GoogleDriveStorageProviderFactory { FakeProvider() },
        stateStore = FakeStateStore(),
        credentialCache = FakeCredentialCache(),
        scheduler = AutomaticBackupScheduler { _, _ -> },
        operationScope = scope,
        loadRemoteFilesOnStart = false,
    )

    private class FakeRepository : BackupRepository {
        val encryptedExport = byteArrayOf(7, 8, 9)
        var imported: ByteArray? = null

        override suspend fun exportEncrypted(password: CharArray): ByteArray =
            encryptedExport.copyOf()

        override fun preview(encrypted: ByteArray, password: CharArray): BackupPreview =
            BackupPreview(
                formatVersion = 1,
                createdAtEpochMillis = 100,
                appVersionName = "test",
                recordCounts = BackupRecordCounts(1, 1, 1, 1, 1, 1),
                contentSha256 = "hash",
                hasVisionImage = true,
            )

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

    private class FakeProvider : BackupStorageProvider {
        override suspend fun list(): List<BackupRemoteFile> = emptyList()

        override suspend fun upload(fileName: String, content: ByteArray): BackupRemoteFile =
            BackupRemoteFile("id", fileName, content.size.toLong(), "now", "now")

        override suspend fun download(fileId: String): ByteArray = byteArrayOf(7, 8, 9)

        override suspend fun delete(fileId: String) = Unit
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

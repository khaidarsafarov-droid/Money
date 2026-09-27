package com.example.money.data.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupDeviceStateStoreTest {

    private lateinit var directory: File
    private lateinit var scope: CoroutineScope
    private lateinit var store: BackupDeviceStateStore

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("backup-state").toFile()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        store = BackupDeviceStateStore(
            PreferenceDataStoreFactory.create(scope = scope) {
                File(directory, "backup.preferences_pb")
            },
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    @Test
    fun settingsAreDeviceLocalAndPersistEveryAutomaticBackupStatusField() = runBlocking {
        assertEquals(BackupDeviceState(), store.settings.first())

        store.update { it.copy(automaticBackupEnabled = true, wifiOnly = false) }
        store.recordBackupError(BackupErrorCode.NETWORK, 100L)
        store.requireGoogleReconnect(200L)

        val failed = store.settings.first()
        assertTrue(failed.automaticBackupEnabled)
        assertFalse(failed.wifiOnly)
        assertTrue(failed.reconnectRequired)
        assertEquals(BackupErrorCode.AUTHORIZATION, failed.lastError)
        assertEquals(200L, failed.lastErrorAtEpochMillis)

        store.recordBackupSuccess(BackupProvider.GOOGLE_DRIVE, "money-backup-test", 300L)

        val successful = store.settings.first()
        assertEquals(300L, successful.lastSuccessAtEpochMillis)
        assertEquals(BackupProvider.GOOGLE_DRIVE, successful.lastSuccessProvider)
        assertEquals("money-backup-test", successful.lastSuccessFileName)
        assertFalse(successful.reconnectRequired)
        assertNull(successful.lastError)
        assertNull(successful.lastErrorAtEpochMillis)
    }
}

package com.example.money.data.backup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.SyncTombstoneEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.data.sync.DeviceIdProvider
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.settings.AppLanguage
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.UserProfile
import com.example.money.ui.home.WelcomePreferences
import com.example.money.widget.WidgetGoalSnapshot
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.io.encoding.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class LocalDataResetterTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferencesStore: MoneyPreferencesStore
    private lateinit var deviceIdProvider: DeviceIdProvider
    private lateinit var visionImageStore: VisionImageStore
    private lateinit var backupStateDirectory: File
    private lateinit var backupStateScope: CoroutineScope
    private lateinit var backupStateStore: BackupDeviceStateStore
    private lateinit var credentialPreferencesName: String
    private lateinit var credentialCache: EncryptedBackupCredentialCache

    @Before
    fun setUp() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .build()
        preferencesStore = MoneyPreferencesStore(context)
        preferencesStore.clearAll()
        deviceIdProvider = DeviceIdProvider(context)
        deviceIdProvider.clear()
        visionImageStore = VisionImageStore(context, preferencesStore)
        visionImageStore.deleteCurrent()
        backupStateDirectory = Files.createTempDirectory("local-reset-backup-state").toFile()
        backupStateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        backupStateStore = BackupDeviceStateStore(
            PreferenceDataStoreFactory.create(scope = backupStateScope) {
                File(backupStateDirectory, "backup.preferences_pb")
            },
        )
        credentialPreferencesName = "local-reset-credential-${System.nanoTime()}"
        credentialCache = EncryptedBackupCredentialCache(
            preferences = context.getSharedPreferences(
                credentialPreferencesName,
                Context.MODE_PRIVATE,
            ),
            cipher = ReversibleCredentialCipher(),
            ioDispatcher = Dispatchers.Unconfined,
        )
        AppLanguageStorage.clear(context)
        WelcomePreferences.clear(context)
    }

    @After
    fun tearDown() {
        database.close()
        backupStateScope.cancel()
        backupStateDirectory.deleteRecursively()
        context.getSharedPreferences(credentialPreferencesName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun resetClearsAllRoomPreferencesImageCredentialAndDeviceState() = runBlocking {
        seedAllRoomTables()
        preferencesStore.writeWidgetSnapshot(
            WidgetGoalSnapshot(
                currentAmount = 10.0,
                targetAmount = 20.0,
                progressFraction = 0.5f,
                progressPercent = 50,
                remainingAmount = 10.0,
                pendingCount = 1,
                updatedAt = 123L,
                currencyRegion = CurrencyRegion.US,
            ),
        )
        preferencesStore.updateUserProfile {
            UserProfile(displayName = "Ada", onboardingCompleted = true)
        }
        backupStateStore.update {
            it.copy(
                automaticBackupEnabled = true,
                lastSuccessFileName = "remote.moneybackup",
            )
        }
        credentialCache.save("secret".toCharArray())
        visionImageStore.writeCurrent(
            com.example.money.domain.backup.BackupVisionImage(
                fileName = "vision.png",
                mimeType = "image/png",
                base64Data = Base64.encode(byteArrayOf(1, 2, 3)),
            ),
        )
        val visionImageFile = File(
            requireNotNull(preferencesStore.goalPsychologyProfile.first().visionPhotoPath),
        )
        assertTrue(visionImageFile.exists())
        AppLanguageStorage.write(context, AppLanguage.EN)
        WelcomePreferences.setWelcomeSeen(context, true)
        val deviceIdBeforeReset = deviceIdProvider.getDeviceId()
        val cleanedCalendarLinks = mutableListOf<LocalCalendarLink>()
        var peerSyncStopped = false
        var schedulesCancelled = false
        var notificationsCancelled = false
        val resetter = resetter(
            cleanCalendarLinks = { links -> cleanedCalendarLinks += links },
            stopPeerSync = { peerSyncStopped = true },
            cancelSchedules = { schedulesCancelled = true },
            cancelNotifications = { notificationsCancelled = true },
        )

        val result = resetter.reset()

        assertTrue(result.warnings.isEmpty())
        assertAllRoomTablesEmpty()
        assertEquals(WidgetGoalSnapshot.Empty, preferencesStore.widgetSnapshot.first())
        assertEquals(UserProfile(), preferencesStore.userProfile.first())
        assertEquals(BackupDeviceState(), backupStateStore.settings.first())
        assertNull(credentialCache.load())
        assertNull(visionImageStore.readCurrent())
        assertFalse(visionImageFile.exists())
        assertEquals(AppLanguage.SYSTEM, AppLanguageStorage.read(context))
        assertFalse(WelcomePreferences.isWelcomeSeen(context))
        assertNotEquals(deviceIdBeforeReset, deviceIdProvider.getDeviceId())
        assertEquals(listOf(LocalCalendarLink(debtorId = 2L, eventId = "calendar-2")), cleanedCalendarLinks)
        assertTrue(peerSyncStopped)
        assertTrue(schedulesCancelled)
        assertTrue(notificationsCancelled)
    }

    @Test
    fun resetWaitsForSyncWriteGuardAndSuppressesSyncRecording() = runBlocking {
        seedAllRoomTables()
        var recordingWasSuppressed = false
        val resetter = resetter(
            cancelSchedules = {
                recordingWasSuppressed = SyncWriteGuard.isRecordingSuppressed
            },
        )
        val lockEntered = CompletableDeferred<Unit>()
        val releaseLock = CompletableDeferred<Unit>()
        val lockHolder = launch {
            SyncWriteGuard.withWriteLock {
                lockEntered.complete(Unit)
                releaseLock.await()
            }
        }
        lockEntered.await()

        val reset = async { resetter.reset() }
        yield()
        assertFalse(reset.isCompleted)

        releaseLock.complete(Unit)
        reset.await()
        lockHolder.join()
        assertTrue(recordingWasSuppressed)
    }

    @Test
    fun postResetCleanupFailuresBecomeWarningsWithoutRestoringLocalData() = runBlocking {
        seedAllRoomTables()
        credentialCache.save("secret".toCharArray())
        val resetter = resetter(
            cancelSchedules = { throw IOException("work cancellation failed") },
            cleanCalendarLinks = { throw SecurityException("calendar permission revoked") },
        )

        val result = resetter.reset()

        assertAllRoomTablesEmpty()
        assertNull(credentialCache.load())
        assertEquals(
            listOf("work_schedules", "calendar"),
            result.warnings.map(LocalDataResetWarning::subsystem),
        )
    }

    private fun resetter(
        cleanCalendarLinks: suspend (List<LocalCalendarLink>) -> Unit = {},
        stopPeerSync: suspend () -> Unit = {},
        cancelSchedules: suspend () -> Unit = {},
        cancelNotifications: suspend () -> Unit = {},
    ) = DefaultLocalDataResetter(
        database = database,
        preferencesStore = preferencesStore,
        visionImageStorage = visionImageStore,
        backupStateStore = backupStateStore,
        credentialCache = credentialCache,
        resetAppState = {
            deviceIdProvider.clear()
            AppLanguageStorage.clear(context)
            WelcomePreferences.clear(context)
        },
        stopPeerSync = stopPeerSync,
        cleanCalendarLinks = cleanCalendarLinks,
        cancelSchedules = cancelSchedules,
        cancelNotifications = cancelNotifications,
    )

    private suspend fun seedAllRoomTables() {
        database.savingsGoalDao().insert(
            SavingsGoalEntity(id = 1, syncId = "goal", name = "Goal", targetAmountCents = 100),
        )
        database.debtorDao().insert(
            DebtorEntity(
                id = 2,
                syncId = "debtor",
                name = "Debtor",
                targetAmountCents = 200,
                dateGiven = 10,
                dateDue = 20,
                calendarEventId = "calendar-2",
            ),
        )
        database.debtPaymentDao().insert(
            DebtPaymentEntity(
                id = 3,
                syncId = "payment",
                debtorId = 2,
                amountCents = 50,
                currency = "USD",
                paidAt = 30,
            ),
        )
        database.transferDao().insert(
            TransferEntity(
                id = 4,
                syncId = "transfer",
                amountCents = 100L,
                date = 40,
                goalId = 1,
                goalSyncId = "goal",
                debtId = 2,
                debtPaymentId = 3,
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                id = 5,
                syncId = "expense",
                amountCents = 200L,
                category = "test",
                date = 50,
                debtId = 2,
                debtPaymentId = 3,
            ),
        )
        database.luckDiaryDao().insert(
            LuckDiaryEntryEntity(id = 6, syncId = "luck", entryDate = 60, note = "test"),
        )
        database.syncQueueDao().insert(
            SyncQueueEntity("queue", "GOAL", 1, "{}", 70),
        )
        database.syncTombstoneDao().upsert(
            SyncTombstoneEntity("GOAL", "deleted-goal", "{}", 80),
        )
    }

    private suspend fun assertAllRoomTablesEmpty() {
        assertTrue(database.savingsGoalDao().getAllGoalsOnce().isEmpty())
        assertTrue(database.debtorDao().getAllDebtorsOnce().isEmpty())
        assertTrue(database.debtPaymentDao().getAllPaymentsOnce().isEmpty())
        assertTrue(database.transferDao().getAllOnce().isEmpty())
        assertTrue(database.expenseDao().getAllOnce().isEmpty())
        assertTrue(database.luckDiaryDao().getAllOnce().isEmpty())
        assertTrue(database.syncQueueDao().getAllPending().isEmpty())
        assertTrue(database.syncTombstoneDao().getAll().isEmpty())
    }

    private class ReversibleCredentialCipher : BackupCredentialCipher {
        override fun encrypt(plaintext: ByteArray): WrappedBackupCredential =
            WrappedBackupCredential(plaintext.copyOf(), byteArrayOf(1))

        override fun decrypt(credential: WrappedBackupCredential): ByteArray =
            credential.ciphertext.copyOf()

        override fun clearKey() = Unit
    }
}

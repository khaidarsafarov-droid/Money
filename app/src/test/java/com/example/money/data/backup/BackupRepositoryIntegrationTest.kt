package com.example.money.data.backup

import android.content.Context
import androidx.room.Room
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.LuckDiaryEntryEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.domain.backup.BackupAuthenticationException
import com.example.money.domain.backup.BackupAutopilotPreferences
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupCalendarPreferences
import com.example.money.domain.backup.BackupFormatException
import com.example.money.domain.backup.BackupGamificationPreferences
import com.example.money.domain.backup.BackupGoalPsychologyPreferences
import com.example.money.domain.backup.BackupPersonalDebtPreferences
import com.example.money.domain.backup.BackupPreferences
import com.example.money.domain.backup.BackupPrivacyPreferences
import com.example.money.domain.backup.BackupUserPreferences
import com.example.money.domain.backup.BackupVisionImage
import com.example.money.domain.backup.BackupVoicePreferences
import com.example.money.domain.backup.BackupWeeklyCheckInPreferences
import com.example.money.messaging.MessagingChannel
import com.example.money.settings.AppLanguage
import com.example.money.ui.home.WelcomePreferences
import java.io.IOException
import kotlin.io.encoding.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class BackupRepositoryIntegrationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferencesStore: MoneyPreferencesStore
    private lateinit var visionImageStore: VisionImageStore
    private lateinit var codec: BackupBundleCodec
    private lateinit var crypto: PasswordBackupCrypto
    private lateinit var repository: BackupRepositoryImpl

    @Before
    fun setUp() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferencesStore = MoneyPreferencesStore(context)
        visionImageStore = VisionImageStore(context, preferencesStore)
        codec = BackupBundleCodec()
        crypto = PasswordBackupCrypto()
        repository = createRepository(visionImageStore)
        visionImageStore.deleteCurrent()
        preferencesStore.importPortable(portablePreferences("SYSTEM", "initial"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun encryptedRoundTrip_restoresAllTablesPreferencesAndImageWithRemappedForeignKeys() = runBlocking {
        seedCompleteRoomState()
        val sourcePreferences = portablePreferences("RU", "source")
        preferencesStore.importPortable(sourcePreferences)
        val sourceImage = BackupVisionImage(
            fileName = "vision.png",
            mimeType = "image/png",
            base64Data = Base64.encode(byteArrayOf(1, 3, 5, 7)),
        )
        visionImageStore.writeCurrent(sourceImage)

        val sourceBundle = repository.exportBundle()
        val encrypted = repository.exportEncrypted(PASSWORD)
        val preview = repository.previewEncrypted(encrypted, PASSWORD)
        assertEquals(6, preview.recordCounts.total)
        assertTrue(preview.hasVisionImage)

        database.clearAllTables()
        database.savingsGoalDao().insert(SavingsGoalEntity(name = "replacement", targetAmountCents = 1))
        database.syncQueueDao().insert(queue("replacement-queue"))
        preferencesStore.importPortable(portablePreferences("EN", "target"))
        preferencesStore.updateVoiceReportSettings {
            it.copy(lastVoiceReportDate = "target-report", lastAppOpenDate = "target-open")
        }
        preferencesStore.updateCalendarSettings {
            it.copy(debtsCalendarId = 999L, permissionRequested = true)
        }
        preferencesStore.setNotificationPermissionRequested(true)
        preferencesStore.markPersonalDebtAutoMessageSent(99L, MessagingChannel.SMS)
        repository.importEncrypted(encrypted, PASSWORD)

        val goal = database.savingsGoalDao().getAllGoalsOnce().single()
        val debtor = database.debtorDao().getAllDebtorsOnce().single()
        val payment = database.debtPaymentDao().getAllPaymentsOnce().single()
        val transfer = database.transferDao().getAllOnce().single()
        val expense = database.expenseDao().getAllOnce().single()
        val luck = database.luckDiaryDao().getAllOnce().single()

        assertNotEquals(41L, goal.id)
        assertNotEquals(42L, debtor.id)
        assertNotEquals(43L, payment.id)
        assertEquals("goal-sync", goal.syncId)
        assertEquals(123_456L, goal.currentAmountCents)
        assertEquals("EUR", debtor.currency)
        assertEquals(321, debtor.interestRateBps)
        assertNull(debtor.calendarEventId)
        assertFalse(debtor.calendarSyncFailed)
        assertNull(debtor.contactLookupKey)
        assertNull(debtor.contactUri)
        assertNull(debtor.contactPhotoUri)
        assertEquals("+15551234567", debtor.phone)
        assertEquals("portable@example.com", debtor.email)
        assertEquals(debtor.id, payment.debtorId)
        assertEquals("GBP", payment.currency)
        assertEquals(goal.id, transfer.goalId)
        assertEquals("goal-sync", transfer.goalSyncId)
        assertEquals(debtor.id, transfer.debtId)
        assertEquals(payment.id, transfer.debtPaymentId)
        assertFalse(transfer.isConfirmed)
        assertEquals(debtor.id, expense.debtId)
        assertEquals(payment.id, expense.debtPaymentId)
        assertTrue(expense.isConfirmed)
        assertEquals("luck-sync", luck.syncId)
        assertTrue(database.syncQueueDao().getAllPending().isEmpty())
        assertEquals(sourcePreferences, preferencesStore.exportPortable())
        assertEquals(AppLanguage.RU, AppLanguageStorage.read(context))
        assertTrue(WelcomePreferences.isWelcomeSeen(context))
        assertEquals(sourceImage.base64Data, visionImageStore.readCurrent()?.base64Data)

        val localVoice = preferencesStore.voiceReportSettings.first()
        assertEquals("target-report", localVoice.lastVoiceReportDate)
        assertEquals("target-open", localVoice.lastAppOpenDate)
        val localCalendar = preferencesStore.calendarSettings.first()
        assertEquals(999L, localCalendar.debtsCalendarId)
        assertTrue(localCalendar.permissionRequested)
        assertTrue(preferencesStore.notificationPermissionRequested.first())
        assertTrue(
            preferencesStore.wasPersonalDebtAutoMessageSentRecently(
                99L,
                MessagingChannel.SMS,
                Long.MAX_VALUE,
            ),
        )
        assertPayloadEquals(sourceBundle, repository.exportBundle())
    }

    @Test
    fun wrongPassword_doesNotMutateAnyState() = runBlocking {
        seedCompleteRoomState()
        preferencesStore.importPortable(portablePreferences("RU", "original"))
        val encrypted = repository.exportEncrypted(PASSWORD)
        val before = repository.exportBundle()

        assertFails<BackupAuthenticationException> {
            repository.importEncrypted(encrypted, "wrong".toCharArray())
        }

        assertPayloadEquals(before, repository.exportBundle())
    }

    @Test
    fun failureAfterRoomAndPreferencesApply_restoresExactStateIncludingSyncQueue() = runBlocking {
        seedCompleteRoomState()
        val originalPreferences = portablePreferences("RU", "original")
        preferencesStore.importPortable(originalPreferences)
        val originalImage = BackupVisionImage(
            fileName = "original.jpg",
            mimeType = "image/jpeg",
            base64Data = Base64.encode(byteArrayOf(8, 6, 4, 2)),
        )
        visionImageStore.writeCurrent(originalImage)
        database.syncQueueDao().insert(queue("original-queue"))
        val before = repository.exportBundle()
        val beforeRoom = captureRoomState()
        val beforeQueue = database.syncQueueDao().getAllPending()
        val incoming = codec.canonicalize(
            before.copy(
                goals = before.goals.map { it.copy(name = "incoming") },
                preferences = portablePreferences("EN", "incoming"),
                visionImage = originalImage.copy(
                    fileName = "incoming.png",
                    mimeType = "image/png",
                    base64Data = Base64.encode(byteArrayOf(9, 9, 9)),
                ),
            ),
        )
        val failingRepository = createRepository(
            FailingOnceVisionImageStorage(visionImageStore),
        )

        assertFails<IOException> {
            failingRepository.importBundle(incoming)
        }

        assertPayloadEquals(before, repository.exportBundle())
        assertEquals(beforeRoom, captureRoomState())
        assertEquals(beforeQueue, database.syncQueueDao().getAllPending())
        assertEquals(originalPreferences, preferencesStore.exportPortable())
        assertEquals(originalImage.base64Data, visionImageStore.readCurrent()?.base64Data)
    }

    @Test
    fun corruptDecryptedBundle_doesNotMutateAnyState() = runBlocking {
        seedCompleteRoomState()
        preferencesStore.importPortable(portablePreferences("RU", "original"))
        val before = repository.exportBundle()
        val corruptEnvelope = crypto.encrypt("{not-json".toByteArray(), PASSWORD)

        assertFails<BackupFormatException> {
            repository.importEncrypted(corruptEnvelope, PASSWORD)
        }

        assertPayloadEquals(before, repository.exportBundle())
    }

    private fun createRepository(imageStorage: VisionImageStorage): BackupRepositoryImpl {
        val exporter = BackupExporter(
            database = database,
            preferencesStore = preferencesStore,
            visionImageStorage = imageStorage,
            codec = codec,
            crypto = crypto,
            appVersionName = "test",
            currentTimeMillis = { 1_753_478_400_000L },
        )
        val importer = BackupImporter(
            roomApplier = BackupRoomApplier(database),
            preferencesStore = preferencesStore,
            visionImageStorage = imageStorage,
            codec = codec,
            crypto = crypto,
        )
        return BackupRepositoryImpl(exporter, importer)
    }

    private suspend fun seedCompleteRoomState() {
        database.savingsGoalDao().insert(
            SavingsGoalEntity(
                id = 41,
                syncId = "goal-sync",
                name = "Emergency",
                targetAmountCents = 900_000,
                currentAmountCents = 123_456,
                currency = "USD",
                updatedAt = 1234,
                deadlineMillis = 5678,
            ),
        )
        database.debtorDao().insert(
            DebtorEntity(
                id = 42,
                syncId = "debtor-sync",
                name = "Taylor",
                targetAmountCents = 77_000,
                currentAmountCents = 22_000,
                currency = "EUR",
                status = "ACTIVE",
                debtType = "DEBTOR",
                affectsBalance = true,
                note = "metadata",
                dateGiven = 100,
                dateDue = 200,
                loanCategory = "PERSONAL",
                interestRateBps = 321,
                calendarEventId = "device-event",
                calendarSyncFailed = true,
                contactLookupKey = "device-lookup",
                contactUri = "content://contact",
                contactPhotoUri = "content://photo",
                phone = "+15551234567",
                email = "portable@example.com",
            ),
        )
        database.debtPaymentDao().insert(
            DebtPaymentEntity(
                id = 43,
                syncId = "payment-sync",
                debtorId = 42,
                amountCents = 4_321,
                currency = "GBP",
                paidAt = 300,
                affectsBalance = false,
            ),
        )
        database.transferDao().insert(
            TransferEntity(
                id = 44,
                syncId = "transfer-sync",
                amount = 12.34,
                date = 400,
                isConfirmed = false,
                goalId = 41,
                goalSyncId = "stale-device-goal-id",
                debtId = 42,
                debtPaymentId = 43,
                note = "transfer metadata",
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                id = 45,
                syncId = "expense-sync",
                amount = 5.67,
                category = "Travel",
                date = 500,
                note = "expense metadata",
                isConfirmed = true,
                debtId = 42,
                debtPaymentId = 43,
            ),
        )
        database.luckDiaryDao().insert(
            LuckDiaryEntryEntity(
                id = 46,
                syncId = "luck-sync",
                entryDate = 600,
                note = "Found a refund",
                amountCents = 800,
                category = "REFUND",
                createdAt = 700,
            ),
        )
    }

    private fun portablePreferences(language: String, marker: String) = BackupPreferences(
        appLanguage = language,
        currencyRegion = "US",
        welcomeSeen = marker != "target",
        voice = BackupVoicePreferences(true, 7, 45, "EN", "FEMALE", true),
        privacy = BackupPrivacyPreferences(true, false),
        gamification = BackupGamificationPreferences(8, "2026-07-24", setOf(marker), marker),
        autopilot = BackupAutopilotPreferences(true, 555, "AVALANCHE", 19, 15, true),
        weeklyCheckIn = BackupWeeklyCheckInPreferences(true, 12),
        advisorHistoryJson = """[{"marker":"$marker"}]""",
        goalPsychology = BackupGoalPsychologyPreferences(
            "SAVER", "[1,2]", true, setOf(marker), false, marker, 27, "SNOWBALL",
            """{"marker":"$marker"}""", """["$marker"]""", setOf(marker),
        ),
        user = BackupUserPreferences(marker, "FEMALE", 35, marker, marker, "HAPPY", marker, true),
        calendar = BackupCalendarPreferences(true, true, false, true, false),
        personalDebt = BackupPersonalDebtPreferences(true, true, false, true, true, false, true, false, true),
    )

    private fun queue(id: String) = SyncQueueEntity(id, "GOAL", 41, """{"id":"$id"}""", 999)

    private suspend fun captureRoomState() = ExactRoomState(
        goals = database.savingsGoalDao().getAllGoalsOnce(),
        debtors = database.debtorDao().getAllDebtorsOnce(),
        payments = database.debtPaymentDao().getAllPaymentsOnce(),
        transfers = database.transferDao().getAllOnce(),
        expenses = database.expenseDao().getAllOnce(),
        luckEntries = database.luckDiaryDao().getAllOnce(),
    )

    private fun assertPayloadEquals(expected: BackupBundle, actual: BackupBundle) {
        assertEquals(expected.goals, actual.goals)
        assertEquals(expected.debtors, actual.debtors)
        assertEquals(expected.debtPayments, actual.debtPayments)
        assertEquals(expected.transfers, actual.transfers)
        assertEquals(expected.expenses, actual.expenses)
        assertEquals(expected.luckDiaryEntries, actual.luckDiaryEntries)
        assertEquals(expected.preferences, actual.preferences)
        assertEquals(expected.visionImage, actual.visionImage)
    }

    private suspend inline fun <reified T : Throwable> assertFails(
        crossinline block: suspend () -> Unit,
    ) {
        try {
            block()
        } catch (error: Throwable) {
            assertTrue("Expected ${T::class.java.name}, got ${error::class.java.name}", error is T)
            return
        }
        throw AssertionError("Expected ${T::class.java.name}")
    }

    private class FailingOnceVisionImageStorage(
        private val delegate: VisionImageStorage,
    ) : VisionImageStorage {
        private var shouldFail = true

        override suspend fun readCurrent(): BackupVisionImage? = delegate.readCurrent()

        override suspend fun replaceCurrent(image: BackupVisionImage?) {
            if (shouldFail) {
                shouldFail = false
                throw IOException("injected image failure")
            }
            delegate.replaceCurrent(image)
        }
    }

    private data class ExactRoomState(
        val goals: List<SavingsGoalEntity>,
        val debtors: List<DebtorEntity>,
        val payments: List<DebtPaymentEntity>,
        val transfers: List<TransferEntity>,
        val expenses: List<ExpenseEntity>,
        val luckEntries: List<LuckDiaryEntryEntity>,
    )

    private companion object {
        val PASSWORD = "correct horse battery staple".toCharArray()
    }
}

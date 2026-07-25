package com.example.money.data.backup

import com.example.money.domain.backup.BackupAutopilotPreferences
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupCalendarPreferences
import com.example.money.domain.backup.BackupDebtPayment
import com.example.money.domain.backup.BackupDebtor
import com.example.money.domain.backup.BackupExpense
import com.example.money.domain.backup.BackupGamificationPreferences
import com.example.money.domain.backup.BackupGoal
import com.example.money.domain.backup.BackupGoalPsychologyPreferences
import com.example.money.domain.backup.BackupIntegrityException
import com.example.money.domain.backup.BackupLuckDiaryEntry
import com.example.money.domain.backup.BackupManifest
import com.example.money.domain.backup.BackupPersonalDebtPreferences
import com.example.money.domain.backup.BackupPreferences
import com.example.money.domain.backup.BackupPrivacyPreferences
import com.example.money.domain.backup.BackupRecordCounts
import com.example.money.domain.backup.BackupTransfer
import com.example.money.domain.backup.BackupUserPreferences
import com.example.money.domain.backup.BackupValidationException
import com.example.money.domain.backup.BackupVisionImage
import com.example.money.domain.backup.BackupVoicePreferences
import com.example.money.domain.backup.BackupWeeklyCheckInPreferences
import com.example.money.domain.backup.UnsupportedBackupVersionException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BackupBundleCodecTest {

    private val codec = BackupBundleCodec()

    @Test
    fun encodeDecode_roundTripsEveryPortableFieldAndUsesStableHash() {
        val source = completeBundle()

        val firstEncoding = codec.encode(source)
        val secondEncoding = codec.encode(source)
        val decoded = codec.decode(firstEncoding)
        val canonical = codec.canonicalize(source)

        assertArrayEquals(firstEncoding, secondEncoding)
        assertEquals(canonical, decoded)
        assertEquals(6, decoded.manifest.recordCounts.total)
        assertEquals(64, decoded.manifest.contentSha256.length)
        assertFalse(decoded.manifest.contentSha256.any { it !in '0'..'9' && it !in 'a'..'f' })
        assertEquals(canonical.manifest.contentSha256, codec.preview(firstEncoding).contentSha256)
    }

    @Test
    fun encode_isCanonicalAcrossRecordAndSetOrdering() {
        val source = completeBundle()
        val secondGoal = source.goals.single().copy(syncId = "goal-2", name = "Vacation")
        val unordered = source.copy(
            goals = listOf(secondGoal, source.goals.single()),
            preferences = source.preferences.copy(
                gamification = source.preferences.gamification.copy(
                    unlockedBadges = linkedSetOf("streak_3", "first_step"),
                ),
            ),
        )
        val reordered = unordered.copy(
            goals = unordered.goals.reversed(),
            preferences = unordered.preferences.copy(
                gamification = unordered.preferences.gamification.copy(
                    unlockedBadges = linkedSetOf("first_step", "streak_3"),
                ),
            ),
        )

        assertArrayEquals(codec.encode(unordered), codec.encode(reordered))
    }

    @Test
    fun encodeDecode_preservesOptionalVisionImage() {
        val image = BackupVisionImage(
            fileName = "vision.png",
            mimeType = "image/png",
            base64Data = "AAECAwQ=",
        )
        val source = completeBundle().copy(visionImage = image)

        val decoded = codec.decode(codec.encode(source))

        assertEquals(image, decoded.visionImage)
        assertTrue(codec.preview(codec.encode(source)).hasVisionImage)
    }

    @Test
    fun decode_rejectsContentChangedWithoutUpdatingHash() {
        val encoded = codec.encode(completeBundle())
        val original = encoded.toString(Charsets.UTF_8)
        val corrupted = original.replace("\"amount\":12.5", "\"amount\":13.5")
        assertFalse(original == corrupted)

        assertThrows(BackupIntegrityException::class.java) {
            codec.decode(corrupted.toByteArray(Charsets.UTF_8))
        }
    }

    @Test
    fun decode_rejectsUnsupportedBundleVersion() {
        val encoded = codec.encode(completeBundle())
        val original = encoded.toString(Charsets.UTF_8)
        val unsupported = original.replace("\"formatVersion\":1", "\"formatVersion\":99")

        val error = assertThrows(UnsupportedBackupVersionException::class.java) {
            codec.decode(unsupported.toByteArray(Charsets.UTF_8))
        }

        assertEquals(99, error.actualVersion)
    }

    @Test
    fun encode_rejectsTooManyRecordsAcrossTables() {
        val template = completeBundle()
        val source = template.copy(
            goals = List(BackupBundleCodec.DEFAULT_MAX_RECORD_COUNT + 1) { index ->
                template.goals.single().copy(syncId = "goal-$index")
            },
            debtors = emptyList(),
            debtPayments = emptyList(),
            transfers = emptyList(),
            expenses = emptyList(),
            luckDiaryEntries = emptyList(),
        )

        assertThrows(BackupValidationException::class.java) {
            codec.encode(source)
        }
    }

    @Test
    fun encodeDecode_rejectPayloadOverConfiguredSizeLimit() {
        val strictCodec = BackupBundleCodec(maxBundleBytes = 32)

        assertThrows(BackupValidationException::class.java) {
            strictCodec.encode(completeBundle())
        }
        assertThrows(BackupValidationException::class.java) {
            strictCodec.decode(ByteArray(33))
        }
    }

    private fun completeBundle(): BackupBundle =
        BackupBundle(
            manifest = BackupManifest(
                formatVersion = BackupBundle.CURRENT_FORMAT_VERSION,
                createdAtEpochMillis = 1_753_478_400_000L,
                appVersionName = "1.1",
                recordCounts = BackupRecordCounts.EMPTY,
                contentSha256 = "",
            ),
            goals = listOf(
                BackupGoal(
                    syncId = "goal-1",
                    name = "Emergency fund",
                    targetAmountCents = 500_000L,
                    currentAmountCents = 125_000L,
                    currency = "USD",
                    updatedAtEpochMillis = 1_753_478_401_000L,
                    deadlineEpochMillis = 1_767_225_600_000L,
                ),
            ),
            debtors = listOf(
                BackupDebtor(
                    syncId = "debt-1",
                    name = "Alex",
                    targetAmountCents = 80_000L,
                    currentAmountCents = 20_000L,
                    currency = "EUR",
                    status = "ACTIVE",
                    debtType = "DEBTOR",
                    affectsBalance = true,
                    note = "Personal loan",
                    dateGivenEpochMillis = 1_750_000_000_000L,
                    dateDueEpochMillis = 1_760_000_000_000L,
                    loanCategory = "PERSONAL",
                    interestRateBps = 350,
                    phone = "+123456789",
                    email = "alex@example.com",
                ),
            ),
            debtPayments = listOf(
                BackupDebtPayment(
                    syncId = "payment-1",
                    debtorSyncId = "debt-1",
                    amountCents = 10_000L,
                    currency = "EUR",
                    paidAtEpochMillis = 1_755_000_000_000L,
                    affectsBalance = true,
                ),
            ),
            transfers = listOf(
                BackupTransfer(
                    syncId = "transfer-1",
                    amount = 12.5,
                    dateEpochMillis = 1_754_000_000_000L,
                    isConfirmed = true,
                    goalSyncId = "goal-1",
                    debtorSyncId = "debt-1",
                    debtPaymentSyncId = "payment-1",
                    note = "Allocated",
                ),
            ),
            expenses = listOf(
                BackupExpense(
                    syncId = "expense-1",
                    amount = 4.75,
                    category = "FOOD",
                    dateEpochMillis = 1_754_500_000_000L,
                    note = "Lunch",
                    isConfirmed = true,
                    debtorSyncId = "debt-1",
                    debtPaymentSyncId = "payment-1",
                ),
            ),
            luckDiaryEntries = listOf(
                BackupLuckDiaryEntry(
                    syncId = "luck-1",
                    entryDateEpochMillis = 1_754_600_000_000L,
                    note = "Unexpected refund",
                    amountCents = 2_500L,
                    category = "REFUND",
                    createdAtEpochMillis = 1_754_600_100_000L,
                ),
            ),
            preferences = BackupPreferences(
                appLanguage = "RU",
                currencyRegion = "EU",
                welcomeSeen = true,
                voice = BackupVoicePreferences(
                    enabled = true,
                    reportHour = 9,
                    reportMinute = 15,
                    language = "RU",
                    gender = "FEMALE",
                    forceInSilentMode = true,
                ),
                privacy = BackupPrivacyPreferences(
                    hideAmounts = true,
                    hideOnWidget = true,
                ),
                gamification = BackupGamificationPreferences(
                    streakCount = 7,
                    lastPaymentIsoDate = "2026-07-24",
                    unlockedBadges = setOf("streak_3", "first_step"),
                    lastWinMessage = "Great work",
                ),
                autopilot = BackupAutopilotPreferences(
                    enabled = true,
                    dailyAmountCents = 900L,
                    strategy = "AVALANCHE",
                    reminderHour = 19,
                    reminderMinute = 30,
                    roundPaymentsToTen = true,
                ),
                weeklyCheckIn = BackupWeeklyCheckInPreferences(
                    enabled = true,
                    hour = 11,
                ),
                advisorHistoryJson = "[{\"message\":\"Keep going\"}]",
                goalPsychology = BackupGoalPsychologyPreferences(
                    psychotype = "SAVER",
                    psychotypeAnswersJson = "[1,2,3]",
                    psychotypeTestCompleted = true,
                    selectedFearIds = setOf("fear-2", "fear-1"),
                    wordPowerEnabled = false,
                    personalMotivation = "Freedom",
                    paydayDayOfMonth = 27,
                    payoffStrategy = "SNOWBALL",
                    weeklyChecklistJson = "{\"done\":true}",
                    activeChallengesJson = "[\"no-spend\"]",
                    goalBadges = setOf("planner"),
                ),
                user = BackupUserPreferences(
                    displayName = "Sam",
                    gender = "OTHER",
                    age = 34,
                    customFear = "Instability",
                    goalText = "Own a home",
                    emotion = "CALM",
                    priority = "SECURITY",
                    onboardingCompleted = true,
                ),
                calendar = BackupCalendarPreferences(
                    autoSyncEnabled = true,
                    remindOneDayBefore = true,
                    remindThreeDaysBefore = true,
                    remindOnDueDay = false,
                    useDebtsCalendar = false,
                ),
                personalDebt = BackupPersonalDebtPreferences(
                    debtNotificationsEnabled = true,
                    debtAdvanceRemind1Day = false,
                    debtAdvanceRemind3Days = true,
                    debtAutoMessageEnabled = true,
                    debtAutoMessageSms = false,
                    debtAutoMessageWhatsapp = true,
                    debtAutoMessageTelegram = true,
                    debtAutoMessageEmail = false,
                    debtDirectSmsEnabled = true,
                ),
            ),
            visionImage = null,
        )
}

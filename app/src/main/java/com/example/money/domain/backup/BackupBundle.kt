package com.example.money.domain.backup

/**
 * Full-fidelity portable records use sync IDs instead of Room primary keys so
 * relationships can be rebuilt on another device.
 */
data class BackupGoal(
    val syncId: String,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val currency: String,
    val updatedAtEpochMillis: Long,
    val deadlineEpochMillis: Long?,
    val kind: String = "MONEY",
    val mode: String = "PERSONAL",
)

data class BackupGoalAction(
    val syncId: String,
    val goalSyncId: String,
    val title: String,
    val completed: Boolean,
    val sortOrder: Int,
    val updatedAtEpochMillis: Long,
)

data class BackupNote(
    val syncId: String,
    val title: String,
    val kind: String,
    val body: String,
    val mark: String,
    val closed: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val mode: String = "PERSONAL",
)

data class BackupNoteLine(
    val syncId: String,
    val noteSyncId: String,
    val label: String,
    val amountCents: Long,
    val sortOrder: Int,
    val updatedAtEpochMillis: Long,
)

data class BackupDebtor(
    val syncId: String,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val currency: String,
    val status: String,
    val debtType: String,
    val affectsBalance: Boolean,
    val note: String,
    val dateGivenEpochMillis: Long,
    val dateDueEpochMillis: Long,
    val loanCategory: String,
    val interestRateBps: Int,
    val phone: String,
    val email: String,
    val mode: String = "PERSONAL",
    val clientSyncId: String? = null,
)

data class BackupClient(
    val syncId: String,
    val name: String,
    val phone: String = "",
    val company: String = "",
    val email: String? = null,
    val lastMeetingAtEpochMillis: Long? = null,
    val note: String = "",
    val createdAtEpochMillis: Long,
    val syncUpdatedAtEpochMillis: Long,
    val deleted: Boolean = false,
    val mode: String = "PROFESSIONAL",
)

data class BackupDebtAdvance(
    val syncId: String,
    val debtorSyncId: String,
    val amountCents: Long,
    val currency: String,
    val givenAtEpochMillis: Long,
    val note: String = "",
    val updatedAtEpochMillis: Long,
    val mode: String = "PERSONAL",
)

data class BackupDebtPayment(
    val syncId: String,
    val debtorSyncId: String,
    val amountCents: Long,
    val currency: String,
    val paidAtEpochMillis: Long,
    val affectsBalance: Boolean,
    val mode: String = "PERSONAL",
)

data class BackupTransfer(
    val syncId: String,
    val amount: Double,
    val dateEpochMillis: Long,
    val isConfirmed: Boolean,
    val goalSyncId: String?,
    val debtorSyncId: String?,
    val debtPaymentSyncId: String?,
    val note: String,
    val mode: String = "PERSONAL",
)

data class BackupExpense(
    val syncId: String,
    val amount: Double,
    val category: String,
    val dateEpochMillis: Long,
    val note: String,
    val isConfirmed: Boolean,
    val debtorSyncId: String?,
    val debtPaymentSyncId: String?,
    val goalSyncId: String? = null,
    val mode: String = "PERSONAL",
)

data class BackupLuckDiaryEntry(
    val syncId: String,
    val entryDateEpochMillis: Long,
    val note: String,
    val amountCents: Long?,
    val category: String,
    val createdAtEpochMillis: Long,
)

data class BackupVoicePreferences(
    val enabled: Boolean,
    val reportHour: Int,
    val reportMinute: Int,
    val language: String,
    val gender: String,
    val forceInSilentMode: Boolean,
)

data class BackupPrivacyPreferences(
    val hideAmounts: Boolean,
    val hideOnWidget: Boolean,
)

data class BackupGamificationPreferences(
    val streakCount: Int,
    val lastPaymentIsoDate: String?,
    val unlockedBadges: Set<String>,
    val lastWinMessage: String?,
)

data class BackupAutopilotPreferences(
    val enabled: Boolean,
    val dailyAmountCents: Long,
    val strategy: String,
    val reminderHour: Int,
    val reminderMinute: Int,
    val roundPaymentsToTen: Boolean,
)

data class BackupWeeklyCheckInPreferences(
    val enabled: Boolean,
    val hour: Int,
)

data class BackupGoalPsychologyPreferences(
    val psychotype: String?,
    val psychotypeAnswersJson: String,
    val psychotypeTestCompleted: Boolean,
    val selectedFearIds: Set<String>,
    val wordPowerEnabled: Boolean,
    val personalMotivation: String,
    val paydayDayOfMonth: Int,
    val payoffStrategy: String,
    val weeklyChecklistJson: String,
    val activeChallengesJson: String,
    val goalBadges: Set<String>,
)

data class BackupUserPreferences(
    val displayName: String,
    val gender: String,
    val age: Int?,
    val customFear: String,
    val goalText: String,
    val emotion: String?,
    val priority: String,
    val onboardingCompleted: Boolean,
)

data class BackupCalendarPreferences(
    val autoSyncEnabled: Boolean,
    val remindOneDayBefore: Boolean,
    val remindThreeDaysBefore: Boolean,
    val remindOnDueDay: Boolean,
    val useDebtsCalendar: Boolean,
)

data class BackupPersonalDebtPreferences(
    val debtNotificationsEnabled: Boolean,
    val debtAdvanceRemind1Day: Boolean,
    val debtAdvanceRemind3Days: Boolean,
    val debtAutoMessageEnabled: Boolean,
    val debtAutoMessageSms: Boolean,
    val debtAutoMessageWhatsapp: Boolean,
    val debtAutoMessageTelegram: Boolean,
    val debtAutoMessageEmail: Boolean,
    val debtDirectSmsEnabled: Boolean,
    val clientNotificationsEnabled: Boolean = true,
)

/**
 * Only user-owned settings are portable. Widget caches, delivery timestamps,
 * permission flags, sync metadata, calendar IDs, and local file paths are
 * intentionally absent.
 */
data class BackupPreferences(
    val appLanguage: String,
    val currencyRegion: String,
    val welcomeSeen: Boolean,
    val voice: BackupVoicePreferences,
    val privacy: BackupPrivacyPreferences,
    val gamification: BackupGamificationPreferences,
    val autopilot: BackupAutopilotPreferences,
    val weeklyCheckIn: BackupWeeklyCheckInPreferences,
    val advisorHistoryJson: String,
    val goalPsychology: BackupGoalPsychologyPreferences,
    val user: BackupUserPreferences,
    val calendar: BackupCalendarPreferences,
    val personalDebt: BackupPersonalDebtPreferences,
)

/** Base64 keeps image transport independent from Android file APIs. */
data class BackupVisionImage(
    val fileName: String,
    val mimeType: String,
    val base64Data: String,
)

data class BackupRecordCounts(
    val goals: Int,
    val debtors: Int,
    val debtPayments: Int,
    val transfers: Int,
    val expenses: Int,
    val luckDiaryEntries: Int,
    val goalActions: Int = 0,
    val notes: Int = 0,
    val noteLines: Int = 0,
    val clients: Int = 0,
    val debtAdvances: Int = 0,
) {
    val total: Int
        get() = goals + debtors + debtPayments + transfers + expenses + luckDiaryEntries +
            goalActions + notes + noteLines + clients + debtAdvances

    companion object {
        val EMPTY = BackupRecordCounts(
            goals = 0,
            debtors = 0,
            debtPayments = 0,
            transfers = 0,
            expenses = 0,
            luckDiaryEntries = 0,
        )

        fun from(bundle: BackupBundle): BackupRecordCounts =
            BackupRecordCounts(
                goals = bundle.goals.size,
                debtors = bundle.debtors.size,
                debtPayments = bundle.debtPayments.size,
                transfers = bundle.transfers.size,
                expenses = bundle.expenses.size,
                luckDiaryEntries = bundle.luckDiaryEntries.size,
                goalActions = bundle.goalActions.size,
                notes = bundle.notes.size,
                noteLines = bundle.noteLines.size,
                clients = bundle.clients.size,
                debtAdvances = bundle.debtAdvances.size,
            )
    }
}

data class BackupManifest(
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val appVersionName: String,
    val recordCounts: BackupRecordCounts,
    val contentSha256: String,
)

data class BackupPreview(
    val formatVersion: Int,
    val createdAtEpochMillis: Long,
    val appVersionName: String,
    val recordCounts: BackupRecordCounts,
    val contentSha256: String,
    val hasVisionImage: Boolean,
)

data class BackupBundle(
    val manifest: BackupManifest,
    val goals: List<BackupGoal>,
    val debtors: List<BackupDebtor>,
    val debtPayments: List<BackupDebtPayment>,
    val transfers: List<BackupTransfer>,
    val expenses: List<BackupExpense>,
    val luckDiaryEntries: List<BackupLuckDiaryEntry>,
    val preferences: BackupPreferences,
    val visionImage: BackupVisionImage?,
    val goalActions: List<BackupGoalAction> = emptyList(),
    val notes: List<BackupNote> = emptyList(),
    val noteLines: List<BackupNoteLine> = emptyList(),
    val clients: List<BackupClient> = emptyList(),
    val debtAdvances: List<BackupDebtAdvance> = emptyList(),
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

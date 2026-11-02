package com.example.money.data.backup

import com.example.money.domain.backup.BackupAutopilotPreferences
import com.example.money.domain.backup.BackupBundle
import com.example.money.domain.backup.BackupClient
import com.example.money.domain.backup.BackupCalendarPreferences
import com.example.money.domain.backup.BackupDebtPayment
import com.example.money.domain.backup.BackupDebtor
import com.example.money.domain.backup.BackupException
import com.example.money.domain.backup.BackupExpense
import com.example.money.domain.backup.BackupFormatException
import com.example.money.domain.backup.BackupGamificationPreferences
import com.example.money.domain.backup.BackupGoal
import com.example.money.domain.backup.BackupGoalAction
import com.example.money.domain.backup.BackupGoalPsychologyPreferences
import com.example.money.domain.backup.BackupNote
import com.example.money.domain.backup.BackupNoteLine
import com.example.money.domain.backup.BackupIntegrityException
import com.example.money.domain.backup.BackupLuckDiaryEntry
import com.example.money.domain.backup.BackupManifest
import com.example.money.domain.backup.BackupPersonalDebtPreferences
import com.example.money.domain.backup.BackupPreferences
import com.example.money.domain.backup.BackupPreview
import com.example.money.domain.backup.BackupPrivacyPreferences
import com.example.money.domain.backup.BackupRecordCounts
import com.example.money.domain.backup.BackupTransfer
import com.example.money.domain.backup.BackupUserPreferences
import com.example.money.domain.backup.BackupValidationException
import com.example.money.domain.backup.BackupVisionImage
import com.example.money.domain.backup.BackupVoicePreferences
import com.example.money.domain.backup.BackupWeeklyCheckInPreferences
import com.example.money.domain.backup.UnsupportedBackupVersionException
import kotlin.io.encoding.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.security.MessageDigest

class BackupBundleCodec(
    private val maxBundleBytes: Int = DEFAULT_MAX_BUNDLE_BYTES,
    private val maxRecordCount: Int = DEFAULT_MAX_RECORD_COUNT,
    private val maxVisionImageBytes: Int = DEFAULT_MAX_VISION_IMAGE_BYTES,
) {

    init {
        require(maxBundleBytes > 0) { "maxBundleBytes must be positive" }
        require(maxRecordCount >= 0) { "maxRecordCount must not be negative" }
        require(maxVisionImageBytes >= 0) { "maxVisionImageBytes must not be negative" }
    }

    fun encode(bundle: BackupBundle): ByteArray {
        val canonical = canonicalize(bundle)
        val encoded = encodeRoot(canonical, includeContentHash = true)
            .toString()
            .toByteArray(Charsets.UTF_8)
        validateEncodedSize(encoded.size)
        return encoded
    }

    fun decode(encoded: ByteArray): BackupBundle {
        validateEncodedSize(encoded.size)
        if (encoded.isEmpty()) {
            throw BackupFormatException("Backup bundle is empty")
        }

        return try {
            val tokener = JSONTokener(encoded.toString(Charsets.UTF_8))
            val root = tokener.nextValue() as? JSONObject
                ?: throw BackupFormatException("Backup root must be an object")
            if (tokener.nextClean() != '\u0000') {
                throw BackupFormatException("Backup contains trailing content")
            }
            val manifestJson = root.requiredObject(KEY_MANIFEST)
            val version = manifestJson.getInt(KEY_FORMAT_VERSION)
            validateVersion(version)

            val storedCounts = decodeCounts(manifestJson.requiredObject(KEY_RECORD_COUNTS))
            val storedHash = manifestJson.getString(KEY_CONTENT_SHA256)
            val data = root.requiredObject(KEY_DATA)
            val decoded = BackupBundle(
                manifest = BackupManifest(
                    formatVersion = version,
                    createdAtEpochMillis = manifestJson.getLong(KEY_CREATED_AT),
                    appVersionName = manifestJson.getString(KEY_APP_VERSION),
                    recordCounts = storedCounts,
                    contentSha256 = storedHash,
                ),
                goals = decodeArray(data, KEY_GOALS, ::decodeGoal),
                debtors = decodeArray(data, KEY_DEBTORS, ::decodeDebtor),
                debtPayments = decodeArray(data, KEY_DEBT_PAYMENTS, ::decodeDebtPayment),
                transfers = decodeArray(data, KEY_TRANSFERS, ::decodeTransfer),
                expenses = decodeArray(data, KEY_EXPENSES, ::decodeExpense),
                luckDiaryEntries = decodeArray(data, KEY_LUCK_DIARY_ENTRIES, ::decodeLuckDiaryEntry),
                preferences = decodePreferences(data.requiredObject(KEY_PREFERENCES)),
                visionImage = root.optionalObject(KEY_VISION_IMAGE)?.let(::decodeVisionImage),
                goalActions = decodeOptionalArray(data, KEY_GOAL_ACTIONS, ::decodeGoalAction),
                notes = decodeOptionalArray(data, KEY_NOTES, ::decodeNote),
                noteLines = decodeOptionalArray(data, KEY_NOTE_LINES, ::decodeNoteLine),
                clients = decodeOptionalArray(data, KEY_CLIENTS, ::decodeClient),
                debtAdvances = decodeOptionalArray(data, KEY_DEBT_ADVANCES, ::decodeDebtAdvance),
            )

            val actualCounts = BackupRecordCounts.from(decoded)
            if (storedCounts != actualCounts) {
                throw BackupIntegrityException("Backup record counts do not match the manifest")
            }
            validateRecordCount(actualCounts)
            if (!SHA_256_PATTERN.matches(storedHash)) {
                throw BackupIntegrityException("Backup content hash is not a valid SHA-256 value")
            }

            manifestJson.remove(KEY_CONTENT_SHA256)
            val encodedContentHash = sha256(root.toString().toByteArray(Charsets.UTF_8))
            if (!hashesMatch(storedHash, encodedContentHash)) {
                throw BackupIntegrityException("Backup content hash validation failed")
            }

            val canonical = canonicalize(decoded)
            if (!hashesMatch(storedHash, canonical.manifest.contentSha256)) {
                throw BackupIntegrityException("Backup content is not in canonical form")
            }
            canonical
        } catch (error: BackupException) {
            throw error
        } catch (error: Exception) {
            throw BackupFormatException("Invalid backup bundle", error)
        }
    }

    fun canonicalize(bundle: BackupBundle): BackupBundle {
        validateVersion(bundle.manifest.formatVersion)
        val recordCounts = BackupRecordCounts.from(bundle)
        validateRecordCount(recordCounts)
        val sorted = bundle.copy(
            manifest = bundle.manifest.copy(
                recordCounts = recordCounts,
                contentSha256 = "",
            ),
            goals = bundle.goals.sortedBy { it.syncId },
            debtors = bundle.debtors.sortedBy { it.syncId },
            debtPayments = bundle.debtPayments.sortedBy { it.syncId },
            transfers = bundle.transfers.sortedBy { it.syncId },
            expenses = bundle.expenses.sortedBy { it.syncId },
            luckDiaryEntries = bundle.luckDiaryEntries.sortedBy { it.syncId },
            goalActions = bundle.goalActions.sortedBy { it.syncId },
            notes = bundle.notes.sortedBy { it.syncId },
            noteLines = bundle.noteLines.sortedBy { it.syncId },
            debtAdvances = bundle.debtAdvances.sortedBy { it.syncId },
        )
        validateBundle(sorted)
        val hash = sha256(
            encodeRoot(sorted, includeContentHash = false)
                .toString()
                .toByteArray(Charsets.UTF_8),
        )
        return sorted.copy(manifest = sorted.manifest.copy(contentSha256 = hash))
    }

    fun preview(encoded: ByteArray): BackupPreview {
        val bundle = decode(encoded)
        return BackupPreview(
            formatVersion = bundle.manifest.formatVersion,
            createdAtEpochMillis = bundle.manifest.createdAtEpochMillis,
            appVersionName = bundle.manifest.appVersionName,
            recordCounts = bundle.manifest.recordCounts,
            contentSha256 = bundle.manifest.contentSha256,
            hasVisionImage = bundle.visionImage != null,
        )
    }

    private fun validateBundle(bundle: BackupBundle) {
        val counts = BackupRecordCounts.from(bundle)
        validateRecordCount(counts)
        if (bundle.manifest.createdAtEpochMillis < 0L) {
            throw BackupValidationException("Backup creation time must not be negative")
        }
        if (bundle.manifest.appVersionName.isBlank()) {
            throw BackupValidationException("Backup app version must not be blank")
        }

        validateUniqueSyncIds(KEY_GOALS, bundle.goals.map { it.syncId })
        validateUniqueSyncIds(KEY_DEBTORS, bundle.debtors.map { it.syncId })
        validateUniqueSyncIds(KEY_DEBT_PAYMENTS, bundle.debtPayments.map { it.syncId })
        validateUniqueSyncIds(KEY_TRANSFERS, bundle.transfers.map { it.syncId })
        validateUniqueSyncIds(KEY_EXPENSES, bundle.expenses.map { it.syncId })
        validateUniqueSyncIds(KEY_LUCK_DIARY_ENTRIES, bundle.luckDiaryEntries.map { it.syncId })
        validateUniqueSyncIds(KEY_GOAL_ACTIONS, bundle.goalActions.map { it.syncId })
        validateUniqueSyncIds(KEY_NOTES, bundle.notes.map { it.syncId })
        validateUniqueSyncIds(KEY_NOTE_LINES, bundle.noteLines.map { it.syncId })
        validateUniqueSyncIds(KEY_CLIENTS, bundle.clients.map { it.syncId })
        validateUniqueSyncIds(KEY_DEBT_ADVANCES, bundle.debtAdvances.map { it.syncId })
        validateRelations(bundle)

        bundle.transfers.forEach {
            if (!it.amount.isFinite()) {
                throw BackupValidationException("Transfer ${it.syncId} has a non-finite amount")
            }
        }
        bundle.expenses.forEach {
            if (!it.amount.isFinite()) {
                throw BackupValidationException("Expense ${it.syncId} has a non-finite amount")
            }
        }
        validatePreferences(bundle.preferences)
        bundle.visionImage?.let(::validateVisionImage)
    }

    private fun validateRecordCount(counts: BackupRecordCounts) {
        if (counts.total > maxRecordCount) {
            throw BackupValidationException(
                "Backup contains ${counts.total} records; maximum is $maxRecordCount",
            )
        }
    }

    private fun validateRelations(bundle: BackupBundle) {
        val goalIds = bundle.goals.mapTo(mutableSetOf()) { it.syncId }
        val debtorIds = bundle.debtors.mapTo(mutableSetOf()) { it.syncId }
        val clientIds = bundle.clients.mapTo(mutableSetOf()) { it.syncId }
        bundle.debtors.forEach { debtor ->
            validateOptionalReference("Debtor ${debtor.syncId}", "client", debtor.clientSyncId, clientIds)
        }
        val paymentIds = bundle.debtPayments.mapTo(mutableSetOf()) { it.syncId }

        bundle.debtPayments.forEach { payment ->
            validateReference("Debt payment ${payment.syncId}", "debtor", payment.debtorSyncId, debtorIds)
        }
        bundle.debtAdvances.forEach { advance ->
            validateReference("Debt advance ${advance.syncId}", "debtor", advance.debtorSyncId, debtorIds)
        }
        bundle.transfers.forEach { transfer ->
            validateOptionalReference("Transfer ${transfer.syncId}", "goal", transfer.goalSyncId, goalIds)
            validateOptionalReference("Transfer ${transfer.syncId}", "debtor", transfer.debtorSyncId, debtorIds)
            validateOptionalReference(
                "Transfer ${transfer.syncId}",
                "debt payment",
                transfer.debtPaymentSyncId,
                paymentIds,
            )
        }
        bundle.expenses.forEach { expense ->
            validateOptionalReference("Expense ${expense.syncId}", "debtor", expense.debtorSyncId, debtorIds)
            validateOptionalReference(
                "Expense ${expense.syncId}",
                "debt payment",
                expense.debtPaymentSyncId,
                paymentIds,
            )
        }
        val noteIds = bundle.notes.mapTo(mutableSetOf()) { it.syncId }
        bundle.goalActions.forEach { action ->
            validateReference("Goal action ${action.syncId}", "goal", action.goalSyncId, goalIds)
        }
        bundle.noteLines.forEach { line ->
            validateReference("Note line ${line.syncId}", "note", line.noteSyncId, noteIds)
        }
    }

    private fun validatePreferences(preferences: BackupPreferences) {
        if (preferences.appLanguage.isBlank() || preferences.currencyRegion.isBlank()) {
            throw BackupValidationException("Portable language and currency preferences must not be blank")
        }
        if (preferences.voice.reportHour !in 0..23 ||
            preferences.voice.reportMinute !in 0..59 ||
            preferences.autopilot.reminderHour !in 0..23 ||
            preferences.autopilot.reminderMinute !in 0..59 ||
            preferences.weeklyCheckIn.hour !in 0..23
        ) {
            throw BackupValidationException("Portable reminder time is outside its valid range")
        }
        if (preferences.goalPsychology.paydayDayOfMonth !in 1..31) {
            throw BackupValidationException("Portable payday must be between 1 and 31")
        }
        if (preferences.user.age != null && preferences.user.age !in 1..150) {
            throw BackupValidationException("Portable user age is outside its valid range")
        }
    }

    private fun validateVisionImage(image: BackupVisionImage) {
        if (image.fileName.isBlank() ||
            image.fileName.contains('/') ||
            image.fileName.contains('\\') ||
            !image.mimeType.startsWith("image/") ||
            image.base64Data.isBlank()
        ) {
            throw BackupValidationException("Vision image metadata is invalid")
        }
        val maxBase64Length = ((maxVisionImageBytes.toLong() + 2L) / 3L) * 4L
        if (image.base64Data.length.toLong() > maxBase64Length) {
            throw BackupValidationException(
                "Vision image exceeds maximum size of $maxVisionImageBytes bytes",
            )
        }
        val imageBytes = try {
            Base64.decode(image.base64Data)
        } catch (error: IllegalArgumentException) {
            throw BackupValidationException("Vision image is not valid base64")
        }
        if (imageBytes.size > maxVisionImageBytes) {
            throw BackupValidationException(
                "Vision image exceeds maximum size of $maxVisionImageBytes bytes",
            )
        }
    }

    private fun validateUniqueSyncIds(recordType: String, ids: List<String>) {
        if (ids.any(String::isBlank)) {
            throw BackupValidationException("$recordType contains a blank sync ID")
        }
        if (ids.toSet().size != ids.size) {
            throw BackupValidationException("$recordType contains duplicate sync IDs")
        }
    }

    private fun validateOptionalReference(
        owner: String,
        field: String,
        syncId: String?,
        validIds: Set<String>,
    ) {
        syncId?.let { validateReference(owner, field, it, validIds) }
    }

    private fun validateReference(
        owner: String,
        field: String,
        syncId: String,
        validIds: Set<String>,
    ) {
        if (syncId.isBlank() || syncId !in validIds) {
            throw BackupValidationException("$owner references an unknown $field sync ID")
        }
    }

    private fun validateVersion(version: Int) {
        if (version != BackupBundle.CURRENT_FORMAT_VERSION) {
            throw UnsupportedBackupVersionException(version)
        }
    }

    private fun validateEncodedSize(size: Int) {
        if (size > maxBundleBytes) {
            throw BackupValidationException(
                "Backup bundle exceeds maximum size of $maxBundleBytes bytes",
            )
        }
    }

    private fun encodeRoot(bundle: BackupBundle, includeContentHash: Boolean): JSONObject =
        JSONObject().apply {
            put(
                KEY_MANIFEST,
                encodeManifest(
                    bundle.manifest,
                    includeContentHash = includeContentHash,
                ),
            )
            put(KEY_DATA, encodeData(bundle))
            bundle.visionImage?.let { put(KEY_VISION_IMAGE, encodeVisionImage(it)) }
        }

    private fun encodeManifest(manifest: BackupManifest, includeContentHash: Boolean): JSONObject =
        JSONObject().apply {
            put(KEY_FORMAT_VERSION, manifest.formatVersion)
            put(KEY_CREATED_AT, manifest.createdAtEpochMillis)
            put(KEY_APP_VERSION, manifest.appVersionName)
            put(KEY_RECORD_COUNTS, encodeCounts(manifest.recordCounts))
            if (includeContentHash) {
                put(KEY_CONTENT_SHA256, manifest.contentSha256)
            }
        }

    private fun encodeCounts(counts: BackupRecordCounts): JSONObject =
        JSONObject().apply {
            put(KEY_GOALS, counts.goals)
            put(KEY_DEBTORS, counts.debtors)
            put(KEY_DEBT_PAYMENTS, counts.debtPayments)
            put(KEY_TRANSFERS, counts.transfers)
            put(KEY_EXPENSES, counts.expenses)
            put(KEY_LUCK_DIARY_ENTRIES, counts.luckDiaryEntries)
            if (counts.goalActions > 0) put(KEY_GOAL_ACTIONS, counts.goalActions)
            if (counts.notes > 0) put(KEY_NOTES, counts.notes)
            if (counts.noteLines > 0) put(KEY_NOTE_LINES, counts.noteLines)
            if (counts.clients > 0) put(KEY_CLIENTS, counts.clients)
            if (counts.debtAdvances > 0) put(KEY_DEBT_ADVANCES, counts.debtAdvances)
            put(KEY_TOTAL, counts.total)
        }

    private fun encodeData(bundle: BackupBundle): JSONObject =
        JSONObject().apply {
            put(KEY_GOALS, encodeArray(bundle.goals, ::encodeGoal))
            put(KEY_DEBTORS, encodeArray(bundle.debtors, ::encodeDebtor))
            put(KEY_DEBT_PAYMENTS, encodeArray(bundle.debtPayments, ::encodeDebtPayment))
            put(KEY_TRANSFERS, encodeArray(bundle.transfers, ::encodeTransfer))
            put(KEY_EXPENSES, encodeArray(bundle.expenses, ::encodeExpense))
            put(KEY_LUCK_DIARY_ENTRIES, encodeArray(bundle.luckDiaryEntries, ::encodeLuckDiaryEntry))
            if (bundle.goalActions.isNotEmpty()) {
                put(KEY_GOAL_ACTIONS, encodeArray(bundle.goalActions, ::encodeGoalAction))
            }
            if (bundle.notes.isNotEmpty()) {
                put(KEY_NOTES, encodeArray(bundle.notes, ::encodeNote))
            }
            if (bundle.noteLines.isNotEmpty()) {
                put(KEY_NOTE_LINES, encodeArray(bundle.noteLines, ::encodeNoteLine))
            }
            if (bundle.clients.isNotEmpty()) {
                put(KEY_CLIENTS, encodeArray(bundle.clients, ::encodeClient))
            }
            if (bundle.debtAdvances.isNotEmpty()) {
                put(KEY_DEBT_ADVANCES, encodeArray(bundle.debtAdvances, ::encodeDebtAdvance))
            }
            put(KEY_PREFERENCES, encodePreferences(bundle.preferences))
        }

    private fun encodeGoal(goal: BackupGoal): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, goal.syncId)
            put(KEY_NAME, goal.name)
            put(KEY_TARGET_AMOUNT_CENTS, goal.targetAmountCents)
            put(KEY_CURRENT_AMOUNT_CENTS, goal.currentAmountCents)
            put(KEY_CURRENCY, goal.currency)
            put(KEY_UPDATED_AT, goal.updatedAtEpochMillis)
            putOptional(KEY_DEADLINE, goal.deadlineEpochMillis)
            if (goal.kind.isNotBlank() && goal.kind != DEFAULT_GOAL_KIND) {
                put(KEY_KIND, goal.kind)
            }
            putMode(goal.mode)
        }

    private fun encodeGoalAction(action: BackupGoalAction): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, action.syncId)
            put(KEY_GOAL_SYNC_ID, action.goalSyncId)
            put(KEY_TITLE, action.title)
            put(KEY_COMPLETED, action.completed)
            put(KEY_SORT_ORDER, action.sortOrder)
            put(KEY_OCCURRED_AT, action.occurredAtEpochMillis)
            put(KEY_UPDATED_AT, action.updatedAtEpochMillis)
        }

    private fun encodeNote(note: BackupNote): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, note.syncId)
            put(KEY_TITLE, note.title)
            put(KEY_KIND, note.kind)
            put(KEY_BODY, note.body)
            put(KEY_MARK, note.mark)
            put(KEY_CLOSED, note.closed)
            put(KEY_CREATED_AT, note.createdAtEpochMillis)
            put(KEY_UPDATED_AT, note.updatedAtEpochMillis)
            putMode(note.mode)
        }

    private fun encodeNoteLine(line: BackupNoteLine): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, line.syncId)
            put(KEY_NOTE_SYNC_ID, line.noteSyncId)
            put(KEY_LABEL, line.label)
            put(KEY_AMOUNT_CENTS, line.amountCents)
            put(KEY_SORT_ORDER, line.sortOrder)
            put(KEY_OCCURRED_AT, line.occurredAtEpochMillis)
            put(KEY_UPDATED_AT, line.updatedAtEpochMillis)
        }

    private fun encodeDebtor(debtor: BackupDebtor): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, debtor.syncId)
            put(KEY_NAME, debtor.name)
            put(KEY_TARGET_AMOUNT_CENTS, debtor.targetAmountCents)
            put(KEY_CURRENT_AMOUNT_CENTS, debtor.currentAmountCents)
            put(KEY_CURRENCY, debtor.currency)
            put(KEY_STATUS, debtor.status)
            put(KEY_DEBT_TYPE, debtor.debtType)
            put(KEY_AFFECTS_BALANCE, debtor.affectsBalance)
            put(KEY_NOTE, debtor.note)
            put(KEY_DATE_GIVEN, debtor.dateGivenEpochMillis)
            put(KEY_DATE_DUE, debtor.dateDueEpochMillis)
            put(KEY_LOAN_CATEGORY, debtor.loanCategory)
            put(KEY_INTEREST_RATE_BPS, debtor.interestRateBps)
            put(KEY_PHONE, debtor.phone)
            put(KEY_EMAIL, debtor.email)
            putMode(debtor.mode)
            putOptional(KEY_CLIENT_SYNC_ID, debtor.clientSyncId)
        }

    private fun encodeDebtAdvance(advance: com.example.money.domain.backup.BackupDebtAdvance): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, advance.syncId)
            put(KEY_DEBTOR_SYNC_ID, advance.debtorSyncId)
            put(KEY_AMOUNT_CENTS, advance.amountCents)
            put(KEY_CURRENCY, advance.currency)
            put(KEY_GIVEN_AT, advance.givenAtEpochMillis)
            put(KEY_NOTE, advance.note)
            put(KEY_UPDATED_AT, advance.updatedAtEpochMillis)
            putMode(advance.mode)
        }

    private fun encodeDebtPayment(payment: BackupDebtPayment): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, payment.syncId)
            put(KEY_DEBTOR_SYNC_ID, payment.debtorSyncId)
            put(KEY_AMOUNT_CENTS, payment.amountCents)
            put(KEY_CURRENCY, payment.currency)
            put(KEY_PAID_AT, payment.paidAtEpochMillis)
            put(KEY_AFFECTS_BALANCE, payment.affectsBalance)
            putMode(payment.mode)
        }

    private fun encodeTransfer(transfer: BackupTransfer): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, transfer.syncId)
            put(KEY_AMOUNT, transfer.amount)
            put(KEY_DATE, transfer.dateEpochMillis)
            put(KEY_IS_CONFIRMED, transfer.isConfirmed)
            putOptional(KEY_GOAL_SYNC_ID, transfer.goalSyncId)
            putOptional(KEY_DEBTOR_SYNC_ID, transfer.debtorSyncId)
            putOptional(KEY_DEBT_PAYMENT_SYNC_ID, transfer.debtPaymentSyncId)
            put(KEY_NOTE, transfer.note)
            putMode(transfer.mode)
        }

    private fun encodeExpense(expense: BackupExpense): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, expense.syncId)
            put(KEY_AMOUNT, expense.amount)
            put(KEY_CATEGORY, expense.category)
            put(KEY_DATE, expense.dateEpochMillis)
            put(KEY_NOTE, expense.note)
            put(KEY_IS_CONFIRMED, expense.isConfirmed)
            putOptional(KEY_DEBTOR_SYNC_ID, expense.debtorSyncId)
            putOptional(KEY_DEBT_PAYMENT_SYNC_ID, expense.debtPaymentSyncId)
            putOptional(KEY_GOAL_SYNC_ID, expense.goalSyncId)
            putMode(expense.mode)
        }

    private fun encodeLuckDiaryEntry(entry: BackupLuckDiaryEntry): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, entry.syncId)
            put(KEY_ENTRY_DATE, entry.entryDateEpochMillis)
            put(KEY_NOTE, entry.note)
            putOptional(KEY_AMOUNT_CENTS, entry.amountCents)
            put(KEY_CATEGORY, entry.category)
            put(KEY_CREATED_AT, entry.createdAtEpochMillis)
        }

    private fun encodePreferences(preferences: BackupPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_APP_LANGUAGE, preferences.appLanguage)
            put(KEY_CURRENCY_REGION, preferences.currencyRegion)
            put(KEY_WELCOME_SEEN, preferences.welcomeSeen)
            put(KEY_VOICE, encodeVoicePreferences(preferences.voice))
            put(KEY_PRIVACY, encodePrivacyPreferences(preferences.privacy))
            put(KEY_GAMIFICATION, encodeGamificationPreferences(preferences.gamification))
            put(KEY_AUTOPILOT, encodeAutopilotPreferences(preferences.autopilot))
            put(KEY_WEEKLY_CHECK_IN, encodeWeeklyCheckInPreferences(preferences.weeklyCheckIn))
            put(KEY_ADVISOR_HISTORY_JSON, preferences.advisorHistoryJson)
            put(KEY_GOAL_PSYCHOLOGY, encodeGoalPsychologyPreferences(preferences.goalPsychology))
            put(KEY_USER, encodeUserPreferences(preferences.user))
            put(KEY_CALENDAR, encodeCalendarPreferences(preferences.calendar))
            put(KEY_PERSONAL_DEBT, encodePersonalDebtPreferences(preferences.personalDebt))
        }

    private fun encodeVoicePreferences(preferences: BackupVoicePreferences): JSONObject =
        JSONObject().apply {
            put(KEY_ENABLED, preferences.enabled)
            put(KEY_REPORT_HOUR, preferences.reportHour)
            put(KEY_REPORT_MINUTE, preferences.reportMinute)
            put(KEY_LANGUAGE, preferences.language)
            put(KEY_GENDER, preferences.gender)
            put(KEY_FORCE_IN_SILENT_MODE, preferences.forceInSilentMode)
        }

    private fun encodePrivacyPreferences(preferences: BackupPrivacyPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_HIDE_AMOUNTS, preferences.hideAmounts)
            put(KEY_HIDE_ON_WIDGET, preferences.hideOnWidget)
        }

    private fun encodeGamificationPreferences(preferences: BackupGamificationPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_STREAK_COUNT, preferences.streakCount)
            putOptional(KEY_LAST_PAYMENT_ISO_DATE, preferences.lastPaymentIsoDate)
            put(KEY_UNLOCKED_BADGES, encodeStrings(preferences.unlockedBadges))
            putOptional(KEY_LAST_WIN_MESSAGE, preferences.lastWinMessage)
        }

    private fun encodeAutopilotPreferences(preferences: BackupAutopilotPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_ENABLED, preferences.enabled)
            put(KEY_DAILY_AMOUNT_CENTS, preferences.dailyAmountCents)
            put(KEY_STRATEGY, preferences.strategy)
            put(KEY_REMINDER_HOUR, preferences.reminderHour)
            put(KEY_REMINDER_MINUTE, preferences.reminderMinute)
            put(KEY_ROUND_PAYMENTS_TO_TEN, preferences.roundPaymentsToTen)
        }

    private fun encodeWeeklyCheckInPreferences(preferences: BackupWeeklyCheckInPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_ENABLED, preferences.enabled)
            put(KEY_HOUR, preferences.hour)
        }

    private fun encodeGoalPsychologyPreferences(
        preferences: BackupGoalPsychologyPreferences,
    ): JSONObject =
        JSONObject().apply {
            putOptional(KEY_PSYCHOTYPE, preferences.psychotype)
            put(KEY_PSYCHOTYPE_ANSWERS_JSON, preferences.psychotypeAnswersJson)
            put(KEY_PSYCHOTYPE_TEST_COMPLETED, preferences.psychotypeTestCompleted)
            put(KEY_SELECTED_FEAR_IDS, encodeStrings(preferences.selectedFearIds))
            put(KEY_WORD_POWER_ENABLED, preferences.wordPowerEnabled)
            put(KEY_PERSONAL_MOTIVATION, preferences.personalMotivation)
            put(KEY_PAYDAY_DAY_OF_MONTH, preferences.paydayDayOfMonth)
            put(KEY_PAYOFF_STRATEGY, preferences.payoffStrategy)
            put(KEY_WEEKLY_CHECKLIST_JSON, preferences.weeklyChecklistJson)
            put(KEY_ACTIVE_CHALLENGES_JSON, preferences.activeChallengesJson)
            put(KEY_GOAL_BADGES, encodeStrings(preferences.goalBadges))
        }

    private fun encodeUserPreferences(preferences: BackupUserPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_DISPLAY_NAME, preferences.displayName)
            put(KEY_GENDER, preferences.gender)
            putOptional(KEY_AGE, preferences.age)
            put(KEY_CUSTOM_FEAR, preferences.customFear)
            put(KEY_GOAL_TEXT, preferences.goalText)
            putOptional(KEY_EMOTION, preferences.emotion)
            put(KEY_PRIORITY, preferences.priority)
            put(KEY_ONBOARDING_COMPLETED, preferences.onboardingCompleted)
        }

    private fun encodeCalendarPreferences(preferences: BackupCalendarPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_AUTO_SYNC_ENABLED, preferences.autoSyncEnabled)
            put(KEY_REMIND_ONE_DAY_BEFORE, preferences.remindOneDayBefore)
            put(KEY_REMIND_THREE_DAYS_BEFORE, preferences.remindThreeDaysBefore)
            put(KEY_REMIND_ON_DUE_DAY, preferences.remindOnDueDay)
            put(KEY_USE_DEBTS_CALENDAR, preferences.useDebtsCalendar)
        }

    private fun encodePersonalDebtPreferences(preferences: BackupPersonalDebtPreferences): JSONObject =
        JSONObject().apply {
            put(KEY_DEBT_NOTIFICATIONS_ENABLED, preferences.debtNotificationsEnabled)
            put(KEY_CLIENT_NOTIFICATIONS_ENABLED, preferences.clientNotificationsEnabled)
            put(KEY_DEBT_ADVANCE_REMIND_1_DAY, preferences.debtAdvanceRemind1Day)
            put(KEY_DEBT_ADVANCE_REMIND_3_DAYS, preferences.debtAdvanceRemind3Days)
            put(KEY_DEBT_AUTO_MESSAGE_ENABLED, preferences.debtAutoMessageEnabled)
            put(KEY_DEBT_AUTO_MESSAGE_SMS, preferences.debtAutoMessageSms)
            put(KEY_DEBT_AUTO_MESSAGE_WHATSAPP, preferences.debtAutoMessageWhatsapp)
            put(KEY_DEBT_AUTO_MESSAGE_TELEGRAM, preferences.debtAutoMessageTelegram)
            put(KEY_DEBT_AUTO_MESSAGE_EMAIL, preferences.debtAutoMessageEmail)
            put(KEY_DEBT_DIRECT_SMS_ENABLED, preferences.debtDirectSmsEnabled)
            put(KEY_DEBT_REMINDER_HOUR, preferences.reminderHour)
            put(KEY_DEBT_REMINDER_MINUTE, preferences.reminderMinute)
        }

    private fun encodeVisionImage(image: BackupVisionImage): JSONObject =
        JSONObject().apply {
            put(KEY_FILE_NAME, image.fileName)
            put(KEY_MIME_TYPE, image.mimeType)
            put(KEY_BASE64_DATA, image.base64Data)
        }

    private fun decodeCounts(json: JSONObject): BackupRecordCounts {
        val counts = BackupRecordCounts(
            goals = json.getInt(KEY_GOALS),
            debtors = json.getInt(KEY_DEBTORS),
            debtPayments = json.getInt(KEY_DEBT_PAYMENTS),
            transfers = json.getInt(KEY_TRANSFERS),
            expenses = json.getInt(KEY_EXPENSES),
            luckDiaryEntries = json.getInt(KEY_LUCK_DIARY_ENTRIES),
            goalActions = json.optInt(KEY_GOAL_ACTIONS, 0),
            notes = json.optInt(KEY_NOTES, 0),
            noteLines = json.optInt(KEY_NOTE_LINES, 0),
            clients = json.optInt(KEY_CLIENTS, 0),
            debtAdvances = json.optInt(KEY_DEBT_ADVANCES, 0),
        )
        if (json.getInt(KEY_TOTAL) != counts.total) {
            throw BackupIntegrityException("Backup manifest total record count is invalid")
        }
        return counts
    }

    private fun decodeGoal(json: JSONObject): BackupGoal =
        BackupGoal(
            syncId = json.getString(KEY_SYNC_ID),
            name = json.getString(KEY_NAME),
            targetAmountCents = json.getLong(KEY_TARGET_AMOUNT_CENTS),
            currentAmountCents = json.getLong(KEY_CURRENT_AMOUNT_CENTS),
            currency = json.getString(KEY_CURRENCY),
            updatedAtEpochMillis = json.getLong(KEY_UPDATED_AT),
            deadlineEpochMillis = json.optionalLong(KEY_DEADLINE),
            kind = json.optionalString(KEY_KIND) ?: DEFAULT_GOAL_KIND,
            mode = json.optMode(),
        )

    private fun decodeGoalAction(json: JSONObject): BackupGoalAction =
        BackupGoalAction(
            syncId = json.getString(KEY_SYNC_ID),
            goalSyncId = json.getString(KEY_GOAL_SYNC_ID),
            title = json.getString(KEY_TITLE),
            completed = json.getBoolean(KEY_COMPLETED),
            sortOrder = json.getInt(KEY_SORT_ORDER),
            occurredAtEpochMillis = json.optionalLong(KEY_OCCURRED_AT) ?: json.getLong(KEY_UPDATED_AT),
            updatedAtEpochMillis = json.getLong(KEY_UPDATED_AT),
        )

    private fun decodeNote(json: JSONObject): BackupNote =
        BackupNote(
            syncId = json.getString(KEY_SYNC_ID),
            title = json.getString(KEY_TITLE),
            kind = json.getString(KEY_KIND),
            body = json.optString(KEY_BODY, ""),
            mark = json.optString(KEY_MARK, "NONE"),
            closed = json.optBoolean(KEY_CLOSED, false),
            createdAtEpochMillis = json.getLong(KEY_CREATED_AT),
            updatedAtEpochMillis = json.getLong(KEY_UPDATED_AT),
            mode = json.optMode(),
        )

    private fun decodeNoteLine(json: JSONObject): BackupNoteLine =
        BackupNoteLine(
            syncId = json.getString(KEY_SYNC_ID),
            noteSyncId = json.getString(KEY_NOTE_SYNC_ID),
            label = json.getString(KEY_LABEL),
            amountCents = json.getLong(KEY_AMOUNT_CENTS),
            sortOrder = json.getInt(KEY_SORT_ORDER),
            occurredAtEpochMillis = json.optionalLong(KEY_OCCURRED_AT) ?: json.getLong(KEY_UPDATED_AT),
            updatedAtEpochMillis = json.getLong(KEY_UPDATED_AT),
        )

    private fun decodeDebtor(json: JSONObject): BackupDebtor =
        BackupDebtor(
            syncId = json.getString(KEY_SYNC_ID),
            name = json.getString(KEY_NAME),
            targetAmountCents = json.getLong(KEY_TARGET_AMOUNT_CENTS),
            currentAmountCents = json.getLong(KEY_CURRENT_AMOUNT_CENTS),
            currency = json.getString(KEY_CURRENCY),
            status = json.getString(KEY_STATUS),
            debtType = json.getString(KEY_DEBT_TYPE),
            affectsBalance = json.getBoolean(KEY_AFFECTS_BALANCE),
            note = json.getString(KEY_NOTE),
            dateGivenEpochMillis = json.getLong(KEY_DATE_GIVEN),
            dateDueEpochMillis = json.getLong(KEY_DATE_DUE),
            loanCategory = json.getString(KEY_LOAN_CATEGORY),
            interestRateBps = json.getInt(KEY_INTEREST_RATE_BPS),
            phone = json.getString(KEY_PHONE),
            email = json.getString(KEY_EMAIL),
            mode = json.optMode(),
            clientSyncId = json.optionalString(KEY_CLIENT_SYNC_ID),
        )

    private fun decodeDebtAdvance(json: JSONObject): com.example.money.domain.backup.BackupDebtAdvance =
        com.example.money.domain.backup.BackupDebtAdvance(
            syncId = json.getString(KEY_SYNC_ID),
            debtorSyncId = json.getString(KEY_DEBTOR_SYNC_ID),
            amountCents = json.getLong(KEY_AMOUNT_CENTS),
            currency = json.getString(KEY_CURRENCY),
            givenAtEpochMillis = json.getLong(KEY_GIVEN_AT),
            note = json.optString(KEY_NOTE, ""),
            updatedAtEpochMillis = json.getLong(KEY_UPDATED_AT),
            mode = json.optMode(),
        )

    private fun decodeDebtPayment(json: JSONObject): BackupDebtPayment =
        BackupDebtPayment(
            syncId = json.getString(KEY_SYNC_ID),
            debtorSyncId = json.getString(KEY_DEBTOR_SYNC_ID),
            amountCents = json.getLong(KEY_AMOUNT_CENTS),
            currency = json.getString(KEY_CURRENCY),
            paidAtEpochMillis = json.getLong(KEY_PAID_AT),
            affectsBalance = json.getBoolean(KEY_AFFECTS_BALANCE),
            mode = json.optMode(),
        )

    private fun decodeTransfer(json: JSONObject): BackupTransfer =
        BackupTransfer(
            syncId = json.getString(KEY_SYNC_ID),
            amount = json.getDouble(KEY_AMOUNT),
            dateEpochMillis = json.getLong(KEY_DATE),
            isConfirmed = json.getBoolean(KEY_IS_CONFIRMED),
            goalSyncId = json.optionalString(KEY_GOAL_SYNC_ID),
            debtorSyncId = json.optionalString(KEY_DEBTOR_SYNC_ID),
            debtPaymentSyncId = json.optionalString(KEY_DEBT_PAYMENT_SYNC_ID),
            note = json.getString(KEY_NOTE),
            mode = json.optMode(),
        )

    private fun decodeExpense(json: JSONObject): BackupExpense =
        BackupExpense(
            syncId = json.getString(KEY_SYNC_ID),
            amount = json.getDouble(KEY_AMOUNT),
            category = json.getString(KEY_CATEGORY),
            dateEpochMillis = json.getLong(KEY_DATE),
            note = json.getString(KEY_NOTE),
            isConfirmed = json.getBoolean(KEY_IS_CONFIRMED),
            debtorSyncId = json.optionalString(KEY_DEBTOR_SYNC_ID),
            debtPaymentSyncId = json.optionalString(KEY_DEBT_PAYMENT_SYNC_ID),
            goalSyncId = json.optionalString(KEY_GOAL_SYNC_ID),
            mode = json.optMode(),
        )

    private fun decodeLuckDiaryEntry(json: JSONObject): BackupLuckDiaryEntry =
        BackupLuckDiaryEntry(
            syncId = json.getString(KEY_SYNC_ID),
            entryDateEpochMillis = json.getLong(KEY_ENTRY_DATE),
            note = json.getString(KEY_NOTE),
            amountCents = json.optionalLong(KEY_AMOUNT_CENTS),
            category = json.getString(KEY_CATEGORY),
            createdAtEpochMillis = json.getLong(KEY_CREATED_AT),
        )

    private fun decodePreferences(json: JSONObject): BackupPreferences =
        BackupPreferences(
            appLanguage = json.getString(KEY_APP_LANGUAGE),
            currencyRegion = json.getString(KEY_CURRENCY_REGION),
            welcomeSeen = json.getBoolean(KEY_WELCOME_SEEN),
            voice = decodeVoicePreferences(json.requiredObject(KEY_VOICE)),
            privacy = decodePrivacyPreferences(json.requiredObject(KEY_PRIVACY)),
            gamification = decodeGamificationPreferences(json.requiredObject(KEY_GAMIFICATION)),
            autopilot = decodeAutopilotPreferences(json.requiredObject(KEY_AUTOPILOT)),
            weeklyCheckIn = decodeWeeklyCheckInPreferences(json.requiredObject(KEY_WEEKLY_CHECK_IN)),
            advisorHistoryJson = json.getString(KEY_ADVISOR_HISTORY_JSON),
            goalPsychology = decodeGoalPsychologyPreferences(json.requiredObject(KEY_GOAL_PSYCHOLOGY)),
            user = decodeUserPreferences(json.requiredObject(KEY_USER)),
            calendar = decodeCalendarPreferences(json.requiredObject(KEY_CALENDAR)),
            personalDebt = decodePersonalDebtPreferences(json.requiredObject(KEY_PERSONAL_DEBT)),
        )

    private fun decodeVoicePreferences(json: JSONObject): BackupVoicePreferences =
        BackupVoicePreferences(
            enabled = json.getBoolean(KEY_ENABLED),
            reportHour = json.getInt(KEY_REPORT_HOUR),
            reportMinute = json.getInt(KEY_REPORT_MINUTE),
            language = json.getString(KEY_LANGUAGE),
            gender = json.getString(KEY_GENDER),
            forceInSilentMode = json.getBoolean(KEY_FORCE_IN_SILENT_MODE),
        )

    private fun decodePrivacyPreferences(json: JSONObject): BackupPrivacyPreferences =
        BackupPrivacyPreferences(
            hideAmounts = json.getBoolean(KEY_HIDE_AMOUNTS),
            hideOnWidget = json.getBoolean(KEY_HIDE_ON_WIDGET),
        )

    private fun decodeGamificationPreferences(json: JSONObject): BackupGamificationPreferences =
        BackupGamificationPreferences(
            streakCount = json.getInt(KEY_STREAK_COUNT),
            lastPaymentIsoDate = json.optionalString(KEY_LAST_PAYMENT_ISO_DATE),
            unlockedBadges = decodeStrings(json.getJSONArray(KEY_UNLOCKED_BADGES)),
            lastWinMessage = json.optionalString(KEY_LAST_WIN_MESSAGE),
        )

    private fun decodeAutopilotPreferences(json: JSONObject): BackupAutopilotPreferences =
        BackupAutopilotPreferences(
            enabled = json.getBoolean(KEY_ENABLED),
            dailyAmountCents = json.getLong(KEY_DAILY_AMOUNT_CENTS),
            strategy = json.getString(KEY_STRATEGY),
            reminderHour = json.getInt(KEY_REMINDER_HOUR),
            reminderMinute = json.getInt(KEY_REMINDER_MINUTE),
            roundPaymentsToTen = json.getBoolean(KEY_ROUND_PAYMENTS_TO_TEN),
        )

    private fun decodeWeeklyCheckInPreferences(json: JSONObject): BackupWeeklyCheckInPreferences =
        BackupWeeklyCheckInPreferences(
            enabled = json.getBoolean(KEY_ENABLED),
            hour = json.getInt(KEY_HOUR),
        )

    private fun decodeGoalPsychologyPreferences(json: JSONObject): BackupGoalPsychologyPreferences =
        BackupGoalPsychologyPreferences(
            psychotype = json.optionalString(KEY_PSYCHOTYPE),
            psychotypeAnswersJson = json.getString(KEY_PSYCHOTYPE_ANSWERS_JSON),
            psychotypeTestCompleted = json.getBoolean(KEY_PSYCHOTYPE_TEST_COMPLETED),
            selectedFearIds = decodeStrings(json.getJSONArray(KEY_SELECTED_FEAR_IDS)),
            wordPowerEnabled = json.getBoolean(KEY_WORD_POWER_ENABLED),
            personalMotivation = json.getString(KEY_PERSONAL_MOTIVATION),
            paydayDayOfMonth = json.getInt(KEY_PAYDAY_DAY_OF_MONTH),
            payoffStrategy = json.getString(KEY_PAYOFF_STRATEGY),
            weeklyChecklistJson = json.getString(KEY_WEEKLY_CHECKLIST_JSON),
            activeChallengesJson = json.getString(KEY_ACTIVE_CHALLENGES_JSON),
            goalBadges = decodeStrings(json.getJSONArray(KEY_GOAL_BADGES)),
        )

    private fun decodeUserPreferences(json: JSONObject): BackupUserPreferences =
        BackupUserPreferences(
            displayName = json.getString(KEY_DISPLAY_NAME),
            gender = json.getString(KEY_GENDER),
            age = json.optionalInt(KEY_AGE),
            customFear = json.getString(KEY_CUSTOM_FEAR),
            goalText = json.getString(KEY_GOAL_TEXT),
            emotion = json.optionalString(KEY_EMOTION),
            priority = json.getString(KEY_PRIORITY),
            onboardingCompleted = json.getBoolean(KEY_ONBOARDING_COMPLETED),
        )

    private fun decodeCalendarPreferences(json: JSONObject): BackupCalendarPreferences =
        BackupCalendarPreferences(
            autoSyncEnabled = json.getBoolean(KEY_AUTO_SYNC_ENABLED),
            remindOneDayBefore = json.getBoolean(KEY_REMIND_ONE_DAY_BEFORE),
            remindThreeDaysBefore = json.getBoolean(KEY_REMIND_THREE_DAYS_BEFORE),
            remindOnDueDay = json.getBoolean(KEY_REMIND_ON_DUE_DAY),
            useDebtsCalendar = json.getBoolean(KEY_USE_DEBTS_CALENDAR),
        )

    private fun decodePersonalDebtPreferences(json: JSONObject): BackupPersonalDebtPreferences =
        BackupPersonalDebtPreferences(
            debtNotificationsEnabled = json.getBoolean(KEY_DEBT_NOTIFICATIONS_ENABLED),
            clientNotificationsEnabled = json.optBoolean(KEY_CLIENT_NOTIFICATIONS_ENABLED, true),
            debtAdvanceRemind1Day = json.getBoolean(KEY_DEBT_ADVANCE_REMIND_1_DAY),
            debtAdvanceRemind3Days = json.getBoolean(KEY_DEBT_ADVANCE_REMIND_3_DAYS),
            debtAutoMessageEnabled = json.getBoolean(KEY_DEBT_AUTO_MESSAGE_ENABLED),
            debtAutoMessageSms = json.getBoolean(KEY_DEBT_AUTO_MESSAGE_SMS),
            debtAutoMessageWhatsapp = json.getBoolean(KEY_DEBT_AUTO_MESSAGE_WHATSAPP),
            debtAutoMessageTelegram = json.getBoolean(KEY_DEBT_AUTO_MESSAGE_TELEGRAM),
            debtAutoMessageEmail = json.getBoolean(KEY_DEBT_AUTO_MESSAGE_EMAIL),
            debtDirectSmsEnabled = json.getBoolean(KEY_DEBT_DIRECT_SMS_ENABLED),
            reminderHour = json.optInt(KEY_DEBT_REMINDER_HOUR, 9).coerceIn(0, 23),
            reminderMinute = json.optInt(KEY_DEBT_REMINDER_MINUTE, 0).coerceIn(0, 59),
        )

    private fun decodeVisionImage(json: JSONObject): BackupVisionImage =
        BackupVisionImage(
            fileName = json.getString(KEY_FILE_NAME),
            mimeType = json.getString(KEY_MIME_TYPE),
            base64Data = json.getString(KEY_BASE64_DATA),
        )

    private fun decodeStrings(array: JSONArray): Set<String> =
        buildSet {
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }

    private fun encodeStrings(values: Set<String>): JSONArray =
        JSONArray().apply {
            values.sorted().forEach(::put)
        }

    private fun <T> encodeArray(values: List<T>, encode: (T) -> JSONObject): JSONArray =
        JSONArray().apply {
            values.forEach { put(encode(it)) }
        }

    private fun <T> decodeArray(
        parent: JSONObject,
        key: String,
        decode: (JSONObject) -> T,
    ): List<T> {
        val array = parent.getJSONArray(key)
        if (array.length() > maxRecordCount) {
            throw BackupValidationException(
                "Backup field $key exceeds maximum record count of $maxRecordCount",
            )
        }
        return buildList {
            for (index in 0 until array.length()) {
                add(decode(array.getJSONObject(index)))
            }
        }
    }

    private fun <T> decodeOptionalArray(
        parent: JSONObject,
        key: String,
        decode: (JSONObject) -> T,
    ): List<T> = if (parent.has(key) && !parent.isNull(key)) {
        decodeArray(parent, key, decode)
    } else {
        emptyList()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance(SHA_256)
            .digest(bytes)
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun hashesMatch(first: String, second: String): Boolean =
        MessageDigest.isEqual(
            first.toByteArray(Charsets.US_ASCII),
            second.toByteArray(Charsets.US_ASCII),
        )

    private fun JSONObject.requiredObject(key: String): JSONObject =
        optJSONObject(key) ?: throw BackupFormatException("Backup field \"$key\" must be an object")

    private fun JSONObject.optionalObject(key: String): JSONObject? =
        if (has(key) && !isNull(key)) {
            optJSONObject(key)
                ?: throw BackupFormatException("Backup field \"$key\" must be an object")
        } else {
            null
        }

    private fun JSONObject.optionalString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optionalLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.optionalInt(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    private fun encodeClient(client: BackupClient): JSONObject =
        JSONObject().apply {
            put(KEY_SYNC_ID, client.syncId)
            put(KEY_NAME, client.name)
            put(KEY_PHONE, client.phone)
            put(KEY_COMPANY, client.company)
            putOptional(KEY_EMAIL, client.email)
            putOptional(KEY_LAST_MEETING_AT, client.lastMeetingAtEpochMillis)
            put(KEY_NOTE, client.note)
            put(KEY_CREATED_AT, client.createdAtEpochMillis)
            put(KEY_SYNC_UPDATED_AT, client.syncUpdatedAtEpochMillis)
            put(KEY_DELETED, client.deleted)
            put(KEY_MODE, client.mode)
        }

    private fun decodeClient(json: JSONObject): BackupClient =
        BackupClient(
            syncId = json.getString(KEY_SYNC_ID),
            name = json.getString(KEY_NAME),
            phone = json.optString(KEY_PHONE, ""),
            company = json.optString(KEY_COMPANY, ""),
            email = json.optionalString(KEY_EMAIL),
            lastMeetingAtEpochMillis = json.optionalLong(KEY_LAST_MEETING_AT),
            note = json.optString(KEY_NOTE, ""),
            createdAtEpochMillis = json.getLong(KEY_CREATED_AT),
            syncUpdatedAtEpochMillis = json.getLong(KEY_SYNC_UPDATED_AT),
            deleted = json.optBoolean(KEY_DELETED, false),
            mode = json.optMode(default = MODE_PROFESSIONAL),
        )

    private fun JSONObject.putMode(mode: String) {
        if (mode.isNotBlank() && mode != MODE_PERSONAL) {
            put(KEY_MODE, mode)
        }
    }

    private fun JSONObject.optMode(default: String = MODE_PERSONAL): String {
        if (!has(KEY_MODE)) return default
        return if (optString(KEY_MODE) == MODE_PROFESSIONAL) MODE_PROFESSIONAL else MODE_PERSONAL
    }

    private fun JSONObject.putOptional(key: String, value: Any?) {
        if (value != null) {
            put(key, value)
        }
    }

    companion object {
        const val DEFAULT_MAX_BUNDLE_BYTES = 25 * 1024 * 1024
        const val DEFAULT_MAX_RECORD_COUNT = 10_000
        const val DEFAULT_MAX_VISION_IMAGE_BYTES = 10 * 1024 * 1024

        private const val SHA_256 = "SHA-256"
        private const val DEFAULT_GOAL_KIND = "MONEY"
        private const val MODE_PERSONAL = "PERSONAL"
        private const val MODE_PROFESSIONAL = "PROFESSIONAL"

        private const val KEY_MANIFEST = "manifest"
        private const val KEY_DATA = "data"
        private const val KEY_VISION_IMAGE = "visionImage"
        private const val KEY_FORMAT_VERSION = "formatVersion"
        private const val KEY_CREATED_AT = "createdAtEpochMillis"
        private const val KEY_APP_VERSION = "appVersionName"
        private const val KEY_RECORD_COUNTS = "recordCounts"
        private const val KEY_CONTENT_SHA256 = "contentSha256"
        private const val KEY_TOTAL = "total"
        private const val KEY_GOALS = "goals"
        private const val KEY_DEBTORS = "debtors"
        private const val KEY_DEBT_PAYMENTS = "debtPayments"
        private const val KEY_TRANSFERS = "transfers"
        private const val KEY_EXPENSES = "expenses"
        private const val KEY_LUCK_DIARY_ENTRIES = "luckDiaryEntries"
        private const val KEY_GOAL_ACTIONS = "goalActions"
        private const val KEY_NOTES = "notes"
        private const val KEY_NOTE_LINES = "noteLines"
        private const val KEY_CLIENTS = "clients"
        private const val KEY_DEBT_ADVANCES = "debtAdvances"
        private const val KEY_GIVEN_AT = "givenAtEpochMillis"
        private const val KEY_MODE = "mode"
        private const val KEY_CLIENT_SYNC_ID = "clientSyncId"
        private const val KEY_COMPANY = "company"
        private const val KEY_LAST_MEETING_AT = "lastMeetingAtEpochMillis"
        private const val KEY_SYNC_UPDATED_AT = "syncUpdatedAtEpochMillis"
        private const val KEY_DELETED = "deleted"
        private const val KEY_PREFERENCES = "preferences"
        private const val KEY_SYNC_ID = "syncId"
        private const val KEY_NAME = "name"
        private const val KEY_TARGET_AMOUNT_CENTS = "targetAmountCents"
        private const val KEY_CURRENT_AMOUNT_CENTS = "currentAmountCents"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_UPDATED_AT = "updatedAtEpochMillis"
        private const val KEY_OCCURRED_AT = "occurredAtEpochMillis"
        private const val KEY_DEADLINE = "deadlineEpochMillis"
        private const val KEY_STATUS = "status"
        private const val KEY_DEBT_TYPE = "debtType"
        private const val KEY_AFFECTS_BALANCE = "affectsBalance"
        private const val KEY_NOTE = "note"
        private const val KEY_DATE_GIVEN = "dateGivenEpochMillis"
        private const val KEY_DATE_DUE = "dateDueEpochMillis"
        private const val KEY_LOAN_CATEGORY = "loanCategory"
        private const val KEY_INTEREST_RATE_BPS = "interestRateBps"
        private const val KEY_PHONE = "phone"
        private const val KEY_EMAIL = "email"
        private const val KEY_DEBTOR_SYNC_ID = "debtorSyncId"
        private const val KEY_AMOUNT_CENTS = "amountCents"
        private const val KEY_PAID_AT = "paidAtEpochMillis"
        private const val KEY_AMOUNT = "amount"
        private const val KEY_DATE = "dateEpochMillis"
        private const val KEY_IS_CONFIRMED = "isConfirmed"
        private const val KEY_GOAL_SYNC_ID = "goalSyncId"
        private const val KEY_NOTE_SYNC_ID = "noteSyncId"
        private const val KEY_KIND = "kind"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_MARK = "mark"
        private const val KEY_CLOSED = "closed"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_SORT_ORDER = "sortOrder"
        private const val KEY_LABEL = "label"
        private const val KEY_DEBT_PAYMENT_SYNC_ID = "debtPaymentSyncId"
        private const val KEY_CATEGORY = "category"
        private const val KEY_ENTRY_DATE = "entryDateEpochMillis"
        private const val KEY_APP_LANGUAGE = "appLanguage"
        private const val KEY_CURRENCY_REGION = "currencyRegion"
        private const val KEY_WELCOME_SEEN = "welcomeSeen"
        private const val KEY_VOICE = "voice"
        private const val KEY_PRIVACY = "privacy"
        private const val KEY_GAMIFICATION = "gamification"
        private const val KEY_AUTOPILOT = "autopilot"
        private const val KEY_WEEKLY_CHECK_IN = "weeklyCheckIn"
        private const val KEY_ADVISOR_HISTORY_JSON = "advisorHistoryJson"
        private const val KEY_GOAL_PSYCHOLOGY = "goalPsychology"
        private const val KEY_USER = "user"
        private const val KEY_CALENDAR = "calendar"
        private const val KEY_PERSONAL_DEBT = "personalDebt"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_REPORT_HOUR = "reportHour"
        private const val KEY_REPORT_MINUTE = "reportMinute"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_GENDER = "gender"
        private const val KEY_FORCE_IN_SILENT_MODE = "forceInSilentMode"
        private const val KEY_HIDE_AMOUNTS = "hideAmounts"
        private const val KEY_HIDE_ON_WIDGET = "hideOnWidget"
        private const val KEY_STREAK_COUNT = "streakCount"
        private const val KEY_LAST_PAYMENT_ISO_DATE = "lastPaymentIsoDate"
        private const val KEY_UNLOCKED_BADGES = "unlockedBadges"
        private const val KEY_LAST_WIN_MESSAGE = "lastWinMessage"
        private const val KEY_DAILY_AMOUNT_CENTS = "dailyAmountCents"
        private const val KEY_STRATEGY = "strategy"
        private const val KEY_REMINDER_HOUR = "reminderHour"
        private const val KEY_REMINDER_MINUTE = "reminderMinute"
        private const val KEY_ROUND_PAYMENTS_TO_TEN = "roundPaymentsToTen"
        private const val KEY_HOUR = "hour"
        private const val KEY_PSYCHOTYPE = "psychotype"
        private const val KEY_PSYCHOTYPE_ANSWERS_JSON = "psychotypeAnswersJson"
        private const val KEY_PSYCHOTYPE_TEST_COMPLETED = "psychotypeTestCompleted"
        private const val KEY_SELECTED_FEAR_IDS = "selectedFearIds"
        private const val KEY_WORD_POWER_ENABLED = "wordPowerEnabled"
        private const val KEY_PERSONAL_MOTIVATION = "personalMotivation"
        private const val KEY_PAYDAY_DAY_OF_MONTH = "paydayDayOfMonth"
        private const val KEY_PAYOFF_STRATEGY = "payoffStrategy"
        private const val KEY_WEEKLY_CHECKLIST_JSON = "weeklyChecklistJson"
        private const val KEY_ACTIVE_CHALLENGES_JSON = "activeChallengesJson"
        private const val KEY_GOAL_BADGES = "goalBadges"
        private const val KEY_DISPLAY_NAME = "displayName"
        private const val KEY_AGE = "age"
        private const val KEY_CUSTOM_FEAR = "customFear"
        private const val KEY_GOAL_TEXT = "goalText"
        private const val KEY_EMOTION = "emotion"
        private const val KEY_PRIORITY = "priority"
        private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"
        private const val KEY_AUTO_SYNC_ENABLED = "autoSyncEnabled"
        private const val KEY_REMIND_ONE_DAY_BEFORE = "remindOneDayBefore"
        private const val KEY_REMIND_THREE_DAYS_BEFORE = "remindThreeDaysBefore"
        private const val KEY_REMIND_ON_DUE_DAY = "remindOnDueDay"
        private const val KEY_USE_DEBTS_CALENDAR = "useDebtsCalendar"
        private const val KEY_DEBT_NOTIFICATIONS_ENABLED = "debtNotificationsEnabled"
        private const val KEY_CLIENT_NOTIFICATIONS_ENABLED = "clientNotificationsEnabled"
        private const val KEY_DEBT_ADVANCE_REMIND_1_DAY = "debtAdvanceRemind1Day"
        private const val KEY_DEBT_ADVANCE_REMIND_3_DAYS = "debtAdvanceRemind3Days"
        private const val KEY_DEBT_AUTO_MESSAGE_ENABLED = "debtAutoMessageEnabled"
        private const val KEY_DEBT_AUTO_MESSAGE_SMS = "debtAutoMessageSms"
        private const val KEY_DEBT_AUTO_MESSAGE_WHATSAPP = "debtAutoMessageWhatsapp"
        private const val KEY_DEBT_AUTO_MESSAGE_TELEGRAM = "debtAutoMessageTelegram"
        private const val KEY_DEBT_AUTO_MESSAGE_EMAIL = "debtAutoMessageEmail"
        private const val KEY_DEBT_DIRECT_SMS_ENABLED = "debtDirectSmsEnabled"
        private const val KEY_DEBT_REMINDER_HOUR = "debtReminderHour"
        private const val KEY_DEBT_REMINDER_MINUTE = "debtReminderMinute"
        private const val KEY_FILE_NAME = "fileName"
        private const val KEY_MIME_TYPE = "mimeType"
        private const val KEY_BASE64_DATA = "base64Data"

        private val SHA_256_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}

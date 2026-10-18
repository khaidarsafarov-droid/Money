package com.example.money.data.sync

import com.example.money.data.local.entity.DebtAdvanceEntity
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.data.local.entity.ClientEntity
import com.example.money.domain.sync.SnapshotClient
import com.example.money.domain.sync.SnapshotDebt
import com.example.money.domain.sync.SnapshotDebtAdvance
import com.example.money.domain.sync.SnapshotDebtPayment
import com.example.money.domain.sync.SnapshotGoal
import com.example.money.domain.sync.SnapshotGoalAction
import com.example.money.domain.sync.SnapshotNote
import com.example.money.domain.sync.SnapshotNoteLine
import com.example.money.domain.sync.SnapshotTransaction
import com.example.money.domain.sync.SnapshotVoiceSettings
import com.example.money.domain.util.MoneyMath
import com.example.money.voice.VoiceReportSettings
import org.json.JSONArray
import org.json.JSONObject

object FinanceSnapshotCodec {

    private const val MAX_SNAPSHOT_BYTES = 5 * 1024 * 1024
    private const val MAX_RECORD_COUNT = 10_000

    private const val KIND_DEPOSIT = "DEPOSIT"
    private const val KIND_GOAL_TRANSFER = "GOAL_TRANSFER"
    private const val KIND_EXPENSE = "EXPENSE"

    fun encode(snapshot: FinanceSnapshot): String = JSONObject().apply {
        put("schemaVersion", snapshot.schemaVersion)
        put("deviceId", snapshot.deviceId)
        put("updatedAt", snapshot.updatedAt)
        put("goals", encodeGoals(snapshot.goals))
        put("debts", encodeDebts(snapshot.debts))
        put("debtPayments", encodeDebtPayments(snapshot.debtPayments))
        put("transactions", encodeTransactions(snapshot.transactions))
        snapshot.voiceSettings?.let { put("voiceSettings", encodeVoiceSettingsObject(it)) }
        snapshot.goalActions?.let { put("goalActions", encodeGoalActions(it)) }
        snapshot.notes?.let { put("notes", encodeNotes(it)) }
        snapshot.noteLines?.let { put("noteLines", encodeNoteLines(it)) }
        snapshot.clients?.let { put("clients", encodeClients(it)) }
        snapshot.debtAdvances?.let { put("debtAdvances", encodeDebtAdvances(it)) }
    }.toString()

    fun decode(raw: String): FinanceSnapshot {
        if (raw.length > MAX_SNAPSHOT_BYTES) {
            throw IllegalArgumentException("Snapshot exceeds maximum size of $MAX_SNAPSHOT_BYTES bytes")
        }

        val json = try {
            JSONObject(raw)
        } catch (_: Exception) {
            throw IllegalArgumentException("Invalid snapshot JSON")
        }

        if (!json.has("schemaVersion") || !json.has("deviceId")) {
            throw IllegalArgumentException("Snapshot missing required fields")
        }

        val deviceId = json.getString("deviceId")
        if (deviceId.isBlank()) {
            throw IllegalArgumentException("Snapshot missing deviceId")
        }

        val schemaVersion = json.getInt("schemaVersion")
        return if (schemaVersion >= 2) {
            validateArraySizes(json)
            decodeV2(json)
        } else {
            decodeLegacyV1(json)
        }
    }

    private fun validateArraySizes(json: JSONObject) {
        listOf("goals", "debts", "debtPayments", "transactions").forEach { key ->
            val array = json.optJSONArray(key) ?: throw IllegalArgumentException("Snapshot field \"$key\" must be an array")
            if (array.length() > MAX_RECORD_COUNT) {
                throw IllegalArgumentException("Snapshot field \"$key\" exceeds maximum record count")
            }
        }
        listOf("goalActions", "notes", "noteLines", "clients", "debtAdvances").forEach { key ->
            val array = json.optJSONArray(key) ?: return@forEach
            if (array.length() > MAX_RECORD_COUNT) {
                throw IllegalArgumentException("Snapshot field \"$key\" exceeds maximum record count")
            }
        }
    }

    fun encodeGoal(entity: SavingsGoalEntity, deleted: Boolean = false): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("name", entity.name)
            put("targetAmountCents", entity.targetAmountCents)
            put("currency", entity.currency)
            entity.deadlineMillis?.let { put("dateDeadline", SyncDates.formatDate(it)) }
            put("goalKind", entity.kind)
            put("syncUpdatedAt", entity.updatedAt)
            put("deleted", deleted)
            put("mode", entity.mode)
        }.toString()

    fun encodeDebt(entity: DebtorEntity, syncUpdatedAt: Long, deleted: Boolean = false): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("name", entity.name)
            put("targetAmountCents", entity.targetAmountCents)
            put("currentAmountCents", entity.currentAmountCents)
            put("currency", entity.currency)
            put("status", entity.status)
            put("dateGiven", SyncDates.formatDate(entity.dateGiven))
            put("dateDue", SyncDates.formatDate(entity.dateDue))
            put("debtType", entity.debtType)
            put("affectsBalance", entity.affectsBalance)
            put("note", entity.note)
            put("syncUpdatedAt", syncUpdatedAt)
            put("deleted", deleted)
            put("mode", entity.mode)
        }.toString()

    fun encodeDebtPayment(
        entity: DebtPaymentEntity,
        debtorSyncId: String,
        syncUpdatedAt: Long = entity.paidAt,
        deleted: Boolean = false,
    ): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("debtorId", debtorSyncId)
            put("amountCents", entity.amountCents)
            put("paidAt", SyncDates.formatDateTime(entity.paidAt))
            put("affectsBalance", entity.affectsBalance)
            put("currency", entity.currency)
            put("syncUpdatedAt", syncUpdatedAt)
            put("deleted", deleted)
            put("mode", entity.mode)
        }.toString()

    fun encodeTransactionFromTransfer(
        entity: TransferEntity,
        debtSyncId: String? = null,
        debtPaymentSyncId: String? = null,
        syncUpdatedAt: Long = entity.date,
        deleted: Boolean = false,
    ): String {
        val kind = if (entity.goalId == null && entity.goalSyncId == null) KIND_DEPOSIT else KIND_GOAL_TRANSFER
        return JSONObject().apply {
            put("id", entity.syncId)
            put("kind", kind)
            put("amountCents", entity.amountCents)
            entity.goalSyncId?.let { put("goalId", it) }
            debtSyncId?.let { put("debtId", it) }
            debtPaymentSyncId?.let { put("debtPaymentId", it) }
            put("createdAt", SyncDates.formatDateTime(entity.date))
            entity.note.takeIf { it.isNotBlank() }?.let { put("note", it) }
            put("syncUpdatedAt", syncUpdatedAt)
            put("deleted", deleted)
            put("mode", entity.mode)
        }.toString()
    }

    fun encodeTransactionFromExpense(
        entity: ExpenseEntity,
        debtSyncId: String? = null,
        debtPaymentSyncId: String? = null,
        syncUpdatedAt: Long = entity.date,
        deleted: Boolean = false,
    ): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("kind", KIND_EXPENSE)
            put("amountCents", entity.amountCents)
            put("createdAt", SyncDates.formatDateTime(entity.date))
            put("category", entity.category)
            put("note", entity.note)
            entity.goalSyncId?.let { put("goalId", it) }
            debtSyncId?.let { put("debtId", it) }
            debtPaymentSyncId?.let { put("debtPaymentId", it) }
            put("syncUpdatedAt", syncUpdatedAt)
            put("deleted", deleted)
            put("mode", entity.mode)
        }.toString()

    fun encodeGoal(record: SnapshotGoal): String = encodeGoalRecord(record).toString()

    fun encodeDebt(record: SnapshotDebt): String = encodeDebtRecord(record).toString()

    fun encodeDebtPayment(record: SnapshotDebtPayment): String =
        encodeDebtPaymentRecord(record).toString()

    fun encodeDebtAdvance(record: SnapshotDebtAdvance): String =
        encodeDebtAdvanceRecord(record).toString()

    fun encodeTransaction(record: SnapshotTransaction): String =
        encodeTransactionRecord(record).toString()

    fun encodeGoalAction(
        entity: GoalActionEntity,
        goalSyncId: String,
        deleted: Boolean = false,
    ): String = encodeGoalActionRecord(
        toSnapshotGoalAction(entity, goalSyncId).copy(deleted = deleted),
    ).toString()

    fun encodeNote(entity: FinanceNoteEntity, deleted: Boolean = false): String =
        encodeNoteRecord(toSnapshotNote(entity).copy(deleted = deleted)).toString()

    fun encodeNoteLine(
        entity: FinanceNoteLineEntity,
        noteSyncId: String,
        deleted: Boolean = false,
    ): String = encodeNoteLineRecord(
        toSnapshotNoteLine(entity, noteSyncId).copy(deleted = deleted),
    ).toString()

    fun encodeGoalAction(record: SnapshotGoalAction): String =
        encodeGoalActionRecord(record).toString()

    fun encodeNote(record: SnapshotNote): String = encodeNoteRecord(record).toString()

    fun encodeNoteLine(record: SnapshotNoteLine): String = encodeNoteLineRecord(record).toString()

    fun decodeGoal(raw: String): SnapshotGoal =
        decodeGoals(JSONArray().put(JSONObject(raw))).single()

    fun decodeDebt(raw: String): SnapshotDebt =
        decodeDebts(JSONArray().put(JSONObject(raw))).single()

    fun decodeDebtPayment(raw: String): SnapshotDebtPayment =
        decodeDebtPayments(JSONArray().put(JSONObject(raw))).single()

    fun decodeDebtAdvance(raw: String): SnapshotDebtAdvance =
        decodeDebtAdvances(JSONArray().put(JSONObject(raw))).single()

    fun decodeTransaction(raw: String): SnapshotTransaction =
        decodeTransactions(JSONArray().put(JSONObject(raw))).single()

    fun decodeGoalAction(raw: String): SnapshotGoalAction =
        decodeGoalActions(JSONArray().put(JSONObject(raw))).single()

    fun decodeNote(raw: String): SnapshotNote =
        decodeNotes(JSONArray().put(JSONObject(raw))).single()

    fun decodeNoteLine(raw: String): SnapshotNoteLine =
        decodeNoteLines(JSONArray().put(JSONObject(raw))).single()

    fun encodeVoiceSettings(settings: VoiceReportSettings, syncUpdatedAt: Long): String =
        encodeVoiceSettings(
            SnapshotVoiceSettings(
                enabled = settings.enabled,
                reportHour = settings.reportHour,
                reportMinute = settings.reportMinute,
                language = settings.language.name,
                gender = settings.gender.name,
                forceInSilentMode = settings.forceInSilentMode,
                lastVoiceReportDate = settings.lastVoiceReportDate,
                lastAppOpenDate = settings.lastAppOpenDate,
                syncUpdatedAt = syncUpdatedAt,
            ),
        )

    fun toSnapshotGoal(entity: SavingsGoalEntity): SnapshotGoal =
        SnapshotGoal(
            id = entity.syncId,
            name = entity.name,
            targetAmountCents = entity.targetAmountCents,
            currency = entity.currency,
            dateDeadline = entity.deadlineMillis?.let(SyncDates::formatDate),
            goalKind = entity.kind,
            syncUpdatedAt = entity.updatedAt,
            mode = entity.mode,
        )

    fun toSnapshotDebt(
        entity: DebtorEntity,
        syncUpdatedAt: Long,
        clientSyncId: String? = null,
    ): SnapshotDebt =
        SnapshotDebt(
            id = entity.syncId,
            name = entity.name,
            targetAmountCents = entity.targetAmountCents,
            currentAmountCents = entity.currentAmountCents,
            currency = entity.currency,
            status = entity.status,
            dateGiven = SyncDates.formatDate(entity.dateGiven),
            dateDue = SyncDates.formatDate(entity.dateDue),
            debtType = entity.debtType,
            affectsBalance = entity.affectsBalance,
            note = entity.note,
            syncUpdatedAt = syncUpdatedAt,
            mode = entity.mode,
            clientSyncId = clientSyncId,
        )

    fun toSnapshotDebtAdvance(entity: DebtAdvanceEntity, debtorSyncId: String): SnapshotDebtAdvance =
        SnapshotDebtAdvance(
            id = entity.syncId,
            debtorId = debtorSyncId,
            amountCents = entity.amountCents,
            givenAt = SyncDates.formatDateTime(entity.givenAt),
            note = entity.note,
            currency = entity.currency,
            syncUpdatedAt = entity.updatedAt,
            mode = entity.mode,
        )

    fun toSnapshotDebtPayment(entity: DebtPaymentEntity, debtorSyncId: String): SnapshotDebtPayment =
        SnapshotDebtPayment(
            id = entity.syncId,
            debtorId = debtorSyncId,
            amountCents = entity.amountCents,
            paidAt = SyncDates.formatDateTime(entity.paidAt),
            affectsBalance = entity.affectsBalance,
            syncUpdatedAt = entity.paidAt,
            currency = entity.currency,
            mode = entity.mode,
        )

    fun toSnapshotTransactionFromTransfer(
        entity: TransferEntity,
        debtSyncId: String? = null,
        debtPaymentSyncId: String? = null,
    ): SnapshotTransaction {
        val kind = if (entity.goalId == null && entity.goalSyncId == null) KIND_DEPOSIT else KIND_GOAL_TRANSFER
        return SnapshotTransaction(
            id = entity.syncId,
            kind = kind,
            amountCents = entity.amountCents,
            goalId = entity.goalSyncId,
            debtId = debtSyncId,
            debtPaymentId = debtPaymentSyncId,
            createdAt = SyncDates.formatDateTime(entity.date),
            note = entity.note.ifBlank { null },
            category = null,
            syncUpdatedAt = entity.date,
            mode = entity.mode,
        )
    }

    fun toSnapshotTransactionFromExpense(
        entity: ExpenseEntity,
        debtSyncId: String? = null,
        debtPaymentSyncId: String? = null,
    ): SnapshotTransaction =
        SnapshotTransaction(
            id = entity.syncId,
            kind = KIND_EXPENSE,
            amountCents = entity.amountCents,
            goalId = entity.goalSyncId,
            debtId = debtSyncId,
            debtPaymentId = debtPaymentSyncId,
            createdAt = SyncDates.formatDateTime(entity.date),
            note = entity.note.ifBlank { null },
            category = entity.category,
            syncUpdatedAt = entity.date,
            mode = entity.mode,
        )

    private fun decodeV2(json: JSONObject): FinanceSnapshot =
        FinanceSnapshot(
            schemaVersion = maxOf(
                json.getInt("schemaVersion"),
                FinanceSnapshot.CURRENT_SCHEMA_VERSION,
            ),
            deviceId = json.getString("deviceId"),
            updatedAt = json.getLong("updatedAt"),
            goals = decodeGoals(json.getJSONArray("goals")),
            debts = decodeDebts(json.getJSONArray("debts")),
            debtPayments = decodeDebtPayments(json.getJSONArray("debtPayments")),
            transactions = decodeTransactions(json.getJSONArray("transactions")),
            voiceSettings = json.optJSONObject("voiceSettings")?.let(::decodeVoiceSettings),
            goalActions = json.optJSONArray("goalActions")?.let(::decodeGoalActions),
            notes = json.optJSONArray("notes")?.let(::decodeNotes),
            noteLines = json.optJSONArray("noteLines")?.let(::decodeNoteLines),
            clients = json.optJSONArray("clients")?.let(::decodeClients),
            debtAdvances = json.optJSONArray("debtAdvances")?.let(::decodeDebtAdvances),
        )

    /** Accepts legacy Android v1 snapshots (Long ids, transfers/expenses). */
    private fun decodeLegacyV1(json: JSONObject): FinanceSnapshot {
        val transfers = json.optJSONArray("transfers") ?: JSONArray()
        val expenses = json.optJSONArray("expenses") ?: JSONArray()
        val transactions = buildList {
            for (index in 0 until transfers.length()) {
                add(decodeLegacyTransfer(transfers.getJSONObject(index)))
            }
            for (index in 0 until expenses.length()) {
                add(decodeLegacyExpense(expenses.getJSONObject(index)))
            }
        }
        return FinanceSnapshot(
            schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
            deviceId = json.getString("deviceId"),
            updatedAt = json.getLong("updatedAt"),
            goals = decodeLegacyGoals(json.getJSONArray("goals")),
            debts = decodeLegacyDebts(json.getJSONArray("debts")),
            debtPayments = decodeLegacyDebtPayments(json.getJSONArray("debtPayments")),
            transactions = transactions,
            voiceSettings = json.optJSONObject("voiceSettings")?.let(::decodeVoiceSettings),
        )
    }

    private fun decodeLegacyGoals(array: JSONArray): List<SnapshotGoal> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotGoal(
                        id = item.getLong("id").toString(),
                        name = item.getString("name"),
                        targetAmountCents = item.getLong("targetAmountCents"),
                        currency = item.getString("currency"),
                        dateDeadline = item.optLong("deadlineMillis").takeIf { item.has("deadlineMillis") }
                            ?.let(SyncDates::formatDate),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                    ),
                )
            }
        }

    private fun decodeLegacyDebts(array: JSONArray): List<SnapshotDebt> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotDebt(
                        id = item.getLong("id").toString(),
                        name = item.getString("name"),
                        targetAmountCents = item.getLong("targetAmountCents"),
                        currentAmountCents = item.getLong("currentAmountCents"),
                        currency = item.getString("currency"),
                        status = item.getString("status"),
                        dateGiven = SyncDates.formatDate(item.getLong("dateGiven")),
                        dateDue = SyncDates.formatDate(item.getLong("dateDue")),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                    ),
                )
            }
        }

    private fun decodeLegacyDebtPayments(array: JSONArray): List<SnapshotDebtPayment> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotDebtPayment(
                        id = item.getLong("id").toString(),
                        debtorId = item.getLong("debtorId").toString(),
                        amountCents = item.getLong("amountCents"),
                        paidAt = SyncDates.formatDateTime(item.getLong("paidAt")),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                    ),
                )
            }
        }

    private fun decodeLegacyTransfer(item: JSONObject): SnapshotTransaction {
        val goalId = item.optLong("goalId").takeIf { item.has("goalId") }?.toString()
        return SnapshotTransaction(
            id = item.getLong("id").toString(),
            kind = if (goalId == null) KIND_DEPOSIT else KIND_GOAL_TRANSFER,
            amountCents = MoneyMath.toCents(item.getDouble("amount")),
            goalId = goalId,
            createdAt = SyncDates.formatDateTime(item.getLong("date")),
            note = null,
            category = null,
            syncUpdatedAt = item.getLong("syncUpdatedAt"),
            deleted = item.optBoolean("deleted", false),
        )
    }

    private fun decodeLegacyExpense(item: JSONObject): SnapshotTransaction =
        SnapshotTransaction(
            id = item.getLong("id").toString(),
            kind = KIND_EXPENSE,
            amountCents = MoneyMath.toCents(item.getDouble("amount")),
            goalId = null,
            createdAt = SyncDates.formatDateTime(item.getLong("date")),
            note = item.optString("note", "").ifBlank { null },
            category = item.getString("category"),
            syncUpdatedAt = item.getLong("syncUpdatedAt"),
            deleted = item.optBoolean("deleted", false),
        )

    private fun encodeGoals(goals: List<SnapshotGoal>): JSONArray =
        JSONArray().apply { goals.forEach { put(encodeGoalRecord(it)) } }

    private fun encodeDebts(debts: List<SnapshotDebt>): JSONArray =
        JSONArray().apply { debts.forEach { put(encodeDebtRecord(it)) } }

    private fun encodeDebtPayments(payments: List<SnapshotDebtPayment>): JSONArray =
        JSONArray().apply { payments.forEach { put(encodeDebtPaymentRecord(it)) } }

    private fun encodeTransactions(transactions: List<SnapshotTransaction>): JSONArray =
        JSONArray().apply { transactions.forEach { put(encodeTransactionRecord(it)) } }

    private fun encodeGoalRecord(goal: SnapshotGoal): JSONObject =
        JSONObject().apply {
            put("id", goal.id)
            put("name", goal.name)
            put("targetAmountCents", goal.targetAmountCents)
            put("currency", goal.currency)
            goal.dateDeadline?.let { put("dateDeadline", it) }
            goal.goalKind?.let { put("goalKind", it) }
            put("syncUpdatedAt", goal.syncUpdatedAt)
            put("deleted", goal.deleted)
            put("mode", goal.mode)
        }

    private fun encodeDebtRecord(debt: SnapshotDebt): JSONObject =
        JSONObject().apply {
            put("id", debt.id)
            put("name", debt.name)
            put("targetAmountCents", debt.targetAmountCents)
            put("currentAmountCents", debt.currentAmountCents)
            put("currency", debt.currency)
            put("status", debt.status)
            put("dateGiven", debt.dateGiven)
            debt.dateDue?.let { put("dateDue", it) }
            put("debtType", debt.debtType)
            put("affectsBalance", debt.affectsBalance)
            put("note", debt.note)
            put("syncUpdatedAt", debt.syncUpdatedAt)
            put("deleted", debt.deleted)
            put("mode", debt.mode)
            debt.clientSyncId?.let { put("clientSyncId", it) }
        }

    private fun encodeDebtAdvances(advances: List<SnapshotDebtAdvance>): JSONArray =
        JSONArray().apply { advances.forEach { put(encodeDebtAdvanceRecord(it)) } }

    private fun encodeDebtAdvanceRecord(advance: SnapshotDebtAdvance): JSONObject =
        JSONObject().apply {
            put("id", advance.id)
            put("debtorId", advance.debtorId)
            put("amountCents", advance.amountCents)
            put("givenAt", advance.givenAt)
            put("note", advance.note)
            advance.currency?.let { put("currency", it) }
            put("syncUpdatedAt", advance.syncUpdatedAt)
            put("deleted", advance.deleted)
            put("mode", advance.mode)
        }

    private fun encodeDebtPaymentRecord(payment: SnapshotDebtPayment): JSONObject =
        JSONObject().apply {
            put("id", payment.id)
            put("debtorId", payment.debtorId)
            put("amountCents", payment.amountCents)
            put("paidAt", payment.paidAt)
            put("affectsBalance", payment.affectsBalance)
            payment.currency?.let { put("currency", it) }
            put("syncUpdatedAt", payment.syncUpdatedAt)
            put("deleted", payment.deleted)
            put("mode", payment.mode)
        }

    private fun encodeTransactionRecord(transaction: SnapshotTransaction): JSONObject =
        JSONObject().apply {
            put("id", transaction.id)
            put("kind", transaction.kind)
            put("amountCents", transaction.amountCents)
            transaction.goalId?.let { put("goalId", it) }
            transaction.debtId?.let { put("debtId", it) }
            transaction.debtPaymentId?.let { put("debtPaymentId", it) }
            put("createdAt", transaction.createdAt)
            transaction.note?.let { put("note", it) }
            transaction.category?.let { put("category", it) }
            put("syncUpdatedAt", transaction.syncUpdatedAt)
            put("deleted", transaction.deleted)
            put("mode", transaction.mode)
        }

    private fun encodeVoiceSettings(settings: SnapshotVoiceSettings): String =
        encodeVoiceSettingsObject(settings).toString()

    private fun encodeVoiceSettingsObject(settings: SnapshotVoiceSettings): JSONObject =
        JSONObject().apply {
            put("enabled", settings.enabled)
            put("reportHour", settings.reportHour)
            put("reportMinute", settings.reportMinute)
            put("language", settings.language)
            put("gender", settings.gender)
            put("forceInSilentMode", settings.forceInSilentMode)
            settings.lastVoiceReportDate?.let { put("lastVoiceReportDate", it) }
            settings.lastAppOpenDate?.let { put("lastAppOpenDate", it) }
            put("syncUpdatedAt", settings.syncUpdatedAt)
        }

    private fun decodeGoals(array: JSONArray): List<SnapshotGoal> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotGoal(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        targetAmountCents = item.getLong("targetAmountCents"),
                        currency = item.getString("currency"),
                        dateDeadline = item.optString("dateDeadline").takeIf { item.has("dateDeadline") },
                        goalKind = item.optString("goalKind").takeIf { item.has("goalKind") && it.isNotBlank() },
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                        mode = item.optMode(),
                    ),
                )
            }
        }

    private fun decodeDebts(array: JSONArray): List<SnapshotDebt> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotDebt(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        targetAmountCents = item.getLong("targetAmountCents"),
                        currentAmountCents = item.getLong("currentAmountCents"),
                        currency = item.getString("currency"),
                        status = item.getString("status"),
                        dateGiven = item.getString("dateGiven"),
                        dateDue = item.optString("dateDue").takeIf { item.has("dateDue") },
                        debtType = item.optString("debtType", "CREDITOR"),
                        affectsBalance = item.optBoolean("affectsBalance", false),
                        note = item.optString("note", ""),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                        mode = item.optMode(),
                        clientSyncId = item.optString("clientSyncId").takeIf { item.has("clientSyncId") && it.isNotBlank() },
                    ),
                )
            }
        }

    private fun decodeDebtAdvances(array: JSONArray): List<SnapshotDebtAdvance> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotDebtAdvance(
                        id = item.getString("id"),
                        debtorId = item.getString("debtorId"),
                        amountCents = item.getLong("amountCents"),
                        givenAt = item.getString("givenAt"),
                        note = item.optString("note", ""),
                        currency = item.optString("currency").takeIf { item.has("currency") && it.isNotBlank() },
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                        mode = item.optMode(),
                    ),
                )
            }
        }

    private fun decodeDebtPayments(array: JSONArray): List<SnapshotDebtPayment> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotDebtPayment(
                        id = item.getString("id"),
                        debtorId = item.getString("debtorId"),
                        amountCents = item.getLong("amountCents"),
                        paidAt = item.getString("paidAt"),
                        affectsBalance = item.optBoolean("affectsBalance", true),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                        currency = item.optString("currency").takeIf { item.has("currency") && it.isNotBlank() },
                        mode = item.optMode(),
                    ),
                )
            }
        }

    private fun decodeTransactions(array: JSONArray): List<SnapshotTransaction> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotTransaction(
                        id = item.getString("id"),
                        kind = item.getString("kind"),
                        amountCents = item.getLong("amountCents"),
                        goalId = item.optString("goalId").takeIf { item.has("goalId") },
                        debtId = item.optString("debtId").takeIf { item.has("debtId") },
                        debtPaymentId = item.optString("debtPaymentId").takeIf { item.has("debtPaymentId") },
                        createdAt = item.getString("createdAt"),
                        note = item.optString("note").takeIf { item.has("note") },
                        category = item.optString("category").takeIf { item.has("category") },
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                        mode = item.optMode(),
                    ),
                )
            }
        }

    private fun decodeVoiceSettings(json: JSONObject): SnapshotVoiceSettings =
        SnapshotVoiceSettings(
            enabled = json.getBoolean("enabled"),
            reportHour = json.getInt("reportHour"),
            reportMinute = json.getInt("reportMinute"),
            language = json.getString("language"),
            gender = json.getString("gender"),
            forceInSilentMode = json.getBoolean("forceInSilentMode"),
            lastVoiceReportDate = json.optString("lastVoiceReportDate").takeIf { json.has("lastVoiceReportDate") },
            lastAppOpenDate = json.optString("lastAppOpenDate").takeIf { json.has("lastAppOpenDate") },
            syncUpdatedAt = json.getLong("syncUpdatedAt"),
        )

    fun toSnapshotGoalAction(entity: GoalActionEntity, goalSyncId: String): SnapshotGoalAction =
        SnapshotGoalAction(
            id = entity.syncId,
            goalId = goalSyncId,
            title = entity.title,
            completed = entity.completed,
            sortOrder = entity.sortOrder,
            syncUpdatedAt = entity.updatedAt,
        )

    fun toSnapshotNote(entity: FinanceNoteEntity): SnapshotNote =
        SnapshotNote(
            id = entity.syncId,
            title = entity.title,
            kind = entity.kind,
            body = entity.body,
            mark = entity.mark,
            closed = entity.closed,
            createdAt = entity.createdAt,
            syncUpdatedAt = entity.updatedAt,
            mode = entity.mode,
        )

    fun toSnapshotClient(entity: ClientEntity): SnapshotClient =
        SnapshotClient(
            id = entity.syncId,
            name = entity.name,
            phone = entity.phone,
            company = entity.company,
            email = entity.email,
            lastMeetingAt = entity.lastMeetingAt,
            note = entity.note,
            createdAt = entity.createdAt,
            mode = entity.mode,
            syncUpdatedAt = entity.syncUpdatedAt,
            deleted = entity.deleted,
        )

    fun toSnapshotNoteLine(entity: FinanceNoteLineEntity, noteSyncId: String): SnapshotNoteLine =
        SnapshotNoteLine(
            id = entity.syncId,
            noteId = noteSyncId,
            label = entity.label,
            amountCents = entity.amountCents,
            sortOrder = entity.sortOrder,
            syncUpdatedAt = entity.updatedAt,
        )

    private fun encodeGoalActions(actions: List<SnapshotGoalAction>): JSONArray =
        JSONArray().apply { actions.forEach { put(encodeGoalActionRecord(it)) } }

    private fun encodeNotes(notes: List<SnapshotNote>): JSONArray =
        JSONArray().apply { notes.forEach { put(encodeNoteRecord(it)) } }

    private fun encodeNoteLines(lines: List<SnapshotNoteLine>): JSONArray =
        JSONArray().apply { lines.forEach { put(encodeNoteLineRecord(it)) } }

    private fun encodeGoalActionRecord(action: SnapshotGoalAction): JSONObject =
        JSONObject().apply {
            put("id", action.id)
            put("goalId", action.goalId)
            put("title", action.title)
            put("completed", action.completed)
            put("sortOrder", action.sortOrder)
            put("syncUpdatedAt", action.syncUpdatedAt)
            put("deleted", action.deleted)
        }

    private fun encodeNoteRecord(note: SnapshotNote): JSONObject =
        JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("kind", note.kind)
            put("body", note.body)
            put("mark", note.mark)
            put("closed", note.closed)
            put("createdAt", note.createdAt)
            put("syncUpdatedAt", note.syncUpdatedAt)
            put("deleted", note.deleted)
            put("mode", note.mode)
        }

    private fun encodeNoteLineRecord(line: SnapshotNoteLine): JSONObject =
        JSONObject().apply {
            put("id", line.id)
            put("noteId", line.noteId)
            put("label", line.label)
            put("amountCents", line.amountCents)
            put("sortOrder", line.sortOrder)
            put("syncUpdatedAt", line.syncUpdatedAt)
            put("deleted", line.deleted)
        }

    private fun decodeGoalActions(array: JSONArray): List<SnapshotGoalAction> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotGoalAction(
                        id = item.getString("id"),
                        goalId = item.getString("goalId"),
                        title = item.getString("title"),
                        completed = item.optBoolean("completed", false),
                        sortOrder = item.optInt("sortOrder", 0),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                    ),
                )
            }
        }

    private fun decodeNotes(array: JSONArray): List<SnapshotNote> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotNote(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        kind = item.optString("kind", "TEXT"),
                        body = item.optString("body", ""),
                        mark = item.optString("mark", "NONE"),
                        closed = item.optBoolean("closed", false),
                        createdAt = item.optLong("createdAt", item.getLong("syncUpdatedAt")),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                        mode = item.optMode(),
                    ),
                )
            }
        }

    private fun decodeNoteLines(array: JSONArray): List<SnapshotNoteLine> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotNoteLine(
                        id = item.getString("id"),
                        noteId = item.getString("noteId"),
                        label = item.getString("label"),
                        amountCents = item.getLong("amountCents"),
                        sortOrder = item.optInt("sortOrder", 0),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                    ),
                )
            }
        }

    private fun encodeClients(clients: List<SnapshotClient>): JSONArray =
        JSONArray().apply { clients.forEach { put(encodeClientRecord(it)) } }

    fun encodeClient(record: SnapshotClient): String = encodeClientRecord(record).toString()

    fun encodeClient(entity: ClientEntity, deleted: Boolean = false): String =
        encodeClientRecord(toSnapshotClient(entity).copy(deleted = deleted)).toString()

    fun decodeClient(raw: String): SnapshotClient =
        decodeClients(JSONArray().put(JSONObject(raw))).single()

    private fun encodeClientRecord(client: SnapshotClient): JSONObject =
        JSONObject().apply {
            put("id", client.id)
            put("name", client.name)
            put("phone", client.phone)
            put("company", client.company)
            client.email?.let { put("email", it) }
            client.lastMeetingAt?.let { put("lastMeetingAt", it) }
            put("note", client.note)
            put("createdAt", client.createdAt)
            put("mode", client.mode)
            put("syncUpdatedAt", client.syncUpdatedAt)
            put("deleted", client.deleted)
        }

    private fun decodeClients(array: JSONArray): List<SnapshotClient> =
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SnapshotClient(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        phone = item.optString("phone", ""),
                        company = item.optString("company", ""),
                        email = item.optString("email").takeIf { item.has("email") && it.isNotBlank() },
                        lastMeetingAt = if (item.has("lastMeetingAt") && !item.isNull("lastMeetingAt")) {
                            item.getLong("lastMeetingAt")
                        } else {
                            null
                        },
                        note = item.optString("note", ""),
                        createdAt = item.optLong("createdAt", item.getLong("syncUpdatedAt")),
                        mode = item.optMode(default = MODE_PROFESSIONAL),
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
                    ),
                )
            }
        }

    private fun JSONObject.optMode(default: String = MODE_PERSONAL): String {
        if (!has("mode")) return default
        return if (optString("mode") == MODE_PROFESSIONAL) MODE_PROFESSIONAL else MODE_PERSONAL
    }

    private const val MODE_PERSONAL = "PERSONAL"
    private const val MODE_PROFESSIONAL = "PROFESSIONAL"
}

package com.example.money.data.sync

import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotDebt
import com.example.money.domain.sync.SnapshotDebtPayment
import com.example.money.domain.sync.SnapshotGoal
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
    }

    fun encodeGoal(entity: SavingsGoalEntity, deleted: Boolean = false): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("name", entity.name)
            put("targetAmountCents", entity.targetAmountCents)
            put("currency", entity.currency)
            entity.deadlineMillis?.let { put("dateDeadline", SyncDates.formatDate(it)) }
            put("syncUpdatedAt", entity.updatedAt)
            put("deleted", deleted)
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
        }.toString()

    fun encodeDebtPayment(entity: DebtPaymentEntity, debtorSyncId: String, deleted: Boolean = false): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("debtorId", debtorSyncId)
            put("amountCents", entity.amountCents)
            put("paidAt", SyncDates.formatDateTime(entity.paidAt))
            put("affectsBalance", entity.affectsBalance)
            put("syncUpdatedAt", entity.paidAt)
            put("deleted", deleted)
        }.toString()

    fun encodeTransactionFromTransfer(
        entity: TransferEntity,
        debtSyncId: String? = null,
        debtPaymentSyncId: String? = null,
        deleted: Boolean = false,
    ): String {
        val kind = if (entity.goalId == null && entity.goalSyncId == null) KIND_DEPOSIT else KIND_GOAL_TRANSFER
        return JSONObject().apply {
            put("id", entity.syncId)
            put("kind", kind)
            put("amountCents", MoneyMath.toCents(entity.amount))
            entity.goalSyncId?.let { put("goalId", it) }
            debtSyncId?.let { put("debtId", it) }
            debtPaymentSyncId?.let { put("debtPaymentId", it) }
            put("createdAt", SyncDates.formatDateTime(entity.date))
            entity.note.takeIf { it.isNotBlank() }?.let { put("note", it) }
            put("syncUpdatedAt", entity.date)
            put("deleted", deleted)
        }.toString()
    }

    fun encodeTransactionFromExpense(
        entity: ExpenseEntity,
        debtSyncId: String? = null,
        debtPaymentSyncId: String? = null,
        deleted: Boolean = false,
    ): String =
        JSONObject().apply {
            put("id", entity.syncId)
            put("kind", KIND_EXPENSE)
            put("amountCents", MoneyMath.toCents(entity.amount))
            put("createdAt", SyncDates.formatDateTime(entity.date))
            put("category", entity.category)
            put("note", entity.note)
            debtSyncId?.let { put("debtId", it) }
            debtPaymentSyncId?.let { put("debtPaymentId", it) }
            put("syncUpdatedAt", entity.date)
            put("deleted", deleted)
        }.toString()

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
            syncUpdatedAt = entity.updatedAt,
        )

    fun toSnapshotDebt(entity: DebtorEntity, syncUpdatedAt: Long): SnapshotDebt =
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
        )

    fun toSnapshotDebtPayment(entity: DebtPaymentEntity, debtorSyncId: String): SnapshotDebtPayment =
        SnapshotDebtPayment(
            id = entity.syncId,
            debtorId = debtorSyncId,
            amountCents = entity.amountCents,
            paidAt = SyncDates.formatDateTime(entity.paidAt),
            affectsBalance = entity.affectsBalance,
            syncUpdatedAt = entity.paidAt,
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
            amountCents = MoneyMath.toCents(entity.amount),
            goalId = entity.goalSyncId,
            debtId = debtSyncId,
            debtPaymentId = debtPaymentSyncId,
            createdAt = SyncDates.formatDateTime(entity.date),
            note = entity.note.ifBlank { null },
            category = null,
            syncUpdatedAt = entity.date,
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
            amountCents = MoneyMath.toCents(entity.amount),
            goalId = null,
            debtId = debtSyncId,
            debtPaymentId = debtPaymentSyncId,
            createdAt = SyncDates.formatDateTime(entity.date),
            note = entity.note.ifBlank { null },
            category = entity.category,
            syncUpdatedAt = entity.date,
        )

    private fun decodeV2(json: JSONObject): FinanceSnapshot =
        FinanceSnapshot(
            schemaVersion = json.getInt("schemaVersion"),
            deviceId = json.getString("deviceId"),
            updatedAt = json.getLong("updatedAt"),
            goals = decodeGoals(json.getJSONArray("goals")),
            debts = decodeDebts(json.getJSONArray("debts")),
            debtPayments = decodeDebtPayments(json.getJSONArray("debtPayments")),
            transactions = decodeTransactions(json.getJSONArray("transactions")),
            voiceSettings = json.optJSONObject("voiceSettings")?.let(::decodeVoiceSettings),
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
            put("syncUpdatedAt", goal.syncUpdatedAt)
            put("deleted", goal.deleted)
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
        }

    private fun encodeDebtPaymentRecord(payment: SnapshotDebtPayment): JSONObject =
        JSONObject().apply {
            put("id", payment.id)
            put("debtorId", payment.debtorId)
            put("amountCents", payment.amountCents)
            put("paidAt", payment.paidAt)
            put("affectsBalance", payment.affectsBalance)
            put("syncUpdatedAt", payment.syncUpdatedAt)
            put("deleted", payment.deleted)
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
                        syncUpdatedAt = item.getLong("syncUpdatedAt"),
                        deleted = item.optBoolean("deleted", false),
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
}

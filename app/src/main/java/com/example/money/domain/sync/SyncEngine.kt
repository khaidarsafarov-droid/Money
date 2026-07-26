package com.example.money.domain.sync

/**
 * Last-write-wins merge for offline P2P sync.
 * Tie-breaker: newer [syncUpdatedAt] wins; equal timestamps prefer the remote side.
 */
object SyncEngine {

    /** Deterministic merge — both peers compute the same result. */
    fun mergeSymmetric(first: FinanceSnapshot, second: FinanceSnapshot): FinanceSnapshot {
        val (local, remote) = if (first.deviceId <= second.deviceId) {
            first to second
        } else {
            second to first
        }
        return merge(local, remote)
    }

    fun merge(local: FinanceSnapshot, remote: FinanceSnapshot): FinanceSnapshot {
        require(local.schemaVersion == remote.schemaVersion) {
            "Schema mismatch: local=${local.schemaVersion}, remote=${remote.schemaVersion}"
        }

        return FinanceSnapshot(
            schemaVersion = local.schemaVersion,
            deviceId = remote.deviceId,
            updatedAt = maxOf(local.updatedAt, remote.updatedAt),
            goals = mergeRecords(local.goals, remote.goals) { it.id },
            debts = mergeRecords(local.debts, remote.debts) { it.id },
            debtPayments = mergeRecords(local.debtPayments, remote.debtPayments) { it.id },
            transactions = mergeRecords(local.transactions, remote.transactions) { it.id },
            voiceSettings = mergeVoiceSettings(local.voiceSettings, remote.voiceSettings),
        )
    }

    fun mergeVoiceSettings(
        local: SnapshotVoiceSettings?,
        remote: SnapshotVoiceSettings?,
    ): SnapshotVoiceSettings? {
        if (local == null) return remote
        if (remote == null) return local
        return if (remote.syncUpdatedAt >= local.syncUpdatedAt) remote else local
    }

    fun <T> mergeRecords(
        local: List<T>,
        remote: List<T>,
        idSelector: (T) -> String,
    ): List<T> where T : LwwRecord {
        val merged = linkedMapOf<String, T>()
        val localById = local.associateBy(idSelector)
        val remoteById = remote.associateBy(idSelector)
        val allIds = localById.keys + remoteById.keys

        for (id in allIds) {
            val localRecord = localById[id]
            val remoteRecord = remoteById[id]

            when {
                localRecord != null && remoteRecord != null -> {
                    val winner = pickLww(localRecord, remoteRecord)
                    merged[id] = winner
                }
                else -> {
                    val sole = localRecord ?: remoteRecord
                    if (sole != null) {
                        merged[id] = sole
                    }
                }
            }
        }

        return merged.values.sortedBy(idSelector)
    }

    fun <T> pickLww(local: T, remote: T): T where T : LwwRecord {
        return when {
            remote.syncUpdatedAt > local.syncUpdatedAt -> remote
            local.syncUpdatedAt > remote.syncUpdatedAt -> local
            else -> remote
        }
    }
}

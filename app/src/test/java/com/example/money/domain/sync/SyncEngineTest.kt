package com.example.money.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {

    private val deviceA = "device-a"
    private val deviceB = "device-b"

    @Test
    fun merge_picksNewerGoalBySyncUpdatedAt() {
        val local = snapshot(
            goals = listOf(
                goal(id = "goal-1", name = "Local", syncUpdatedAt = 100L),
            ),
        )
        val remote = snapshot(
            deviceId = deviceB,
            goals = listOf(
                goal(id = "goal-1", name = "Remote", syncUpdatedAt = 200L),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertEquals("Remote", merged.goals.single().name)
    }

    @Test
    fun merge_prefersRemoteOnTimestampTie() {
        val local = snapshot(
            goals = listOf(
                goal(id = "goal-1", name = "Local", syncUpdatedAt = 100L),
            ),
        )
        val remote = snapshot(
            deviceId = deviceB,
            goals = listOf(
                goal(id = "goal-1", name = "Remote", syncUpdatedAt = 100L),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertEquals("Remote", merged.goals.single().name)
    }

    @Test
    fun merge_appliesTombstoneWhenNewerThanLocalRecord() {
        val local = snapshot(
            goals = listOf(
                goal(id = "goal-1", name = "Active", syncUpdatedAt = 100L),
            ),
        )
        val remote = snapshot(
            deviceId = deviceB,
            goals = listOf(
                goal(id = "goal-1", name = "Deleted", syncUpdatedAt = 200L, deleted = true),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertTrue(merged.goals.single().deleted)
        assertEquals(200L, merged.goals.single().syncUpdatedAt)
    }

    @Test
    fun merge_keepsLocalWhenTombstoneIsOlder() {
        val local = snapshot(
            goals = listOf(
                goal(id = "goal-1", name = "Active", syncUpdatedAt = 300L),
            ),
        )
        val remote = snapshot(
            deviceId = deviceB,
            goals = listOf(
                goal(id = "goal-1", name = "Deleted", syncUpdatedAt = 100L, deleted = true),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertEquals("Active", merged.goals.single().name)
    }

    @Test
    fun merge_appliesLocalTombstoneWhenNewerThanRemoteActiveRecord() {
        val local = snapshot(
            goals = listOf(
                goal(id = "goal-1", name = "Deleted", syncUpdatedAt = 300L, deleted = true),
            ),
        )
        val remote = snapshot(
            deviceId = deviceB,
            goals = listOf(
                goal(id = "goal-1", name = "Active", syncUpdatedAt = 100L),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertTrue(merged.goals.single().deleted)
        assertEquals(300L, merged.goals.single().syncUpdatedAt)
    }

    @Test
    fun merge_keepsRemoteOnlyRecordWhenLocalHasNoTombstone() {
        val local = snapshot()
        val remote = snapshot(
            deviceId = deviceB,
            goals = listOf(
                goal(id = "goal-1", name = "Remote only", syncUpdatedAt = 100L),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertEquals("Remote only", merged.goals.single().name)
    }

    @Test
    fun merge_threePeersDoNotResurrectDeletedGoalOrDebt() {
        val deletedOnA = snapshot(
            deviceId = "device-a",
            goals = listOf(goal("goal-1", "Deleted goal", 300L, deleted = true)),
            debts = listOf(debt("debt-1", "Deleted debt", 300L, deleted = true)),
        )
        val staleOnB = snapshot(
            deviceId = "device-b",
            goals = listOf(goal("goal-1", "Stale goal B", 100L)),
            debts = listOf(debt("debt-1", "Stale debt B", 100L)),
        )
        val staleOnC = snapshot(
            deviceId = "device-c",
            goals = listOf(goal("goal-1", "Stale goal C", 100L)),
            debts = listOf(debt("debt-1", "Stale debt C", 100L)),
        )

        val afterAB = SyncEngine.mergeSymmetric(deletedOnA, staleOnB)
        val afterBC = SyncEngine.mergeSymmetric(afterAB, staleOnC)

        assertTrue(afterAB.goals.single().deleted)
        assertTrue(afterAB.debts.single().deleted)
        assertTrue(afterBC.goals.single().deleted)
        assertTrue(afterBC.debts.single().deleted)
    }

    @Test
    fun merge_unionsDistinctTransactionRecords() {
        val local = snapshot(
            transactions = listOf(
                transaction(id = "tx-1", amountCents = 1_000L, syncUpdatedAt = 100L),
            ),
        )
        val remote = snapshot(
            deviceId = deviceB,
            transactions = listOf(
                transaction(id = "tx-2", amountCents = 2_500L, syncUpdatedAt = 150L),
            ),
        )

        val merged = SyncEngine.merge(local, remote)

        assertEquals(listOf("tx-1", "tx-2"), merged.transactions.map { it.id })
    }

    @Test
    fun mergeVoiceSettings_picksNewerSide() {
        val older = SnapshotVoiceSettings(
            enabled = false,
            reportHour = 8,
            reportMinute = 0,
            language = "EN",
            gender = "FEMALE",
            forceInSilentMode = false,
            lastVoiceReportDate = null,
            lastAppOpenDate = null,
            syncUpdatedAt = 100L,
        )
        val newer = older.copy(enabled = true, syncUpdatedAt = 200L)

        assertEquals(newer, SyncEngine.mergeVoiceSettings(older, newer))
        assertEquals(newer, SyncEngine.mergeVoiceSettings(newer, older))
    }

    @Test
    fun mergeSymmetric_producesSameResultRegardlessOfArgumentOrder() {
        val deviceA = snapshot(
            deviceId = "aaa-device",
            goals = listOf(goal("goal-1", "From A", 100L)),
        )
        val deviceB = snapshot(
            deviceId = "bbb-device",
            goals = listOf(goal("goal-1", "From B", 200L)),
        )

        val mergedAB = SyncEngine.mergeSymmetric(deviceA, deviceB)
        val mergedBA = SyncEngine.mergeSymmetric(deviceB, deviceA)

        assertEquals(mergedAB, mergedBA)
        assertEquals("From B", mergedAB.goals.single().name)
    }

    @Test
    fun mergeVoiceSettings_returnsNullWhenBothMissing() {
        assertNull(SyncEngine.mergeVoiceSettings(null, null))
    }

    private fun snapshot(
        deviceId: String = deviceA,
        goals: List<SnapshotGoal> = emptyList(),
        debts: List<SnapshotDebt> = emptyList(),
        transactions: List<SnapshotTransaction> = emptyList(),
    ): FinanceSnapshot =
        FinanceSnapshot(
            schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
            deviceId = deviceId,
            updatedAt = 0L,
            goals = goals,
            debts = debts,
            debtPayments = emptyList(),
            transactions = transactions,
            voiceSettings = null,
        )

    private fun goal(
        id: String,
        name: String,
        syncUpdatedAt: Long,
        deleted: Boolean = false,
    ): SnapshotGoal =
        SnapshotGoal(
            id = id,
            name = name,
            targetAmountCents = 1_000L,
            currency = "USD",
            dateDeadline = null,
            syncUpdatedAt = syncUpdatedAt,
            deleted = deleted,
        )

    private fun debt(
        id: String,
        name: String,
        syncUpdatedAt: Long,
        deleted: Boolean = false,
    ): SnapshotDebt =
        SnapshotDebt(
            id = id,
            name = name,
            targetAmountCents = 1_000L,
            currentAmountCents = 0L,
            currency = "USD",
            status = "ACTIVE",
            dateGiven = "2026-01-01",
            dateDue = null,
            syncUpdatedAt = syncUpdatedAt,
            deleted = deleted,
        )

    private fun transaction(
        id: String,
        amountCents: Long,
        syncUpdatedAt: Long,
    ): SnapshotTransaction =
        SnapshotTransaction(
            id = id,
            kind = "DEPOSIT",
            amountCents = amountCents,
            goalId = null,
            createdAt = "2026-01-01T00:00:00Z",
            note = null,
            category = null,
            syncUpdatedAt = syncUpdatedAt,
        )
}

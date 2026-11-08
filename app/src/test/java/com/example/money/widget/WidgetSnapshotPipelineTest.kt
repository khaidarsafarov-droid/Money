package com.example.money.widget

import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.PrivacySettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DataStore отдаёт все производные Flow после любой записи. Снимок с новым
 * updatedAt не должен из-за этого записываться снова, а смена целей, долгов,
 * валюты и приватности обновляет виджет один раз.
 */
class WidgetSnapshotPipelineTest {

    private val clockStart = 1_700_000_000_000L
    private var now = clockStart

    @Test
    fun ownWriteAndForeignPreferencesDoNotLoop_contentChangesUpdateTheWidgetOnce() = runBlocking {
        now = clockStart
        val store = FanOutPreferences()
        store.emitInitial()
        val goals = MutableStateFlow(listOf(moneyGoal(current = 4_000)))
        val debts = MutableStateFlow<List<Debtor>>(emptyList())
        val writer = GoalWidgetSnapshotWriter(
            goals = goals,
            debts = debts,
            pendingTransactionCount = MutableStateFlow(0),
            currencyRegion = store.currency,
            privacySettings = store.privacy,
            nowMillis = { now.also { now += 60_000L } },
            readStored = { store.stored.value },
            writeStored = { snapshot -> store.write(snapshot) },
        )
        val seen = mutableListOf<WidgetGoalSnapshot>()
        val refreshes = mutableListOf<WidgetGoalSnapshot>()
        val writerJob = launch {
            writer.observeSnapshotChanges().collect { snapshot ->
                writer.persist(snapshot)
                seen += snapshot
                check(seen.size <= 8) { "Snapshot pipeline repeated ${seen.size} times" }
            }
        }
        val refreshJob = launch {
            store.stored.distinctWidgetContent().collect { refreshes += it }
        }

        awaitCount(seen, 1)
        val first = seen.single()
        assertEquals(40, first.progressPercent)
        assertEquals("Отпуск", first.featuredGoalName)
        assertEquals(4_000L, first.featuredGoalSavedCents)
        assertEquals(0L, first.owedToMeCents)
        assertEquals(CurrencyRegion.US, first.currencyRegion)
        assertFalse(first.hideAmounts)
        assertEquals(1, store.writes)
        assertTrue(store.stored.value.contentEquals(first))
        store.rewriteTimestampOnly()
        store.touchForeignKey()
        yieldUntilIdle()
        assertEquals(1, seen.size)
        assertEquals(1, store.writes)
        val refreshesAfterEcho = refreshes.count { it.contentEquals(first) }
        assertEquals(1, refreshesAfterEcho)

        goals.value = listOf(moneyGoal(current = 5_000))
        awaitCount(seen, 2)
        val afterGoal = seen[1]
        assertEquals(50, afterGoal.progressPercent)
        assertEquals(5_000L, afterGoal.featuredGoalSavedCents)
        yieldUntilIdle()
        assertEquals(2, seen.size)
        assertEquals(2, store.writes)

        debts.value = listOf(owedDebt())
        awaitCount(seen, 3)
        assertEquals(8_000L, seen[2].owedToMeCents)
        assertEquals(5_000L, seen[2].featuredGoalSavedCents)
        yieldUntilIdle()
        assertEquals(3, store.writes)

        store.currency.emit(CurrencyRegion.fromCode("EUR"))
        awaitCount(seen, 4)
        assertEquals(CurrencyRegion.fromCode("EUR"), seen[3].currencyRegion)
        assertEquals(8_000L, seen[3].owedToMeCents)
        yieldUntilIdle()
        assertEquals(4, store.writes)

        store.privacy.emit(PrivacySettings(hideAmounts = true))
        awaitCount(seen, 5)
        val afterPrivacy = seen[4]
        assertTrue(afterPrivacy.hideAmounts)
        assertEquals(CurrencyRegion.fromCode("EUR"), afterPrivacy.currencyRegion)
        assertEquals(8_000L, afterPrivacy.owedToMeCents)
        assertEquals(50, afterPrivacy.progressPercent)
        assertTrue(store.stored.value.contentEquals(afterPrivacy))
        assertEquals(5, store.writes)
        store.touchForeignKey()
        yieldUntilIdle()
        assertEquals(5, seen.size)
        assertEquals(5, store.writes)
        val contentRefreshes = refreshes.filterNot { it.contentEquals(WidgetGoalSnapshot.Empty) }
        assertEquals(
            listOf(first, afterGoal, seen[2], seen[3], afterPrivacy).map { it.copy(updatedAt = 0L) },
            contentRefreshes.map { it.copy(updatedAt = 0L) },
        )

        writerJob.cancel()
        refreshJob.cancel()
    }

    private suspend fun awaitCount(seen: List<*>, count: Int) {
        repeat(100) {
            if (seen.size >= count) return
            yield()
        }
        error("Expected $count snapshots, saw ${seen.size}")
    }

    private suspend fun yieldUntilIdle() {
        repeat(20) { yield() }
    }

    private fun moneyGoal(current: Long) = SavingsGoal(
        id = 1,
        name = "Отпуск",
        targetAmountCents = 10_000,
        currentAmountCents = current,
        updatedAt = clockStart,
        kind = GoalKind.MONEY,
    )

    private fun owedDebt() = Debtor(
        id = 3,
        name = "Owes me",
        targetAmountCents = 8_000,
        currentAmountCents = 0,
        debtType = DebtType.CREDITOR,
        dateGiven = clockStart - 86_400_000L,
        dateDue = clockStart + 86_400_000L,
    )

    /**
     * Повторяет поведение DataStore: любая запись снова рассылает валюту и приватность,
     * даже если эти значения не менялись.
     */
    private class FanOutPreferences {
        val currency = MutableSharedFlow<CurrencyRegion>(replay = 1, extraBufferCapacity = 8)
        val privacy = MutableSharedFlow<PrivacySettings>(replay = 1, extraBufferCapacity = 8)
        val stored = MutableStateFlow(WidgetGoalSnapshot.Empty)
        var writes = 0

        suspend fun emitInitial() {
            currency.emit(CurrencyRegion.US)
            privacy.emit(PrivacySettings())
        }

        suspend fun write(snapshot: WidgetGoalSnapshot) {
            writes += 1
            check(writes <= 8) { "Snapshot write repeated $writes times" }
            stored.value = snapshot
            fanOut()
        }

        suspend fun rewriteTimestampOnly() {
            stored.value = stored.value.copy(updatedAt = stored.value.updatedAt + 60_000L)
            fanOut()
        }

        suspend fun touchForeignKey() {
            fanOut()
        }

        private suspend fun fanOut() {
            currency.emit(currency.replayCache.single())
            privacy.emit(privacy.replayCache.single())
        }
    }
}

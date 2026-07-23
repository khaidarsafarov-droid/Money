package com.example.money.domain.util

import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.settings.AutopilotStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtAutopilotEngineTest {

    @Test
    fun pickTargetDebt_ignoresCreditorDebts() {
        val owedToMe = debtor(id = 1, type = DebtType.CREDITOR, remaining = 100)
        val iOwe = debtor(id = 2, type = DebtType.DEBTOR, remaining = 500)
        val picked = DebtAutopilotEngine.pickTargetDebt(
            debts = listOf(owedToMe, iOwe),
            strategy = AutopilotStrategy.SNOWBALL,
        )
        assertEquals(2L, picked?.id)
    }

    @Test
    fun pickTargetDebt_returnsNullWhenOnlyCreditors() {
        val owedToMe = debtor(id = 1, type = DebtType.CREDITOR, remaining = 100)
        assertNull(
            DebtAutopilotEngine.pickTargetDebt(
                debts = listOf(owedToMe),
                strategy = AutopilotStrategy.AVALANCHE,
            ),
        )
    }

    private fun debtor(id: Long, type: DebtType, remaining: Long): Debtor = Debtor(
        id = id,
        name = "D$id",
        targetAmountCents = remaining,
        currentAmountCents = 0,
        status = DebtStatus.ACTIVE,
        debtType = type,
        dateGiven = 1_700_000_000_000L,
        dateDue = 1_700_100_000_000L,
    )
}

class GoalChallengeEnginesTest {

    @Test
    fun closeDebtChallenge_completesWhenDebtPaid() {
        val debt = Debtor(
            id = 9,
            name = "Bank",
            targetAmountCents = 1000,
            currentAmountCents = 1000,
            status = DebtStatus.COMPLETED,
            debtType = DebtType.DEBTOR,
            dateGiven = 1L,
            dateDue = 2L,
        )
        val challenge = GoalChallengeEngines.createChallenge(
            templateId = GoalChallengeEngines.ChallengeTemplateId.CLOSE_DEBT_14D,
            debtId = 9,
            debtName = "Bank",
            now = 1_000L,
        )
        val synced = GoalChallengeEngines.syncPaymentChallengeProgress(
            challenges = listOf(challenge),
            debtors = listOf(debt),
            now = 1_000L,
        )
        assertTrue(synced.single().completed)
    }

    @Test
    fun closeDebtChallenge_removedAfterDeadline() {
        val debt = Debtor(
            id = 9,
            name = "Bank",
            targetAmountCents = 1000,
            currentAmountCents = 0,
            status = DebtStatus.ACTIVE,
            debtType = DebtType.DEBTOR,
            dateGiven = 1L,
            dateDue = 2L,
        )
        val started = 1_000L
        val challenge = GoalChallengeEngines.createChallenge(
            templateId = GoalChallengeEngines.ChallengeTemplateId.CLOSE_DEBT_14D,
            debtId = 9,
            debtName = "Bank",
            now = started,
        )
        val afterDeadline = started + 15L * 24L * 60L * 60L * 1000L
        val synced = GoalChallengeEngines.syncPaymentChallengeProgress(
            challenges = listOf(challenge),
            debtors = listOf(debt),
            now = afterDeadline,
        )
        assertTrue(synced.isEmpty())
    }
}

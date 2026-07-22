package com.example.money.domain.util

import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.settings.AutopilotStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalFinancialAdvisorTest {

    private val now = 1_700_000_000_000L

    @Test
    fun classifyIntent_mapsCommonPhrases() {
        assertEquals("which_debt_first", LocalFinancialAdvisor.classifyIntent("С чего начать?"))
        assertEquals("what_if_extra", LocalFinancialAdvisor.classifyIntent("что если +50"))
        assertEquals("motivation", LocalFinancialAdvisor.classifyIntent("дай мотивацию"))
        assertEquals("overdue", LocalFinancialAdvisor.classifyIntent("есть просрочки?"))
        assertEquals("goals", LocalFinancialAdvisor.classifyIntent("как мои цели"))
        assertEquals("budget", LocalFinancialAdvisor.classifyIntent("бюджет"))
        assertEquals("overview", LocalFinancialAdvisor.classifyIntent("привет"))
    }

    @Test
    fun overview_mentionsDebtTotals() {
        val debts = listOf(
            debtor(1, DebtType.CREDITOR, remaining = 10_000),
            debtor(2, DebtType.DEBTOR, remaining = 5_000),
        )
        val reply = LocalFinancialAdvisor.buildReply(
            LocalFinancialAdvisor.AdviceRequest(promptId = "overview"),
            LocalFinancialAdvisor.AdviceContext(debts = debts, now = now),
        )
        assertTrue(reply.contains("100") || reply.contains("$100"))
        assertTrue(reply.contains("50") || reply.contains("$50"))
    }

    @Test
    fun whichDebtFirst_picksSnowballWhenNoInterest() {
        val debts = listOf(
            debtor(1, DebtType.DEBTOR, remaining = 20_000),
            debtor(2, DebtType.DEBTOR, remaining = 5_000, name = "Small"),
        )
        val reply = LocalFinancialAdvisor.buildReply(
            LocalFinancialAdvisor.AdviceRequest(promptId = "which_debt_first"),
            LocalFinancialAdvisor.AdviceContext(
                debts = debts,
                now = now,
                payoffStrategy = AutopilotStrategy.SNOWBALL,
            ),
        )
        assertTrue(reply.contains("Small"))
    }

    private fun debtor(
        id: Long,
        type: DebtType,
        remaining: Long,
        name: String = "Person $id",
    ): Debtor = Debtor(
        id = id,
        name = name,
        targetAmountCents = remaining,
        currentAmountCents = 0,
        status = DebtStatus.ACTIVE,
        debtType = type,
        dateGiven = now - 86_400_000L,
        dateDue = now + 86_400_000L,
    )
}

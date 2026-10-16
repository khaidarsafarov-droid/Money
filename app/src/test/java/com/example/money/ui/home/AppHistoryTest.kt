package com.example.money.ui.home

import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import org.junit.Assert.assertEquals
import org.junit.Test

class AppHistoryTest {

    @Test
    fun buildAppHistory_ordersNewestFirstAndSkipsLinkedPayments() {
        val transfer = Transfer(
            id = 1,
            syncId = "t1",
            amountCents = 20_000L,
            date = 300,
            debtPaymentId = 7,
            goalId = 5,
        )
        val expense = Expense(
            id = 2,
            syncId = "e1",
            amountCents = 1_500L,
            category = "food",
            date = 200,
            debtPaymentId = 8,
        )
        val debtor = Debtor(
            id = 3,
            name = "Ann",
            targetAmountCents = 1_000,
            debtType = DebtType.DEBTOR,
            dateGiven = 1,
            dateDue = 2,
            paymentHistory = listOf(
                DebtPayment(id = 7, debtorId = 3, amountCents = 100, currency = "USD", paidAt = 300),
                DebtPayment(id = 8, debtorId = 3, amountCents = 80, currency = "USD", paidAt = 250),
                DebtPayment(id = 9, debtorId = 3, amountCents = 50, currency = "USD", paidAt = 100),
            ),
        )
        val goal = SavingsGoal(id = 5, name = "Trip", targetAmountCents = 100, updatedAt = 1)
        val note = GoalAction(id = 4, goalId = 5, title = "called the bank", completed = true)

        val rows = buildAppHistory(
            transfers = listOf(transfer),
            expenses = listOf(expense),
            debtors = listOf(debtor),
            goals = listOf(goal),
            goalActions = listOf(note),
        )

        assertEquals(listOf("t-t1", "e-e1", "p-9", "d-id-3", "g-id-5", "n-4"), rows.map { it.key })
    }
}

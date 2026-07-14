package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.RecurringBusinessRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessAutomationServiceTest {

    @Test
    fun `due rule without last run is due`() {
        val rule = RecurringBusinessRule(
            label = "Rent",
            type = BusinessTransactionType.EXPENSE,
            amountCents = 100_000,
            category = "Rent",
            intervalDays = 30,
            lastRunAt = null,
        )
        assertTrue(rule.isDue())
    }

    @Test
    fun `recently run rule is not due`() {
        val now = System.currentTimeMillis()
        val rule = RecurringBusinessRule(
            label = "Rent",
            type = BusinessTransactionType.EXPENSE,
            amountCents = 100_000,
            category = "Rent",
            intervalDays = 30,
            lastRunAt = now,
        )
        assertTrue(!rule.isDue(now))
    }
}

package com.example.money.domain.util

import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.RecurringBusinessRule
import com.example.money.domain.repository.BusinessRepository

object BusinessAutomationService {

    suspend fun processRecurringRules(
        repository: BusinessRepository,
        rules: List<RecurringBusinessRule>,
        now: Long = System.currentTimeMillis(),
    ): List<RecurringBusinessRule> {
        val updated = mutableListOf<RecurringBusinessRule>()
        rules.forEach { rule ->
            if (!rule.isDue(now)) {
                updated.add(rule)
                return@forEach
            }
            repository.addTransaction(
                BusinessTransaction(
                    type = rule.type,
                    amountCents = rule.amountCents,
                    category = rule.category,
                    ledgerType = BusinessLedgerType.OPERATING,
                    date = now,
                    note = rule.label,
                ),
            )
            updated.add(rule.copy(lastRunAt = now))
        }
        return updated
    }
}

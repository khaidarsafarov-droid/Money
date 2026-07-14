package com.example.money.domain.util.ai

import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.CashflowForecastPoint
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

object AiCashflowPredictionService {

    fun predict(
        transactions: List<BusinessTransaction>,
        horizonDays: Int = 30,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<CashflowForecastPoint> {
        val businessTx = transactions.filter { it.ledgerType != BusinessLedgerType.PERSONAL }
        if (businessTx.isEmpty()) return emptyList()

        val lookbackMillis = nowMillis - TimeUnit.DAYS.toMillis(56)
        val recent = businessTx.filter { it.date >= lookbackMillis }
        val lookbackDays = 56.0
        val avgDailyIncome = recent
            .filter { it.type == BusinessTransactionType.INCOME }
            .sumOf { it.amountCents } / lookbackDays
        val avgDailyExpense = recent
            .filter { it.type == BusinessTransactionType.EXPENSE }
            .sumOf { it.amountCents } / lookbackDays

        val allTimeIncome = businessTx.filter { it.type == BusinessTransactionType.INCOME }.sumOf { it.amountCents }
        val allTimeExpense = businessTx.filter { it.type == BusinessTransactionType.EXPENSE }.sumOf { it.amountCents }
        var runningBalance = allTimeIncome - allTimeExpense

        val stepDays = when {
            horizonDays <= 30 -> 7
            else -> 14
        }
        val steps = (horizonDays / stepDays).coerceAtLeast(1)

        return (1..steps).map { step ->
            val daysInStep = stepDays.toLong()
            val projectedIncome = (avgDailyIncome * daysInStep).roundToLong()
            val projectedExpense = (avgDailyExpense * daysInStep).roundToLong()
            runningBalance += projectedIncome - projectedExpense
            CashflowForecastPoint(
                label = "+${step * stepDays}d",
                projectedIncomeCents = projectedIncome,
                projectedExpenseCents = projectedExpense,
                projectedBalanceCents = runningBalance,
            )
        }
    }
}

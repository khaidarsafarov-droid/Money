package com.example.money.domain.util

import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.settings.AppMode

object DebtMetricsEngine {

    fun computeMetrics(debtors: List<Debtor>, mode: AppMode? = null): DashboardDebtMetrics {
        val active = debtors.filter { debtor ->
            (mode == null || debtor.mode == mode) && !debtor.isPaid
        }
        val owedToMe = active.filter { it.debtType == DebtType.CREDITOR }
        val iOwe = active.filter { it.debtType == DebtType.DEBTOR }
        return DashboardDebtMetrics(
            owedToMeCents = owedToMe.sumOf { it.remainingAmountCents },
            owedToMeActiveCount = owedToMe.size,
            iOweCents = iOwe.sumOf { it.remainingAmountCents },
            iOweActiveCount = iOwe.size,
        )
    }
}

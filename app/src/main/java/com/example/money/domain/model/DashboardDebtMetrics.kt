package com.example.money.domain.model

import com.example.money.domain.util.MoneyMath

data class DashboardDebtMetrics(
    val owedToMeCents: Long,
    val owedToMeActiveCount: Int,
    val iOweCents: Long,
    val iOweActiveCount: Int,
) {
    val owedToMeAmount: Double get() = MoneyMath.fromCents(owedToMeCents)
    val iOweAmount: Double get() = MoneyMath.fromCents(iOweCents)
}

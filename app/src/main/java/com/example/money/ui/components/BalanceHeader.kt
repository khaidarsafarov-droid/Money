package com.example.money.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics

@Composable
fun GlobalBalanceHeader(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    onOwesMeClick: () -> Unit = {},
    onIOweClick: () -> Unit = {},
    hideAmounts: Boolean = false,
    progressPercent: Int? = null,
    modifier: Modifier = Modifier,
) {
    MintFinanceHeader(
        goalMetrics = goalMetrics,
        debtMetrics = debtMetrics,
        hideAmounts = hideAmounts,
        onOwesMeClick = onOwesMeClick,
        onIOweClick = onIOweClick,
        progressPercent = progressPercent,
        modifier = modifier,
    )
}

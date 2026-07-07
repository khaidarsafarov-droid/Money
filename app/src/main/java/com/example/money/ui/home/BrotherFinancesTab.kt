package com.example.money.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.ui.theme.CardBorder
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.util.Formatters

@Composable
fun GlobalBalanceHeader(
    summary: BudgetSummary,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    onOwesMeClick: () -> Unit = {},
    onIOweClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        border = BorderStroke(1.dp, CardBorder),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(
                    R.string.global_balance_available,
                    Formatters.currencyCompact(summary.availableBalance),
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.global_balance_available_hint,
                    Formatters.currencyCompact(summary.globalBalance),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricTile(
                    label = stringResource(R.string.global_balance_accumulated_label),
                    value = Formatters.currencyCompact(goalMetrics.totalAccumulated),
                    valueColor = FinancePositive,
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    label = stringResource(R.string.global_balance_remaining_label),
                    value = Formatters.currencyCompact(goalMetrics.totalRemainingToGoal),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DebtMetricTile(
                    label = stringResource(R.string.debt_dashboard_owes_me),
                    amountCents = debtMetrics.owedToMeCents,
                    activeCount = debtMetrics.owedToMeActiveCount,
                    positive = true,
                    onClick = onOwesMeClick,
                    modifier = Modifier.weight(1f),
                )
                DebtMetricTile(
                    label = stringResource(R.string.debt_dashboard_i_owe),
                    amountCents = debtMetrics.iOweCents,
                    activeCount = debtMetrics.iOweActiveCount,
                    positive = false,
                    onClick = onIOweClick,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text = stringResource(R.string.global_balance_debts_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(
                    R.string.global_balance_remaining_hint,
                    Formatters.currencyCompact(goalMetrics.totalRemainingToGoal),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.global_balance_deposits_label),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = Formatters.currencyCompact(summary.totalDeposits),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinancePositive,
                )
                Text(
                    text = stringResource(R.string.global_balance_expenses_label),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = Formatters.currencyCompact(summary.totalExpenses),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = FinanceNegative,
                )
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        border = BorderStroke(1.dp, CardBorder),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
        }
    }
}

@Composable
private fun DebtMetricTile(
    label: String,
    amountCents: Long,
    activeCount: Int,
    positive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = amountCents <= 0L
    val prefix = if (positive) "+" else "−"
    val amount = Formatters.currencyFromCents(amountCents)
    val valueColor = when {
        isEmpty -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        positive -> FinancePositive
        else -> FinanceNegative
    }

    Card(
        modifier = modifier.then(
            if (!isEmpty) Modifier.clickable(onClick = onClick) else Modifier,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isEmpty) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                CreamSurface
            },
        ),
        border = BorderStroke(1.dp, CardBorder),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (isEmpty) Formatters.currencyFromCents(0) else "$prefix$amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
            )
            Text(
                text = stringResource(R.string.debt_dashboard_active_count, activeCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BrotherFinancesHeader(
    summary: BudgetSummary,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics = DashboardDebtMetrics(0, 0, 0, 0),
    modifier: Modifier = Modifier,
) {
    GlobalBalanceHeader(
        summary = summary,
        goalMetrics = goalMetrics,
        debtMetrics = debtMetrics,
        modifier = modifier,
    )
}

@Composable
fun FinanceListItem(
    title: String,
    subtitle: String,
    amountColor: Color,
    isPending: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) MaterialTheme.colorScheme.surfaceVariant else CreamSurface,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (isPending && onConfirm != null) {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.action_confirm))
                }
            }
        }
    }
}

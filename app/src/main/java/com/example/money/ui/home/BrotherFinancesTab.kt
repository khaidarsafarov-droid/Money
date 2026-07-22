package com.example.money.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.theme.moneyDisplayStyle
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            SignMetric(
                sign = stringResource(R.string.metric_sign_saved),
                accessibilityLabel = stringResource(R.string.global_balance_accumulated_label),
                value = Formatters.currencyCompact(goalMetrics.totalAccumulated),
                valueColor = FinancePositive,
                modifier = Modifier.weight(1f),
            )
            SignMetric(
                sign = stringResource(R.string.metric_sign_to_goal),
                accessibilityLabel = stringResource(R.string.global_balance_remaining_label),
                value = Formatters.currencyCompact(goalMetrics.totalRemainingToGoal),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            SignMetric(
                sign = stringResource(R.string.metric_sign_owes_me),
                accessibilityLabel = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(debtMetrics.owedToMeCents),
                valueColor = if (debtMetrics.owedToMeCents > 0L) {
                    FinancePositive
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onOwesMeClick.takeIf { debtMetrics.owedToMeCents > 0L },
                modifier = Modifier.weight(1f),
            )
            SignMetric(
                sign = stringResource(R.string.metric_sign_i_owe),
                accessibilityLabel = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(debtMetrics.iOweCents),
                valueColor = if (debtMetrics.iOweCents > 0L) {
                    FinanceNegative
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onIOweClick.takeIf { debtMetrics.iOweCents > 0L },
                modifier = Modifier.weight(1f),
            )
        }
        if (summary.totalDeposits > 0.0 || summary.totalExpenses > 0.0) {
            FlowSignSummary(summary = summary)
        }
    }
}

@Composable
private fun FlowSignSummary(summary: BudgetSummary) {
    val depositsLabel = stringResource(R.string.global_balance_deposits_label)
    val expensesLabel = stringResource(R.string.global_balance_expenses_label)
    val deposits = Formatters.currencyCompact(summary.totalDeposits)
    val expenses = Formatters.currencyCompact(summary.totalExpenses)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoneySpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.lg),
    ) {
        Text(
            text = "${stringResource(R.string.metric_sign_deposit)} $deposits",
            style = moneyCaptionStyle(),
            color = FinancePositive,
            modifier = Modifier.semantics {
                contentDescription = "$depositsLabel $deposits"
            },
        )
        Text(
            text = "${stringResource(R.string.metric_sign_expense)} $expenses",
            style = moneyCaptionStyle(),
            color = FinanceNegative,
            modifier = Modifier.semantics {
                contentDescription = "$expensesLabel $expenses"
            },
        )
    }
}

@Composable
private fun SignMetric(
    sign: String,
    accessibilityLabel: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val description = "$accessibilityLabel $value"
    Row(
        modifier = modifier
            .semantics { contentDescription = description }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = MoneySpacing.xs, horizontal = MoneySpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
    ) {
        Text(
            text = sign,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
        )
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
        Column(modifier = Modifier.padding(MoneySpacing.cardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = moneyDisplayStyle(),
                        color = amountColor,
                    )
                    Text(
                        text = subtitle,
                        style = moneyCaptionStyle(),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.defaultMinSize(
                        minWidth = MoneySpacing.minTouchTarget,
                        minHeight = MoneySpacing.minTouchTarget,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (isPending && onConfirm != null) {
                MoneyButton(
                    text = stringResource(R.string.action_confirm),
                    onClick = onConfirm,
                    priority = MoneyButtonPriority.Secondary,
                )
            }
        }
    }
}

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
    @Suppress("UNUSED_PARAMETER") summary: BudgetSummary,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    onOwesMeClick: () -> Unit = {},
    onIOweClick: () -> Unit = {},
    hideAmounts: Boolean = false,
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
            LabeledMetric(
                sign = stringResource(R.string.metric_sign_saved),
                label = stringResource(R.string.global_balance_accumulated_label),
                value = Formatters.currencyCompact(goalMetrics.totalAccumulated, hide = hideAmounts),
                valueColor = FinancePositive,
                modifier = Modifier.weight(1f),
            )
            LabeledMetric(
                sign = stringResource(R.string.metric_sign_to_goal),
                label = stringResource(R.string.global_balance_remaining_label),
                value = Formatters.currencyCompact(goalMetrics.totalRemainingToGoal, hide = hideAmounts),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            LabeledMetric(
                sign = stringResource(R.string.metric_sign_owes_me),
                label = stringResource(R.string.debt_dashboard_owes_me),
                value = Formatters.currencyFromCents(debtMetrics.owedToMeCents, hide = hideAmounts),
                valueColor = if (debtMetrics.owedToMeCents > 0L) {
                    FinancePositive
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onOwesMeClick.takeIf { debtMetrics.owedToMeCents > 0L },
                modifier = Modifier.weight(1f),
            )
            LabeledMetric(
                sign = stringResource(R.string.metric_sign_i_owe),
                label = stringResource(R.string.debt_dashboard_i_owe),
                value = Formatters.currencyFromCents(debtMetrics.iOweCents, hide = hideAmounts),
                valueColor = if (debtMetrics.iOweCents > 0L) {
                    FinanceNegative
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = onIOweClick.takeIf { debtMetrics.iOweCents > 0L },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LabeledMetric(
    sign: String,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val description = "$label $value"
    Column(
        modifier = modifier
            .semantics { contentDescription = description }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = MoneySpacing.xs, horizontal = MoneySpacing.xs),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "$sign $label",
            style = moneyCaptionStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
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

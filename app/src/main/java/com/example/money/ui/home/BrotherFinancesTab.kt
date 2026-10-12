package com.example.money.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.ui.components.MintFinanceHeader
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.moneyCaptionStyle
import com.example.money.ui.theme.moneyDisplayStyle

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

@Composable
fun BrotherFinancesHeader(
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics = DashboardDebtMetrics(0, 0, 0, 0),
    modifier: Modifier = Modifier,
) {
    GlobalBalanceHeader(
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
    onDelete: (() -> Unit)? = null,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(amountColor.copy(alpha = 0.16f)),
                )
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
                if (onDelete != null) {
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

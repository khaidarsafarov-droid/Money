package com.example.money.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.StatusActiveBg

/** Actions offered by the single primary control (the dock FAB). */
enum class AddAction {
    Expense,
    Deposit,
    Debt,
    GoalContribution,
    NewGoal,
}

/**
 * Bottom sheet that replaces the old quick-add dialog.
 * [highlighted] is listed first and tinted so the section the user came from
 * (debts, goals) stays one tap away without a second filled button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(
    highlighted: AddAction,
    onDismiss: () -> Unit,
    onExpense: () -> Unit,
    onDeposit: () -> Unit,
    onDebt: () -> Unit,
    onGoalContribution: () -> Unit,
    onNewGoal: () -> Unit,
) {
    val ordered = AddAction.entries.sortedBy { action -> if (action == highlighted) 0 else 1 }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MoneySpacing.lg)
                .padding(bottom = MoneySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.add_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = MoneySpacing.sm),
            )
            ordered.forEach { action ->
                AddActionRow(
                    label = stringResource(action.labelRes),
                    icon = action.icon,
                    highlighted = action == highlighted,
                    onClick = {
                        onDismiss()
                        when (action) {
                            AddAction.Expense -> onExpense()
                            AddAction.Deposit -> onDeposit()
                            AddAction.Debt -> onDebt()
                            AddAction.GoalContribution -> onGoalContribution()
                            AddAction.NewGoal -> onNewGoal()
                        }
                    },
                )
            }
        }
    }
}

private val AddAction.labelRes: Int
    get() = when (this) {
        AddAction.Expense -> R.string.add_sheet_expense
        AddAction.Deposit -> R.string.add_sheet_deposit
        AddAction.Debt -> R.string.add_sheet_debt
        AddAction.GoalContribution -> R.string.add_sheet_goal_contribution
        AddAction.NewGoal -> R.string.add_sheet_new_goal
    }

private val AddAction.icon: ImageVector
    get() = when (this) {
        AddAction.Expense -> Icons.AutoMirrored.Outlined.TrendingDown
        AddAction.Deposit -> Icons.Outlined.AccountBalanceWallet
        AddAction.Debt -> Icons.Outlined.People
        AddAction.GoalContribution -> Icons.Outlined.Savings
        AddAction.NewGoal -> Icons.Outlined.Flag
    }

@Composable
private fun AddActionRow(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlighted) StatusActiveBg else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
            color = if (highlighted) GoldPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Savings
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
import com.example.money.ui.theme.MoneySpacing

/** Actions a page plus button can open from the add sheet. */
enum class AddAction {
    Expense,
    Deposit,
    Debt,
    GoalContribution,
    NewGoal,
}

/**
 * Bottom sheet for pages whose plus button offers more than one record type.
 * [actions] limits the list to that page. [highlighted] is listed first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActionSheet(
    highlighted: AddAction,
    actions: List<AddAction> = AddAction.entries,
    onDismiss: () -> Unit,
    onExpense: () -> Unit,
    onDeposit: () -> Unit,
    onDebt: () -> Unit,
    onGoalContribution: () -> Unit,
    onNewGoal: () -> Unit,
) {
    val ordered = actions.sortedBy { action -> if (action == highlighted) 0 else 1 }
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

/** Debts plus menu: type a debt, or import one from a photo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtAddMenuSheet(
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MoneySpacing.lg)
                .padding(bottom = MoneySpacing.xl),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.add_sheet_debt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = MoneySpacing.sm),
            )
            AddActionRow(
                label = stringResource(R.string.debt_add_manual),
                icon = Icons.Outlined.Edit,
                highlighted = true,
                onClick = {
                    onDismiss()
                    onManual()
                },
            )
            AddActionRow(
                label = stringResource(R.string.debt_import_camera),
                icon = Icons.Outlined.PhotoCamera,
                highlighted = false,
                onClick = {
                    onDismiss()
                    onCamera()
                },
            )
            AddActionRow(
                label = stringResource(R.string.debt_import_gallery),
                icon = Icons.Outlined.Image,
                highlighted = false,
                onClick = {
                    onDismiss()
                    onGallery()
                },
            )
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
            .background(if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MoneySpacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Medium,
            color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

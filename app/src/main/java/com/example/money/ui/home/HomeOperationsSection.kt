package com.example.money.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Expense
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.SwipeToDeleteContainer
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters

private sealed class RecentOperation {
    abstract val date: Long

    data class TransferOp(val transfer: Transfer) : RecentOperation() {
        override val date: Long = transfer.date
    }

    data class ExpenseOp(val expense: Expense) : RecentOperation() {
        override val date: Long = expense.date
    }
}

fun LazyListScope.homeOperationsSection(
    transfers: List<Transfer>,
    expenses: List<Expense>,
    goals: List<SavingsGoal>,
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onConfirmExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    limit: Int? = null,
    onOpenAll: (() -> Unit)? = null,
) {
    val operations = buildList {
        transfers.forEach { add(RecentOperation.TransferOp(it)) }
        expenses.forEach { add(RecentOperation.ExpenseOp(it)) }
    }.sortedByDescending { it.date }.let { rows ->
        if (limit != null) rows.take(limit) else rows
    }
    if (operations.isEmpty()) {
        item {
            EmptyStateMessage(
                title = stringResource(R.string.empty_budget),
                hint = stringResource(R.string.empty_budget_hint),
                modifier = Modifier.padding(
                    horizontal = MoneySpacing.lg,
                    vertical = MoneySpacing.sm,
                ),
            )
        }
    } else {
        item {
            Text(
                text = stringResource(R.string.operations_section_title),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .then(if (onOpenAll != null) Modifier.clickable(onClick = onOpenAll) else Modifier),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        items(operations, key = { operation ->
            when (operation) {
                is RecentOperation.TransferOp ->
                    "t-${operation.transfer.syncId.ifBlank { "id-${operation.transfer.id}" }}"
                is RecentOperation.ExpenseOp ->
                    "e-${operation.expense.syncId.ifBlank { "id-${operation.expense.id}" }}"
            }
        }) { operation ->
            when (operation) {
                is RecentOperation.TransferOp -> {
                    val transfer = operation.transfer
                    val goalName = transfer.goalId?.let { goalId ->
                        goals.find { it.id == goalId }?.name
                    }
                    val operationLabel = if (goalName != null) {
                        stringResource(R.string.operation_goal_transfer, goalName)
                    } else {
                        stringResource(R.string.operation_deposit)
                    }
                    SwipeToDeleteContainer(
                        onDelete = { onDeleteTransfer(transfer) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        FinanceListItem(
                            title = "+${Formatters.currencyCompact(transfer.amount)}",
                            subtitle = buildString {
                                append(operationLabel)
                                append(" · ")
                                append(Formatters.shortDate(transfer.date))
                                if (!transfer.isConfirmed) append(stringResource(R.string.operation_pending_suffix))
                            },
                            amountColor = FinancePositive,
                            isPending = !transfer.isConfirmed,
                            onConfirm = if (!transfer.isConfirmed) {
                                { onConfirmTransfer(transfer.id) }
                            } else {
                                null
                            },
                            onDelete = { onDeleteTransfer(transfer) },
                        )
                    }
                }
                is RecentOperation.ExpenseOp -> {
                    val expense = operation.expense
                    SwipeToDeleteContainer(
                        onDelete = { onDeleteExpense(expense) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        FinanceListItem(
                            title = "−${Formatters.currencyCompact(expense.amount)}",
                            subtitle = buildString {
                                append(stringResource(R.string.operation_expense))
                                append(" · ")
                                append(expense.category)
                                append(" · ")
                                append(Formatters.shortDate(expense.date))
                                if (!expense.isConfirmed) append(stringResource(R.string.operation_pending_suffix))
                            },
                            amountColor = FinanceNegative,
                            isPending = !expense.isConfirmed,
                            onConfirm = if (!expense.isConfirmed) {
                                { onConfirmExpense(expense.id) }
                            } else {
                                null
                            },
                            onDelete = { onDeleteExpense(expense) },
                        )
                    }
                }
            }
        }
    }
}

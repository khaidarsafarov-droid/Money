package com.example.money.ui.home

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

fun LazyListScope.homeOperationsSection(
    transfers: List<Transfer>,
    expenses: List<Expense>,
    goals: List<SavingsGoal>,
    onQuickAdd: () -> Unit,
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onConfirmExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
) {
    if (transfers.isEmpty() && expenses.isEmpty()) {
        item {
            EmptyStateMessage(
                title = stringResource(R.string.empty_budget),
                hint = stringResource(R.string.empty_budget_hint),
                actionLabel = stringResource(R.string.empty_budget_action),
                onAction = onQuickAdd,
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (transfers.isNotEmpty()) {
            items(transfers, key = { "t-${it.syncId.ifBlank { "id-${it.id}" }}" }) { transfer ->
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
                            if (!transfer.isConfirmed) append(" · Ожидает")
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
        }
        if (expenses.isNotEmpty()) {
            items(expenses, key = { "e-${it.syncId.ifBlank { "id-${it.id}" }}" }) { expense ->
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
                            if (!expense.isConfirmed) append(" · Ожидает")
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

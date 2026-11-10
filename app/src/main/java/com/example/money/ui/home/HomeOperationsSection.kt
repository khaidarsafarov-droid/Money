package com.example.money.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.FinanceListItem
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters

/**
 * One line in the read-only history. Money rows and goal notes use [sortAt]
 * from the record date.
 */
internal sealed class AppHistoryEvent {
    abstract val sortAt: Long
    abstract val key: String

    data class TransferEvent(val transfer: Transfer) : AppHistoryEvent() {
        override val sortAt: Long = transfer.date
        override val key: String = "t-${transfer.syncId.ifBlank { "id-${transfer.id}" }}"
    }

    data class ExpenseEvent(val expense: Expense) : AppHistoryEvent() {
        override val sortAt: Long = expense.date
        override val key: String = "e-${expense.syncId.ifBlank { "id-${expense.id}" }}"
    }

    data class DebtPaymentEvent(val debtor: Debtor, val payment: DebtPayment) : AppHistoryEvent() {
        override val sortAt: Long = payment.paidAt
        override val key: String = "p-${payment.id}"
    }

    data class GoalNoteEvent(val goalName: String, val action: GoalAction) : AppHistoryEvent() {
        override val sortAt: Long = action.occurredAt
        override val key: String = "n-${action.id}"
    }

    data class DebtOpenedEvent(val debtor: Debtor) : AppHistoryEvent() {
        override val sortAt: Long = debtor.dateGiven
        override val key: String = "d-${debtor.syncId.ifBlank { "id-${debtor.id}" }}"
    }

    data class GoalRecordEvent(val goal: SavingsGoal) : AppHistoryEvent() {
        override val sortAt: Long = goal.updatedAt
        override val key: String = "g-${goal.syncId.ifBlank { "id-${goal.id}" }}"
    }
}

internal fun buildAppHistory(
    transfers: List<Transfer>,
    expenses: List<Expense>,
    debtors: List<Debtor>,
    goals: List<SavingsGoal>,
    goalActions: List<GoalAction>,
): List<AppHistoryEvent> {
    val linkedPaymentIds = buildSet {
        transfers.mapNotNull { it.debtPaymentId }.forEach { add(it) }
        expenses.mapNotNull { it.debtPaymentId }.forEach { add(it) }
    }
    return buildList {
        transfers.forEach { add(AppHistoryEvent.TransferEvent(it)) }
        expenses.forEach { add(AppHistoryEvent.ExpenseEvent(it)) }
        debtors.forEach { debtor ->
            add(AppHistoryEvent.DebtOpenedEvent(debtor))
            debtor.paymentHistory.forEach { payment ->
                if (payment.id !in linkedPaymentIds) {
                    add(AppHistoryEvent.DebtPaymentEvent(debtor, payment))
                }
            }
        }
        goals.forEach { add(AppHistoryEvent.GoalRecordEvent(it)) }
        goalActions.forEach { action ->
            val goalName = goals.find { it.id == action.goalId }?.name.orEmpty()
            add(AppHistoryEvent.GoalNoteEvent(goalName, action))
        }
    }.sortedByDescending { it.sortAt }
}

fun LazyListScope.homeOperationsSection(
    transfers: List<Transfer>,
    expenses: List<Expense>,
    goals: List<SavingsGoal>,
    debtors: List<Debtor> = emptyList(),
    goalActions: List<GoalAction> = emptyList(),
    limit: Int? = null,
    onOpenAll: (() -> Unit)? = null,
) {
    val operations = buildAppHistory(
        transfers = transfers,
        expenses = expenses,
        debtors = debtors,
        goals = goals,
        goalActions = goalActions,
    ).let { rows -> if (limit != null) rows.take(limit) else rows }
    item {
        Text(
            text = stringResource(
                if (onOpenAll != null) R.string.operations_section_title else R.string.history_title,
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .then(if (onOpenAll != null) Modifier.clickable(onClick = onOpenAll) else Modifier),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (onOpenAll == null) {
            Text(
                text = stringResource(R.string.history_readonly_hint),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (operations.isEmpty()) {
        item {
            EmptyStateMessage(
                title = stringResource(R.string.empty_budget),
                modifier = Modifier.padding(
                    horizontal = MoneySpacing.lg,
                    vertical = MoneySpacing.sm,
                ),
            )
        }
    } else {
        items(operations, key = { it.key }, contentType = { "operation" }) { event ->
            HistoryRow(event = event, goals = goals)
        }
    }
}

internal fun historyEventTitle(event: AppHistoryEvent): String = when (event) {
    is AppHistoryEvent.TransferEvent -> "+${Formatters.currencyFromCents(event.transfer.amountCents)}"
    is AppHistoryEvent.ExpenseEvent -> "−${Formatters.currencyFromCents(event.expense.amountCents)}"
    is AppHistoryEvent.DebtPaymentEvent -> {
        val amount = Formatters.currencyFromCents(event.payment.amountCents)
        if (event.debtor.debtType == DebtType.CREDITOR) "+$amount" else "−$amount"
    }
    is AppHistoryEvent.GoalNoteEvent -> event.action.title
    is AppHistoryEvent.DebtOpenedEvent -> event.debtor.name
    is AppHistoryEvent.GoalRecordEvent -> event.goal.name
}

@Composable
internal fun historyEventSubtitle(event: AppHistoryEvent, goals: List<SavingsGoal>): String = when (event) {
    is AppHistoryEvent.TransferEvent -> {
        val transfer = event.transfer
        val goalName = transfer.goalId?.let { goalId -> goals.find { it.id == goalId }?.name }
        buildString {
            append(
                if (goalName != null) {
                    stringResource(R.string.operation_goal_transfer, goalName)
                } else {
                    stringResource(R.string.operation_deposit)
                },
            )
            append(" · ")
            append(Formatters.shortDate(transfer.date))
            if (!transfer.isConfirmed) append(stringResource(R.string.operation_pending_suffix))
        }
    }
    is AppHistoryEvent.ExpenseEvent -> {
        val expense = event.expense
        val goalName = expense.goalId?.let { goalId -> goals.find { it.id == goalId }?.name }
        buildString {
            append(
                when {
                    goalName != null -> stringResource(R.string.history_goal_expense, goalName)
                    expense.goalId != null -> stringResource(R.string.goal_expense_title)
                    else -> stringResource(R.string.operation_expense)
                },
            )
            append(" · ")
            append(expense.category)
            append(" · ")
            append(Formatters.shortDate(expense.date))
            if (!expense.isConfirmed) append(stringResource(R.string.operation_pending_suffix))
        }
    }
    is AppHistoryEvent.DebtPaymentEvent ->
        stringResource(R.string.history_debt_payment, event.debtor.name) +
            " · " + Formatters.shortDate(event.payment.paidAt)
    is AppHistoryEvent.GoalNoteEvent ->
        stringResource(R.string.history_goal_note, event.goalName) +
            " · " + Formatters.shortDate(event.action.occurredAt)
    is AppHistoryEvent.DebtOpenedEvent ->
        stringResource(R.string.history_debt_opened, Formatters.shortDate(event.debtor.dateGiven))
    is AppHistoryEvent.GoalRecordEvent ->
        stringResource(R.string.history_goal_record, Formatters.shortDate(event.goal.updatedAt))
}

@Composable
internal fun historyEventColor(event: AppHistoryEvent): Color = when (event) {
    is AppHistoryEvent.TransferEvent -> MaterialTheme.colorScheme.primary
    is AppHistoryEvent.ExpenseEvent -> MaterialTheme.colorScheme.error
    is AppHistoryEvent.DebtPaymentEvent ->
        if (event.debtor.debtType == DebtType.CREDITOR) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    is AppHistoryEvent.GoalNoteEvent -> MaterialTheme.colorScheme.primary
    is AppHistoryEvent.DebtOpenedEvent -> MaterialTheme.colorScheme.tertiary
    is AppHistoryEvent.GoalRecordEvent -> MaterialTheme.colorScheme.primary
}

@Composable
private fun HistoryRow(
    event: AppHistoryEvent,
    goals: List<SavingsGoal>,
) {
    val title = historyEventTitle(event)
    val subtitle = historyEventSubtitle(event, goals)
    val color = historyEventColor(event)
    FinanceListItem(
        title = title,
        subtitle = subtitle,
        amountColor = color,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

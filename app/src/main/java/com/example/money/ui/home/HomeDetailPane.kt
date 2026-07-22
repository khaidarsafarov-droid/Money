package com.example.money.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GoalProgress
import com.example.money.ui.components.DebtorListItem
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.debtors.DebtContactActions
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.util.Formatters

@Composable
fun HomeDetailPane(
    selection: HomeListSelection,
    summary: BudgetSummary,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics = DashboardDebtMetrics(0, 0, 0, 0),
    onEditGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onExpense: () -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onAccelerateDebtPayment: (Debtor, Long) -> Unit = { _, _ -> },
    hideAmounts: Boolean = false,
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit = {},
    onSyncCalendar: (Debtor) -> Unit = {},
    onRemoveCalendar: (Debtor) -> Unit = {},
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onConfirmExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (selection) {
            HomeListSelection.Overview -> {
                GlobalBalanceHeader(
                    summary = summary,
                    goalMetrics = goalMetrics,
                    debtMetrics = debtMetrics,
                )
                Text(
                    text = stringResource(R.string.tablet_detail_overview_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is HomeListSelection.GoalItem -> {
                Text(
                    text = stringResource(R.string.tablet_detail_goal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                GoalListItem(
                    goal = selection.goal,
                    onEdit = { onEditGoal(selection.goal) },
                    onDelete = { onDeleteGoal(selection.goal) },
                    onTransfer = if (!selection.goal.isCompleted) {
                        { onGoalTransfer(selection.goal) }
                    } else {
                        null
                    },
                    onExpense = onExpense,
                )
                selection.goal.deadlineMillis?.let { deadline ->
                    val daysLeft = GoalProgress.daysUntilDeadline(deadline)
                    if (daysLeft != null) {
                        Text(
                            text = stringResource(R.string.tablet_detail_goal_deadline, daysLeft),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            is HomeListSelection.DebtorItem -> {
                Text(
                    text = stringResource(R.string.tablet_detail_debtor),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val context = LocalContext.current
                val accelerateCents = DebtFreedomEngine.suggestAcceleratePaymentCents(
                    remainingCents = selection.debtor.remainingAmountCents,
                    targetCents = selection.debtor.targetAmountCents,
                    currentCents = selection.debtor.currentAmountCents,
                )
                DebtorListItem(
                    debtor = selection.debtor,
                    onRemind = { onRemindDebtor(selection.debtor) },
                    onAddPayment = { onAddDebtPayment(selection.debtor) },
                    onAccelerate = { onAccelerateDebtPayment(selection.debtor, accelerateCents) },
                    onEdit = { onEditDebtor(selection.debtor) },
                    onDelete = { onDeleteDebtor(selection.debtor) },
                    hideAmounts = hideAmounts,
                    onViewContactHistory = selection.debtor.contactLookupKey?.let { _ ->
                        { onViewContactHistory(selection.debtor) }
                    },
                    onCalendar = {
                        if (selection.debtor.calendarEventId.isNullOrBlank()) {
                            onSyncCalendar(selection.debtor)
                        } else {
                            onRemoveCalendar(selection.debtor)
                        }
                    },
                    calendarSynced = !selection.debtor.calendarEventId.isNullOrBlank(),
                    onCall = selection.debtor.phone.takeIf { it.isNotBlank() }?.let {
                        { DebtContactActions.dialPhone(context, selection.debtor.phone) }
                    },
                    onWhatsApp = selection.debtor.phone.takeIf { it.isNotBlank() }?.let {
                        {
                            DebtContactActions.openWhatsApp(
                                context,
                                selection.debtor.phone,
                                selection.debtor.name,
                            )
                        }
                    },
                )
            }
            is HomeListSelection.TransferItem -> {
                Text(
                    text = stringResource(R.string.tablet_detail_transfer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                FinanceListItem(
                    title = "+${Formatters.currencyCompact(selection.transfer.amount)}",
                    subtitle = buildString {
                        append(
                            selection.goalName?.let {
                                stringResource(R.string.operation_goal_transfer, it)
                            } ?: stringResource(R.string.operation_deposit),
                        )
                        append(" · ")
                        append(Formatters.shortDate(selection.transfer.date))
                        if (!selection.transfer.isConfirmed) append(" · Ожидает")
                    },
                    amountColor = FinancePositive,
                    isPending = !selection.transfer.isConfirmed,
                    onConfirm = if (!selection.transfer.isConfirmed) {
                        { onConfirmTransfer(selection.transfer.id) }
                    } else {
                        null
                    },
                    onDelete = { onDeleteTransfer(selection.transfer) },
                )
            }
            is HomeListSelection.ExpenseItem -> {
                Text(
                    text = stringResource(R.string.tablet_detail_expense),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                FinanceListItem(
                    title = "−${Formatters.currencyCompact(selection.expense.amount)}",
                    subtitle = buildString {
                        append(stringResource(R.string.operation_expense))
                        append(" · ")
                        append(selection.expense.category)
                        append(" · ")
                        append(Formatters.shortDate(selection.expense.date))
                        if (!selection.expense.isConfirmed) append(" · Ожидает")
                    },
                    amountColor = FinanceNegative,
                    isPending = !selection.expense.isConfirmed,
                    onConfirm = if (!selection.expense.isConfirmed) {
                        { onConfirmExpense(selection.expense.id) }
                    } else {
                        null
                    },
                    onDelete = { onDeleteExpense(selection.expense) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeListCompactRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

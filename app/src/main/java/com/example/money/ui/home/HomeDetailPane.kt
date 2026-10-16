package com.example.money.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.ui.components.DebtorListItem
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.debtors.DebtContactActions
@Composable
fun HomeDetailPane(
    selection: HomeListSelection,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics = DashboardDebtMetrics(0, 0, 0, 0),
    onEditGoal: (SavingsGoal) -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onLogGoal: (SavingsGoal) -> Unit = {},
    onExpense: (SavingsGoal) -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onQuickPayment: (Debtor) -> Unit = {},
    onAccelerateDebtPayment: (Debtor, Long) -> Unit = { _, _ -> },
    onMarkDebtPaid: (Debtor) -> Unit = {},
    hideAmounts: Boolean = false,
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit = {},
    onSyncCalendar: (Debtor) -> Unit = {},
    onRemoveCalendar: (Debtor) -> Unit = {},
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
                    goalMetrics = goalMetrics,
                    debtMetrics = debtMetrics,
                    hideAmounts = hideAmounts,
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
                    onOpen = { onOpenGoal(selection.goal) },
                    onDelete = { onDeleteGoal(selection.goal) },
                    onTransfer = if (!selection.goal.isCompleted) {
                        { onGoalTransfer(selection.goal) }
                    } else {
                        null
                    },
                    onLog = { onLogGoal(selection.goal) },
                    onExpense = { onExpense(selection.goal) },
                    hideAmounts = hideAmounts,
                )
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
                    onQuickPayment = { onQuickPayment(selection.debtor) },
                    onAccelerate = { onAccelerateDebtPayment(selection.debtor, accelerateCents) },
                    onMarkPaid = if (!selection.debtor.isPaid) {
                        { onMarkDebtPaid(selection.debtor) }
                    } else {
                        null
                    },
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
    onQuickPayment: (() -> Unit)? = null,
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
            if (onQuickPayment != null) {
                androidx.compose.material3.TextButton(
                    onClick = onQuickPayment,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.quick_payment_add))
                }
            }
        }
    }
}

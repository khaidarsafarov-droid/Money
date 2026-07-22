package com.example.money.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.money.domain.sync.SyncConnectionState
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.debtors.DebtTab
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.GamificationState
import com.example.money.settings.PrivacySettings
import com.example.money.ui.debtors.DebtCalendarTab
import com.example.money.ui.debtors.DebtFreedomTab
import com.example.money.ui.debtors.GamificationBanner
import com.example.money.ui.debtors.DebtTabRow
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtSearchField
import com.example.money.ui.debtors.DebtorFilterRow
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.util.Formatters

@Composable
fun TabletHomeLayout(
    section: HomeSection,
    onSectionChange: (HomeSection) -> Unit,
    selection: HomeListSelection,
    onSelectionChange: (HomeListSelection) -> Unit,
    syncState: SyncConnectionState,
    snackbarHostState: SnackbarHostState,
    topBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    goals: List<SavingsGoal>,
    debtors: List<Debtor>,
    allDebtors: List<Debtor>,
    debtTab: DebtTab,
    onDebtTabChange: (DebtTab) -> Unit,
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    transfers: List<Transfer>,
    expenses: List<Expense>,
    budgetSummary: BudgetSummary,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onExpense: () -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onAccelerateDebtPayment: (Debtor, Long) -> Unit,
    freedomProjection: DebtFreedomEngine.FreedomProjection,
    extraMonthlyCents: Long,
    onExtraMonthlyChange: (Long) -> Unit,
    gamificationState: GamificationState = GamificationState(),
    privacySettings: PrivacySettings = PrivacySettings(),
    autopilotSettings: AutopilotSettings = AutopilotSettings(),
    onOpenAdvisor: () -> Unit = {},
    onApplyAutopilot: () -> Unit = {},
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit = {},
    onSyncCalendar: (Debtor) -> Unit = {},
    onRemoveCalendar: (Debtor) -> Unit = {},
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onConfirmExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    emptyDebtorsTitle: @Composable (DebtorFilter) -> String,
    emptyDebtorsHint: @Composable (DebtorFilter) -> String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = topBar,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavigationRail(containerColor = CreamBackground) {
                SyncStatusIndicator(
                    state = syncState,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                HomeSection.entries.forEach { item ->
                    NavigationRailItem(
                        selected = section == item,
                        onClick = {
                            onSectionChange(item)
                            onSelectionChange(HomeListSelection.Overview)
                        },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    HomeSection.Goals -> Icons.Outlined.AccountBalanceWallet
                                    HomeSection.Debts -> Icons.Outlined.People
                                    HomeSection.Operations -> Icons.AutoMirrored.Outlined.ListAlt
                                },
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            ) {
                item {
                    GlobalBalanceHeader(
                        summary = budgetSummary,
                        goalMetrics = goalMetrics,
                        debtMetrics = debtMetrics,
                        onOwesMeClick = {
                            onSectionChange(HomeSection.Debts)
                            onDebtTabChange(DebtTab.OwesMe)
                        },
                        onIOweClick = {
                            onSectionChange(HomeSection.Debts)
                            onDebtTabChange(DebtTab.IOwe)
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                when (section) {
                    HomeSection.Goals -> {
                        item {
                            RowActions(
                                title = stringResource(R.string.goals_section_title, goals.size),
                                actionLabel = stringResource(R.string.goal_add_title),
                                onAction = onAddGoal,
                            )
                        }
                        if (goals.isEmpty()) {
                            item {
                                EmptyStateMessage(
                                    title = stringResource(R.string.empty_goals_title),
                                    hint = stringResource(R.string.empty_goals_hint),
                                )
                            }
                        } else {
                            items(goals, key = { "tablet-goal-${it.syncId.ifBlank { "id-${it.id}" }}" }) { goal ->
                                HomeListCompactRow(
                                    title = goal.name,
                                    subtitle = stringResource(
                                        R.string.tablet_list_goal_subtitle,
                                        Formatters.currencyFromCents(goal.currentAmountCents, hide = privacySettings.hideAmounts),
                                        Formatters.currencyFromCents(goal.targetAmountCents, hide = privacySettings.hideAmounts),
                                        goal.progressPercent,
                                    ),
                                    selected = selection is HomeListSelection.GoalItem &&
                                        selection.goal.id == goal.id,
                                    onClick = { onSelectionChange(HomeListSelection.GoalItem(goal)) },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    HomeSection.Debts -> {
                        item {
                            DebtTabRow(
                                selectedTab = debtTab,
                                onTabSelected = onDebtTabChange,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        if (debtTab == DebtTab.Calendar) {
                            item {
                                DebtCalendarTab(
                                    debtors = allDebtors,
                                    hideAmounts = privacySettings.hideAmounts,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else if (debtTab == DebtTab.Freedom) {
                            item {
                                DebtFreedomTab(
                                    projection = freedomProjection,
                                    extraMonthlyCents = extraMonthlyCents,
                                    onExtraMonthlyChange = onExtraMonthlyChange,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            item {
                                GamificationBanner(
                                    state = gamificationState,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = onOpenAdvisor,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Text(stringResource(R.string.advisor_open))
                                    }
                                    if (autopilotSettings.enabled) {
                                        androidx.compose.material3.Button(
                                            onClick = onApplyAutopilot,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.autopilot_apply_now))
                                        }
                                    }
                                }
                            }
                            item {
                                DebtorFilterRow(
                                    selectedFilter = debtorFilter,
                                    onFilterSelected = onDebtorFilterChange,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            item {
                                DebtSearchField(
                                    query = debtSearchQuery,
                                    onQueryChange = onDebtSearchQueryChange,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            if (debtors.isEmpty()) {
                                item {
                                    EmptyStateMessage(
                                        title = emptyDebtorsTitle(debtorFilter),
                                        hint = emptyDebtorsHint(debtorFilter),
                                    )
                                }
                            } else {
                                items(debtors, key = { "tablet-debtor-${it.syncId.ifBlank { "id-${it.id}" }}" }) { debtor ->
                                    HomeListCompactRow(
                                        title = debtor.name,
                                        subtitle = stringResource(
                                            R.string.tablet_list_debtor_subtitle,
                                            Formatters.currencyFromCents(debtor.remainingAmountCents, hide = privacySettings.hideAmounts),
                                            Formatters.currencyFromCents(debtor.targetAmountCents, hide = privacySettings.hideAmounts),
                                        ),
                                        selected = selection is HomeListSelection.DebtorItem &&
                                            selection.debtor.id == debtor.id,
                                        onClick = { onSelectionChange(HomeListSelection.DebtorItem(debtor)) },
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.Operations -> {
                        if (transfers.isEmpty() && expenses.isEmpty()) {
                            item {
                                EmptyStateMessage(
                                    title = stringResource(R.string.empty_budget),
                                    hint = stringResource(R.string.empty_budget_hint),
                                )
                            }
                        } else {
                            items(transfers, key = { "tablet-t-${it.syncId.ifBlank { "id-${it.id}" }}" }) { transfer ->
                                val goalName = transfer.goalId?.let { id ->
                                    goals.find { it.id == id }?.name
                                }
                                val subtitle = when {
                                    transfer.note.isNotBlank() -> transfer.note
                                    goalName != null -> stringResource(R.string.operation_goal_transfer, goalName)
                                    else -> stringResource(R.string.operation_deposit)
                                }
                                HomeListCompactRow(
                                    title = "+${Formatters.currencyCompact(transfer.amount)}",
                                    subtitle = subtitle,
                                    selected = selection is HomeListSelection.TransferItem &&
                                        selection.transfer.id == transfer.id,
                                    onClick = {
                                        onSelectionChange(HomeListSelection.TransferItem(transfer, goalName))
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                            items(expenses, key = { "tablet-e-${it.syncId.ifBlank { "id-${it.id}" }}" }) { expense ->
                                val subtitle = expense.note.ifBlank { expense.category }
                                HomeListCompactRow(
                                    title = "−${Formatters.currencyCompact(expense.amount)}",
                                    subtitle = subtitle,
                                    selected = selection is HomeListSelection.ExpenseItem &&
                                        selection.expense.id == expense.id,
                                    onClick = { onSelectionChange(HomeListSelection.ExpenseItem(expense)) },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
            )

            HomeDetailPane(
                selection = selection,
                summary = budgetSummary,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                onEditGoal = onEditGoal,
                onDeleteGoal = onDeleteGoal,
                onGoalTransfer = onGoalTransfer,
                onExpense = onExpense,
                onRemindDebtor = onRemindDebtor,
                onAddDebtPayment = onAddDebtPayment,
                onAccelerateDebtPayment = onAccelerateDebtPayment,
                hideAmounts = privacySettings.hideAmounts,
                onEditDebtor = onEditDebtor,
                onDeleteDebtor = onDeleteDebtor,
                onViewContactHistory = onViewContactHistory,
                onSyncCalendar = onSyncCalendar,
                onRemoveCalendar = onRemoveCalendar,
                onConfirmTransfer = onConfirmTransfer,
                onDeleteTransfer = onDeleteTransfer,
                onConfirmExpense = onConfirmExpense,
                onDeleteExpense = onDeleteExpense,
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun RowActions(
    title: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
        }
    }
}

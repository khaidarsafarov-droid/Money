package com.example.money.ui.home

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.BudgetSummary
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.domain.sync.SyncConnectionState
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalsSectionSummary
import com.example.money.ui.components.HomePreviewCards
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.debtors.DebtTab
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GoalProgress
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.GamificationState
import com.example.money.settings.PrivacySettings
import com.example.money.ui.debtors.DebtCalendarTab
import com.example.money.ui.debtors.DebtCheckInSheet
import com.example.money.ui.debtors.DebtFreedomTab
import com.example.money.ui.debtors.GamificationBanner
import com.example.money.ui.debtors.DebtTabRow
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtSearchField
import com.example.money.ui.debtors.DebtorFilterRow
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.GoldPrimary
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
    debtProgressPercent: Int = 0,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onExpense: () -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onAccelerateDebtPayment: (Debtor, Long) -> Unit,
    onMarkDebtPaid: (Debtor) -> Unit = {},
    freedomProjection: DebtFreedomEngine.FreedomProjection,
    extraMonthlyCents: Long,
    onExtraMonthlyChange: (Long) -> Unit,
    gamificationState: GamificationState = GamificationState(),
    privacySettings: PrivacySettings = PrivacySettings(),
    autopilotSettings: AutopilotSettings = AutopilotSettings(),
    onOpenAdvisor: () -> Unit = {},
    onApplyAutopilot: () -> Unit = {},
    showCheckIn: Boolean = false,
    onDismissCheckIn: () -> Unit = {},
    onOpenGoalHub: () -> Unit = {},
    onOpenAppLanguage: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenAutopilotSettings: () -> Unit = {},
    onOpenCalendarSettings: () -> Unit = {},
    onOpenDebtNotificationSettings: () -> Unit = {},
    onOpenBackupSettings: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {},
    onOpenIncomeIdeas: () -> Unit = {},
    calendarPermissionGranted: Boolean = true,
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onAddDebt: () -> Unit = {},
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
                val railColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = GoldPrimary,
                    selectedTextColor = GoldPrimary,
                    indicatorColor = Color(0xFFD9F6E6),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var showSettingsMenu by remember { mutableStateOf(false) }
                SyncStatusIndicator(
                    state = syncState,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                listOf(HomeSection.Home, HomeSection.Operations, HomeSection.Debts).forEach { item ->
                    NavigationRailItem(
                        selected = section == item,
                        onClick = {
                            onSectionChange(item)
                            onSelectionChange(HomeListSelection.Overview)
                        },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    HomeSection.Home -> Icons.Outlined.Home
                                    HomeSection.Debts -> Icons.Outlined.People
                                    HomeSection.Operations -> Icons.AutoMirrored.Outlined.ListAlt
                                    HomeSection.Goals -> Icons.Outlined.Home
                                },
                                contentDescription = stringResource(item.titleRes),
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                        colors = railColors,
                    )
                }
                NavigationRailItem(
                    selected = false,
                    onClick = { showSettingsMenu = true },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_open),
                        )
                    },
                    label = { Text(stringResource(R.string.nav_more)) },
                    colors = railColors,
                )
                DropdownMenu(
                    expanded = showSettingsMenu,
                    onDismissRequest = { showSettingsMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.advisor_open)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenAdvisor()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.section_goals)) },
                        onClick = {
                            showSettingsMenu = false
                            onSectionChange(HomeSection.Goals)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.backup_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenBackupSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.income_ideas_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenIncomeIdeas()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.voice_settings_menu)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenVoiceSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.autopilot_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenAutopilotSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.app_language_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenAppLanguage()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.privacy_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenPrivacySettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.debt_notification_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenDebtNotificationSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.calendar_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenCalendarSettings()
                        },
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
                        hideAmounts = privacySettings.hideAmounts,
                        progressPercent = debtProgressPercent,
                        compact = true,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }

                when (section) {
                    HomeSection.Home -> {
                        val debtActive = debtMetrics.owedToMeActiveCount + debtMetrics.iOweActiveCount
                        item {
                            HomePreviewCards(
                                goalMetrics = goalMetrics,
                                debtMetrics = debtMetrics,
                                hideAmounts = privacySettings.hideAmounts,
                                progressPercent = debtProgressPercent,
                                onDebtsClick = {
                                    onDebtTabChange(
                                        if (debtMetrics.iOweCents > debtMetrics.owedToMeCents) {
                                            DebtTab.IOwe
                                        } else {
                                            DebtTab.OwesMe
                                        },
                                    )
                                    onSectionChange(HomeSection.Debts)
                                },
                                onAccumulatedClick = { onSectionChange(HomeSection.Goals) },
                                onRemainingClick = { onSectionChange(HomeSection.Goals) },
                                onProgressClick = {
                                    onSectionChange(
                                        if (debtActive > 0) HomeSection.Debts else HomeSection.Goals,
                                    )
                                },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        if (transfers.isEmpty() && expenses.isEmpty()) {
                            item {
                                EmptyStateMessage(
                                    title = stringResource(R.string.empty_budget),
                                    hint = stringResource(R.string.empty_budget_hint),
                                )
                            }
                        } else {
                            val recent = (transfers.map { it.date to it as Any } + expenses.map { it.date to it as Any })
                                .sortedByDescending { it.first }
                                .take(5)
                            item {
                                Text(
                                    text = stringResource(R.string.operations_section_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .clickable { onSectionChange(HomeSection.Operations) },
                                )
                            }
                            items(recent, key = { (_, op) ->
                                when (op) {
                                    is Transfer -> "home-t-${op.syncId.ifBlank { "id-${op.id}" }}"
                                    is Expense -> "home-e-${op.syncId.ifBlank { "id-${op.id}" }}"
                                    else -> "home-op"
                                }
                            }) { (_, op) ->
                                when (op) {
                                    is Transfer -> {
                                        val goalName = op.goalId?.let { id -> goals.find { it.id == id }?.name }
                                        HomeListCompactRow(
                                            title = "+${Formatters.currencyCompact(op.amount)}",
                                            subtitle = goalName ?: stringResource(R.string.operation_deposit),
                                            selected = false,
                                            onClick = {
                                                onSectionChange(HomeSection.Operations)
                                                onSelectionChange(HomeListSelection.TransferItem(op, goalName))
                                            },
                                            modifier = Modifier.padding(vertical = 4.dp),
                                        )
                                    }
                                    is Expense -> HomeListCompactRow(
                                        title = "−${Formatters.currencyCompact(op.amount)}",
                                        subtitle = op.category,
                                        selected = false,
                                        onClick = {
                                            onSectionChange(HomeSection.Operations)
                                            onSelectionChange(HomeListSelection.ExpenseItem(op))
                                        },
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.Goals -> {
                        val orderedGoals = GoalProgress.orderedForSection(goals)
                        item {
                            Text(
                                text = stringResource(R.string.goals_section_title, goals.size),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        item {
                            TextButton(onClick = onOpenGoalHub) {
                                Text(stringResource(R.string.goal_hub_open))
                            }
                        }
                        if (orderedGoals.isNotEmpty()) {
                            item {
                                GoalsSectionSummary(
                                    goals = orderedGoals,
                                    hideAmounts = privacySettings.hideAmounts,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                        }
                        if (goals.isEmpty()) {
                            item {
                                EmptyStateMessage(
                                    title = stringResource(R.string.empty_goals_title),
                                    hint = stringResource(R.string.empty_goals_hint),
                                )
                            }
                        } else {
                            items(orderedGoals, key = { "tablet-goal-${it.syncId.ifBlank { "id-${it.id}" }}" }) { goal ->
                                val pace = goal.dailyGoalCents
                                val daysLeft = goal.deadlineMillis?.let { GoalProgress.daysUntilDeadline(it) }
                                val baseSubtitle = if (goal.kind == GoalKind.ACTION) {
                                    if (goal.actionTotal > 0) {
                                        stringResource(
                                            R.string.goal_action_progress,
                                            goal.actionDone,
                                            goal.actionTotal,
                                        )
                                    } else {
                                        stringResource(R.string.goal_kind_action)
                                    }
                                } else {
                                    stringResource(
                                        R.string.tablet_list_goal_subtitle,
                                        Formatters.currencyFromCents(goal.currentAmountCents, hide = privacySettings.hideAmounts),
                                        Formatters.currencyFromCents(goal.targetAmountCents, hide = privacySettings.hideAmounts),
                                        goal.progressPercent,
                                    )
                                }
                                HomeListCompactRow(
                                    title = goal.name,
                                    subtitle = if (!goal.isCompleted && pace != null && daysLeft != null) {
                                        baseSubtitle + "\n" + stringResource(
                                            R.string.goal_pace_line,
                                            Formatters.currencyFromCents(pace, hide = privacySettings.hideAmounts),
                                            daysLeft,
                                        )
                                    } else {
                                        baseSubtitle
                                    },
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
                        if (autopilotSettings.enabled) {
                            item {
                                TextButton(onClick = onApplyAutopilot) {
                                    Text(stringResource(R.string.autopilot_apply_now))
                                }
                            }
                        }
                        if (debtTab == DebtTab.Calendar) {
                            item {
                                DebtCalendarTab(
                                    debtors = allDebtors,
                                    hideAmounts = privacySettings.hideAmounts,
                                    calendarPermissionGranted = calendarPermissionGranted,
                                    onOpenCalendarSettings = onOpenCalendarSettings,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else if (debtTab == DebtTab.Freedom) {
                            item {
                                DebtFreedomTab(
                                    projection = freedomProjection,
                                    extraMonthlyCents = extraMonthlyCents,
                                    onExtraMonthlyChange = onExtraMonthlyChange,
                                    hideAmounts = privacySettings.hideAmounts,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        } else {
                            if (showCheckIn) {
                                item {
                                    DebtCheckInSheet(
                                        debtors = allDebtors,
                                        hideAmounts = privacySettings.hideAmounts,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                                item {
                                    TextButton(onClick = onDismissCheckIn) {
                                        Text(stringResource(R.string.action_close))
                                    }
                                }
                            }
                            item {
                                GamificationBanner(
                                    state = gamificationState,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
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
                onOpenGoal = onOpenGoal,
                onDeleteGoal = onDeleteGoal,
                onGoalTransfer = onGoalTransfer,
                onExpense = onExpense,
                onRemindDebtor = onRemindDebtor,
                onAddDebtPayment = onAddDebtPayment,
                onAccelerateDebtPayment = onAccelerateDebtPayment,
                onMarkDebtPaid = onMarkDebtPaid,
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


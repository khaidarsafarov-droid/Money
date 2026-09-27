package com.example.money.ui.home

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.ui.budget.AddExpenseDialog
import com.example.money.ui.budget.AddTransferDialog
import com.example.money.ui.budget.BudgetActionResult
import com.example.money.ui.budget.BudgetViewModel
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.components.ConfirmDeleteDialog
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.components.GoalsSectionSummary
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.components.SwipeToDeleteContainer
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.GoalProgress
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.domain.util.MoneyMath
import com.example.money.ui.debtors.ContactDebtHistorySheet
import com.example.money.ui.debtors.DebtAdvisorBottomSheet
import com.example.money.ui.debtors.DebtCelebrationDialog
import com.example.money.ui.debtors.DebtListViewModel
import com.example.money.ui.debtors.DebtMessageChannelSheet
import com.example.money.ui.debtors.DebtPaymentDialog
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtorFormDialog
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.debtors.IncomeIdeasDialog
import com.example.money.ui.goal.GoalFormDialog
import com.example.money.ui.goal.GoalViewModel
import com.example.money.settings.UserProfile
import androidx.compose.runtime.rememberCoroutineScope
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.theme.StatusActiveBg
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.isTabletWidth
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.theme.moneySubheadingStyle
import com.example.money.voice.VoiceReportEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhoneHomeLayout(
    drawerState: androidx.compose.material3.DrawerState,
    snackbarHostState: SnackbarHostState,
    topBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
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
    budgetSummary: com.example.money.domain.model.BudgetSummary,
    goalMetrics: com.example.money.domain.model.DashboardGoalMetrics,
    debtMetrics: com.example.money.domain.model.DashboardDebtMetrics,
    greetingName: String,
    debtProgressPercent: Int,
    highlightedDebtorId: Long?,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onExpense: () -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onAccelerateDebtPayment: (Debtor, Long) -> Unit,
    onMarkDebtPaid: (Debtor) -> Unit,
    freedomProjection: DebtFreedomEngine.FreedomProjection,
    extraMonthlyCents: Long,
    onExtraMonthlyChange: (Long) -> Unit,
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit,
    onSyncCalendar: (Debtor) -> Unit,
    onRemoveCalendar: (Debtor) -> Unit,
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onConfirmExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenAppLanguage: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenAutopilotSettings: () -> Unit,
    onOpenCalendarSettings: () -> Unit,
    onOpenDebtNotificationSettings: () -> Unit,
    onOpenBackupSettings: () -> Unit,
    onOpenIncomeIdeas: () -> Unit,
    onPlayVoiceReport: () -> Unit,
    calendarPermissionGranted: Boolean,
    onOpenGoalHub: () -> Unit,
    onAddDebt: () -> Unit,
    onQuickAdd: () -> Unit,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    showCheckIn: Boolean,
    onDismissCheckIn: () -> Unit,
    gamificationState: com.example.money.settings.GamificationState,
    privacySettings: com.example.money.settings.PrivacySettings,
    autopilotSettings: com.example.money.settings.AutopilotSettings,
    onOpenAdvisor: () -> Unit,
    onApplyAutopilot: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    emptyDebtorsTitle: @Composable (DebtorFilter) -> String,
    emptyDebtorsHint: @Composable (DebtorFilter) -> String,
) {
    var showDebtsSection by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(highlightedDebtorId, showCheckIn) {
        if (highlightedDebtorId != null || showCheckIn) {
            showDebtsSection = true
        }
    }
    val activeDebtCount = allDebtors.count { !it.isPaid }
    val transactionCount = transfers.size + expenses.size
    val contactContext = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn(
                    modifier = Modifier.padding(vertical = MoneySpacing.lg),
                    contentPadding = PaddingValues(bottom = MoneySpacing.xl),
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.drawer_group_goals),
                            style = moneySubheadingStyle(),
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.xl,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.goal_add_title),
                            icon = Icons.Outlined.Flag,
                            onClick = {
                                onAddGoal()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(MoneySpacing.md))
                        Text(
                            text = stringResource(R.string.drawer_group_settings),
                            style = moneySubheadingStyle(),
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.xl,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.app_language_menu),
                            icon = Icons.Outlined.Translate,
                            onClick = {
                                onOpenAppLanguage()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.privacy_settings_title),
                            icon = Icons.Outlined.Lock,
                            onClick = {
                                onOpenPrivacySettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.autopilot_settings_title),
                            icon = Icons.Outlined.AutoMode,
                            onClick = {
                                onOpenAutopilotSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.debt_notification_settings_title),
                            icon = Icons.Outlined.Notifications,
                            onClick = {
                                onOpenDebtNotificationSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.goal_hub_open),
                            icon = Icons.Outlined.Flag,
                            onClick = {
                                onOpenGoalHub()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.calendar_settings_title),
                            icon = Icons.Outlined.CalendarMonth,
                            onClick = {
                                onOpenCalendarSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.backup_settings_title),
                            icon = Icons.Outlined.CloudUpload,
                            onClick = {
                                onOpenBackupSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(MoneySpacing.md))
                        Text(
                            text = stringResource(R.string.drawer_group_tools),
                            style = moneySubheadingStyle(),
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.xl,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.income_ideas_title),
                            icon = Icons.Outlined.Lightbulb,
                            onClick = {
                                onOpenIncomeIdeas()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.voice_play_report),
                            icon = Icons.Outlined.Mic,
                            onClick = {
                                onPlayVoiceReport()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.voice_settings_menu),
                            icon = Icons.Outlined.Mic,
                            onClick = {
                                onOpenVoiceSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = CreamBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
            floatingActionButton = floatingActionButton,
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                item {
                    Text(
                        text = if (debtProgressPercent > 0) {
                            stringResource(R.string.home_greeting_progress, greetingName, debtProgressPercent)
                        } else {
                            stringResource(R.string.home_greeting_simple, greetingName)
                        },
                        modifier = Modifier.padding(
                            horizontal = MoneySpacing.lg,
                            vertical = MoneySpacing.sm,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldPrimary,
                    )
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = MoneySpacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AssistChip(
                            onClick = onOpenIncomeIdeas,
                            label = { Text(stringResource(R.string.income_ideas_title)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lightbulb,
                                    contentDescription = null,
                                )
                            },
                        )
                        AssistChip(
                            onClick = onPlayVoiceReport,
                            label = { Text(stringResource(R.string.voice_play_report)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
                if (!notificationsEnabled) {
                    item {
                        NotificationPermissionBanner(
                            onRequestPermission = onRequestNotificationPermission,
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                }
                item {
                    GlobalBalanceHeader(
                        summary = budgetSummary,
                        goalMetrics = goalMetrics,
                        debtMetrics = debtMetrics,
                        hideAmounts = privacySettings.hideAmounts,
                        onOwesMeClick = {
                            showDebtsSection = true
                            onTabSelected(HomeTab.Debts.ordinal)
                            onDebtTabChange(DebtTab.OwesMe)
                        },
                        onIOweClick = {
                            showDebtsSection = true
                            onTabSelected(HomeTab.Debts.ordinal)
                            onDebtTabChange(DebtTab.IOwe)
                        },
                        modifier = Modifier.padding(
                            horizontal = MoneySpacing.lg,
                            vertical = MoneySpacing.sm,
                        ),
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(MoneySpacing.sm))
                }

                item {
                    SecondaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CreamBackground,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        indicator = {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTab),
                                color = GoldPrimary,
                                height = 3.dp,
                            )
                        },
                        divider = {},
                    ) {
                        HomeTab.entries.forEachIndexed { index, tab ->
                            val selected = selectedTab == index
                            Tab(
                                selected = selected,
                                onClick = { onTabSelected(index) },
                                modifier = Modifier.then(
                                    if (selected) {
                                        Modifier.background(
                                            StatusActiveBg,
                                            MaterialTheme.shapes.small,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                                text = {
                                    Text(
                                        text = stringResource(tab.titleRes),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }

                val orderedGoals = GoalProgress.orderedForSection(goals)
                if (HomeTab.entries[selectedTab] == HomeTab.Goals) {
                item {
                    Text(
                        text = stringResource(R.string.goals_section_title, goals.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = MoneySpacing.lg),
                    )
                }

                if (orderedGoals.isNotEmpty()) {
                    item {
                        GoalsSectionSummary(
                            goals = orderedGoals,
                            hideAmounts = privacySettings.hideAmounts,
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                }

                if (goals.isEmpty()) {
                    item {
                        EmptyStateMessage(
                            title = stringResource(R.string.empty_goals_title),
                            hint = stringResource(R.string.empty_goals_hint),
                            actionLabel = stringResource(R.string.empty_goals_action),
                            onAction = onAddGoal,
                            icon = Icons.Outlined.Flag,
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                } else {
                    items(orderedGoals, key = { "goal-${it.syncId.ifBlank { "id-${it.id}" }}" }) { goal ->
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteGoal(goal) },
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        ) {
                            GoalListItem(
                                goal = goal,
                                onEdit = { onEditGoal(goal) },
                                onOpen = { onOpenGoal(goal) },
                                onDelete = { onDeleteGoal(goal) },
                                onTransfer = if (!goal.isCompleted) {
                                    { onGoalTransfer(goal) }
                                } else {
                                    null
                                },
                                onExpense = onExpense,
                                hideAmounts = privacySettings.hideAmounts,
                            )
                        }
                    }
                }
                }

                if (HomeTab.entries[selectedTab] == HomeTab.Debts) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDebtsSection = !showDebtsSection }
                            .padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.dashboard_debts_section),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(
                                    R.string.dashboard_debts_summary,
                                    activeDebtCount,
                                    transactionCount,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            imageVector = if (showDebtsSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(
                                if (showDebtsSection) R.string.cd_collapse_debts else R.string.cd_expand_debts,
                            ),
                            tint = GoldPrimary,
                        )
                    }
                }

                if (showDebtsSection) {
                homeDebtsSection(
                        debtTab = debtTab,
                        onDebtTabChange = onDebtTabChange,
                        allDebtors = allDebtors,
                        debtors = debtors,
                        debtorFilter = debtorFilter,
                        onDebtorFilterChange = onDebtorFilterChange,
                        debtSearchQuery = debtSearchQuery,
                        onDebtSearchQueryChange = onDebtSearchQueryChange,
                        freedomProjection = freedomProjection,
                        extraMonthlyCents = extraMonthlyCents,
                        onExtraMonthlyChange = onExtraMonthlyChange,
                        showCheckIn = showCheckIn,
                        onDismissCheckIn = onDismissCheckIn,
                        gamificationState = gamificationState,
                        privacySettings = privacySettings,
                        autopilotSettings = autopilotSettings,
                        onOpenAdvisor = onOpenAdvisor,
                        onApplyAutopilot = onApplyAutopilot,
                        onOpenGoalHub = onOpenGoalHub,
                        onOpenIncomeIdeas = onOpenIncomeIdeas,
                        calendarPermissionGranted = calendarPermissionGranted,
                        onOpenCalendarSettings = onOpenCalendarSettings,
                        highlightedDebtorId = highlightedDebtorId,
                        contactContext = contactContext,
                        onRemindDebtor = onRemindDebtor,
                        onAddDebtPayment = onAddDebtPayment,
                        onAccelerateDebtPayment = onAccelerateDebtPayment,
                        onMarkDebtPaid = onMarkDebtPaid,
                        onEditDebtor = onEditDebtor,
                        onDeleteDebtor = onDeleteDebtor,
                        onViewContactHistory = onViewContactHistory,
                        onSyncCalendar = onSyncCalendar,
                        onRemoveCalendar = onRemoveCalendar,
                        onAddDebt = onAddDebt,
                        emptyDebtorsTitle = emptyDebtorsTitle,
                        emptyDebtorsHint = emptyDebtorsHint,
                    )
                }
                if (HomeTab.entries[selectedTab] == HomeTab.BrotherFinances) {
                    homeOperationsSection(
                        transfers = transfers,
                        expenses = expenses,
                        goals = goals,
                        onQuickAdd = onQuickAdd,
                        onConfirmTransfer = onConfirmTransfer,
                        onDeleteTransfer = onDeleteTransfer,
                        onConfirmExpense = onConfirmExpense,
                        onDeleteExpense = onDeleteExpense,
                    )
                }
            }
        }
    }
    }
}

@Composable
internal fun NotificationPermissionBanner(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CreamSurface),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(MoneySpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(MoneySpacing.sm),
        ) {
            Text(
                text = stringResource(R.string.notification_permission_needed),
                style = MaterialTheme.typography.bodyMedium,
            )
            MoneyButton(
                text = stringResource(R.string.notification_permission_action),
                onClick = onRequestPermission,
                priority = MoneyButtonPriority.Secondary,
            )
        }
    }
}

@Composable
internal fun SettingsDrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        modifier = Modifier.padding(horizontal = MoneySpacing.md),
    )
}

@Composable
internal fun emptyDebtorsTitle(filter: DebtorFilter): String = when (filter) {
    DebtorFilter.ALL -> stringResource(R.string.empty_debtors_all)
    DebtorFilter.ACTIVE -> stringResource(R.string.empty_debtors_active)
    DebtorFilter.OVERDUE -> stringResource(R.string.empty_debtors_overdue)
    DebtorFilter.PAID -> stringResource(R.string.empty_debtors_paid)
}

@Composable
internal fun emptyDebtorsHint(filter: DebtorFilter): String = when (filter) {
    DebtorFilter.ALL -> stringResource(R.string.empty_debtors_all_hint)
    DebtorFilter.ACTIVE -> stringResource(R.string.empty_debtors_active_hint)
    DebtorFilter.OVERDUE -> stringResource(R.string.empty_debtors_overdue_hint)
    DebtorFilter.PAID -> stringResource(R.string.empty_debtors_paid_hint)
}

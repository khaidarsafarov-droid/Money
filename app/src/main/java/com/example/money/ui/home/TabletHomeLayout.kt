package com.example.money.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.DashboardDebtMetrics
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteMark
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.sync.SyncConnectionState
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.notes.NotesPage
import com.example.money.ui.components.GoalsSectionSummary
import com.example.money.ui.components.HomePreviewCards
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.debtors.DebtTab
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GoalProgress
import com.example.money.settings.AppMode
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
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.HapticStrength
import com.example.money.ui.util.vibrate

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
    goalActions: List<com.example.money.domain.model.GoalAction> = emptyList(),
    debtors: List<Debtor>,
    allDebtors: List<Debtor>,
    debtTab: DebtTab,
    onDebtTabChange: (DebtTab) -> Unit,
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    goalMetrics: DashboardGoalMetrics,
    debtMetrics: DashboardDebtMetrics,
    planProgressPercent: Int = 0,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onLogGoal: (SavingsGoal) -> Unit = {},
    onExpense: (SavingsGoal) -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onQuickPayment: (Debtor) -> Unit = {},
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
    onOpenSyncSettings: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {},
    onOpenIncomeIdeas: () -> Unit = {},
    onOpenCurrency: () -> Unit = {},
    notes: List<FinanceNote> = emptyList(),
    noteLines: List<FinanceNoteLine> = emptyList(),
    openNoteId: Long? = null,
    onOpenNote: (Long) -> Unit = {},
    onCloseNote: () -> Unit = {},
    onUpdateNote: (FinanceNote) -> Unit = {},
    onSetMark: (FinanceNote, NoteMark) -> Unit = { _, _ -> },
    onSetClosed: (FinanceNote, Boolean) -> Unit = { _, _ -> },
    onDeleteNote: (Long) -> Unit = {},
    onAddLine: (Long, String, Long) -> Unit = { _, _, _ -> },
    onUpdateLine: (FinanceNoteLine) -> Unit = {},
    onDeleteLine: (Long, Long) -> Unit = { _, _ -> },
    onNoteTitleChange: (FinanceNote, String) -> Unit = { _, _ -> },
    onNoteBodyChange: (FinanceNote, String) -> Unit = { _, _ -> },
    onFlushNoteDraft: (FinanceNote, String, String) -> Unit = { _, _, _ -> },
    noteDraftBlocksRemote: (Long) -> Boolean = { false },
    calendarPermissionGranted: Boolean = true,
    onRequestCalendarPermission: () -> Unit = {},
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onAddDebt: () -> Unit = {},
    onViewContactHistory: (Debtor) -> Unit = {},
    onSyncCalendar: (Debtor) -> Unit = {},
    onRemoveCalendar: (Debtor) -> Unit = {},
    emptyDebtorsTitle: @Composable (DebtorFilter) -> String,
    emptyDebtorsHint: @Composable (DebtorFilter) -> String,
    appMode: AppMode = AppMode.PERSONAL,
    onOpenClients: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = topBar,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val safeTop = innerPadding.calculateTopPadding()
        val safeBottom = innerPadding.calculateBottomPadding()
        val safeStart = innerPadding.calculateStartPadding(layoutDirection)
        val safeEnd = innerPadding.calculateEndPadding(layoutDirection)
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.padding(start = safeStart, top = safeTop, bottom = safeBottom),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                val railColors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                var showSettingsMenu by remember { mutableStateOf(false) }
                SyncStatusIndicator(
                    state = syncState,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                val railContext = LocalContext.current
                listOf(HomeSection.Home, HomeSection.Goals, HomeSection.Debts, HomeSection.Notes).forEach { item ->
                    val selectedItem = section == item
                    NavigationRailItem(
                        selected = selectedItem,
                        onClick = {
                            if (!selectedItem) vibrate(railContext, HapticStrength.MEDIUM)
                            onSectionChange(item)
                            onSelectionChange(HomeListSelection.Overview)
                        },
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    HomeSection.Home -> if (selectedItem) Icons.Filled.Home else Icons.Outlined.Home
                                    HomeSection.Goals -> if (selectedItem) Icons.Filled.Flag else Icons.Outlined.Flag
                                    HomeSection.Debts -> if (selectedItem) Icons.Filled.People else Icons.Outlined.People
                                    HomeSection.Notes -> if (selectedItem) Icons.Filled.EditNote else Icons.Outlined.EditNote
                                },
                                contentDescription = stringResource(item.titleRes),
                            )
                        },
                        label = { Text(stringResource(item.titleRes)) },
                        colors = railColors,
                    )
                }
                if (appMode == AppMode.PROFESSIONAL) {
                    NavigationRailItem(
                        selected = false,
                        onClick = onOpenClients,
                        icon = {
                            Icon(
                                Icons.Outlined.Work,
                                contentDescription = stringResource(R.string.clients_title),
                            )
                        },
                        label = { Text(stringResource(R.string.clients_title)) },
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
                        text = { Text(stringResource(R.string.currency_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenCurrency()
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
                        text = { Text(stringResource(R.string.sync_settings_title)) },
                        onClick = {
                            showSettingsMenu = false
                            onOpenSyncSettings()
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

            if (section == HomeSection.Notes) {
                NotesPage(
                    notes = notes,
                    lines = noteLines,
                    openNoteId = openNoteId,
                    onOpenNote = onOpenNote,
                    onCloseNote = onCloseNote,
                    onUpdateNote = onUpdateNote,
                    onSetMark = onSetMark,
                    onSetClosed = onSetClosed,
                    onDeleteNote = onDeleteNote,
                    onAddLine = onAddLine,
                    onUpdateLine = onUpdateLine,
                    onDeleteLine = onDeleteLine,
                    hideAmounts = privacySettings.hideAmounts,
                    onNoteTitleChange = onNoteTitleChange,
                    onNoteBodyChange = onNoteBodyChange,
                    onFlushNoteDraft = onFlushNoteDraft,
                    noteDraftBlocksRemote = noteDraftBlocksRemote,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(top = safeTop, bottom = safeBottom, end = safeEnd)
                        .consumeWindowInsets(innerPadding)
                        .imePadding(),
                )
            } else LazyColumn(
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight()
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = safeTop + 8.dp,
                    bottom = safeBottom + 8.dp,
                ),
            ) {
                when (section) {
                    HomeSection.Home -> {
                        item {
                            HomePreviewCards(
                                goalMetrics = goalMetrics,
                                debtMetrics = debtMetrics,
                                hideAmounts = privacySettings.hideAmounts,
                                progressPercent = planProgressPercent,
                                onOwesMeClick = {
                                    onDebtTabChange(DebtTab.OwesMe)
                                    onSectionChange(HomeSection.Debts)
                                },
                                onIOweClick = {
                                    onDebtTabChange(DebtTab.IOwe)
                                    onSectionChange(HomeSection.Debts)
                                },
                                onAccumulatedClick = { onSectionChange(HomeSection.Goals) },
                                onRemainingClick = { onSectionChange(HomeSection.Goals) },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }
                        homeGoalAndDebtPreview(
                            goals = goals,
                            debtors = allDebtors,
                            hideAmounts = privacySettings.hideAmounts,
                            horizontalPadding = PaddingValues(),
                            onOpenGoals = { onSectionChange(HomeSection.Goals) },
                            onOpenGoal = { goal ->
                                onSectionChange(HomeSection.Goals)
                                onOpenGoal(goal)
                            },
                            onEditGoal = onEditGoal,
                            onDeleteGoal = onDeleteGoal,
                            onGoalTransfer = onGoalTransfer,
                            onExpense = onExpense,
                            onLogGoal = onLogGoal,
                            onOpenDebts = { onSectionChange(HomeSection.Debts) },
                        )
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
                                    illustration = EmptyIllustration.Goals,
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
                                    onRequestCalendarPermission = onRequestCalendarPermission,
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
                                        illustration = if (debtSearchQuery.isNotBlank()) {
                                            EmptyIllustration.Search
                                        } else {
                                            EmptyIllustration.Debts
                                        },
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
                                        onQuickPayment = if (debtor.isPaid) {
                                            null
                                        } else {
                                            { onQuickPayment(debtor) }
                                        },
                                        modifier = Modifier.padding(vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    HomeSection.Notes -> Unit
                }
            }

            if (section != HomeSection.Notes) {
            Spacer(modifier = Modifier.width(24.dp).fillMaxHeight())

            HomeDetailPane(
                selection = selection,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                onEditGoal = onEditGoal,
                onOpenGoal = onOpenGoal,
                onDeleteGoal = onDeleteGoal,
                onGoalTransfer = onGoalTransfer,
                onLogGoal = onLogGoal,
                onExpense = onExpense,
                onRemindDebtor = onRemindDebtor,
                onAddDebtPayment = onAddDebtPayment,
                onQuickPayment = onQuickPayment,
                onAccelerateDebtPayment = onAccelerateDebtPayment,
                onMarkDebtPaid = onMarkDebtPaid,
                hideAmounts = privacySettings.hideAmounts,
                progressPercent = planProgressPercent,
                onEditDebtor = onEditDebtor,
                onDeleteDebtor = onDeleteDebtor,
                onViewContactHistory = onViewContactHistory,
                onSyncCalendar = onSyncCalendar,
                onRemoveCalendar = onRemoveCalendar,
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .padding(top = safeTop, bottom = safeBottom, end = safeEnd)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
            )
            }
        }
    }
}


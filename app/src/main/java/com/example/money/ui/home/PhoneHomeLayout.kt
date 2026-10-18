package com.example.money.ui.home

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.FinanceNote
import com.example.money.domain.model.FinanceNoteLine
import com.example.money.domain.model.NoteMark
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.budget.AddExpenseDialog
import com.example.money.ui.budget.AddTransferDialog
import com.example.money.ui.budget.BudgetActionResult
import com.example.money.ui.budget.BudgetViewModel
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.components.ConfirmDeleteDialog
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.components.HomePreviewCards
import com.example.money.ui.components.GoalsSectionSummary
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.components.SwipeToDeleteContainer
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
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.debtors.IncomeIdeasDialog
import com.example.money.ui.goal.GoalFormDialog
import com.example.money.ui.notes.NotesPage
import com.example.money.ui.goal.GoalViewModel
import com.example.money.settings.AppMode
import com.example.money.settings.UserProfile
import androidx.compose.runtime.rememberCoroutineScope
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.isTabletWidth
import com.example.money.ui.util.systemMotionEnabled
import com.example.money.ui.theme.moneySubheadingStyle
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
    goalActions: List<com.example.money.domain.model.GoalAction> = emptyList(),
    debtors: List<Debtor>,
    allDebtors: List<Debtor>,
    debtTab: DebtTab,
    onDebtTabChange: (DebtTab) -> Unit,
    debtorFilter: DebtorFilter,
    onDebtorFilterChange: (DebtorFilter) -> Unit,
    debtSearchQuery: String,
    onDebtSearchQueryChange: (String) -> Unit,
    goalMetrics: com.example.money.domain.model.DashboardGoalMetrics,
    debtMetrics: com.example.money.domain.model.DashboardDebtMetrics,
    greetingName: String,
    planProgressPercent: Int,
    highlightedDebtorId: Long?,
    onAddGoal: () -> Unit,
    onEditGoal: (SavingsGoal) -> Unit,
    onOpenGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onGoalTransfer: (SavingsGoal) -> Unit,
    onLogGoal: (SavingsGoal) -> Unit,
    onExpense: (SavingsGoal) -> Unit,
    onRemindDebtor: (Debtor) -> Unit,
    onAddDebtPayment: (Debtor) -> Unit,
    onOpenDebtor: (Debtor) -> Unit = {},
    onAccelerateDebtPayment: (Debtor, Long) -> Unit,
    onMarkDebtPaid: (Debtor) -> Unit,
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onViewContactHistory: (Debtor) -> Unit,
    onSyncCalendar: (Debtor) -> Unit,
    onRemoveCalendar: (Debtor) -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenAppLanguage: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenAutopilotSettings: () -> Unit,
    onOpenCalendarSettings: () -> Unit,
    onOpenDebtNotificationSettings: () -> Unit,
    onOpenBackupSettings: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenIncomeIdeas: () -> Unit,
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
    calendarPermissionGranted: Boolean,
    onRequestCalendarPermission: () -> Unit = {},
    onOpenGoalHub: () -> Unit,
    onAddDebt: () -> Unit,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismissNotificationPrompt: () -> Unit = {},
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
    appMode: AppMode = AppMode.PERSONAL,
    onOpenClients: () -> Unit = {},
) {
    val contactContext = LocalContext.current
    val homeListState = rememberLazyListState()
    val goalsListState = rememberLazyListState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                LazyColumn(
                    modifier = Modifier.padding(vertical = MoneySpacing.lg),
                    contentPadding = PaddingValues(bottom = MoneySpacing.xl),
                ) {
                    if (appMode == AppMode.PROFESSIONAL) {
                        item {
                            SettingsDrawerItem(
                                label = stringResource(R.string.clients_title),
                                icon = Icons.Outlined.Work,
                                onClick = {
                                    onOpenClients()
                                    scope.launch { drawerState.close() }
                                },
                            )
                        }
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.advisor_open),
                            icon = Icons.Outlined.Psychology,
                            onClick = {
                                onOpenAdvisor()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        DrawerGroupLabel(stringResource(R.string.drawer_group_data))
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.section_goals),
                            icon = Icons.Outlined.Flag,
                            onClick = {
                                onTabSelected(HomeTab.Goals.ordinal)
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
                        DrawerGroupLabel(stringResource(R.string.drawer_group_tools))
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
                            label = stringResource(R.string.voice_settings_menu),
                            icon = Icons.Outlined.Mic,
                            onClick = {
                                onOpenVoiceSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                    item {
                        DrawerGroupLabel(stringResource(R.string.drawer_group_autopilot))
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
                        DrawerGroupLabel(stringResource(R.string.drawer_group_settings))
                    }
                    item {
                        SettingsDrawerItem(
                            label = stringResource(R.string.currency_settings_title),
                            icon = Icons.Outlined.Payments,
                            onClick = {
                                onOpenCurrency()
                                scope.launch { drawerState.close() }
                            },
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
                            label = stringResource(R.string.sync_settings_title),
                            icon = Icons.Outlined.Sync,
                            onClick = {
                                onOpenSyncSettings()
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
                            label = stringResource(R.string.calendar_settings_title),
                            icon = Icons.Outlined.CalendarMonth,
                            onClick = {
                                onOpenCalendarSettings()
                                scope.launch { drawerState.close() }
                            },
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
            floatingActionButton = floatingActionButton,
            bottomBar = {
                val dockTab = when (HomeTab.entries[selectedTab]) {
                    HomeTab.Home -> PhoneDockTab.Home
                    HomeTab.Goals -> PhoneDockTab.Goals
                    HomeTab.Debts -> PhoneDockTab.Debts
                    HomeTab.Notes -> PhoneDockTab.Notes
                }
                PhoneDockBar(
                    selected = dockTab,
                    onHome = {
                        onTabSelected(HomeTab.Home.ordinal)
                        scope.launch { homeListState.animateScrollToItem(0) }
                    },
                    onGoals = { onTabSelected(HomeTab.Goals.ordinal) },
                    onDebts = { onTabSelected(HomeTab.Debts.ordinal) },
                    onNotes = { onTabSelected(HomeTab.Notes.ordinal) },
                )
            },
        ) { innerPadding ->
            val motionEnabled = systemMotionEnabled(LocalContext.current)
            val tabSlide = tween<IntOffset>(durationMillis = 250)
            val tabFade = tween<Float>(durationMillis = 250)
            val layoutDirection = LocalLayoutDirection.current
            val listPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            )
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (!motionEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val forward = targetState > initialState
                        val enterOffset: (Int) -> Int = { width -> if (forward) width / 5 else -width / 5 }
                        val exitOffset: (Int) -> Int = { width -> if (forward) -width / 5 else width / 5 }
                        (slideInHorizontally(tabSlide, enterOffset) + fadeIn(tabFade)) togetherWith
                            (slideOutHorizontally(tabSlide, exitOffset) + fadeOut(tabFade))
                    }
                },
                label = "phoneHomeTab",
            ) { tabIndex ->
            val page = HomeTab.entries[tabIndex]
            if (page == HomeTab.Notes) {
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
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .imePadding(),
                )
            } else LazyColumn(
                state = when (page) {
                    HomeTab.Home -> homeListState
                    HomeTab.Goals -> goalsListState
                    HomeTab.Debts -> listState
                    HomeTab.Notes -> listState
                },
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
                contentPadding = listPadding,
            ) {
                if (page == HomeTab.Home) {
                item {
                    Text(
                        text = stringResource(R.string.home_greeting_simple, greetingName),
                        modifier = Modifier.padding(
                            horizontal = MoneySpacing.lg,
                            vertical = MoneySpacing.sm,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                }
                if (!notificationsEnabled) {
                    item {
                        NotificationPermissionBanner(
                            onRequestPermission = onRequestNotificationPermission,
                            onDismiss = onDismissNotificationPrompt,
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                }
                if (page == HomeTab.Home) {
                    item {
                        HomePreviewCards(
                            goalMetrics = goalMetrics,
                            debtMetrics = debtMetrics,
                            hideAmounts = privacySettings.hideAmounts,
                            progressPercent = planProgressPercent,
                            onOwesMeClick = {
                                onDebtTabChange(DebtTab.OwesMe)
                                onTabSelected(HomeTab.Debts.ordinal)
                            },
                            onIOweClick = {
                                onDebtTabChange(DebtTab.IOwe)
                                onTabSelected(HomeTab.Debts.ordinal)
                            },
                            onAccumulatedClick = { onTabSelected(HomeTab.Goals.ordinal) },
                            onRemainingClick = { onTabSelected(HomeTab.Goals.ordinal) },
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                    homeGoalAndDebtPreview(
                        goals = goals,
                        debtors = allDebtors,
                        hideAmounts = privacySettings.hideAmounts,
                        horizontalPadding = PaddingValues(horizontal = MoneySpacing.lg),
                        onOpenGoals = { onTabSelected(HomeTab.Goals.ordinal) },
                        onOpenGoal = { goal ->
                            onTabSelected(HomeTab.Goals.ordinal)
                            onOpenGoal(goal)
                        },
                        onEditGoal = onEditGoal,
                        onDeleteGoal = onDeleteGoal,
                        onGoalTransfer = onGoalTransfer,
                        onExpense = onExpense,
                        onLogGoal = onLogGoal,
                        onOpenDebts = { onTabSelected(HomeTab.Debts.ordinal) },
                    )
                }

                val orderedGoals = GoalProgress.orderedForSection(goals)
                if (page == HomeTab.Goals) {
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
                            illustration = EmptyIllustration.Goals,
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
                                onTransfer = if (!goal.isCompleted && goal.kind == GoalKind.MONEY) {
                                    { onGoalTransfer(goal) }
                                } else {
                                    null
                                },
                                onExpense = { onExpense(goal) },
                                onLog = { onLogGoal(goal) },
                                hideAmounts = privacySettings.hideAmounts,
                            )
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = onOpenGoalHub,
                        modifier = Modifier.padding(horizontal = MoneySpacing.sm),
                    ) {
                        Text(stringResource(R.string.goal_hub_open))
                    }
                }
                }

                if (page == HomeTab.Debts) {
                homeDebtsSection(
                        debtTab = debtTab,
                        onDebtTabChange = onDebtTabChange,
                        allDebtors = allDebtors,
                        debtors = debtors,
                        debtorFilter = debtorFilter,
                        onDebtorFilterChange = onDebtorFilterChange,
                        debtSearchQuery = debtSearchQuery,
                        onDebtSearchQueryChange = onDebtSearchQueryChange,
                        showCheckIn = showCheckIn,
                        onDismissCheckIn = onDismissCheckIn,
                        gamificationState = gamificationState,
                        privacySettings = privacySettings,
                        autopilotSettings = autopilotSettings,
                        onApplyAutopilot = onApplyAutopilot,
                        calendarPermissionGranted = calendarPermissionGranted,
                        onOpenCalendarSettings = onOpenCalendarSettings,
                        onRequestCalendarPermission = onRequestCalendarPermission,
                        highlightedDebtorId = highlightedDebtorId,
                        contactContext = contactContext,
                        onRemindDebtor = onRemindDebtor,
                        onAddDebtPayment = onAddDebtPayment,
                        onOpenDebtor = onOpenDebtor,
                        onAccelerateDebtPayment = onAccelerateDebtPayment,
                        onMarkDebtPaid = onMarkDebtPaid,
                        onEditDebtor = onEditDebtor,
                        onDeleteDebtor = onDeleteDebtor,
                        onViewContactHistory = onViewContactHistory,
                        onSyncCalendar = onSyncCalendar,
                        onRemoveCalendar = onRemoveCalendar,
                        emptyDebtorsTitle = emptyDebtorsTitle,
                        emptyDebtorsHint = emptyDebtorsHint,
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
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_later))
            }
        }
    }
}

@Composable
private fun DrawerGroupLabel(text: String) {
    Text(
        text = text,
        style = moneySubheadingStyle(),
        modifier = Modifier.padding(
            start = MoneySpacing.xl,
            end = MoneySpacing.xl,
            top = MoneySpacing.md,
            bottom = MoneySpacing.sm,
        ),
    )
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

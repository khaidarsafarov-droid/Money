package com.example.money.ui.home

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.money.ui.theme.MoneyMotion
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
import com.example.money.ui.components.EmptyIllustration
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.components.HomePreviewCards
import com.example.money.ui.components.GoalsSectionSummary
import com.example.money.ui.components.SyncStatusIndicator
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
import com.example.money.ui.debtors.DebtorSort
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
    onAddDebt: () -> Unit,
    notificationsEnabled: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onDismissNotificationPrompt: () -> Unit = {},
    showCheckIn: Boolean,
    onDismissCheckIn: () -> Unit,
    privacySettings: com.example.money.settings.PrivacySettings,
    autopilotSettings: com.example.money.settings.AutopilotSettings,
    onOpenAdvisor: () -> Unit,
    onApplyAutopilot: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    emptyDebtorsTitle: @Composable (DebtorFilter) -> String,
    appMode: AppMode = AppMode.PERSONAL,
    onAppModeChange: (AppMode) -> Unit = {},
    onOpenCheckIn: () -> Unit = {},
    onOpenClients: () -> Unit = {},
    showClients: Boolean = false,
    onToggleClients: () -> Unit = {},
    onImportCamera: (() -> Unit)? = null,
    onImportGallery: (() -> Unit)? = null,
    openDebtSearch: Boolean = false,
    onDebtSearchOpened: () -> Unit = {},
) {
    val contactContext = LocalContext.current
    val homeListState = rememberLazyListState()
    val goalsListState = rememberLazyListState()
    var returnedPicker by remember { mutableStateOf(false) }
    val activeDebts = DebtorSort.sort(allDebtors.filter { !it.isPaid })

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = topBar,
            floatingActionButton = floatingActionButton,
            bottomBar = {
                val dockTab = when (HomeTab.entries[selectedTab]) {
                    HomeTab.Home -> PhoneDockTab.Home
                    HomeTab.Debts -> PhoneDockTab.Debts
                    HomeTab.Goals -> PhoneDockTab.Goals
                    HomeTab.Notes, HomeTab.More -> PhoneDockTab.More
                }
                PhoneDockBar(
                    selected = dockTab,
                    onHome = {
                        onTabSelected(HomeTab.Home.ordinal)
                        scope.launch { homeListState.animateScrollToItem(0) }
                    },
                    onDebts = { onTabSelected(HomeTab.Debts.ordinal) },
                    onGoals = { onTabSelected(HomeTab.Goals.ordinal) },
                    onMore = { onTabSelected(HomeTab.More.ordinal) },
                )
            },
        ) { innerPadding ->
            val motionEnabled = systemMotionEnabled(LocalContext.current)
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
                        fadeIn(
                            tween(
                                MoneyMotion.fadeIn,
                                delayMillis = MoneyMotion.fadeOut,
                                easing = MoneyMotion.emphasizedDecelerate,
                            ),
                        ) togetherWith fadeOut(
                            tween(MoneyMotion.fadeOut, easing = MoneyMotion.emphasizedAccelerate),
                        )
                    }
                },
                label = "phoneHomeTab",
            ) { tabIndex ->
            CompositionLocalProvider(com.example.money.ui.motion.LocalMoneyVisibility provides this) {
            val page = HomeTab.entries[tabIndex]
            if (page == HomeTab.More) {
                MorePage(
                    appMode = appMode,
                    onAppModeChange = onAppModeChange,
                    onOpenNotes = { onTabSelected(HomeTab.Notes.ordinal) },
                    onOpenClients = onOpenClients,
                    onOpenAdvisor = onOpenAdvisor,
                    onOpenBackupSettings = onOpenBackupSettings,
                    onOpenIncomeIdeas = onOpenIncomeIdeas,
                    onOpenVoiceSettings = onOpenVoiceSettings,
                    onOpenAutopilotSettings = onOpenAutopilotSettings,
                    onOpenCurrency = onOpenCurrency,
                    onOpenAppLanguage = onOpenAppLanguage,
                    onOpenSyncSettings = onOpenSyncSettings,
                    onOpenPrivacySettings = onOpenPrivacySettings,
                    onOpenDebtNotificationSettings = onOpenDebtNotificationSettings,
                    onOpenCalendarSettings = onOpenCalendarSettings,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                )
            } else if (page == HomeTab.Notes) {
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
                    HomeTab.Notes, HomeTab.More -> listState
                },
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding),
                contentPadding = PaddingValues(
                    start = listPadding.calculateStartPadding(layoutDirection),
                    top = listPadding.calculateTopPadding(),
                    end = listPadding.calculateEndPadding(layoutDirection),
                    bottom = listPadding.calculateBottomPadding() + 72.dp,
                ),
            ) {
                if (page == HomeTab.Home) {
                    homeFinora(
                        goals = goals,
                        debtors = allDebtors,
                        owedToMeCents = debtMetrics.owedToMeCents,
                        iOweCents = debtMetrics.iOweCents,
                        owedToMeCount = debtMetrics.owedToMeActiveCount,
                        iOweCount = debtMetrics.iOweActiveCount,
                        hideAmounts = privacySettings.hideAmounts,
                        onOwesMe = {
                            onDebtTabChange(DebtTab.OwesMe)
                            onDebtorFilterChange(DebtorFilter.ACTIVE)
                            onTabSelected(HomeTab.Debts.ordinal)
                        },
                        onIOwe = {
                            onDebtTabChange(DebtTab.IOwe)
                            onDebtorFilterChange(DebtorFilter.ACTIVE)
                            onTabSelected(HomeTab.Debts.ordinal)
                        },
                        onOpenGoals = { onTabSelected(HomeTab.Goals.ordinal) },
                        onOpenGoal = { goal ->
                            onTabSelected(HomeTab.Goals.ordinal)
                            onOpenGoal(goal)
                        },
                        onOpenDebts = { onTabSelected(HomeTab.Debts.ordinal) },
                        onOpenOverdue = {
                            val overdueCreditors = allDebtors.any {
                                it.debtType == DebtType.CREDITOR && !it.isPaid && it.dateDue > 0L &&
                                    com.example.money.domain.util.DebtDueDateUtils.isOverdue(it.dateDue)
                            }
                            onDebtTabChange(if (overdueCreditors) DebtTab.OwesMe else DebtTab.IOwe)
                            onDebtorFilterChange(DebtorFilter.OVERDUE)
                            onTabSelected(HomeTab.Debts.ordinal)
                        },
                        onOpenDueSoon = {
                            onDebtorFilterChange(DebtorFilter.DUE_SOON)
                            onTabSelected(HomeTab.Debts.ordinal)
                        },
                        onOpenDebtor = onOpenDebtor,
                        onReturned = {
                            when (activeDebts.size) {
                                0 -> onAddDebt()
                                1 -> onAddDebtPayment(activeDebts.first())
                                else -> returnedPicker = true
                            }
                        },
                        onAddDebt = onAddDebt,
                        onPhotoCamera = onImportCamera,
                        onPhotoGallery = onImportGallery,
                        showNotificationPrompt = !notificationsEnabled,
                        onEnableNotifications = onRequestNotificationPermission,
                        onDismissNotificationPrompt = onDismissNotificationPrompt,
                    )
                }

                val orderedGoals = GoalProgress.orderedForSection(goals)
                if (page == HomeTab.Goals) {
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
                            illustration = EmptyIllustration.Goals,
                            modifier = Modifier.padding(
                                horizontal = MoneySpacing.lg,
                                vertical = MoneySpacing.sm,
                            ),
                        )
                    }
                } else {
                    items(
                        orderedGoals,
                        key = { "goal-${it.syncId.ifBlank { "id-${it.id}" }}" },
                        contentType = { "goal" },
                    ) { goal ->
                        val motion = systemMotionEnabled(LocalContext.current)
                        GoalListItem(
                            goal = goal,
                            modifier = Modifier
                                .animateItem(
                                    fadeInSpec = MoneyMotion.spec(motion),
                                    placementSpec = MoneyMotion.springSpec(motion),
                                    fadeOutSpec = MoneyMotion.spec(motion),
                                )
                                .padding(
                                    horizontal = MoneySpacing.lg,
                                    vertical = MoneySpacing.sm,
                                ),
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
                        onOpenCheckIn = onOpenCheckIn,
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
                        appMode = appMode,
                        showClients = showClients,
                        onToggleClients = onToggleClients,
                        openSearch = openDebtSearch,
                        onSearchOpened = onDebtSearchOpened,
                    )
                }
            }
            }
            }
        }
    if (returnedPicker) {
        ModalBottomSheet(onDismissRequest = { returnedPicker = false }) {
            Text(
                text = stringResource(R.string.qa_returned),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.sm),
            )
            activeDebts.forEach { debtor ->
                FinoraDebtRow(
                    debtor = debtor,
                    hideAmounts = privacySettings.hideAmounts,
                    onClick = {
                        returnedPicker = false
                        onAddDebtPayment(debtor)
                    },
                    modifier = Modifier.padding(horizontal = MoneySpacing.lg),
                )
            }
            Spacer(Modifier.height(MoneySpacing.xl))
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
internal fun DrawerGroupLabel(text: String) {
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
    ListItem(
        headlineContent = {
            Text(
                text = label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
internal fun emptyDebtorsTitle(filter: DebtorFilter): String = when (filter) {
    DebtorFilter.ALL -> stringResource(R.string.empty_debtors_all)
    DebtorFilter.ACTIVE -> stringResource(R.string.empty_debtors_active)
    DebtorFilter.OVERDUE -> stringResource(R.string.empty_debtors_overdue)
    DebtorFilter.DUE_SOON -> stringResource(R.string.empty_debtors_active)
    DebtorFilter.PAID -> stringResource(R.string.empty_debtors_paid)
}

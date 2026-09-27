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
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.components.SwipeToDeleteContainer
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GlobalFinanceEngine
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
fun HomeScreen(
    launchConfig: HomeLaunchConfig = HomeLaunchConfig(),
    notificationsEnabled: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {},
    onOpenAppLanguage: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenAutopilotSettings: () -> Unit = {},
    onOpenCalendarSettings: () -> Unit = {},
    onOpenDebtNotificationSettings: () -> Unit = {},
    onOpenBackupSettings: () -> Unit = {},
    onOpenGoalHub: () -> Unit = {},
    onLaunchConfigConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication

    val goalViewModel: GoalViewModel = viewModel(
        factory = GoalViewModel.Factory(app.goalRepository),
    )
    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModel.Factory(app.budgetRepository),
    )
    val debtListViewModel: DebtListViewModel = viewModel(
        factory = DebtListViewModel.Factory(app.debtorRepository),
    )
    val debtorsViewModel: DebtorsViewModel = viewModel(
        factory = DebtorsViewModel.Factory(
            app.debtorRepository,
            app.debtReminderScheduler,
            app.preferencesStore,
            app.debtCalendarSyncService,
            app.personalDebtMessageScheduler,
        ),
    )

    val goals by goalViewModel.goals.collectAsStateWithLifecycle()
    val allDebtors by debtorsViewModel.allDebtorsList.collectAsStateWithLifecycle()
    val debtors by debtListViewModel.debtors.collectAsStateWithLifecycle()
    val debtTab by debtListViewModel.debtTab.collectAsStateWithLifecycle()
    val canAddDebtor = debtTab == DebtTab.OwesMe || debtTab == DebtTab.IOwe
    val debtorFilter by debtListViewModel.filter.collectAsStateWithLifecycle()
    val debtSearchQuery by debtListViewModel.searchQuery.collectAsStateWithLifecycle()
    val debtMetrics by debtorsViewModel.debtMetrics.collectAsStateWithLifecycle()
    val freedomProjection by debtListViewModel.freedomProjection.collectAsStateWithLifecycle()
    val extraMonthlyCents by debtListViewModel.extraMonthlyCents.collectAsStateWithLifecycle()
    val privacySettings by debtorsViewModel.privacySettings.collectAsStateWithLifecycle()
    val gamificationState by debtorsViewModel.gamificationState.collectAsStateWithLifecycle()
    val autopilotSettings by debtorsViewModel.autopilotSettings.collectAsStateWithLifecycle()
    val advisorMessages by debtorsViewModel.advisorMessages.collectAsStateWithLifecycle()
    val budgetSummary by budgetViewModel.budgetSummary.collectAsStateWithLifecycle()
    val availableBalanceUsd = budgetSummary.availableBalance
    val goalMetrics = remember(goals) { GlobalFinanceEngine.activeGoalMetrics(goals) }
    val transfers by budgetViewModel.transfers.collectAsStateWithLifecycle()
    val expenses by budgetViewModel.expenses.collectAsStateWithLifecycle()
    val userProfile by app.preferencesStore.userProfile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val debtProgressPercent = remember(allDebtors) {
        GoalPsychologyEngines.totalDebtProgressPercent(allDebtors)
    }

    var selectedTab by remember { mutableIntStateOf(launchConfig.initialTab.ordinal) }
    var showAddDebtorDialog by remember { mutableStateOf(false) }
    var editDebtor by remember { mutableStateOf<Debtor?>(null) }
    var showDebtPaymentDialog by remember { mutableStateOf<Debtor?>(null) }
    var debtPaymentPrefillCents by remember { mutableLongStateOf(0L) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showGoalTransferDialog by remember { mutableStateOf<SavingsGoal?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var showWelcomeDialog by remember { mutableStateOf(false) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showAdvisorSheet by remember { mutableStateOf(false) }
    var showIncomeIdeas by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }
    var pendingMarkPaid by remember { mutableStateOf<Debtor?>(null) }
    var remindDebtor by remember { mutableStateOf<Debtor?>(null) }

    val personalBranchSettings by debtorsViewModel.personalBranchSettings.collectAsStateWithLifecycle()

    val celebrationDebtor by debtorsViewModel.celebrationDebtor.collectAsStateWithLifecycle()
    val pendingCalendarClose by debtorsViewModel.pendingCalendarClose.collectAsStateWithLifecycle()
    val calendarMessage by debtorsViewModel.calendarMessage.collectAsStateWithLifecycle()
    val showCheckIn by debtorsViewModel.showCheckIn.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tabletSection by remember {
        mutableStateOf(
            when (launchConfig.initialTab) {
                HomeTab.Goals -> HomeSection.Goals
                HomeTab.Debts -> HomeSection.Debts
                HomeTab.BrotherFinances -> HomeSection.Operations
            },
        )
    }
    var tabletSelection by remember { mutableStateOf<HomeListSelection>(HomeListSelection.Overview) }
    var highlightedDebtorId by remember { mutableStateOf<Long?>(null) }
    var contactHistoryTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    val openContactHistory: (String, String) -> Unit = { lookupKey, displayName ->
        contactHistoryTarget = lookupKey to displayName
    }

    suspend fun showMessage(message: String) {
        snackbarHostState.showSnackbar(message)
    }

    LaunchedEffect(userProfile.onboardingCompleted) {
        showWelcomeDialog = userProfile.onboardingCompleted && WelcomePreferences.shouldShowWelcome(context)
    }

    LaunchedEffect(
        launchConfig.openExpenseDialog,
        launchConfig.openQuickAddDialog,
        launchConfig.initialTab,
        launchConfig.initialDebtTab,
        launchConfig.highlightDebtorId,
        launchConfig.openDebtPaymentForDebtorId,
        launchConfig.openWeeklyCheckIn,
    ) {
        selectedTab = launchConfig.initialTab.ordinal
        tabletSection = when (launchConfig.initialTab) {
            HomeTab.Goals -> HomeSection.Goals
            HomeTab.Debts -> HomeSection.Debts
            HomeTab.BrotherFinances -> HomeSection.Operations
        }
        debtListViewModel.setDebtTab(launchConfig.initialDebtTab)

        if (launchConfig.openExpenseDialog) {
            showExpenseDialog = true
        }
        if (launchConfig.openQuickAddDialog) {
            showQuickAddDialog = true
        }

        val targetDebtorId = launchConfig.openDebtPaymentForDebtorId
            ?: launchConfig.highlightDebtorId
        if (targetDebtorId != null) {
            debtListViewModel.setFilter(DebtorFilter.ALL)
            val debtor = debtorsViewModel.getDebtorById(targetDebtorId)
            if (debtor != null) {
                debtListViewModel.setDebtTab(DebtTab.forDebtType(debtor.debtType))
                highlightedDebtorId = targetDebtorId
                tabletSection = HomeSection.Debts
                tabletSelection = HomeListSelection.DebtorItem(debtor)
                if (launchConfig.openDebtPaymentForDebtorId != null && !debtor.isPaid) {
                    showDebtPaymentDialog = debtor
                }
            } else if (launchConfig.highlightDebtorId != null) {
                showMessage(context.getString(R.string.debtor_not_found))
            }
        }

        if (launchConfig.openWeeklyCheckIn) {
            debtListViewModel.setDebtTab(DebtTab.IOwe)
            debtorsViewModel.openCheckIn()
        }

        onLaunchConfigConsumed()
    }

    LaunchedEffect(highlightedDebtorId, debtors) {
        val debtorId = highlightedDebtorId ?: return@LaunchedEffect
        val index = debtors.indexOfFirst { it.id == debtorId }
        if (index >= 0) {
            listState.animateScrollToItem(index + 2)
        }
    }

    LaunchedEffect(calendarMessage) {
        when (calendarMessage?.substringBefore('#')) {
            "calendar_failed" -> showMessage(context.getString(R.string.calendar_sync_failed))
            "calendar_removed" -> showMessage(context.getString(R.string.calendar_removed))
        }
        if (calendarMessage != null) {
            debtorsViewModel.dismissCalendarMessage()
        }
    }

    val calendarSettings by debtorsViewModel.calendarSettings.collectAsStateWithLifecycle()
    var hasCalendarPermissions by remember {
        mutableStateOf(app.debtCalendarSyncService.hasPermissions())
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasCalendarPermissions = app.debtCalendarSyncService.hasPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(hasCalendarPermissions, calendarSettings.autoSyncEnabled) {
        if (hasCalendarPermissions && calendarSettings.autoSyncEnabled) {
            debtorsViewModel.retryCalendarSyncIfNeeded()
        }
    }

    if (pendingCalendarClose != null) {
        AlertDialog(
            onDismissRequest = { debtorsViewModel.dismissCalendarClosePrompt() },
            title = { Text(stringResource(R.string.calendar_close_title)) },
            text = { Text(stringResource(R.string.calendar_close_body)) },
            confirmButton = {
                TextButton(onClick = { debtorsViewModel.confirmRemoveCalendarEvent(remove = true) }) {
                    Text(stringResource(R.string.calendar_close_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { debtorsViewModel.confirmRemoveCalendarEvent(remove = false) }) {
                    Text(stringResource(R.string.calendar_close_keep_mark))
                }
            },
        )
    }

    if (showWelcomeDialog) {
        AlertDialog(
            onDismissRequest = {
                WelcomePreferences.markWelcomeSeen(context)
                showWelcomeDialog = false
            },
            title = { Text(stringResource(R.string.welcome_title)) },
            text = { Text(stringResource(R.string.welcome_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        WelcomePreferences.markWelcomeSeen(context)
                        showWelcomeDialog = false
                    },
                ) {
                    Text(stringResource(R.string.action_got_it))
                }
            },
        )
    }

    if (showAddDebtorDialog && canAddDebtor) {
        DebtorFormDialog(
            debtType = when (debtTab) {
                DebtTab.OwesMe -> DebtType.CREDITOR
                DebtTab.IOwe -> DebtType.DEBTOR
                DebtTab.Freedom, DebtTab.Calendar -> DebtType.CREDITOR
            },
            availableBalance = availableBalanceUsd,
            allDebtors = allDebtors,
            onViewContactHistory = openContactHistory,
            onDismiss = { showAddDebtorDialog = false },
            onConfirm = { name, amount, dateGiven, dateDue, affectsBalance, note, loanCategory, interestRateBps, contactLookupKey, contactUri, contactPhotoUri, phone, email ->
                debtorsViewModel.addDebtor(
                    name = name,
                    amount = amount,
                    dateGiven = dateGiven,
                    dateDue = dateDue,
                    debtType = when (debtTab) {
                        DebtTab.OwesMe -> DebtType.CREDITOR
                        DebtTab.IOwe -> DebtType.DEBTOR
                        DebtTab.Freedom, DebtTab.Calendar -> DebtType.CREDITOR
                    },
                    affectsBalance = affectsBalance,
                    note = note,
                    loanCategory = loanCategory,
                    interestRateBps = interestRateBps,
                    contactLookupKey = contactLookupKey,
                    contactUri = contactUri,
                    contactPhotoUri = contactPhotoUri,
                    phone = phone,
                    email = email,
                )
                showAddDebtorDialog = false
                scope.launch { showMessage(context.getString(R.string.msg_debtor_added)) }
            },
        )
    }
    editDebtor?.let { debtor ->
        DebtorFormDialog(
            debtType = debtor.debtType,
            existingDebtor = debtor,
            availableBalance = availableBalanceUsd,
            allDebtors = allDebtors,
            onViewContactHistory = openContactHistory,
            onDismiss = { editDebtor = null },
            onConfirm = { name, amount, dateGiven, dateDue, affectsBalance, note, loanCategory, interestRateBps, contactLookupKey, contactUri, contactPhotoUri, phone, email ->
                debtorsViewModel.updateDebtor(
                    debtor = debtor,
                    name = name,
                    amount = amount,
                    dateGiven = dateGiven,
                    dateDue = dateDue,
                    affectsBalance = affectsBalance,
                    note = note,
                    loanCategory = loanCategory,
                    interestRateBps = interestRateBps,
                    contactLookupKey = contactLookupKey,
                    contactUri = contactUri,
                    contactPhotoUri = contactPhotoUri,
                    phone = phone,
                    email = email,
                )
                editDebtor = null
                scope.launch { showMessage(context.getString(R.string.msg_debtor_updated)) }
            },
        )
    }
    contactHistoryTarget?.let { (lookupKey, displayName) ->
        ContactDebtHistorySheet(
            lookupKey = lookupKey,
            displayName = displayName,
            allDebtors = allDebtors,
            hideAmounts = privacySettings.hideAmounts,
            onDismiss = { contactHistoryTarget = null },
            onOpenDebt = { debtor ->
                contactHistoryTarget = null
                editDebtor = debtor
            },
        )
    }
    remindDebtor?.let { debtor ->
        DebtMessageChannelSheet(
            debtor = debtor,
            settings = personalBranchSettings,
            onDismiss = { remindDebtor = null },
        )
    }
    showDebtPaymentDialog?.let { debtor ->
        DebtPaymentDialog(
            debtor = debtor,
            availableBalance = availableBalanceUsd,
            initialAmountCents = debtPaymentPrefillCents,
            hideAmounts = privacySettings.hideAmounts,
            onDismiss = {
                showDebtPaymentDialog = null
                debtPaymentPrefillCents = 0L
            },
            onConfirm = { amount, affectsBalance ->
                scope.launch {
                    val ok = debtorsViewModel.processPaymentSync(debtor.id, amount, affectsBalance)
                    showDebtPaymentDialog = null
                    debtPaymentPrefillCents = 0L
                    if (ok) {
                        val updated = debtorsViewModel.getDebtorById(debtor.id)
                        val remaining = updated?.remainingAmountCents ?: 0L
                        showMessage(
                            context.getString(
                                R.string.msg_payment_neutral,
                                Formatters.currencyFromCents(
                                    remaining,
                                    debtor.currency,
                                    hide = privacySettings.hideAmounts,
                                ),
                            ),
                        )
                    } else {
                        showMessage(context.getString(R.string.error_payment_failed))
                    }
                }
            },
        )
    }
    celebrationDebtor?.let { debtor ->
        DebtCelebrationDialog(
            debtor = debtor,
            onDismiss = debtorsViewModel::dismissCelebration,
        )
    }
    if (showIncomeIdeas) {
        IncomeIdeasDialog(
            debtors = allDebtors,
            hideAmounts = privacySettings.hideAmounts,
            onDismiss = { showIncomeIdeas = false },
        )
    }
    if (showTransferDialog) {
        AddTransferDialog(
            onDismiss = { showTransferDialog = false },
            onConfirm = { amount, date ->
                budgetViewModel.addDeposit(amount, date)
                showTransferDialog = false
                scope.launch { showMessage(context.getString(R.string.msg_transfer_added)) }
            },
        )
    }
    showGoalTransferDialog?.let { goal ->
        AddTransferDialog(
            availableBalance = availableBalanceUsd,
            onDismiss = { showGoalTransferDialog = null },
            onConfirm = { amount, date ->
                budgetViewModel.addGoalTransfer(goal.id, amount, date) { result ->
                    showGoalTransferDialog = null
                    scope.launch {
                        showMessage(
                            context.getString(
                                when (result) {
                                    BudgetActionResult.Success -> R.string.msg_goal_transfer_added
                                    BudgetActionResult.InsufficientFunds -> R.string.msg_insufficient_funds
                                },
                            ),
                        )
                    }
                }
            },
        )
    }
    if (showExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onConfirm = { amount, category, date, note ->
                budgetViewModel.addExpense(amount, category, date, note) { result ->
                    showExpenseDialog = false
                    scope.launch {
                        showMessage(
                            context.getString(
                                when (result) {
                                    BudgetActionResult.Success -> R.string.msg_expense_added
                                    BudgetActionResult.InsufficientFunds -> R.string.msg_insufficient_funds
                                },
                            ),
                        )
                    }
                }
            },
        )
    }
    if (showAddGoalDialog) {
        GoalFormDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { name, target, current, deadline ->
                goalViewModel.addGoal(name, target, current, deadline)
                showAddGoalDialog = false
                scope.launch { showMessage(context.getString(R.string.msg_goal_added)) }
            },
        )
    }
    editGoal?.let { goal ->
        GoalFormDialog(
            existingGoal = goal,
            onDismiss = { editGoal = null },
            onConfirm = { name, target, current, deadline ->
                goalViewModel.updateGoal(goal, name, target, current, deadline)
                editGoal = null
                scope.launch { showMessage(context.getString(R.string.msg_goal_saved)) }
            },
        )
    }
    if (showQuickAddDialog) {
        QuickAddDialog(
            hasActiveDebts = allDebtors.any { !it.isPaid },
            onDismiss = { showQuickAddDialog = false },
            onDeposit = { showTransferDialog = true },
            onExpense = { showExpenseDialog = true },
            onDebtPayment = {
                allDebtors.firstOrNull { !it.isPaid }?.let { showDebtPaymentDialog = it }
            },
        )
    }

    pendingDelete?.let { delete ->
        PendingDeleteDialog(
            pending = delete,
            onDismiss = { pendingDelete = null },
            onDeleteDebtor = { debtor ->
                debtorsViewModel.deleteDebtor(debtor)
                scope.launch { showMessage(context.getString(R.string.msg_debtor_deleted)) }
            },
            onDeleteGoal = { goal ->
                goalViewModel.deleteGoal(goal)
                scope.launch { showMessage(context.getString(R.string.msg_goal_deleted)) }
            },
            onDeleteTransfer = { transfer ->
                budgetViewModel.deleteTransfer(transfer, app.debtorRepository)
                scope.launch { showMessage(context.getString(R.string.msg_item_deleted)) }
            },
            onDeleteExpense = { expense ->
                budgetViewModel.deleteExpense(expense, app.debtorRepository)
                scope.launch { showMessage(context.getString(R.string.msg_item_deleted)) }
            },
        )
    }

    pendingMarkPaid?.let { debtor ->
        AlertDialog(
            onDismissRequest = { pendingMarkPaid = null },
            title = { Text(stringResource(R.string.mark_debt_paid_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.mark_debt_paid_message,
                        debtor.name,
                        Formatters.currencyFromCents(
                            debtor.remainingAmountCents,
                            debtor.currency,
                            hide = privacySettings.hideAmounts,
                        ),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        debtorsViewModel.markAsPaid(debtor.id)
                        pendingMarkPaid = null
                        scope.launch { showMessage(context.getString(R.string.msg_debtor_paid)) }
                    },
                ) {
                    Text(stringResource(R.string.action_mark_paid))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingMarkPaid = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val isTablet = isTabletWidth(maxWidth)
        val syncState by app.syncService.connectionState.collectAsStateWithLifecycle()

        val topBar: @Composable () -> Unit = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    if (!isTablet) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.cd_open_drawer))
                        }
                    }
                },
                actions = {
                    if (!isTablet) {
                        SyncStatusIndicator(
                            state = syncState,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_goal)) },
                            onClick = {
                                showTopMenu = false
                                showAddGoalDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_debt)) },
                            onClick = {
                                showTopMenu = false
                                if (canAddDebtor) {
                                    showAddDebtorDialog = true
                                } else {
                                    debtListViewModel.setDebtTab(DebtTab.OwesMe)
                                    showAddDebtorDialog = true
                                }
                            },
                        )
                        syncState.webSocketUrl?.let { webSocketUrl ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(stringResource(R.string.sync_web_url_menu))
                                        Text(
                                            text = webSocketUrl,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2,
                                        )
                                    }
                                },
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                        as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("sync-url", webSocketUrl),
                                    )
                                    showTopMenu = false
                                    scope.launch {
                                        showMessage(context.getString(R.string.sync_web_url_copied))
                                    }
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground),
            )
        }

        val fab: @Composable () -> Unit = {
            val showFab = if (isTablet) {
                when (tabletSection) {
                    HomeSection.Goals, HomeSection.Operations -> true
                    HomeSection.Debts -> canAddDebtor
                }
            } else {
                when (HomeTab.entries[selectedTab]) {
                    HomeTab.Goals, HomeTab.BrotherFinances -> true
                    HomeTab.Debts -> canAddDebtor
                }
            }
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        if (isTablet) {
                            when (tabletSection) {
                                HomeSection.Goals -> showAddGoalDialog = true
                                HomeSection.Debts -> showAddDebtorDialog = true
                                HomeSection.Operations -> showQuickAddDialog = true
                            }
                        } else {
                            when (HomeTab.entries[selectedTab]) {
                                HomeTab.Goals -> showAddGoalDialog = true
                                HomeTab.Debts -> showAddDebtorDialog = true
                                HomeTab.BrotherFinances -> showQuickAddDialog = true
                            }
                        }
                    },
                    containerColor = GoldPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.widget_quick_add))
                }
            }
        }

        if (isTablet) {
            TabletHomeLayout(
                section = tabletSection,
                onSectionChange = { tabletSection = it },
                selection = tabletSelection,
                onSelectionChange = { tabletSelection = it },
                syncState = syncState,
                snackbarHostState = snackbarHostState,
                topBar = topBar,
                floatingActionButton = fab,
                goals = goals,
                debtors = debtors,
                allDebtors = allDebtors,
                debtTab = debtTab,
                onDebtTabChange = debtListViewModel::setDebtTab,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = debtListViewModel::setFilter,
                debtSearchQuery = debtSearchQuery,
                onDebtSearchQueryChange = debtListViewModel::setSearchQuery,
                transfers = transfers,
                expenses = expenses,
                budgetSummary = budgetSummary,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                onAddGoal = { showAddGoalDialog = true },
                onEditGoal = { editGoal = it },
                onDeleteGoal = { pendingDelete = PendingDelete.GoalItem(it) },
                onGoalTransfer = { showGoalTransferDialog = it },
                onExpense = { showExpenseDialog = true },
                onRemindDebtor = { debtor -> remindDebtor = debtor },
                onAddDebtPayment = { debtor ->
                    debtPaymentPrefillCents = 0L
                    showDebtPaymentDialog = debtor
                },
                onAccelerateDebtPayment = { debtor, prefillCents ->
                    debtPaymentPrefillCents = prefillCents
                    showDebtPaymentDialog = debtor
                },
                onMarkDebtPaid = { debtor -> pendingMarkPaid = debtor },
                freedomProjection = freedomProjection,
                extraMonthlyCents = extraMonthlyCents,
                onExtraMonthlyChange = debtListViewModel::setExtraMonthlyCents,
                gamificationState = gamificationState,
                privacySettings = privacySettings,
                autopilotSettings = autopilotSettings,
                showCheckIn = showCheckIn,
                onDismissCheckIn = debtorsViewModel::dismissCheckIn,
                onOpenAdvisor = { showAdvisorSheet = true },
                onApplyAutopilot = {
                    scope.launch {
                        val plan = debtorsViewModel.applyMicroPaymentSync()
                        if (plan != null) {
                            showMessage(
                                context.getString(
                                    R.string.autopilot_applied,
                                    Formatters.currencyFromCents(
                                        plan.amountCents,
                                        hide = privacySettings.hideAmounts,
                                    ),
                                    plan.debtorName,
                                ),
                            )
                        } else {
                            showMessage(context.getString(R.string.autopilot_apply_failed))
                        }
                    }
                },
                onOpenGoalHub = onOpenGoalHub,
                onOpenAppLanguage = onOpenAppLanguage,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenAutopilotSettings = onOpenAutopilotSettings,
                onOpenCalendarSettings = onOpenCalendarSettings,
                onOpenDebtNotificationSettings = onOpenDebtNotificationSettings,
                onOpenBackupSettings = onOpenBackupSettings,
                onOpenVoiceSettings = onOpenVoiceSettings,
                onPlayVoiceReport = {
                    scope.launch { VoiceReportEngine(app).runTestReport() }
                },
                calendarPermissionGranted = hasCalendarPermissions,
                onOpenIncomeIdeas = { showIncomeIdeas = true },
                onEditDebtor = { editDebtor = it },
                onDeleteDebtor = { pendingDelete = PendingDelete.DebtorItem(it) },
                onViewContactHistory = { debtor ->
                    debtor.contactLookupKey?.let { key ->
                        openContactHistory(key, debtor.name)
                    }
                },
                onSyncCalendar = debtorsViewModel::syncDebtToCalendarManually,
                onRemoveCalendar = debtorsViewModel::removeDebtFromCalendarManually,
                onAddDebt = {
                    if (canAddDebtor) {
                        showAddDebtorDialog = true
                    } else {
                        debtListViewModel.setDebtTab(DebtTab.OwesMe)
                        showAddDebtorDialog = true
                    }
                },
                onConfirmTransfer = budgetViewModel::confirmTransfer,
                onDeleteTransfer = { pendingDelete = PendingDelete.TransferItem(it) },
                onConfirmExpense = budgetViewModel::confirmExpense,
                onDeleteExpense = { pendingDelete = PendingDelete.ExpenseItem(it) },
                emptyDebtorsTitle = { filter -> emptyDebtorsTitle(filter) },
                emptyDebtorsHint = { filter -> emptyDebtorsHint(filter) },
            )
        } else {
            PhoneHomeLayout(
                drawerState = drawerState,
                snackbarHostState = snackbarHostState,
                topBar = topBar,
                floatingActionButton = fab,
                listState = listState,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                goals = goals,
                debtors = debtors,
                allDebtors = allDebtors,
                debtTab = debtTab,
                onDebtTabChange = debtListViewModel::setDebtTab,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = debtListViewModel::setFilter,
                debtSearchQuery = debtSearchQuery,
                onDebtSearchQueryChange = debtListViewModel::setSearchQuery,
                transfers = transfers,
                expenses = expenses,
                budgetSummary = budgetSummary,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                greetingName = userProfile.resolvedDisplayName(
                    stringResource(R.string.onboarding_default_name),
                ),
                debtProgressPercent = debtProgressPercent,
                highlightedDebtorId = highlightedDebtorId,
                onAddGoal = { showAddGoalDialog = true },
                onEditGoal = { editGoal = it },
                onDeleteGoal = { pendingDelete = PendingDelete.GoalItem(it) },
                onGoalTransfer = { showGoalTransferDialog = it },
                onExpense = { showExpenseDialog = true },
                onRemindDebtor = { debtor -> remindDebtor = debtor },
                onAddDebtPayment = { debtor ->
                    debtPaymentPrefillCents = 0L
                    showDebtPaymentDialog = debtor
                },
                onAccelerateDebtPayment = { debtor, prefillCents ->
                    debtPaymentPrefillCents = prefillCents
                    showDebtPaymentDialog = debtor
                },
                onMarkDebtPaid = { debtor -> pendingMarkPaid = debtor },
                freedomProjection = freedomProjection,
                extraMonthlyCents = extraMonthlyCents,
                onExtraMonthlyChange = debtListViewModel::setExtraMonthlyCents,
                onEditDebtor = { editDebtor = it },
                onDeleteDebtor = { pendingDelete = PendingDelete.DebtorItem(it) },
                onViewContactHistory = { debtor ->
                    debtor.contactLookupKey?.let { key ->
                        openContactHistory(key, debtor.name)
                    }
                },
                onSyncCalendar = debtorsViewModel::syncDebtToCalendarManually,
                onRemoveCalendar = debtorsViewModel::removeDebtFromCalendarManually,
                onConfirmTransfer = budgetViewModel::confirmTransfer,
                onDeleteTransfer = { pendingDelete = PendingDelete.TransferItem(it) },
                onConfirmExpense = budgetViewModel::confirmExpense,
                onDeleteExpense = { pendingDelete = PendingDelete.ExpenseItem(it) },
                onOpenVoiceSettings = onOpenVoiceSettings,
                onOpenAppLanguage = onOpenAppLanguage,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenAutopilotSettings = onOpenAutopilotSettings,
                onOpenCalendarSettings = onOpenCalendarSettings,
                onOpenDebtNotificationSettings = onOpenDebtNotificationSettings,
                onOpenBackupSettings = onOpenBackupSettings,
                onOpenIncomeIdeas = { showIncomeIdeas = true },
                onPlayVoiceReport = {
                    scope.launch { VoiceReportEngine(app).runTestReport() }
                },
                calendarPermissionGranted = hasCalendarPermissions,
                onOpenGoalHub = onOpenGoalHub,
                onAddDebt = {
                    showAddDebtorDialog = true
                },
                onQuickAdd = { showQuickAddDialog = true },
                notificationsEnabled = notificationsEnabled,
                onRequestNotificationPermission = onRequestNotificationPermission,
                showCheckIn = showCheckIn,
                onDismissCheckIn = debtorsViewModel::dismissCheckIn,
                gamificationState = gamificationState,
                privacySettings = privacySettings,
                autopilotSettings = autopilotSettings,
                onOpenAdvisor = { showAdvisorSheet = true },
                onApplyAutopilot = {
                    scope.launch {
                        val plan = debtorsViewModel.applyMicroPaymentSync()
                        if (plan != null) {
                            showMessage(
                                context.getString(
                                    R.string.autopilot_applied,
                                    Formatters.currencyFromCents(
                                        plan.amountCents,
                                        hide = privacySettings.hideAmounts,
                                    ),
                                    plan.debtorName,
                                ),
                            )
                        } else {
                            showMessage(context.getString(R.string.autopilot_apply_failed))
                        }
                    }
                },
                scope = scope,
                emptyDebtorsTitle = { filter -> emptyDebtorsTitle(filter) },
                emptyDebtorsHint = { filter -> emptyDebtorsHint(filter) },
            )
        }
    }

    if (showAdvisorSheet) {
        val accumulatedCents = MoneyMath.toCents(goalMetrics.totalAccumulated)
        val remainingCents = MoneyMath.toCents(goalMetrics.totalRemainingToGoal)
        val depositsCents = MoneyMath.toCents(budgetSummary.totalDeposits)
        val expensesCents = MoneyMath.toCents(budgetSummary.totalExpenses)
        DebtAdvisorBottomSheet(
            debts = allDebtors,
            messages = advisorMessages,
            onPromptSelected = { promptId ->
                debtorsViewModel.askAdvisor(
                    promptId = promptId,
                    totalAccumulatedCents = accumulatedCents,
                    remainingToGoalCents = remainingCents,
                    totalDepositsCents = depositsCents,
                    totalExpensesCents = expensesCents,
                )
            },
            onFreeText = { text ->
                debtorsViewModel.askAdvisorFreeText(
                    text = text,
                    totalAccumulatedCents = accumulatedCents,
                    remainingToGoalCents = remainingCents,
                    totalDepositsCents = depositsCents,
                    totalExpensesCents = expensesCents,
                )
            },
            onClearHistory = debtorsViewModel::clearAdvisorHistory,
            onDismiss = { showAdvisorSheet = false },
            totalAccumulatedCents = accumulatedCents,
            remainingToGoalCents = remainingCents,
            totalDepositsCents = depositsCents,
            totalExpensesCents = expensesCents,
        )
    }
}

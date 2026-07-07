package com.example.money.ui.home

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.money.ui.components.ConfirmDeleteDialog
import com.example.money.ui.components.DebtorListItem
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.ui.components.SwipeToDeleteContainer
import com.example.money.domain.util.DebtFreedomEngine
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.debtors.DebtReminderShare
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtTabRow
import com.example.money.ui.debtors.DebtorFormDialog
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.DebtAdvisorBottomSheet
import com.example.money.ui.debtors.DebtFreedomTab
import com.example.money.ui.debtors.DebtCelebrationDialog
import com.example.money.ui.debtors.DebtCheckInSheet
import com.example.money.ui.debtors.DebtOverviewCard
import com.example.money.ui.debtors.GamificationBanner
import com.example.money.ui.debtors.IncomeIdeasDialog
import com.example.money.ui.debtors.DebtPaymentDialog
import com.example.money.ui.debtors.DebtorFilterRow
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.goal.GoalFormDialog
import com.example.money.ui.goal.GoalViewModel
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.isTabletWidth
import kotlinx.coroutines.launch

private sealed class PendingDelete {
    data class DebtorItem(val debtor: Debtor) : PendingDelete()
    data class GoalItem(val goal: SavingsGoal) : PendingDelete()
    data class TransferItem(val transfer: Transfer) : PendingDelete()
    data class ExpenseItem(val expense: Expense) : PendingDelete()
}

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
    val debtorsViewModel: DebtorsViewModel = viewModel(
        factory = DebtorsViewModel.Factory(
            app.debtorRepository,
            app.debtReminderScheduler,
            app.preferencesStore,
        ),
    )

    val goals by goalViewModel.goals.collectAsStateWithLifecycle()
    val allDebtors by debtorsViewModel.allDebtorsList.collectAsStateWithLifecycle()
    val debtors by debtorsViewModel.debtors.collectAsStateWithLifecycle()
    val debtTab by debtorsViewModel.debtTab.collectAsStateWithLifecycle()
    val debtorFilter by debtorsViewModel.filter.collectAsStateWithLifecycle()
    val debtMetrics by debtorsViewModel.debtMetrics.collectAsStateWithLifecycle()
    val freedomProjection by debtorsViewModel.freedomProjection.collectAsStateWithLifecycle()
    val extraMonthlyCents by debtorsViewModel.extraMonthlyCents.collectAsStateWithLifecycle()
    val privacySettings by debtorsViewModel.privacySettings.collectAsStateWithLifecycle()
    val gamificationState by debtorsViewModel.gamificationState.collectAsStateWithLifecycle()
    val autopilotSettings by debtorsViewModel.autopilotSettings.collectAsStateWithLifecycle()
    val advisorMessages by debtorsViewModel.advisorMessages.collectAsStateWithLifecycle()
    val budgetSummary by budgetViewModel.budgetSummary.collectAsStateWithLifecycle()
    val availableBalanceUsd = budgetSummary.availableBalance
    val goalMetrics = remember(goals) { GlobalFinanceEngine.activeGoalMetrics(goals) }
    val transfers by budgetViewModel.transfers.collectAsStateWithLifecycle()
    val expenses by budgetViewModel.expenses.collectAsStateWithLifecycle()

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
    var showWelcomeDialog by remember { mutableStateOf(WelcomePreferences.shouldShowWelcome(context)) }
    var showQuickAddDialog by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showAdvisorSheet by remember { mutableStateOf(false) }
    var showIncomeIdeas by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingDelete?>(null) }

    val celebrationDebtor by debtorsViewModel.celebrationDebtor.collectAsStateWithLifecycle()
    val showCheckIn by debtorsViewModel.showCheckIn.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tabletSection by remember {
        mutableStateOf(
            when (launchConfig.initialTab) {
                HomeTab.Debts -> HomeSection.Debts
                HomeTab.BrotherFinances -> HomeSection.Operations
            },
        )
    }
    var tabletSelection by remember { mutableStateOf<HomeListSelection>(HomeListSelection.Overview) }
    var highlightedDebtorId by remember { mutableStateOf<Long?>(null) }

    suspend fun showMessage(message: String) {
        snackbarHostState.showSnackbar(message)
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
            HomeTab.Debts -> HomeSection.Debts
            HomeTab.BrotherFinances -> HomeSection.Operations
        }
        debtorsViewModel.setDebtTab(launchConfig.initialDebtTab)

        if (launchConfig.openExpenseDialog) {
            showExpenseDialog = true
        }
        if (launchConfig.openQuickAddDialog) {
            showQuickAddDialog = true
        }

        val targetDebtorId = launchConfig.openDebtPaymentForDebtorId
            ?: launchConfig.highlightDebtorId
        if (targetDebtorId != null) {
            debtorsViewModel.setFilter(DebtorFilter.ALL)
            val debtor = app.debtorRepository.getDebtorById(targetDebtorId)
            if (debtor != null) {
                debtorsViewModel.setDebtTab(DebtTab.forDebtType(debtor.debtType))
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
            debtorsViewModel.setDebtTab(DebtTab.IOwe)
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

    if (showAddDebtorDialog) {
        DebtorFormDialog(
            debtType = when (debtTab) {
                DebtTab.OwesMe -> DebtType.CREDITOR
                DebtTab.IOwe -> DebtType.DEBTOR
            },
            availableBalance = availableBalanceUsd,
            onDismiss = { showAddDebtorDialog = false },
            onConfirm = { name, amount, dateGiven, dateDue, affectsBalance, note, loanCategory, interestRateBps ->
                debtorsViewModel.addDebtor(
                    name = name,
                    amount = amount,
                    dateGiven = dateGiven,
                    dateDue = dateDue,
                    debtType = when (debtTab) {
                        DebtTab.OwesMe -> DebtType.CREDITOR
                        DebtTab.IOwe -> DebtType.DEBTOR
                    },
                    affectsBalance = affectsBalance,
                    note = note,
                    loanCategory = loanCategory,
                    interestRateBps = interestRateBps,
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
            onDismiss = { editDebtor = null },
            onConfirm = { name, amount, dateGiven, dateDue, affectsBalance, note, loanCategory, interestRateBps ->
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
                )
                editDebtor = null
                scope.launch { showMessage(context.getString(R.string.msg_debtor_updated)) }
            },
        )
    }
    showDebtPaymentDialog?.let { debtor ->
        DebtPaymentDialog(
            debtor = debtor,
            availableBalance = availableBalanceUsd,
            initialAmountCents = debtPaymentPrefillCents,
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
                        val updated = app.debtorRepository.getDebtorById(debtor.id)
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
        when (delete) {
            is PendingDelete.DebtorItem -> ConfirmDeleteDialog(
                title = stringResource(R.string.delete_debtor_title),
                message = stringResource(R.string.delete_debtor_message, delete.debtor.name),
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    debtorsViewModel.deleteDebtor(delete.debtor)
                    pendingDelete = null
                    scope.launch { showMessage(context.getString(R.string.msg_debtor_deleted)) }
                },
            )
            is PendingDelete.GoalItem -> ConfirmDeleteDialog(
                title = stringResource(R.string.delete_goal_title),
                message = stringResource(R.string.delete_goal_message, delete.goal.name),
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    goalViewModel.deleteGoal(delete.goal)
                    pendingDelete = null
                    scope.launch { showMessage(context.getString(R.string.msg_goal_deleted)) }
                },
            )
            is PendingDelete.TransferItem, is PendingDelete.ExpenseItem -> ConfirmDeleteDialog(
                title = stringResource(R.string.delete_item_title),
                message = stringResource(R.string.delete_item_message),
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    when (delete) {
                        is PendingDelete.TransferItem -> {
                            budgetViewModel.deleteTransfer(delete.transfer, app.debtorRepository)
                        }
                        is PendingDelete.ExpenseItem -> {
                            budgetViewModel.deleteExpense(delete.expense, app.debtorRepository)
                        }
                        is PendingDelete.DebtorItem, is PendingDelete.GoalItem -> Unit
                    }
                    pendingDelete = null
                    scope.launch { showMessage(context.getString(R.string.msg_item_deleted)) }
                },
            )
        }
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
                            Icon(Icons.Default.Menu, contentDescription = null)
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
                        Icon(Icons.Default.MoreVert, contentDescription = null)
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
                                showAddDebtorDialog = true
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
                debtTab = debtTab,
                onDebtTabChange = debtorsViewModel::setDebtTab,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = debtorsViewModel::setFilter,
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
                onRemindDebtor = { debtor ->
                    val intent = DebtReminderShare.createShareIntent(context, debtor)
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.debt_remind_chooser_title),
                        ),
                    )
                },
                onAddDebtPayment = { debtor ->
                    debtPaymentPrefillCents = 0L
                    showDebtPaymentDialog = debtor
                },
                onAccelerateDebtPayment = { debtor, prefillCents ->
                    debtPaymentPrefillCents = prefillCents
                    showDebtPaymentDialog = debtor
                },
                freedomProjection = freedomProjection,
                extraMonthlyCents = extraMonthlyCents,
                onExtraMonthlyChange = debtorsViewModel::setExtraMonthlyCents,
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
                onEditDebtor = { editDebtor = it },
                onDeleteDebtor = { pendingDelete = PendingDelete.DebtorItem(it) },
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
                onDebtTabChange = debtorsViewModel::setDebtTab,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = debtorsViewModel::setFilter,
                transfers = transfers,
                expenses = expenses,
                budgetSummary = budgetSummary,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                highlightedDebtorId = highlightedDebtorId,
                onAddGoal = { showAddGoalDialog = true },
                onEditGoal = { editGoal = it },
                onDeleteGoal = { pendingDelete = PendingDelete.GoalItem(it) },
                onGoalTransfer = { showGoalTransferDialog = it },
                onExpense = { showExpenseDialog = true },
                onRemindDebtor = { debtor ->
                    val intent = DebtReminderShare.createShareIntent(context, debtor)
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.getString(R.string.debt_remind_chooser_title),
                        ),
                    )
                },
                onAddDebtPayment = { debtor ->
                    debtPaymentPrefillCents = 0L
                    showDebtPaymentDialog = debtor
                },
                onAccelerateDebtPayment = { debtor, prefillCents ->
                    debtPaymentPrefillCents = prefillCents
                    showDebtPaymentDialog = debtor
                },
                freedomProjection = freedomProjection,
                extraMonthlyCents = extraMonthlyCents,
                onExtraMonthlyChange = debtorsViewModel::setExtraMonthlyCents,
                onEditDebtor = { editDebtor = it },
                onDeleteDebtor = { pendingDelete = PendingDelete.DebtorItem(it) },
                onConfirmTransfer = budgetViewModel::confirmTransfer,
                onDeleteTransfer = { pendingDelete = PendingDelete.TransferItem(it) },
                onConfirmExpense = budgetViewModel::confirmExpense,
                onDeleteExpense = { pendingDelete = PendingDelete.ExpenseItem(it) },
                onOpenVoiceSettings = onOpenVoiceSettings,
                onOpenAppLanguage = onOpenAppLanguage,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenAutopilotSettings = onOpenAutopilotSettings,
                onOpenIncomeIdeas = { showIncomeIdeas = true },
                onOpenGoalHub = onOpenGoalHub,
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
        DebtAdvisorBottomSheet(
            debts = allDebtors,
            messages = advisorMessages,
            onPromptSelected = debtorsViewModel::askAdvisor,
            onDismiss = { showAdvisorSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneHomeLayout(
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
    transfers: List<Transfer>,
    expenses: List<Expense>,
    budgetSummary: com.example.money.domain.model.BudgetSummary,
    goalMetrics: com.example.money.domain.model.DashboardGoalMetrics,
    debtMetrics: com.example.money.domain.model.DashboardDebtMetrics,
    highlightedDebtorId: Long?,
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
    onEditDebtor: (Debtor) -> Unit,
    onDeleteDebtor: (Debtor) -> Unit,
    onConfirmTransfer: (Long) -> Unit,
    onDeleteTransfer: (Transfer) -> Unit,
    onConfirmExpense: (Long) -> Unit,
    onDeleteExpense: (Expense) -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenAppLanguage: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenAutopilotSettings: () -> Unit,
    onOpenIncomeIdeas: () -> Unit,
    onOpenGoalHub: () -> Unit,
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
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(vertical = 24.dp)) {
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.goal_add_title)) },
                        selected = false,
                        onClick = {
                            onAddGoal()
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.app_language_menu)) },
                        selected = false,
                        onClick = {
                            onOpenAppLanguage()
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.privacy_settings_title)) },
                        selected = false,
                        onClick = {
                            onOpenPrivacySettings()
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.autopilot_settings_title)) },
                        selected = false,
                        onClick = {
                            onOpenAutopilotSettings()
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.income_ideas_title)) },
                        selected = false,
                        onClick = {
                            onOpenIncomeIdeas()
                            scope.launch { drawerState.close() }
                        },
                    )
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.voice_settings_menu)) },
                        selected = false,
                        onClick = {
                            onOpenVoiceSettings()
                            scope.launch { drawerState.close() }
                        },
                    )
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
                    GlobalBalanceHeader(
                        summary = budgetSummary,
                        goalMetrics = goalMetrics,
                        debtMetrics = debtMetrics,
                        onOwesMeClick = {
                            onTabSelected(HomeTab.Debts.ordinal)
                            onDebtTabChange(DebtTab.OwesMe)
                        },
                        onIOweClick = {
                            onTabSelected(HomeTab.Debts.ordinal)
                            onDebtTabChange(DebtTab.IOwe)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.goals_section_title, goals.size),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row {
                            TextButton(onClick = onOpenGoalHub) {
                                Text(stringResource(R.string.goal_hub_open))
                            }
                            TextButton(onClick = onAddGoal) {
                                Text(stringResource(R.string.goal_add_title))
                            }
                        }
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
                    items(goals, key = { "goal-${it.syncId.ifBlank { "id-${it.id}" }}" }) { goal ->
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteGoal(goal) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            GoalListItem(
                                goal = goal,
                                onEdit = { onEditGoal(goal) },
                                onDelete = { onDeleteGoal(goal) },
                                onTransfer = if (!goal.isCompleted) {
                                    { onGoalTransfer(goal) }
                                } else {
                                    null
                                },
                                onExpense = onExpense,
                            )
                        }
                    }
                }

                item {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = CreamBackground,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    height = 3.dp,
                                )
                            }
                        },
                        divider = {},
                    ) {
                        HomeTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { onTabSelected(index) },
                                text = {
                                    Text(
                                        text = stringResource(tab.titleRes),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                            )
                        }
                    }
                }

                when (HomeTab.entries[selectedTab]) {
                    HomeTab.Debts -> {
                        item {
                            DebtTabRow(
                                selectedTab = debtTab,
                                onTabSelected = onDebtTabChange,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        if (debtTab != DebtTab.Freedom) {
                            if (debtTab == DebtTab.IOwe) {
                                item {
                                    DebtOverviewCard(
                                        debtors = allDebtors,
                                        hideAmounts = privacySettings.hideAmounts,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                }
                            }
                            if (showCheckIn) {
                                item {
                                    DebtCheckInSheet(
                                        debtors = allDebtors,
                                        hideAmounts = privacySettings.hideAmounts,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    )
                                }
                                item {
                                    TextButton(
                                        onClick = onDismissCheckIn,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    ) {
                                        Text(stringResource(R.string.action_close))
                                    }
                                }
                            }
                            item {
                                GamificationBanner(
                                    state = gamificationState,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
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
                        }
                        if (debtTab == DebtTab.Freedom) {
                            item {
                                DebtFreedomTab(
                                    projection = freedomProjection,
                                    extraMonthlyCents = extraMonthlyCents,
                                    onExtraMonthlyChange = onExtraMonthlyChange,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        } else {
                            item {
                                DebtorFilterRow(
                                    selectedFilter = debtorFilter,
                                    onFilterSelected = onDebtorFilterChange,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
                                items(debtors, key = { "debtor-${it.syncId.ifBlank { "id-${it.id}" }}" }) { debtor ->
                                    val isHighlighted = debtor.id == highlightedDebtorId
                                    val accelerateCents = DebtFreedomEngine.suggestAcceleratePaymentCents(
                                        remainingCents = debtor.remainingAmountCents,
                                        targetCents = debtor.targetAmountCents,
                                        currentCents = debtor.currentAmountCents,
                                    )
                                    DebtorListItem(
                                        debtor = debtor,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                            .then(
                                                if (isHighlighted) {
                                                    Modifier.border(2.dp, GoldPrimary, MaterialTheme.shapes.large)
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                        onRemind = { onRemindDebtor(debtor) },
                                        onAddPayment = { onAddDebtPayment(debtor) },
                                        onAccelerate = { onAccelerateDebtPayment(debtor, accelerateCents) },
                                        onEdit = { onEditDebtor(debtor) },
                                        onDelete = { onDeleteDebtor(debtor) },
                                        hideAmounts = privacySettings.hideAmounts,
                                    )
                                }
                            }
                        }
                    }
                    HomeTab.BrotherFinances -> {
                        if (transfers.isEmpty() && expenses.isEmpty()) {
                            item {
                                EmptyStateMessage(
                                    title = stringResource(R.string.empty_budget),
                                    hint = stringResource(R.string.empty_budget_hint),
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = stringResource(R.string.operations_section_title),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            if (transfers.isNotEmpty()) {
                                items(transfers, key = { "t-${it.syncId.ifBlank { "id-${it.id}" }}" }) { transfer ->
                                    val goalName = transfer.goalId?.let { goalId ->
                                        goals.find { it.id == goalId }?.name
                                    }
                                    val operationLabel = if (goalName != null) {
                                        stringResource(R.string.operation_goal_transfer, goalName)
                                    } else {
                                        stringResource(R.string.operation_deposit)
                                    }
                                    SwipeToDeleteContainer(
                                        onDelete = { onDeleteTransfer(transfer) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    ) {
                                        FinanceListItem(
                                            title = "+${Formatters.currencyCompact(transfer.amount)}",
                                            subtitle = buildString {
                                                append(operationLabel)
                                                append(" · ")
                                                append(Formatters.shortDate(transfer.date))
                                                if (!transfer.isConfirmed) append(" · Ожидает")
                                            },
                                            amountColor = FinancePositive,
                                            isPending = !transfer.isConfirmed,
                                            onConfirm = if (!transfer.isConfirmed) {
                                                { onConfirmTransfer(transfer.id) }
                                            } else {
                                                null
                                            },
                                            onDelete = { onDeleteTransfer(transfer) },
                                        )
                                    }
                                }
                            }
                            if (expenses.isNotEmpty()) {
                                items(expenses, key = { "e-${it.syncId.ifBlank { "id-${it.id}" }}" }) { expense ->
                                    SwipeToDeleteContainer(
                                        onDelete = { onDeleteExpense(expense) },
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    ) {
                                        FinanceListItem(
                                            title = "−${Formatters.currencyCompact(expense.amount)}",
                                            subtitle = buildString {
                                                append(stringResource(R.string.operation_expense))
                                                append(" · ")
                                                append(expense.category)
                                                append(" · ")
                                                append(Formatters.shortDate(expense.date))
                                                if (!expense.isConfirmed) append(" · Ожидает")
                                            },
                                            amountColor = FinanceNegative,
                                            isPending = !expense.isConfirmed,
                                            onConfirm = if (!expense.isConfirmed) {
                                                { onConfirmExpense(expense.id) }
                                            } else {
                                                null
                                            },
                                            onDelete = { onDeleteExpense(expense) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun emptyDebtorsTitle(filter: DebtorFilter): String = when (filter) {
    DebtorFilter.ALL -> stringResource(R.string.empty_debtors_all)
    DebtorFilter.ACTIVE -> stringResource(R.string.empty_debtors_active)
    DebtorFilter.OVERDUE -> stringResource(R.string.empty_debtors_overdue)
    DebtorFilter.PAID -> stringResource(R.string.empty_debtors_paid)
}

@Composable
private fun emptyDebtorsHint(filter: DebtorFilter): String = when (filter) {
    DebtorFilter.ALL -> stringResource(R.string.empty_debtors_all_hint)
    DebtorFilter.ACTIVE -> stringResource(R.string.empty_debtors_active_hint)
    DebtorFilter.OVERDUE -> stringResource(R.string.empty_debtors_overdue_hint)
    DebtorFilter.PAID -> stringResource(R.string.empty_debtors_paid_hint)
}

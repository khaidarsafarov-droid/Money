package com.example.money.ui.goal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.GoalProgress
import com.example.money.settings.PrivacySettings
import com.example.money.ui.budget.AddExpenseDialog
import com.example.money.ui.budget.AddTransferDialog
import com.example.money.ui.budget.BudgetActionResult
import com.example.money.ui.budget.BudgetViewModel
import com.example.money.ui.components.GoalStatusBadge
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.theme.finoraGoalBrush
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.launch

private data class GoalDetailLoad(
    val ready: Boolean = false,
    val goal: SavingsGoal? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    onBack: () -> Unit,
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
    val actions by goalViewModel.actions.collectAsStateWithLifecycle()
    val privacySettings by app.preferencesStore.privacySettings.collectAsStateWithLifecycle(
        initialValue = PrivacySettings(),
    )
    val hideAmounts = privacySettings.hideAmounts
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val load by produceState(initialValue = GoalDetailLoad(), goalId) {
        app.goalRepository.observeAllGoals().collect { list ->
            value = GoalDetailLoad(ready = true, goal = list.find { it.id == goalId })
        }
    }
    val goal = load.goal
    var showEdit by remember { mutableStateOf(false) }
    var showTransfer by remember { mutableStateOf(false) }
    var showExpense by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = goal?.name ?: stringResource(R.string.tablet_detail_goal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (goal != null) {
                        TextButton(onClick = { showEdit = true }) {
                            Text(stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            !load.ready -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            goal == null -> {
                Text(
                    text = stringResource(R.string.goal_missing),
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp),
                )
            }
            else -> {
                val isDeadlinePassed = goal.deadlineMillis != null &&
                    GoalProgress.isDeadlinePassed(goal.deadlineMillis) &&
                    !goal.isCompleted
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .background(finoraGoalBrush(goal.name.hashCode())),
                    )
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GoalStatusBadge(
                                label = when {
                                    goal.isCompleted -> stringResource(R.string.goal_status_completed)
                                    isDeadlinePassed -> stringResource(R.string.goal_deadline_passed_short)
                                    else -> stringResource(R.string.goal_status_active)
                                },
                                isCompleted = goal.isCompleted,
                                isOverdue = isDeadlinePassed,
                            )
                            Text(
                                text = if (goal.kind == GoalKind.MONEY) {
                                    stringResource(R.string.goal_kind_money)
                                } else {
                                    stringResource(R.string.goal_kind_action)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (goal.kind == GoalKind.MONEY && goal.isOverAchieved) {
                            Text(
                                text = stringResource(
                                    R.string.goal_over_achieved,
                                    Formatters.currencyFromCents(goal.surplusAmountCents, hide = hideAmounts),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (goal.kind == GoalKind.MONEY && !goal.isCompleted) {
                            MoneyButton(
                                text = stringResource(R.string.goal_action_transfer),
                                onClick = { showTransfer = true },
                                priority = MoneyButtonPriority.Primary,
                                icon = Icons.Outlined.SwapHoriz,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (goal.kind == GoalKind.MONEY) {
                            MoneyButton(
                                text = stringResource(R.string.goal_action_expense),
                                onClick = { showExpense = true },
                                priority = MoneyButtonPriority.Secondary,
                                icon = Icons.Outlined.Payments,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        MoneyButton(
                            text = stringResource(R.string.goal_log_button),
                            onClick = { showLog = true },
                            priority = MoneyButtonPriority.Secondary,
                            icon = Icons.Outlined.EditNote,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GoalActionsPanel(
                            goal = goal,
                            actions = actions.filter { it.goalId == goal.id },
                            hideAmounts = hideAmounts,
                            onAddAction = { title, occurredAt ->
                                goalViewModel.addAction(goal.id, title, occurredAt = occurredAt)
                            },
                            onToggleAction = { action, completed ->
                                goalViewModel.setActionCompleted(action.id, completed)
                            },
                            onDeleteAction = { action -> goalViewModel.deleteAction(action.id) },
                        )
                    }
                }
            }
        }
    }

    if (showEdit && goal != null) {
        GoalFormDialog(
            existingGoal = goal,
            onDismiss = { showEdit = false },
            onConfirm = { name, kind, target, current, deadline ->
                goalViewModel.updateGoal(goal, name, target, current, deadline, kind)
                showEdit = false
                showMessage(context.getString(R.string.msg_goal_saved))
            },
        )
    }
    if (showTransfer && goal != null) {
        AddTransferDialog(
            onDismiss = { showTransfer = false },
            onConfirm = { amount, date ->
                budgetViewModel.addGoalTransfer(goal.id, amount, date) { result ->
                    showTransfer = false
                    showMessage(
                        context.getString(
                            when (result) {
                                BudgetActionResult.Success -> R.string.msg_goal_transfer_added
                                BudgetActionResult.InsufficientFunds -> R.string.msg_insufficient_funds
                                BudgetActionResult.Error -> R.string.msg_save_failed
                            },
                        ),
                    )
                }
            },
        )
    }
    if (showExpense && goal != null) {
        AddExpenseDialog(
            titleRes = R.string.goal_expense_title,
            spendLimitCents = goal.currentAmountCents,
            onDismiss = { showExpense = false },
            onConfirm = { amount, category, date, note ->
                budgetViewModel.addExpense(amount, category, date, note, goal.id) { result ->
                    showExpense = false
                    showMessage(
                        context.getString(
                            when (result) {
                                BudgetActionResult.Success -> R.string.msg_goal_expense_added
                                BudgetActionResult.InsufficientFunds -> R.string.msg_goal_insufficient
                                BudgetActionResult.Error -> R.string.msg_save_failed
                            },
                        ),
                    )
                }
            },
        )
    }
    if (showLog && goal != null) {
        GoalProgressLogDialog(
            goal = goal,
            hideAmounts = hideAmounts,
            onDismiss = { showLog = false },
            onSave = { amount, note, date ->
                fun saveNote() {
                    if (note.isNotBlank()) {
                        goalViewModel.addAction(goal.id, note, completed = true, occurredAt = date)
                    }
                    showLog = false
                    showMessage(context.getString(R.string.goal_log_saved))
                }
                if (amount != null && goal.kind == GoalKind.MONEY) {
                    budgetViewModel.addGoalTransfer(goal.id, amount, date) { result ->
                        when (result) {
                            BudgetActionResult.Success -> saveNote()
                            BudgetActionResult.InsufficientFunds -> {
                                showLog = false
                                showMessage(context.getString(R.string.msg_insufficient_funds))
                            }
                            BudgetActionResult.Error -> {
                                showLog = false
                                showMessage(context.getString(R.string.msg_save_failed))
                            }
                        }
                    }
                } else {
                    saveNote()
                }
            },
        )
    }
    if (showDelete && goal != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_goal_title)) },
            text = { Text(stringResource(R.string.delete_goal_message, goal.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        goalViewModel.deleteGoal(goal)
                        showDelete = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

package com.example.money.ui.home

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.PersonalBranchSettings
import com.example.money.settings.PrivacySettings
import com.example.money.ui.budget.AddExpenseDialog
import com.example.money.ui.budget.AddTransferDialog
import com.example.money.ui.budget.BudgetActionResult
import com.example.money.ui.budget.BudgetViewModel
import com.example.money.ui.debtors.ContactDebtHistorySheet
import com.example.money.ui.debtors.DebtAdvisorBottomSheet
import com.example.money.ui.debtors.DebtCelebrationDialog
import com.example.money.ui.debtors.DebtMessageChannelSheet
import com.example.money.ui.debtors.DebtHistorySheet
import com.example.money.ui.debtors.DebtPaymentDialog
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtorFormDialog
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.debtors.AdvisorChatMessage
import com.example.money.ui.debtors.IncomeIdeasDialog
import com.example.money.ui.goal.GoalActionsDialog
import com.example.money.ui.goal.GoalFormDialog
import com.example.money.ui.goal.GoalProgressLogDialog
import com.example.money.ui.goal.GoalViewModel
import com.example.money.ui.notes.NoteFormDialog
import com.example.money.ui.notes.NoteViewModel
import com.example.money.ui.settings.CurrencyPickerDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun HomeDialogs(
    context: Context,
    app: MoneyApplication,
    scope: CoroutineScope,
    showMessage: suspend (String) -> Unit,
    openContactHistory: (String, String) -> Unit,
    debtorsViewModel: DebtorsViewModel,
    budgetViewModel: BudgetViewModel,
    goalViewModel: GoalViewModel,
    noteViewModel: NoteViewModel,
    debtTab: DebtTab,
    availableBalanceCents: Long,
    allDebtors: List<Debtor>,
    privacySettings: PrivacySettings,
    personalBranchSettings: PersonalBranchSettings,
    goals: List<SavingsGoal>,
    goalActions: List<GoalAction>,
    goalMetrics: DashboardGoalMetrics,
    advisorMessages: List<AdvisorChatMessage>,
    currencyRegion: CurrencyRegion,
    celebrationDebtor: Debtor?,
    pendingCalendarClose: Debtor?,
    addHighlight: AddAction,
    addSheetActions: List<AddAction>,
    showAddDebtorDialogState: MutableState<Boolean>,
    editDebtorState: MutableState<Debtor?>,
    contactHistoryTargetState: MutableState<Pair<String, String>?>,
    remindDebtorState: MutableState<Debtor?>,
    showDebtPaymentDialogState: MutableState<Debtor?>,
    debtPaymentPrefillCentsState: MutableLongState,
    showIncomeIdeasState: MutableState<Boolean>,
    showTransferDialogState: MutableState<Boolean>,
    showGoalTransferDialogState: MutableState<SavingsGoal?>,
    showExpenseDialogState: MutableState<Boolean>,
    goalExpenseTargetState: MutableState<SavingsGoal?>,
    showAddGoalDialogState: MutableState<Boolean>,
    progressLogGoalState: MutableState<SavingsGoal?>,
    editGoalState: MutableState<SavingsGoal?>,
    showNoteFormState: MutableState<Boolean>,
    openNoteIdState: MutableState<Long?>,
    showCurrencyPickerState: MutableState<Boolean>,
    showAddSheetState: MutableState<Boolean>,
    showAdvisorSheetState: MutableState<Boolean>,
    onDebtWithDueDateSaved: () -> Unit = {},
) {
    var showAddDebtorDialog by showAddDebtorDialogState
    var chosenDebtType by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<DebtType?>(null) }
    var editDebtor by editDebtorState
    var contactHistoryTarget by contactHistoryTargetState
    var remindDebtor by remindDebtorState
    var showDebtPaymentDialog by showDebtPaymentDialogState
    var debtPaymentPrefillCents by debtPaymentPrefillCentsState
    var showIncomeIdeas by showIncomeIdeasState
    var showTransferDialog by showTransferDialogState
    var showGoalTransferDialog by showGoalTransferDialogState
    var showExpenseDialog by showExpenseDialogState
    var goalExpenseTarget by goalExpenseTargetState
    var showAddGoalDialog by showAddGoalDialogState
    var progressLogGoal by progressLogGoalState
    var editGoal by editGoalState
    var showNoteForm by showNoteFormState
    var openNoteId by openNoteIdState
    var showCurrencyPicker by showCurrencyPickerState
    var showAddSheet by showAddSheetState
    var showAdvisorSheet by showAdvisorSheetState

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

    if (showAddDebtorDialog) {
        val typeForNew = chosenDebtType ?: when (debtTab) {
            DebtTab.OwesMe -> DebtType.CREDITOR
            DebtTab.IOwe -> DebtType.DEBTOR
            DebtTab.Calendar -> DebtType.CREDITOR
        }
        DebtorFormDialog(
            debtType = typeForNew,
            onDebtTypeChange = { chosenDebtType = it },
            availableBalance = availableBalanceCents,
            allDebtors = allDebtors,
            onViewContactHistory = openContactHistory,
            onDismiss = { showAddDebtorDialog = false },
            onConfirm = { name, amount, dateGiven, dateDue, affectsBalance, note, loanCategory, interestRateBps, contactLookupKey, contactUri, contactPhotoUri, phone, email ->
                debtorsViewModel.addDebtor(
                    name = name,
                    amountCents = amount,
                    dateGiven = dateGiven,
                    dateDue = dateDue,
                    debtType = typeForNew,
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
                if (dateDue > 0L) onDebtWithDueDateSaved()
                scope.launch { showMessage(context.getString(R.string.msg_debtor_added)) }
            },
        )
    }
    editDebtor?.let { debtor ->
        DebtorFormDialog(
            debtType = debtor.debtType,
            existingDebtor = debtor,
            availableBalance = availableBalanceCents,
            allDebtors = allDebtors,
            onViewContactHistory = openContactHistory,
            onDismiss = { editDebtor = null },
            onConfirm = { name, amount, dateGiven, dateDue, affectsBalance, note, loanCategory, interestRateBps, contactLookupKey, contactUri, contactPhotoUri, phone, email ->
                debtorsViewModel.updateDebtor(
                    debtor = debtor,
                    name = name,
                    amountCents = amount,
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
                if (dateDue > 0L) onDebtWithDueDateSaved()
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
            availableBalance = availableBalanceCents,
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
                        showMessage(context.getString(R.string.msg_payment_neutral))
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
                                    BudgetActionResult.Error -> R.string.msg_save_failed
                                },
                            ),
                        )
                    }
                }
            },
        )
    }
    val expenseGoal = goalExpenseTarget
    if (showExpenseDialog || expenseGoal != null) {
        AddExpenseDialog(
            titleRes = if (expenseGoal != null) {
                R.string.goal_expense_title
            } else {
                R.string.budget_new_expense_title
            },
            spendLimitCents = expenseGoal?.currentAmountCents,
            onDismiss = {
                showExpenseDialog = false
                goalExpenseTarget = null
            },
            onConfirm = { amount, category, date, note ->
                val goalId = expenseGoal?.id
                budgetViewModel.addExpense(amount, category, date, note, goalId) { result ->
                    showExpenseDialog = false
                    goalExpenseTarget = null
                    scope.launch {
                        showMessage(
                            context.getString(
                                when (result) {
                                    BudgetActionResult.Success ->
                                        if (goalId != null) {
                                            R.string.msg_goal_expense_added
                                        } else {
                                            R.string.msg_expense_added
                                        }
                                    BudgetActionResult.InsufficientFunds ->
                                        if (goalId != null) {
                                            R.string.msg_goal_insufficient
                                        } else {
                                            R.string.msg_insufficient_funds
                                        }
                                    BudgetActionResult.Error -> R.string.msg_save_failed
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
            onConfirm = { name, kind, target, _, deadline ->
                goalViewModel.addGoal(name, target, deadline, kind)
                showAddGoalDialog = false
                scope.launch { showMessage(context.getString(R.string.msg_goal_added)) }
            },
        )
    }
    progressLogGoal?.let { goal ->
        GoalProgressLogDialog(
            goal = goal,
            hideAmounts = privacySettings.hideAmounts,
            onDismiss = { progressLogGoal = null },
            onSave = { amount, note, date ->
                fun saveNote() {
                    if (note.isNotBlank()) {
                        goalViewModel.addAction(goal.id, note, completed = true, occurredAt = date)
                    }
                    progressLogGoal = null
                    scope.launch {
                        showMessage(context.getString(R.string.goal_log_saved))
                    }
                }
                if (amount != null && goal.kind == GoalKind.MONEY) {
                    budgetViewModel.addGoalTransfer(goal.id, amount, date) { result ->
                        when (result) {
                            BudgetActionResult.Success -> saveNote()
                            BudgetActionResult.InsufficientFunds -> {
                                progressLogGoal = null
                                scope.launch {
                                    showMessage(context.getString(R.string.msg_insufficient_funds))
                                }
                            }
                            BudgetActionResult.Error -> {
                                progressLogGoal = null
                                scope.launch {
                                    showMessage(context.getString(R.string.msg_save_failed))
                                }
                            }
                        }
                    }
                } else {
                    saveNote()
                }
            },
        )
    }
    editGoal?.let { goal ->
        GoalFormDialog(
            existingGoal = goal,
            onDismiss = { editGoal = null },
            onConfirm = { name, kind, target, current, deadline ->
                goalViewModel.updateGoal(goal, name, target, current, deadline, kind)
                editGoal = null
                scope.launch { showMessage(context.getString(R.string.msg_goal_saved)) }
            },
        )
    }
    if (showNoteForm) {
        NoteFormDialog(
            onDismiss = { showNoteForm = false },
            onConfirm = { title, kind ->
                showNoteForm = false
                noteViewModel.addNote(title, kind) { id -> openNoteId = id }
            },
        )
    }
    if (showCurrencyPicker) {
        CurrencyPickerDialog(
            selected = currencyRegion,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { region ->
                showCurrencyPicker = false
                scope.launch { app.preferencesStore.setCurrencyRegion(region) }
            },
        )
    }
    if (showAddSheet) {
        AddActionSheet(
            highlighted = addHighlight,
            actions = addSheetActions,
            onDismiss = { showAddSheet = false },
            onExpense = { showExpenseDialog = true },
            onDeposit = { showTransferDialog = true },
            onDebt = { showAddDebtorDialog = true },
            onGoalContribution = {
                val goal = goals.firstOrNull { it.kind == GoalKind.MONEY && !it.isCompleted }
                if (goal != null) {
                    showGoalTransferDialog = goal
                } else {
                    showAddGoalDialog = true
                }
            },
            onNewGoal = { showAddGoalDialog = true },
        )
    }

    if (showAdvisorSheet) {
        val accumulatedCents = goalMetrics.totalAccumulatedCents
        val remainingCents = goalMetrics.totalRemainingToGoalCents
        DebtAdvisorBottomSheet(
            debts = allDebtors,
            messages = advisorMessages,
            onPromptSelected = { promptId ->
                debtorsViewModel.askAdvisor(
                    promptId = promptId,
                    totalAccumulatedCents = accumulatedCents,
                    remainingToGoalCents = remainingCents,
                )
            },
            onFreeText = { text ->
                debtorsViewModel.askAdvisorFreeText(
                    text = text,
                    totalAccumulatedCents = accumulatedCents,
                    remainingToGoalCents = remainingCents,
                )
            },
            onClearHistory = debtorsViewModel::clearAdvisorHistory,
            onDismiss = { showAdvisorSheet = false },
        )
    }
}

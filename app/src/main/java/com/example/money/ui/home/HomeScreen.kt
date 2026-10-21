package com.example.money.ui.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.core.content.FileProvider
import com.example.money.domain.model.DebtType
import com.example.money.domain.util.ParsedDebtLine
import com.example.money.ocr.DebtPhotoFiles
import com.example.money.ocr.DebtPhotoOcr
import com.example.money.ui.debtors.DebtPhotoReviewSheet
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.expandVertically
import androidx.compose.runtime.CompositionLocalProvider
import com.example.money.ui.motion.LocalMoneySharedTransition
import com.example.money.ui.motion.pressScale
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import com.example.money.ui.budget.BudgetViewModel
import com.example.money.ui.components.MoneyButton
import com.example.money.ui.components.MoneyButtonPriority
import com.example.money.ui.components.EmptyStateMessage
import com.example.money.ui.components.GoalListItem
import com.example.money.ui.components.SyncStatusIndicator
import com.example.money.domain.util.GlobalFinanceEngine
import com.example.money.domain.util.GoalPsychologyEngines
import com.example.money.ui.debtors.DebtListViewModel
import com.example.money.ui.debtors.DebtTab
import com.example.money.ui.debtors.DebtDueStatus
import com.example.money.ui.debtors.DebtorFilter
import com.example.money.ui.debtors.dueStatus
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.goal.GoalViewModel
import com.example.money.ui.notes.NoteViewModel
import com.example.money.settings.AppMode
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.UserProfile
import androidx.compose.runtime.rememberCoroutineScope
import com.example.money.ui.theme.MoneySpacing
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.isTabletWidth
import com.example.money.ui.util.HapticStrength
import com.example.money.ui.util.vibrate
import com.example.money.ui.util.vibrateShort
import com.example.money.ui.navigation.NotificationPermissionReason
import com.example.money.ui.theme.moneySubheadingStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    launchConfig: HomeLaunchConfig = HomeLaunchConfig(),
    notificationsEnabled: Boolean = true,
    notificationPromptDismissed: Boolean = false,
    requestNotificationPermissionIfNeeded: (NotificationPermissionReason) -> Unit = {},
    onDismissNotificationPrompt: () -> Unit = {},
    onRequestCalendarPermission: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {},
    onOpenAppLanguage: () -> Unit = {},
    onOpenPrivacySettings: () -> Unit = {},
    onOpenAutopilotSettings: () -> Unit = {},
    onOpenCalendarSettings: () -> Unit = {},
    onOpenDebtNotificationSettings: () -> Unit = {},
    onOpenBackupSettings: () -> Unit = {},
    onOpenSyncSettings: () -> Unit = {},
    onOpenGoalHub: () -> Unit = {},
    onOpenClients: () -> Unit = {},
    onLaunchConfigConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val debtorNotFoundMessage = stringResource(R.string.debtor_not_found)
    val calendarSyncFailedMessage = stringResource(R.string.calendar_sync_failed)
    val calendarRemovedMessage = stringResource(R.string.calendar_removed)
    val syncUrlCopiedMessage = stringResource(R.string.sync_web_url_copied)
    val autopilotAppliedTemplate = stringResource(R.string.autopilot_applied)
    val autopilotFailedMessage = stringResource(R.string.autopilot_apply_failed)
    val app = context.applicationContext as MoneyApplication

    val goalViewModel: GoalViewModel = viewModel(
        factory = GoalViewModel.Factory(app.goalRepository),
    )
    val budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModel.Factory(app.budgetRepository),
    )
    val debtListViewModel: DebtListViewModel = viewModel(
        factory = DebtListViewModel.Factory(app.debtorRepository, app.preferencesStore),
    )
    val noteViewModel: NoteViewModel = viewModel(
        factory = NoteViewModel.Factory(app.noteRepository),
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
    val notes by noteViewModel.visibleNotes.collectAsStateWithLifecycle()
    val noteLines by noteViewModel.visibleLines.collectAsStateWithLifecycle()
    val currencyRegion by app.preferencesStore.currencyRegion.collectAsStateWithLifecycle(
        initialValue = CurrencyRegion.US,
    )
    val appMode by app.preferencesStore.appMode.collectAsStateWithLifecycle(
        initialValue = AppMode.PERSONAL,
    )
    val modeAccent = if (appMode == AppMode.PROFESSIONAL) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val modeOnAccent = if (appMode == AppMode.PROFESSIONAL) {
        MaterialTheme.colorScheme.onTertiary
    } else {
        MaterialTheme.colorScheme.onPrimary
    }
    SideEffect { Formatters.activeRegion = currencyRegion }
    val goalActions by goalViewModel.actions.collectAsStateWithLifecycle()
    val allDebtors by debtorsViewModel.allDebtorsList.collectAsStateWithLifecycle()
    val debtors by debtListViewModel.debtors.collectAsStateWithLifecycle()
    val debtTab by debtListViewModel.debtTab.collectAsStateWithLifecycle()
    val debtorFilter by debtListViewModel.filter.collectAsStateWithLifecycle()
    val debtSearchQuery by debtListViewModel.searchQuery.collectAsStateWithLifecycle()
    val debtMetrics by debtorsViewModel.debtMetrics.collectAsStateWithLifecycle()
    val privacySettings by debtorsViewModel.privacySettings.collectAsStateWithLifecycle()
    val autopilotSettings by debtorsViewModel.autopilotSettings.collectAsStateWithLifecycle()
    val advisorMessages by debtorsViewModel.advisorMessages.collectAsStateWithLifecycle()
    val budgetSummary by budgetViewModel.budgetSummary.collectAsStateWithLifecycle()
    val availableBalanceCents = budgetSummary.availableBalanceCents
    val goalMetrics = remember(goals) { GlobalFinanceEngine.activeGoalMetrics(goals) }
    val userProfile by app.preferencesStore.userProfile.collectAsStateWithLifecycle(initialValue = UserProfile())
    val goalProgressPercent = remember(goalMetrics) {
        val pool = goalMetrics.totalAccumulatedCents + goalMetrics.totalRemainingToGoalCents
        if (pool <= 0L) 0 else ((goalMetrics.totalAccumulatedCents * 100L) / pool).toInt().coerceIn(0, 100)
    }

    var selectedTab by remember { mutableIntStateOf(launchConfig.initialTab.ordinal) }
    val showAddDebtorDialogState = remember { mutableStateOf(false) }
    var showAddDebtorDialog by showAddDebtorDialogState
    val editDebtorState = remember { mutableStateOf<Debtor?>(null) }
    var editDebtor by editDebtorState
    val showDebtPaymentDialogState = remember { mutableStateOf<Debtor?>(null) }
    var showDebtPaymentDialog by showDebtPaymentDialogState
    val historyDebtorState = remember { mutableStateOf<Debtor?>(null) }
    val debtPaymentPrefillCentsState = remember { mutableLongStateOf(0L) }
    var debtPaymentPrefillCents by debtPaymentPrefillCentsState
    val showTransferDialogState = remember { mutableStateOf(false) }
    var showTransferDialog by showTransferDialogState
    val showExpenseDialogState = remember { mutableStateOf(false) }
    var showExpenseDialog by showExpenseDialogState
    val goalExpenseTargetState = remember { mutableStateOf<SavingsGoal?>(null) }
    var goalExpenseTarget by goalExpenseTargetState
    val showGoalTransferDialogState = remember { mutableStateOf<SavingsGoal?>(null) }
    var showGoalTransferDialog by showGoalTransferDialogState
    val showAddGoalDialogState = remember { mutableStateOf(false) }
    var showAddGoalDialog by showAddGoalDialogState
    val progressLogGoalState = remember { mutableStateOf<SavingsGoal?>(null) }
    var progressLogGoal by progressLogGoalState
    val editGoalState = remember { mutableStateOf<SavingsGoal?>(null) }
    var editGoal by editGoalState
    val stepsGoalIdState = remember { mutableStateOf<Long?>(null) }
    var stepsGoalId by stepsGoalIdState
    val showAddSheetState = remember { mutableStateOf(false) }
    var showAddSheet by showAddSheetState
    var showDebtAddMenu by remember { mutableStateOf(false) }
    val showNoteFormState = remember { mutableStateOf(false) }
    var showNoteForm by showNoteFormState
    val openNoteIdState = rememberSaveable(
        saver = Saver(
            save = { state -> state.value },
            restore = { saved -> mutableStateOf(saved) },
        ),
    ) { mutableStateOf<Long?>(null) }
    var openNoteId by openNoteIdState
    val showCurrencyPickerState = remember { mutableStateOf(false) }
    var showCurrencyPicker by showCurrencyPickerState
    var addHighlight by remember { mutableStateOf(AddAction.Expense) }
    var addSheetActions by remember { mutableStateOf(AddAction.entries.toList()) }
    var showTopMenu by remember { mutableStateOf(false) }
    val showAdvisorSheetState = remember { mutableStateOf(false) }
    var showAdvisorSheet by showAdvisorSheetState
    val showIncomeIdeasState = remember { mutableStateOf(false) }
    var showIncomeIdeas by showIncomeIdeasState
    val remindDebtorState = remember { mutableStateOf<Debtor?>(null) }
    var remindDebtor by remindDebtorState

    val personalBranchSettings by debtorsViewModel.personalBranchSettings.collectAsStateWithLifecycle()

    val celebrationDebtor by debtorsViewModel.celebrationDebtor.collectAsStateWithLifecycle()
    val pendingCalendarClose by debtorsViewModel.pendingCalendarClose.collectAsStateWithLifecycle()
    val calendarMessage by debtorsViewModel.calendarMessage.collectAsStateWithLifecycle()
    val showCheckIn by debtorsViewModel.showCheckIn.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var openDebtSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showWorkClients by rememberSaveable { mutableStateOf(false) }
    var photoLines by remember { mutableStateOf<List<ParsedDebtLine>?>(null) }
    var photoBusy by remember { mutableStateOf(false) }
    val cameraFile = remember { mutableStateOf<java.io.File?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = cameraFile.value
        cameraFile.value = null
        if (!saved || file == null) {
            file?.delete()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            photoBusy = true
            try {
                photoLines = DebtPhotoOcr.recognize(context, listOf(file))
            } finally {
                file.delete()
                photoBusy = false
            }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(8),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        scope.launch {
            photoBusy = true
            val files = uris.mapNotNull { DebtPhotoFiles.copyUri(context, it) }
            try {
                photoLines = DebtPhotoOcr.recognize(context, files)
            } finally {
                files.forEach { it.delete() }
                photoBusy = false
            }
        }
    }
    val startDebtCamera: () -> Unit = {
        val file = DebtPhotoFiles.newCapture(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraFile.value = file
        cameraLauncher.launch(uri)
    }
    val startDebtGallery: () -> Unit = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val importFromPhoto = appMode == AppMode.PROFESSIONAL
    var tabletSection by remember {
        mutableStateOf(
            when (launchConfig.initialTab) {
                HomeTab.Home -> HomeSection.Home
                HomeTab.Goals -> HomeSection.Goals
                HomeTab.Debts -> HomeSection.Debts
                HomeTab.Notes -> HomeSection.Notes
                HomeTab.More -> HomeSection.Notes
            },
        )
    }
    var tabletSelection by remember { mutableStateOf<HomeListSelection>(HomeListSelection.Overview) }
    var highlightedDebtorId by remember { mutableStateOf<Long?>(null) }
    val contactHistoryTargetState = remember { mutableStateOf<Pair<String, String>?>(null) }
    var contactHistoryTarget by contactHistoryTargetState

    val openContactHistory: (String, String) -> Unit = { lookupKey, displayName ->
        contactHistoryTarget = lookupKey to displayName
    }

    suspend fun showMessage(message: String) {
        snackbarHostState.showSnackbar(message)
    }

    val noteDeletedMessage = stringResource(R.string.note_deleted)
    val debtorDeletedMessage = stringResource(R.string.msg_debtor_deleted)
    val goalDeletedMessage = stringResource(R.string.msg_goal_deleted)
    val debtorPaidMessage = stringResource(R.string.msg_debtor_paid)
    val undoDeleteLabel = stringResource(R.string.action_undo)
    val undoJob = remember { mutableStateOf<Job?>(null) }
    fun offerUndo(
        message: String = noteDeletedMessage,
        start: () -> Unit = {},
        commit: () -> Unit = {},
        undo: () -> Unit,
    ) {
        start()
        val previous = undoJob.value
        val session = UndoSession(commit = commit, undo = undo)
        val job = scope.launch {
            previous?.join()
            var undone = false
            try {
                val result = withTimeoutOrNull(5_000) {
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = undoDeleteLabel,
                        duration = SnackbarDuration.Indefinite,
                    )
                }
                undone = result == SnackbarResult.ActionPerformed
                if (undone) vibrate(context, HapticStrength.MEDIUM)
            } finally {
                snackbarHostState.currentSnackbarData?.dismiss()
                session.finish(undone && isActive)
            }
        }
        previous?.cancel()
        undoJob.value = job
    }
    fun deleteDebtorWithUndo(debtor: Debtor) {
        offerUndo(
            message = debtorDeletedMessage,
            start = { debtorsViewModel.stageDelete(debtor) },
            commit = { debtorsViewModel.commitDelete(debtor) },
            undo = { debtorsViewModel.unstageDelete(debtor.id) },
        )
    }
    fun deleteGoalWithUndo(goal: SavingsGoal) {
        offerUndo(
            message = goalDeletedMessage,
            start = { goalViewModel.stageDelete(goal) },
            commit = { goalViewModel.commitDelete(goal) },
            undo = { goalViewModel.unstageDelete(goal.id) },
        )
    }
    fun markDebtPaidWithUndo(debtor: Debtor) {
        scope.launch {
            val paymentId = debtorsViewModel.markAsPaidReturningId(debtor.id) ?: return@launch
            offerUndo(
                message = debtorPaidMessage,
                undo = {
                    scope.launch { debtorsViewModel.undoPaymentSync(debtor.id, paymentId) }
                },
            )
        }
    }

    val welcomeMessage = stringResource(R.string.welcome_title)
    LaunchedEffect(userProfile.onboardingCompleted) {
        if (userProfile.onboardingCompleted && WelcomePreferences.shouldShowWelcome(context)) {
            WelcomePreferences.markWelcomeSeen(context)
            showMessage(welcomeMessage)
        }
    }

    LaunchedEffect(
        launchConfig.openExpenseDialog,
        launchConfig.openQuickAddDialog,
        launchConfig.openAddDebtDialog,
        launchConfig.initialTab,
        launchConfig.initialDebtTab,
        launchConfig.highlightDebtorId,
        launchConfig.openDebtPaymentForDebtorId,
        launchConfig.openWeeklyCheckIn,
    ) {
        selectedTab = launchConfig.initialTab.ordinal
        tabletSection = when (launchConfig.initialTab) {
            HomeTab.Home -> HomeSection.Home
            HomeTab.Goals -> HomeSection.Goals
            HomeTab.Debts -> HomeSection.Debts
            HomeTab.Notes -> HomeSection.Notes
            HomeTab.More -> HomeSection.Notes
        }
        debtListViewModel.setDebtTab(launchConfig.initialDebtTab)

        if (launchConfig.openAddDebtDialog) {
            showAddDebtorDialog = true
        }

        if (launchConfig.openExpenseDialog || launchConfig.openQuickAddDialog) {
            addHighlight = plusHighlight(PagePlusAction.AddAnything)
            addSheetActions = plusSheetActions(PagePlusAction.AddAnything).orEmpty()
            showAddSheet = true
        }

        val targetDebtorId = launchConfig.openDebtPaymentForDebtorId
            ?: launchConfig.highlightDebtorId
        if (targetDebtorId != null) {
            val debtor = debtorsViewModel.getDebtorById(targetDebtorId)
            if (debtor != null) {
                debtListViewModel.setFilter(
                    when {
                        debtor.isPaid -> DebtorFilter.PAID
                        debtor.dueStatus() == DebtDueStatus.OVERDUE ->
                            DebtorFilter.OVERDUE
                        else -> DebtorFilter.ACTIVE
                    },
                )
                debtListViewModel.setDebtTab(DebtTab.forDebtType(debtor.debtType))
                highlightedDebtorId = targetDebtorId
                tabletSection = HomeSection.Debts
                tabletSelection = HomeListSelection.DebtorItem(debtor)
                if (launchConfig.openDebtPaymentForDebtorId != null && !debtor.isPaid) {
                    showDebtPaymentDialog = debtor
                }
            } else if (launchConfig.highlightDebtorId != null) {
                showMessage(debtorNotFoundMessage)
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
            "calendar_failed" -> showMessage(calendarSyncFailedMessage)
            "calendar_removed" -> showMessage(calendarRemovedMessage)
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
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP &&
                (debtorsViewModel.hasStagedDeletes() || goalViewModel.hasStagedDeletes())
            ) {
                undoJob.value?.cancel()
                debtorsViewModel.commitStagedDeletes()
                goalViewModel.commitStagedDeletes()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(appMode) {
        if (launchConfig.highlightDebtorId == null && launchConfig.openDebtPaymentForDebtorId == null) {
            debtListViewModel.setFilter(
                if (appMode == AppMode.PROFESSIONAL) DebtorFilter.ALL else DebtorFilter.ACTIVE,
            )
        }
    }
    LaunchedEffect(hasCalendarPermissions, calendarSettings.autoSyncEnabled) {
        if (hasCalendarPermissions && calendarSettings.autoSyncEnabled) {
            debtorsViewModel.retryCalendarSyncIfNeeded()
        }
    }

    HomeDialogs(
        context = context,
        app = app,
        scope = scope,
        showMessage = ::showMessage,
        openContactHistory = openContactHistory,
        debtorsViewModel = debtorsViewModel,
        budgetViewModel = budgetViewModel,
        goalViewModel = goalViewModel,
        noteViewModel = noteViewModel,
        debtTab = debtTab,
        availableBalanceCents = availableBalanceCents,
        allDebtors = allDebtors,
        privacySettings = privacySettings,
        personalBranchSettings = personalBranchSettings,
        goals = goals,
        goalActions = goalActions,
        goalMetrics = goalMetrics,
        advisorMessages = advisorMessages,
        currencyRegion = currencyRegion,
        celebrationDebtor = celebrationDebtor,
        pendingCalendarClose = pendingCalendarClose,
        addHighlight = addHighlight,
        addSheetActions = addSheetActions,
        showAddDebtorDialogState = showAddDebtorDialogState,
        editDebtorState = editDebtorState,
        contactHistoryTargetState = contactHistoryTargetState,
        remindDebtorState = remindDebtorState,
        showDebtPaymentDialogState = showDebtPaymentDialogState,
        debtPaymentPrefillCentsState = debtPaymentPrefillCentsState,
        showIncomeIdeasState = showIncomeIdeasState,
        showTransferDialogState = showTransferDialogState,
        showGoalTransferDialogState = showGoalTransferDialogState,
        showExpenseDialogState = showExpenseDialogState,
        goalExpenseTargetState = goalExpenseTargetState,
        showAddGoalDialogState = showAddGoalDialogState,
        progressLogGoalState = progressLogGoalState,
        editGoalState = editGoalState,
        showNoteFormState = showNoteFormState,
        openNoteIdState = openNoteIdState,
        showCurrencyPickerState = showCurrencyPickerState,
        showAddSheetState = showAddSheetState,
        showAdvisorSheetState = showAdvisorSheetState,
        onDebtWithDueDateSaved = {
            requestNotificationPermissionIfNeeded(NotificationPermissionReason.FIRST_DUE_DEBT)
        },
    )

    photoLines?.let { lines ->
        val savedMessage = stringResource(R.string.debt_import_saved)
        DebtPhotoReviewSheet(
            lines = lines,
            onDismiss = { photoLines = null },
            onSave = { imported ->
                scope.launch {
                    val type = if (debtTab == com.example.money.ui.debtors.DebtTab.IOwe) {
                        DebtType.DEBTOR
                    } else {
                        DebtType.CREDITOR
                    }
                    val count = debtorsViewModel.importDebts(
                        imported,
                        currencyRegion.currencyCode,
                        type,
                    )
                    photoLines = null
                    if (count > 0) {
                        vibrate(context, HapticStrength.MEDIUM)
                        showMessage(savedMessage)
                        if (imported.any { it.dateDue > 0L }) {
                            requestNotificationPermissionIfNeeded(NotificationPermissionReason.FIRST_DUE_DEBT)
                        }
                    }
                }
            },
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val isTablet = isTabletWidth(maxWidth)
        val syncState by app.syncService.connectionState.collectAsStateWithLifecycle()

        val topBar: @Composable () -> Unit = {
            val page = HomeTab.entries.getOrElse(selectedTab) { HomeTab.Home }
            val title = when {
                page == HomeTab.Debts && showWorkClients -> stringResource(R.string.clients_title)
                page == HomeTab.Debts -> stringResource(R.string.tab_debts)
                page == HomeTab.Goals -> stringResource(R.string.section_goals)
                page == HomeTab.More -> stringResource(R.string.nav_more)
                else -> stringResource(R.string.home_brand)
            }
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    if (isTablet) {
                        SingleChoiceSegmentedButtonRow {
                            AppMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = appMode == mode,
                                    onClick = {
                                        scope.launch {
                                            app.preferencesStore.setAppMode(mode)
                                            app.rescheduleDebtReminders()
                                        }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, AppMode.entries.size),
                                    colors = SegmentedButtonDefaults.colors(
                                        activeContainerColor = modeAccent,
                                        activeContentColor = modeOnAccent,
                                    ),
                                    label = {
                                        Text(
                                            stringResource(
                                                if (mode == AppMode.PROFESSIONAL) {
                                                    R.string.mode_professional_short
                                                } else {
                                                    R.string.mode_personal_short
                                                },
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    } else {
                        if (page == HomeTab.Goals) {
                            IconButton(onClick = onOpenGoalHub) {
                                Icon(
                                    Icons.Outlined.Flag,
                                    contentDescription = stringResource(R.string.goal_hub_open),
                                )
                            }
                        }
                        IconButton(onClick = {
                            if (selectedTab != HomeTab.Debts.ordinal) vibrate(context, HapticStrength.MEDIUM)
                            openDebtSearch = true
                            selectedTab = HomeTab.Debts.ordinal
                        }) {
                            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.debt_search_hint))
                        }
                        IconButton(onClick = onOpenDebtNotificationSettings) {
                            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.settings_open))
                        }
                    }
                    syncState.webSocketUrl?.let { webSocketUrl ->
                    IconButton(onClick = { showTopMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                    }
                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                    ) {
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
                                        showMessage(syncUrlCopiedMessage)
                                    }
                                },
                            )
                    }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        }

        val fab: @Composable () -> Unit = {
            val pagePlus = if (isTablet) {
                plusActionFor(tabletSection)
            } else {
                plusActionFor(HomeTab.entries[selectedTab])
            }
            val showFab = if (isTablet) {
                true
            } else {
                showsPageFab(HomeTab.entries.getOrElse(selectedTab) { HomeTab.Home }, tablet = false)
            }
            if (showFab) FloatingActionButton(
                modifier = Modifier.pressScale(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                onClick = {
                    when (pagePlus) {
                        PagePlusAction.AddDebt -> {
                            if (importFromPhoto) showDebtAddMenu = true else showAddDebtorDialog = true
                        }
                        PagePlusAction.AddGoal -> showAddGoalDialog = true
                        PagePlusAction.AddNote -> showNoteForm = true
                        PagePlusAction.AddAnything -> {
                            addHighlight = plusHighlight(pagePlus)
                            addSheetActions = plusSheetActions(pagePlus).orEmpty()
                            showAddSheet = true
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(plusContentRes(pagePlus)),
                )
            }
        }

        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMoneySharedTransition provides this) {
        if (isTablet) {
            androidx.compose.animation.AnimatedVisibility(visible = true) {
            CompositionLocalProvider(com.example.money.ui.motion.LocalMoneyVisibility provides this) {
            TabletHomeLayout(
                section = tabletSection,
                onSectionChange = { section ->
                    if (section != HomeSection.Notes) openNoteId = null
                    tabletSection = section
                },
                selection = tabletSelection,
                onSelectionChange = { tabletSelection = it },
                syncState = syncState,
                snackbarHostState = snackbarHostState,
                topBar = topBar,
                floatingActionButton = fab,
                goals = goals,
                goalActions = goalActions,
                debtors = debtors,
                allDebtors = allDebtors,
                debtTab = debtTab,
                onDebtTabChange = debtListViewModel::setDebtTab,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = debtListViewModel::setFilter,
                debtSearchQuery = debtSearchQuery,
                onDebtSearchQueryChange = debtListViewModel::setSearchQuery,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                planProgressPercent = goalProgressPercent,
                onAddGoal = { showAddGoalDialog = true },
                onEditGoal = { editGoal = it },
                onOpenGoal = { stepsGoalId = it.id },
                onDeleteGoal = ::deleteGoalWithUndo,
                onGoalTransfer = { showGoalTransferDialog = it },
                onLogGoal = { progressLogGoal = it },
                onExpense = { goalExpenseTarget = it },
                onRemindDebtor = { debtor -> remindDebtor = debtor },
                onAddDebtPayment = { debtor ->
                    debtPaymentPrefillCents = 0L
                    showDebtPaymentDialog = debtor
                },
                onOpenDebtor = { historyDebtorState.value = it },
                onAccelerateDebtPayment = { debtor, prefillCents ->
                    debtPaymentPrefillCents = prefillCents
                    showDebtPaymentDialog = debtor
                },
                onMarkDebtPaid = ::markDebtPaidWithUndo,
                privacySettings = privacySettings,
                autopilotSettings = autopilotSettings,
                showCheckIn = showCheckIn,
                onDismissCheckIn = debtorsViewModel::dismissCheckIn,
                onOpenCheckIn = debtorsViewModel::openCheckIn,
                onOpenAdvisor = { showAdvisorSheet = true },
                onApplyAutopilot = {
                    scope.launch {
                        val plan = debtorsViewModel.applyMicroPaymentSync()
                        if (plan != null) {
                            showMessage(
                                autopilotAppliedTemplate.format(
                                    Formatters.currencyFromCents(
                                        plan.amountCents,
                                        hide = privacySettings.hideAmounts,
                                    ),
                                    plan.debtorName,
                                ),
                            )
                        } else {
                            showMessage(autopilotFailedMessage)
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
                onOpenSyncSettings = onOpenSyncSettings,
                onOpenVoiceSettings = onOpenVoiceSettings,
                calendarPermissionGranted = hasCalendarPermissions,
                onRequestCalendarPermission = onRequestCalendarPermission,
                onOpenIncomeIdeas = { showIncomeIdeas = true },
                onOpenCurrency = { showCurrencyPicker = true },
                notes = notes,
                noteLines = noteLines,
                openNoteId = openNoteId,
                onOpenNote = { openNoteId = it },
                onCloseNote = { openNoteId = null },
                onUpdateNote = noteViewModel::updateNote,
                onSetMark = noteViewModel::setMark,
                onSetClosed = noteViewModel::setClosed,
                onDeleteNote = { id ->
                    openNoteId = null
                    offerUndo(
                        start = { noteViewModel.scheduleDeleteNote(id) },
                        undo = { noteViewModel.undoDeleteNote(id) },
                    )
                },
                onAddLine = noteViewModel::addLine,
                onUpdateLine = noteViewModel::updateLine,
                onDeleteLine = { noteId, lineId ->
                    offerUndo(
                        start = { noteViewModel.scheduleDeleteLine(noteId, lineId) },
                        undo = { noteViewModel.undoDeleteLine(lineId) },
                    )
                },
                onNoteTitleChange = noteViewModel::onTitleChange,
                onNoteBodyChange = noteViewModel::onBodyChange,
                onFlushNoteDraft = noteViewModel::flushDraft,
                noteDraftBlocksRemote = noteViewModel::hasUnsavedDraft,
                onEditDebtor = { editDebtor = it },
                onDeleteDebtor = ::deleteDebtorWithUndo,
                onViewContactHistory = { debtor ->
                    debtor.contactLookupKey?.let { key ->
                        openContactHistory(key, debtor.name)
                    }
                },
                onSyncCalendar = debtorsViewModel::syncDebtToCalendarManually,
                onRemoveCalendar = debtorsViewModel::removeDebtFromCalendarManually,
                onAddDebt = { showAddDebtorDialog = true },
                emptyDebtorsTitle = { filter -> emptyDebtorsTitle(filter) },
                appMode = appMode,
                onOpenClients = onOpenClients,
                showClients = showWorkClients,
                onToggleClients = { showWorkClients = !showWorkClients },
            )
            }
            }
        } else {
            PhoneHomeLayout(
                snackbarHostState = snackbarHostState,
                topBar = topBar,
                floatingActionButton = fab,
                listState = listState,
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab != selectedTab) vibrate(context, HapticStrength.MEDIUM)
                    if (HomeTab.entries[tab] != HomeTab.Notes) openNoteId = null
                    selectedTab = tab
                },
                goals = goals,
                goalActions = goalActions,
                debtors = debtors,
                allDebtors = allDebtors,
                debtTab = debtTab,
                onDebtTabChange = debtListViewModel::setDebtTab,
                debtorFilter = debtorFilter,
                onDebtorFilterChange = debtListViewModel::setFilter,
                debtSearchQuery = debtSearchQuery,
                onDebtSearchQueryChange = debtListViewModel::setSearchQuery,
                goalMetrics = goalMetrics,
                debtMetrics = debtMetrics,
                planProgressPercent = goalProgressPercent,
                highlightedDebtorId = highlightedDebtorId,
                onAddGoal = { showAddGoalDialog = true },
                onEditGoal = { editGoal = it },
                onOpenGoal = { stepsGoalId = it.id },
                onDeleteGoal = ::deleteGoalWithUndo,
                onGoalTransfer = { showGoalTransferDialog = it },
                onLogGoal = { progressLogGoal = it },
                onExpense = { goalExpenseTarget = it },
                onRemindDebtor = { debtor -> remindDebtor = debtor },
                onAddDebtPayment = { debtor ->
                    debtPaymentPrefillCents = 0L
                    showDebtPaymentDialog = debtor
                },
                onOpenDebtor = { historyDebtorState.value = it },
                onAccelerateDebtPayment = { debtor, prefillCents ->
                    debtPaymentPrefillCents = prefillCents
                    showDebtPaymentDialog = debtor
                },
                onMarkDebtPaid = ::markDebtPaidWithUndo,
                onEditDebtor = { editDebtor = it },
                onDeleteDebtor = ::deleteDebtorWithUndo,
                onViewContactHistory = { debtor ->
                    debtor.contactLookupKey?.let { key ->
                        openContactHistory(key, debtor.name)
                    }
                },
                onSyncCalendar = debtorsViewModel::syncDebtToCalendarManually,
                onRemoveCalendar = debtorsViewModel::removeDebtFromCalendarManually,
                onOpenVoiceSettings = onOpenVoiceSettings,
                onOpenAppLanguage = onOpenAppLanguage,
                onOpenPrivacySettings = onOpenPrivacySettings,
                onOpenAutopilotSettings = onOpenAutopilotSettings,
                onOpenCalendarSettings = onOpenCalendarSettings,
                onOpenDebtNotificationSettings = onOpenDebtNotificationSettings,
                onOpenBackupSettings = onOpenBackupSettings,
                onOpenSyncSettings = onOpenSyncSettings,
                onOpenIncomeIdeas = { showIncomeIdeas = true },
                onOpenCurrency = { showCurrencyPicker = true },
                notes = notes,
                noteLines = noteLines,
                openNoteId = openNoteId,
                onOpenNote = { openNoteId = it },
                onCloseNote = { openNoteId = null },
                onUpdateNote = noteViewModel::updateNote,
                onSetMark = noteViewModel::setMark,
                onSetClosed = noteViewModel::setClosed,
                onDeleteNote = { id ->
                    openNoteId = null
                    offerUndo(
                        start = { noteViewModel.scheduleDeleteNote(id) },
                        undo = { noteViewModel.undoDeleteNote(id) },
                    )
                },
                onAddLine = noteViewModel::addLine,
                onUpdateLine = noteViewModel::updateLine,
                onDeleteLine = { noteId, lineId ->
                    offerUndo(
                        start = { noteViewModel.scheduleDeleteLine(noteId, lineId) },
                        undo = { noteViewModel.undoDeleteLine(lineId) },
                    )
                },
                onNoteTitleChange = noteViewModel::onTitleChange,
                onNoteBodyChange = noteViewModel::onBodyChange,
                onFlushNoteDraft = noteViewModel::flushDraft,
                noteDraftBlocksRemote = noteViewModel::hasUnsavedDraft,
                calendarPermissionGranted = hasCalendarPermissions,
                onRequestCalendarPermission = onRequestCalendarPermission,
                onAddDebt = { showAddDebtorDialog = true },
                notificationsEnabled = notificationsEnabled || notificationPromptDismissed,
                onRequestNotificationPermission = {
                    requestNotificationPermissionIfNeeded(NotificationPermissionReason.BANNER)
                },
                onDismissNotificationPrompt = onDismissNotificationPrompt,
                showCheckIn = showCheckIn,
                onDismissCheckIn = debtorsViewModel::dismissCheckIn,
                onOpenCheckIn = debtorsViewModel::openCheckIn,
                privacySettings = privacySettings,
                autopilotSettings = autopilotSettings,
                onOpenAdvisor = { showAdvisorSheet = true },
                onApplyAutopilot = {
                    scope.launch {
                        val plan = debtorsViewModel.applyMicroPaymentSync()
                        if (plan != null) {
                            showMessage(
                                autopilotAppliedTemplate.format(
                                    Formatters.currencyFromCents(
                                        plan.amountCents,
                                        hide = privacySettings.hideAmounts,
                                    ),
                                    plan.debtorName,
                                ),
                            )
                        } else {
                            showMessage(autopilotFailedMessage)
                        }
                    }
                },
                scope = scope,
                emptyDebtorsTitle = { filter -> emptyDebtorsTitle(filter) },
                appMode = appMode,
                onAppModeChange = { mode ->
                    scope.launch {
                        app.preferencesStore.setAppMode(mode)
                        app.rescheduleDebtReminders()
                    }
                },
                onOpenClients = onOpenClients,
                showClients = showWorkClients,
                onToggleClients = { showWorkClients = !showWorkClients },
                onImportCamera = if (importFromPhoto) startDebtCamera else null,
                onImportGallery = if (importFromPhoto) startDebtGallery else null,
                openDebtSearch = openDebtSearch,
                onDebtSearchOpened = { openDebtSearch = false },
            )
        }
        HomeMotionOverlay(
            historyDebtorState = historyDebtorState,
            allDebtors = allDebtors,
            hideAmounts = privacySettings.hideAmounts,
            onAddAdvance = { debtor, amountCents, givenAt, note ->
                scope.launch {
                    val ok = debtorsViewModel.addDebtAdvanceSync(debtor.id, amountCents, givenAt, note)
                    showMessage(
                        context.getString(
                            if (ok) R.string.msg_debt_advance_saved else R.string.error_debt_advance_failed,
                        ),
                    )
                }
            },
            onAddPayment = { debtor, amountCents, paidAt ->
                scope.launch {
                    val ok = debtorsViewModel.processPaymentSync(debtor.id, amountCents, false, paidAt)
                    showMessage(
                        context.getString(
                            if (ok) R.string.msg_payment_neutral else R.string.error_payment_failed,
                        ),
                    )
                }
            },
            stepsGoalId = stepsGoalId,
            onDismissGoal = { stepsGoalId = null },
            goals = goals,
            goalActions = goalActions,
            onAddAction = { goalId, title -> goalViewModel.addAction(goalId, title) },
            onToggleAction = { action, completed ->
                goalViewModel.setActionCompleted(action.id, completed)
            },
            onDeleteAction = { action -> goalViewModel.deleteAction(action.id) },
        )
        if (showDebtAddMenu) {
            DebtAddMenuSheet(
                onDismiss = { showDebtAddMenu = false },
                onManual = { showAddDebtorDialog = true },
                onCamera = startDebtCamera,
                onGallery = startDebtGallery,
            )
        }
        if (photoBusy) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        }
        }
    }

}

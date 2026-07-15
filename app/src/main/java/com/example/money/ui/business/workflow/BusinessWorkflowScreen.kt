package com.example.money.ui.business.workflow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.messaging.MessagingChannel
import com.example.money.ui.business.BusinessTab
import com.example.money.ui.theme.GoldPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessWorkflowScreen(
    onSelectBusinessTab: (BusinessTab) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: BusinessWorkflowViewModel = viewModel(
        factory = BusinessWorkflowViewModel.Factory(
            app.businessWorkflowUseCases,
            app.businessCommunicationUseCases,
            app.preferencesStore,
        ),
    )
    val route by viewModel.route.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val processes by viewModel.processes.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val debts by viewModel.filteredDebts.collectAsStateWithLifecycle()
    val allDebts by viewModel.debts.collectAsStateWithLifecycle()
    val selectedDebtIds by viewModel.selectedDebtIds.collectAsStateWithLifecycle()
    val messageBody by viewModel.messageBody.collectAsStateWithLifecycle()
    val debtFilter by viewModel.debtFilter.collectAsStateWithLifecycle()
    val billDraft by viewModel.billDraft.collectAsStateWithLifecycle()
    val currentBill by viewModel.currentBill.collectAsStateWithLifecycle()
    val breadcrumb by viewModel.breadcrumb.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingSmsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) pendingSmsAction?.invoke()
        pendingSmsAction = null
    }

    fun showSendResult(result: com.example.money.domain.model.business.BusinessCommunicationSendResult) {
        scope.launch {
            snackbarHostState.showSnackbar(
                context.getString(
                    R.string.workflow_send_result,
                    result.sentCount,
                    result.skippedCount,
                    result.failedCount,
                ),
            )
        }
    }

    fun sendWithSmsPermission(useDirectSms: Boolean, action: suspend () -> com.example.money.domain.model.business.BusinessCommunicationSendResult) {
        scope.launch {
            if (useDirectSms && !BusinessSmsPermissions.canSendDirect(context)) {
                pendingSmsAction = { scope.launch { showSendResult(action()) } }
                smsPermissionLauncher.launch(BusinessSmsPermissions.sendSmsPermission)
            } else {
                showSendResult(action())
            }
        }
    }

    val title = when (val r = route) {
        WorkflowRoute.Categories -> stringResource(R.string.workflow_ux_categories_title)
        WorkflowRoute.CommunicationCenter -> stringResource(R.string.workflow_ux_comm_center_title)
        is WorkflowRoute.Processes -> stringResource(R.string.workflow_ux_branch_title, r.categoryName)
        is WorkflowRoute.Bills -> r.processName
        is WorkflowRoute.BillDetail -> stringResource(R.string.workflow_ux_debt_list_title)
        is WorkflowRoute.MessageComposer -> stringResource(R.string.workflow_ux_message_title)
        is WorkflowRoute.CreateProcess -> stringResource(R.string.workflow_ux_new_process_title)
        is WorkflowRoute.CreateBillInfo -> stringResource(R.string.workflow_ux_bill_title)
        is WorkflowRoute.CreateBillParticipants -> stringResource(R.string.workflow_ux_participants_title)
        is WorkflowRoute.CreateBillDistribution -> stringResource(R.string.workflow_ux_distribution_title)
    }

    val showBack = route !is WorkflowRoute.Categories
    val showFab = route is WorkflowRoute.Processes || route is WorkflowRoute.Bills

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text(title)
                        if (breadcrumb.steps.isNotEmpty()) {
                            WorkflowBreadcrumbBar(breadcrumb = breadcrumb)
                        }
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = viewModel::navigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        when (val r = route) {
                            is WorkflowRoute.Processes -> viewModel.openCreateProcess(r.categoryId)
                            is WorkflowRoute.Bills -> viewModel.startCreateBill(r.processId)
                            else -> Unit
                        }
                    },
                    containerColor = GoldPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        when (val r = route) {
            WorkflowRoute.Categories -> WorkflowCategoriesScreen(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onSelectCategory = viewModel::selectCategory,
                onContinue = viewModel::continueFromCategorySelection,
                onOpenCommunicationCenter = viewModel::openCommunicationCenter,
                modifier = Modifier.padding(padding),
            )
            WorkflowRoute.CommunicationCenter -> WorkflowCommunicationCenterScreen(
                onOpenSmsTab = { onSelectBusinessTab(BusinessTab.Sms) },
                onOpenAutomation = { onSelectBusinessTab(BusinessTab.Settings) },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.Processes -> WorkflowProcessesScreen(
                categoryName = r.categoryName,
                processes = processes,
                onProcessClick = viewModel::openProcess,
                onCreateProcess = { viewModel.openCreateProcess(r.categoryId) },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.Bills -> WorkflowBillsScreen(
                processName = r.processName,
                bills = bills,
                onBillClick = { viewModel.openBill(it.id) },
                onCreateBill = { viewModel.startCreateBill(r.processId) },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.BillDetail -> WorkflowDebtListScreen(
                bill = currentBill,
                debts = debts,
                stats = workflowDebtStatsFrom(allDebts),
                selectedDebtIds = selectedDebtIds,
                debtFilter = debtFilter,
                onFilterChange = viewModel::setDebtFilter,
                onToggleDebt = viewModel::toggleDebtSelection,
                onSelectAll = { viewModel.selectAllDebts(debts.map { it.debt.id }) },
                onMarkPaid = viewModel::markDebtPaid,
                onSendReminders = { viewModel.openMessageComposer(r.billId) },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.MessageComposer -> WorkflowMessageComposerScreen(
                messageBody = messageBody,
                selectedCount = selectedDebtIds.size,
                onMessageChange = viewModel::updateMessageBody,
                onAiImprove = { viewModel.aiImproveMessage(r.billId) },
                onAiShorten = viewModel::aiShortenMessage,
                onAiFormal = { viewModel.aiFormalMessage(r.billId) },
                onSendWhatsApp = {
                    scope.launch {
                        showSendResult(viewModel.sendReminders(context, r.billId, MessagingChannel.WHATSAPP))
                    }
                },
                onSendSms = {
                    scope.launch {
                        val settings = app.preferencesStore.businessBranchSettings.first()
                        sendWithSmsPermission(settings.smsDirectSendEnabled) {
                            viewModel.sendReminders(context, r.billId, MessagingChannel.SMS, settings.smsDirectSendEnabled)
                        }
                    }
                },
                onSendPush = {
                    scope.launch { showSendResult(viewModel.sendPushReminders(context, r.billId)) }
                },
                onSendAll = {
                    scope.launch {
                        val settings = app.preferencesStore.businessBranchSettings.first()
                        sendWithSmsPermission(settings.smsDirectSendEnabled) {
                            viewModel.sendAllChannels(context, r.billId, settings.smsDirectSendEnabled, includePush = true)
                        }
                    }
                },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.CreateProcess -> WorkflowCreateProcessScreen(
                onCreate = { name, description ->
                    viewModel.createProcess(r.categoryId, name, description) { processId ->
                        viewModel.openProcess(
                            WorkflowProcess(id = processId, categoryId = r.categoryId, name = name),
                        )
                    }
                },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.CreateBillInfo -> WorkflowCreateBillInfoScreen(
                draft = billDraft,
                onDraftChange = { draft -> viewModel.updateBillDraft { draft } },
                onContinue = viewModel::continueToParticipants,
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.CreateBillParticipants -> WorkflowCreateBillParticipantsScreen(
                draft = billDraft,
                onDraftChange = { draft -> viewModel.updateBillDraft { draft } },
                onContinue = viewModel::continueToDistribution,
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.CreateBillDistribution -> WorkflowCreateBillDistributionScreen(
                draft = billDraft,
                onDraftChange = { draft -> viewModel.updateBillDraft { draft } },
                onSplitEqually = viewModel::splitDraftEqually,
                onAiDistribute = viewModel::aiDistributeDraft,
                onReset = viewModel::resetDraftAmounts,
                onCreateDebts = {
                    viewModel.createBillFromDraft { billId -> viewModel.openBill(billId) }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

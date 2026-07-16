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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.messaging.MessagingChannel
import com.example.money.ui.business.BusinessTab
import com.example.money.ui.business.navigation.BusinessNavHost
import com.example.money.ui.business.navigation.BusinessRoute
import com.example.money.ui.business.sms.BusinessSmsPanel
import com.example.money.ui.theme.GoldPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessWorkflowScreen(
    onSelectBusinessTab: (BusinessTab, BusinessSmsPanel?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val viewModel: BusinessWorkflowViewModel = viewModel(
        factory = BusinessWorkflowViewModel.Factory(
            app.businessWorkflowUseCases,
            app.businessCommunicationUseCases,
            app.preferencesStore,
        ),
    )
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

    LaunchedEffect(currentRoute, navBackStackEntry?.id) {
        val args = navBackStackEntry?.arguments
        when {
            currentRoute == null || currentRoute == BusinessRoute.Categories.route -> {
                viewModel.bindCategoriesScope()
            }
            currentRoute.startsWith("business_category_flow") -> {
                args?.getString(BusinessRoute.CategoryFlow.ARG_CATEGORY_ID)?.toLongOrNull()
                    ?.let { viewModel.bindCategoryScope(it) }
            }
            currentRoute.startsWith("business_process_bills") -> {
                args?.getString(BusinessRoute.ProcessBills.ARG_PROCESS_ID)?.toLongOrNull()
                    ?.let { viewModel.bindProcessScope(it) }
            }
            currentRoute.startsWith("business_process_create") -> {
                args?.getString(BusinessRoute.ProcessCreate.ARG_CATEGORY_ID)?.toLongOrNull()
                    ?.let { viewModel.openCreateProcess(it) }
            }
            currentRoute.startsWith("business_bill_create") -> {
                args?.getString(BusinessRoute.BillCreate.ARG_PROCESS_ID)?.toLongOrNull()
                    ?.let { processId ->
                        if (billDraft?.processId != processId) {
                            viewModel.startCreateBill(processId)
                        } else {
                            viewModel.bindBillWizardScope(processId)
                        }
                    }
            }
            currentRoute.startsWith("business_participants") -> {
                args?.getString(BusinessRoute.Participants.ARG_PROCESS_ID)?.toLongOrNull()
                    ?.let { viewModel.bindBillWizardScope(it) }
            }
            currentRoute.startsWith("business_debts") -> {
                args?.getString(BusinessRoute.Debts.ARG_PROCESS_ID)?.toLongOrNull()
                    ?.let { viewModel.bindBillWizardScope(it) }
            }
            currentRoute.startsWith("business_bill_detail") -> {
                args?.getString(BusinessRoute.BillDetail.ARG_BILL_ID)?.toLongOrNull()
                    ?.let { viewModel.bindBillScope(it) }
            }
            currentRoute.startsWith("business_message_composer") -> {
                args?.getString(BusinessRoute.MessageComposer.ARG_BILL_ID)?.toLongOrNull()
                    ?.let { viewModel.openMessageComposer(it) }
            }
            currentRoute == BusinessRoute.CommunicationCenter.route -> {
                viewModel.bindCommunicationCenterScope()
            }
        }
    }

    val title = when {
        currentRoute == BusinessRoute.Categories.route -> stringResource(R.string.workflow_ux_categories_title)
        currentRoute == BusinessRoute.CommunicationCenter.route -> stringResource(R.string.workflow_ux_comm_center_title)
        currentRoute?.startsWith("business_category_flow") == true -> {
            val categoryId = navBackStackEntry?.arguments?.getString(BusinessRoute.CategoryFlow.ARG_CATEGORY_ID)?.toLongOrNull()
            val name = categories.find { it.id == categoryId }?.name.orEmpty()
            stringResource(R.string.workflow_ux_branch_title, name)
        }
        currentRoute?.startsWith("business_process_bills") == true -> {
            val processId = navBackStackEntry?.arguments?.getString(BusinessRoute.ProcessBills.ARG_PROCESS_ID)?.toLongOrNull()
            processes.find { it.id == processId }?.name.orEmpty()
        }
        currentRoute?.startsWith("business_bill_detail") == true -> stringResource(R.string.workflow_ux_debt_list_title)
        currentRoute?.startsWith("business_message_composer") == true -> stringResource(R.string.workflow_ux_message_title)
        currentRoute?.startsWith("business_process_create") == true -> stringResource(R.string.workflow_ux_new_process_title)
        currentRoute?.startsWith("business_bill_create") == true -> stringResource(R.string.workflow_ux_bill_title)
        currentRoute?.startsWith("business_participants") == true -> stringResource(R.string.workflow_ux_participants_title)
        currentRoute?.startsWith("business_debts/") == true -> stringResource(R.string.workflow_ux_distribution_title)
        currentRoute == BusinessRoute.Automation.route -> stringResource(R.string.business_automation_title)
        currentRoute == BusinessRoute.History.route -> stringResource(R.string.business_ds_history_title)
        currentRoute == BusinessRoute.Templates.route -> stringResource(R.string.business_ds_templates_title)
        else -> stringResource(R.string.workflow_ux_categories_title)
    }

    val showBack = currentRoute != BusinessRoute.Categories.route
    val showFab = currentRoute?.startsWith("business_category_flow") == true ||
        currentRoute?.startsWith("business_process_bills") == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
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
                        when {
                            currentRoute?.startsWith("business_category_flow") == true -> {
                                val categoryId = navBackStackEntry?.arguments
                                    ?.getString(BusinessRoute.CategoryFlow.ARG_CATEGORY_ID)?.toLongOrNull()
                                categoryId?.let {
                                    viewModel.openCreateProcess(it)
                                    navController.navigate(BusinessRoute.ProcessCreate.create(it))
                                }
                            }
                            currentRoute?.startsWith("business_process_bills") == true -> {
                                val processId = navBackStackEntry?.arguments
                                    ?.getString(BusinessRoute.ProcessBills.ARG_PROCESS_ID)?.toLongOrNull()
                                processId?.let {
                                    viewModel.startCreateBill(it)
                                    navController.navigate(BusinessRoute.BillCreate.create(it))
                                }
                            }
                        }
                    },
                    containerColor = GoldPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        BusinessNavHost(
            navController = navController,
            viewModel = viewModel,
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            processes = processes,
            bills = bills,
            debts = debts,
            allDebts = allDebts,
            selectedDebtIds = selectedDebtIds,
            messageBody = messageBody,
            debtFilter = debtFilter,
            billDraft = billDraft,
            currentBill = currentBill,
            onSelectBusinessTab = onSelectBusinessTab,
            onSendWhatsApp = { billId ->
                scope.launch {
                    showSendResult(viewModel.sendReminders(context, billId, MessagingChannel.WHATSAPP))
                }
            },
            onSendSms = { billId ->
                scope.launch {
                    val settings = app.preferencesStore.businessBranchSettings.first()
                    sendWithSmsPermission(settings.smsDirectSendEnabled) {
                        viewModel.sendReminders(context, billId, MessagingChannel.SMS, settings.smsDirectSendEnabled)
                    }
                }
            },
            onSendPush = { billId ->
                scope.launch { showSendResult(viewModel.sendPushReminders(context, billId)) }
            },
            onSendAll = { billId ->
                scope.launch {
                    val settings = app.preferencesStore.businessBranchSettings.first()
                    sendWithSmsPermission(settings.smsDirectSendEnabled) {
                        viewModel.sendAllChannels(context, billId, settings.smsDirectSendEnabled, includePush = true)
                    }
                }
            },
            modifier = Modifier.padding(padding),
        )
    }
}

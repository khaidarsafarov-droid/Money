package com.example.money.ui.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.money.domain.model.FinanceBranch
import com.example.money.domain.model.business.BusinessCategories
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessInvoice
import com.example.money.domain.model.business.BusinessLedgerType
import com.example.money.domain.model.business.BusinessProject
import com.example.money.domain.model.business.BusinessTransaction
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.InvoiceStatus
import android.content.Context
import android.content.Intent
import com.example.money.ui.business.assistant.BusinessAiAssistantScreen
import com.example.money.ui.business.assistant.BusinessAssistantPrompt
import com.example.money.ui.business.components.BusinessDashboardContent
import com.example.money.ui.business.components.clients.ClientList
import com.example.money.ui.business.components.invoices.InvoiceTable
import com.example.money.ui.business.components.navigation.BusinessNavigationLayout
import com.example.money.ui.business.components.settings.BusinessAutomationSettings
import com.example.money.ui.business.components.transactions.BusinessTransactionTable
import com.example.money.domain.model.business.BusinessDashboardSummary
import com.example.money.domain.model.business.BusinessReportFilters
import com.example.money.domain.model.business.BusinessReportsBundle
import com.example.money.domain.model.business.InvoiceRiskAssessment
import com.example.money.settings.BusinessBranchSettings
import com.example.money.ui.business.assistant.BusinessAssistantMessage
import com.example.money.ui.business.reports.BusinessReportTab
import com.example.money.ui.business.reports.BusinessReportsContent
import com.example.money.ui.business.sms.BusinessSmsScreen
import com.example.money.ui.business.workflow.BusinessWorkflowScreen
import com.example.money.ui.business.theme.BusinessTheme
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHomeScreen(
    onSwitchBranch: (FinanceBranch) -> Unit,
    modifier: Modifier = Modifier,
) {
    BusinessTheme {
        BusinessHomeScreenContent(onSwitchBranch = onSwitchBranch, modifier = modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BusinessHomeScreenContent(
    onSwitchBranch: (FinanceBranch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: BusinessViewModel = viewModel(
        factory = BusinessViewModel.Factory(
            app.businessRepository,
            app.preferencesStore,
            app.businessUseCases,
            app.businessSmsUseCases,
            app.businessInvoiceReminderScheduler,
            app.businessSmsScheduler,
            app.workflowDebtScheduler,
            app.businessCommunicationUseCases,
        ),
    )

    val branchSettings by viewModel.branchSettings.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val reportFilters by viewModel.reportFilters.collectAsStateWithLifecycle()
    val selectedReportTab by viewModel.selectedReportTab.collectAsStateWithLifecycle()
    val invoiceRisks by viewModel.invoiceRisks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val assistantMessages by viewModel.assistantMessages.collectAsStateWithLifecycle()
    val smsHistory by viewModel.smsHistory.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var quickActionDialog by remember { mutableStateOf<BusinessQuickAction?>(null) }

    val pendingQuickAction by viewModel.pendingQuickAction.collectAsStateWithLifecycle()
    LaunchedEffect(pendingQuickAction) {
        pendingQuickAction?.let { action ->
            quickActionDialog = action
            viewModel.consumeQuickAction()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = branchSettings.businessName.ifBlank {
                                stringResource(R.string.business_branch_default_name)
                            },
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(R.string.business_branch_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onSwitchBranch(FinanceBranch.PERSONAL) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.business_switch_personal))
                    }
                },
                actions = {
                    BranchToggle(
                        activeBranch = FinanceBranch.BUSINESS,
                        onSwitch = onSwitchBranch,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (selectedTab != BusinessTab.Dashboard &&
                selectedTab != BusinessTab.Reports &&
                selectedTab != BusinessTab.Settings &&
                selectedTab != BusinessTab.Assistant &&
                selectedTab != BusinessTab.Sms
            ) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GoldPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        BusinessNavigationLayout(
            selectedTab = selectedTab,
            onTabSelected = viewModel::selectTab,
            modifier = Modifier.padding(padding),
            content = {
                BusinessTabContent(
                    selectedTab = selectedTab,
                    branchSettings = branchSettings,
                    dashboard = dashboard,
                    clients = clients,
                    projects = projects,
                    invoices = invoices,
                    transactions = transactions,
                    filters = filters,
                    reports = reports,
                    reportFilters = reportFilters,
                    selectedReportTab = selectedReportTab,
                    invoiceRisks = invoiceRisks,
                    categories = categories,
                    assistantMessages = assistantMessages,
                    smsHistory = smsHistory,
                    smsTemplates = viewModel.smsTemplates,
                    viewModel = viewModel,
                    context = context,
                )
            },
        )
    }

    if (showAddDialog) {
        BusinessAddDialog(
            tab = selectedTab,
            clients = clients,
            projects = projects,
            onDismiss = { showAddDialog = false },
            onAddClient = { name, email, phone, info ->
                viewModel.addClient(name, email, phone, info)
                showAddDialog = false
            },
            onAddProject = { clientId, name, budget ->
                viewModel.addProject(clientId, name, budget)
                showAddDialog = false
            },
            onAddInvoice = { clientId, projectId, amount, issue, due, status ->
                viewModel.addInvoice(clientId, projectId, amount, issue, due, status)
                showAddDialog = false
            },
            onAddTransaction = { type, amount, category, ledger, clientId, projectId, date, note ->
                viewModel.addTransaction(type, amount, category, ledger, clientId, projectId, date, note)
                showAddDialog = false
            },
        )
    }

    quickActionDialog?.let { action ->
        when (action) {
            BusinessQuickAction.ADD_CATEGORY -> AddCategoryDialog(
                onDismiss = { quickActionDialog = null },
                onAdd = { name ->
                    viewModel.addCustomCategory(name)
                    quickActionDialog = null
                },
            )
            else -> BusinessQuickActionDialog(
                action = action,
                clients = clients,
                projects = projects,
                onDismiss = { quickActionDialog = null },
                onAddClient = { name, email, phone, info ->
                    viewModel.addClient(name, email, phone, info)
                    quickActionDialog = null
                },
                onAddInvoice = { clientId, projectId, amount, issue, due, status ->
                    viewModel.addInvoice(clientId, projectId, amount, issue, due, status)
                    quickActionDialog = null
                },
                onAddTransaction = { type, amount, category, ledger, clientId, projectId, date, note ->
                    viewModel.addTransaction(type, amount, category, ledger, clientId, projectId, date, note)
                    quickActionDialog = null
                },
            )
        }
    }
}

@Composable
private fun BusinessTabContent(
    selectedTab: BusinessTab,
    branchSettings: BusinessBranchSettings,
    dashboard: BusinessDashboardSummary,
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    invoices: List<BusinessInvoice>,
    transactions: List<BusinessTransaction>,
    filters: com.example.money.domain.model.business.BusinessDashboardFilters,
    reports: BusinessReportsBundle,
    reportFilters: BusinessReportFilters,
    selectedReportTab: BusinessReportTab,
    invoiceRisks: Map<Long, InvoiceRiskAssessment>,
    categories: List<com.example.money.domain.model.business.BusinessCategory>,
    assistantMessages: List<BusinessAssistantMessage>,
    smsHistory: List<com.example.money.domain.model.business.BusinessSmsLog>,
    smsTemplates: List<com.example.money.domain.model.business.BusinessSmsTemplate>,
    viewModel: BusinessViewModel,
    context: Context,
) {
    when (selectedTab) {
        BusinessTab.Dashboard -> {
            val availableCategories = BusinessCategories.mergeWithCustom(
                branchSettings.customCategories() +
                    categories.map { it.name } +
                    transactions.map { it.category },
            )
            BusinessDashboardContent(
                dashboard = dashboard,
                clients = clients,
                projects = projects,
                transactions = transactions,
                filters = filters,
                dashboardView = branchSettings.dashboardView,
                savedPresets = branchSettings.savedFilterPresets(),
                availableCategories = availableCategories,
                onFiltersChange = { newFilters -> viewModel.updateFilters { newFilters } },
                onApplyPreset = viewModel::applyFilterPreset,
                onApplySavedPreset = viewModel::applySavedFilterPreset,
                onSavePreset = viewModel::saveCurrentFilterPreset,
                onDeleteSavedPreset = viewModel::deleteSavedFilterPreset,
                onClearFilters = viewModel::clearFilters,
                onDashboardViewChange = viewModel::setDashboardView,
                onQuickAction = viewModel::requestQuickAction,
            )
        }
        BusinessTab.Workflows -> BusinessWorkflowScreen(modifier = Modifier.fillMaxSize())
        BusinessTab.Clients -> ClientList(clients = clients)
        BusinessTab.Projects -> BusinessListTab(
            items = projects.map { project ->
                val clientName = clients.find { it.id == project.clientId }?.name ?: "—"
                project.name to clientName
            },
            emptyText = stringResource(R.string.business_empty_projects),
        )
        BusinessTab.Invoices -> InvoiceTable(
            invoices = invoices,
            clients = clients,
            invoiceRisks = invoiceRisks,
            onMarkPaid = viewModel::markInvoicePaid,
        )
        BusinessTab.Cashflow -> BusinessTransactionTable(transactions = transactions)
        BusinessTab.Reports -> BusinessReportsContent(
            reports = reports,
            filters = reportFilters,
            selectedTab = selectedReportTab,
            onTabSelected = viewModel::selectReportTab,
            onPeriodChange = viewModel::setReportPeriod,
            onExport = { type ->
                context.startActivity(
                    Intent.createChooser(
                        viewModel.createReportExportIntent(context, type),
                        context.getString(R.string.business_report_export_csv),
                    ),
                )
            },
        )
        BusinessTab.Assistant -> BusinessAiAssistantScreen(
            messages = assistantMessages,
            onPromptSelected = { promptId ->
                val labelRes = BusinessAssistantPrompt.entries.find { it.id == promptId }?.labelRes
                val label = labelRes?.let { context.getString(it) } ?: promptId
                viewModel.askAssistant(promptId, label)
            },
            modifier = Modifier.fillMaxSize(),
        )
        BusinessTab.Sms -> BusinessSmsScreen(
            clients = clients,
            templates = smsTemplates,
            smsHistory = smsHistory,
            settings = branchSettings,
            onSendSms = { recipients, body, templateId, useDirectSend ->
                viewModel.sendSms(recipients, body, templateId, useDirectSend)
            },
            onSendBulk = { clientIds, body, templateId, useDirectSend ->
                viewModel.sendBulkSms(clientIds, body, templateId, useDirectSend)
            },
            onRenderTemplate = { templateId, clientId ->
                viewModel.renderSmsTemplate(templateId, clientId)
            },
            onAiGenerate = { templateId, clientId, tone ->
                viewModel.aiGenerateSms(templateId, clientId, tone)
            },
            modifier = Modifier.fillMaxSize(),
        )
        BusinessTab.Settings -> BusinessSettingsTab(
            businessName = branchSettings.businessName,
            settings = branchSettings,
            onSaveName = viewModel::updateBusinessName,
            onAutomationChange = viewModel::updateAutomationSettings,
            onAddRule = viewModel::addRecurringRule,
            onDeleteRule = viewModel::deleteRecurringRule,
            onToggleRule = viewModel::toggleRecurringRule,
            onRunNow = { viewModel.runAutomationNow(context) },
        )
    }
}

@Composable
fun BranchToggle(
    activeBranch: FinanceBranch,
    onSwitch: (FinanceBranch) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.padding(end = 8.dp)) {
        SegmentedButton(
            selected = activeBranch == FinanceBranch.PERSONAL,
            onClick = { onSwitch(FinanceBranch.PERSONAL) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
        ) {
            Text(stringResource(R.string.branch_personal))
        }
        SegmentedButton(
            selected = activeBranch == FinanceBranch.BUSINESS,
            onClick = { onSwitch(FinanceBranch.BUSINESS) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = { Icon(Icons.Default.Business, contentDescription = null) },
        ) {
            Text(stringResource(R.string.branch_business))
        }
    }
}

@Composable
private fun BusinessListTab(items: List<Pair<String, String>>, emptyText: String) {
    if (items.isEmpty()) {
        Text(emptyText, modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { (title, subtitle) ->
            ListRow(title, subtitle)
        }
    }
}

@Composable
private fun BusinessPlaceholderTab(message: String) {
    Text(message, modifier = Modifier.padding(24.dp))
}

@Composable
private fun BusinessSettingsTab(
    businessName: String,
    settings: BusinessBranchSettings,
    onSaveName: (String) -> Unit,
    onAutomationChange: ((BusinessBranchSettings) -> BusinessBranchSettings) -> Unit,
    onAddRule: (com.example.money.domain.model.business.RecurringBusinessRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onRunNow: () -> Unit,
) {
    var name by remember(businessName) { mutableStateOf(businessName) }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.business_settings_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { onSaveName(name) }) {
                Text(stringResource(R.string.action_save))
            }
        }
        BusinessAutomationSettings(
            settings = settings,
            recurringRules = settings.recurringRules(),
            onAutomationChange = onAutomationChange,
            onAddRule = onAddRule,
            onDeleteRule = onDeleteRule,
            onToggleRule = onToggleRule,
            onRunNow = onRunNow,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ListRow(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_add_category)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.business_field_category)) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onAdd(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun BusinessQuickActionDialog(
    action: BusinessQuickAction,
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    onDismiss: () -> Unit,
    onAddClient: (String, String, String, String) -> Unit,
    onAddInvoice: (Long, Long?, Double, Long, Long, InvoiceStatus) -> Unit,
    onAddTransaction: (BusinessTransactionType, Double, String, BusinessLedgerType, Long?, Long?, Long, String) -> Unit,
) {
    val tab = when (action) {
        BusinessQuickAction.CREATE_INVOICE -> BusinessTab.Invoices
        BusinessQuickAction.ADD_TRANSACTION -> BusinessTab.Cashflow
        BusinessQuickAction.ADD_CLIENT -> BusinessTab.Clients
        BusinessQuickAction.ADD_CATEGORY -> BusinessTab.Settings
    }
    BusinessAddDialog(
        tab = tab,
        clients = clients,
        projects = projects,
        onDismiss = onDismiss,
        onAddClient = onAddClient,
        onAddProject = { _, _, _ -> },
        onAddInvoice = onAddInvoice,
        onAddTransaction = onAddTransaction,
    )
}

@Composable
private fun BusinessAddDialog(
    tab: BusinessTab,
    clients: List<BusinessClient>,
    projects: List<BusinessProject>,
    onDismiss: () -> Unit,
    onAddClient: (String, String, String, String) -> Unit,
    onAddProject: (Long, String, Double) -> Unit,
    onAddInvoice: (Long, Long?, Double, Long, Long, InvoiceStatus) -> Unit,
    onAddTransaction: (BusinessTransactionType, Double, String, BusinessLedgerType, Long?, Long?, Long, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactInfo by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf(clients.firstOrNull()?.id ?: 0L) }
    var selectedProjectId by remember { mutableStateOf<Long?>(projects.firstOrNull()?.id) }
    var txType by remember { mutableStateOf(BusinessTransactionType.INCOME) }
    var category by remember { mutableStateOf(BusinessCategories.revenue.first()) }
    var ledgerType by remember { mutableStateOf(BusinessLedgerType.OPERATING) }

    val now = System.currentTimeMillis()
    val dueDefault = Calendar.getInstance().apply {
        timeInMillis = now
        add(Calendar.DAY_OF_MONTH, 14)
    }.timeInMillis

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (tab) {
                    BusinessTab.Clients -> stringResource(R.string.business_add_client)
                    BusinessTab.Projects -> stringResource(R.string.business_add_project)
                    BusinessTab.Invoices -> stringResource(R.string.business_add_invoice)
                    BusinessTab.Cashflow -> stringResource(R.string.business_add_transaction)
                    else -> ""
                },
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    BusinessTab.Clients -> {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.business_field_name)) })
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.business_field_email)) })
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(stringResource(R.string.business_field_phone)) })
                        OutlinedTextField(value = contactInfo, onValueChange = { contactInfo = it }, label = { Text(stringResource(R.string.business_field_contact)) })
                    }
                    BusinessTab.Projects -> {
                        if (clients.isEmpty()) {
                            Text(stringResource(R.string.business_need_client_first))
                        } else {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.business_field_project)) })
                            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.business_field_budget)) })
                        }
                    }
                    BusinessTab.Invoices -> {
                        if (clients.isEmpty()) {
                            Text(stringResource(R.string.business_need_client_first))
                        } else {
                            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.business_field_amount)) })
                        }
                    }
                    BusinessTab.Cashflow -> {
                        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text(stringResource(R.string.business_field_amount)) })
                        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text(stringResource(R.string.business_field_category)) })
                    }
                    else -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                    when (tab) {
                        BusinessTab.Clients -> if (name.isNotBlank()) onAddClient(name, email, phone, contactInfo)
                        BusinessTab.Projects -> if (name.isNotBlank() && selectedClientId > 0) onAddProject(selectedClientId, name, parsedAmount)
                        BusinessTab.Invoices -> if (selectedClientId > 0 && parsedAmount > 0) {
                            onAddInvoice(selectedClientId, selectedProjectId, parsedAmount, now, dueDefault, InvoiceStatus.SENT)
                        }
                        BusinessTab.Cashflow -> if (parsedAmount > 0) {
                            onAddTransaction(txType, parsedAmount, category, ledgerType, null, null, now, "")
                        }
                        else -> Unit
                    }
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

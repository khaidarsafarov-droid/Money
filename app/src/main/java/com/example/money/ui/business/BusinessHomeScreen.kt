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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.FinanceNegative
import com.example.money.ui.theme.FinancePositive
import com.example.money.ui.theme.GoldPrimary
import com.example.money.ui.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessHomeScreen(
    onSwitchBranch: (FinanceBranch) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: BusinessViewModel = viewModel(factory = BusinessViewModel.Factory(app.businessRepository, app.preferencesStore))

    val branchSettings by viewModel.branchSettings.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CreamBackground,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground),
            )
        },
        floatingActionButton = {
            if (selectedTab != BusinessTab.Dashboard) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GoldPrimary,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                BusinessTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab.ordinal == index,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    BusinessTab.Dashboard -> stringResource(R.string.business_tab_dashboard)
                                    BusinessTab.Clients -> stringResource(R.string.business_tab_clients)
                                    BusinessTab.Projects -> stringResource(R.string.business_tab_projects)
                                    BusinessTab.Invoices -> stringResource(R.string.business_tab_invoices)
                                    BusinessTab.Cashflow -> stringResource(R.string.business_tab_cashflow)
                                },
                            )
                        },
                    )
                }
            }

            when (selectedTab) {
                BusinessTab.Dashboard -> BusinessDashboardTab(dashboard)
                BusinessTab.Clients -> BusinessListTab(
                    items = clients.map { it.name to (it.email.ifBlank { it.phone }.ifBlank { it.contactInfo }) },
                    emptyText = stringResource(R.string.business_empty_clients),
                )
                BusinessTab.Projects -> BusinessListTab(
                    items = projects.map { project ->
                        val clientName = clients.find { it.id == project.clientId }?.name ?: "—"
                        project.name to clientName
                    },
                    emptyText = stringResource(R.string.business_empty_projects),
                )
                BusinessTab.Invoices -> BusinessInvoiceTab(invoices, clients, viewModel::markInvoicePaid)
                BusinessTab.Cashflow -> BusinessCashflowTab(transactions)
            }
        }
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
private fun BusinessDashboardTab(dashboard: com.example.money.domain.model.business.BusinessDashboardSummary) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MetricCard(
                title = stringResource(R.string.business_metric_revenue),
                amountCents = dashboard.monthRevenueCents,
                color = FinancePositive,
            )
        }
        item {
            MetricCard(
                title = stringResource(R.string.business_metric_expenses),
                amountCents = dashboard.monthExpenseCents,
                color = FinanceNegative,
            )
        }
        item {
            MetricCard(
                title = stringResource(R.string.business_metric_profit),
                amountCents = dashboard.monthNetProfitCents,
                color = if (dashboard.monthNetProfitCents >= 0) FinancePositive else FinanceNegative,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = stringResource(R.string.business_metric_receivables),
                    amountCents = dashboard.expectedReceivablesCents,
                    color = GoldPrimary,
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = stringResource(R.string.business_metric_overdue),
                    amountCents = dashboard.overdueReceivablesCents,
                    color = FinanceNegative,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (dashboard.topClients.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_top_clients)) }
            items(dashboard.topClients) { row ->
                ListRow(row.clientName, Formatters.currencyFromCents(row.revenueCents))
            }
        }
        if (dashboard.topExpenseCategories.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.business_top_expenses)) }
            items(dashboard.topExpenseCategories) { row ->
                ListRow(row.category, Formatters.currencyFromCents(row.amountCents))
            }
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
private fun BusinessInvoiceTab(
    invoices: List<BusinessInvoice>,
    clients: List<BusinessClient>,
    onMarkPaid: (BusinessInvoice) -> Unit,
) {
    if (invoices.isEmpty()) {
        Text(stringResource(R.string.business_empty_invoices), modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(invoices) { invoice ->
            val clientName = clients.find { it.id == invoice.clientId }?.name ?: "—"
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(clientName, fontWeight = FontWeight.SemiBold)
                    Text(Formatters.currencyFromCents(invoice.amountCents))
                    Text("${stringResource(R.string.business_invoice_due)}: ${Formatters.shortDate(invoice.dueDate)}")
                    Text(invoice.status.name)
                    if (invoice.status != InvoiceStatus.PAID) {
                        TextButton(onClick = { onMarkPaid(invoice) }) {
                            Text(stringResource(R.string.business_mark_paid))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BusinessCashflowTab(transactions: List<BusinessTransaction>) {
    if (transactions.isEmpty()) {
        Text(stringResource(R.string.business_empty_cashflow), modifier = Modifier.padding(24.dp))
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(transactions) { tx ->
            val sign = if (tx.type == BusinessTransactionType.INCOME) "+" else "−"
            ListRow(
                "${tx.category} · ${tx.ledgerType.name}",
                "$sign${Formatters.currencyFromCents(tx.amountCents)}",
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amountCents: Long,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                Formatters.currencyFromCents(amountCents),
                style = MaterialTheme.typography.headlineSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
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

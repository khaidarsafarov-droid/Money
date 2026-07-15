package com.example.money.ui.business.workflow

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.contacts.ContactResolver
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtStatus
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowParticipantDraft
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.domain.util.MoneyMath
import com.example.money.messaging.ContactInfoResolver
import com.example.money.messaging.MessagingChannel
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessWorkflowScreen(modifier: Modifier = Modifier) {
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
    val processes by viewModel.processes.collectAsStateWithLifecycle()
    val bills by viewModel.bills.collectAsStateWithLifecycle()
    val debts by viewModel.debts.collectAsStateWithLifecycle()
    val selectedDebtIds by viewModel.selectedDebtIds.collectAsStateWithLifecycle()
    val messageBody by viewModel.messageBody.collectAsStateWithLifecycle()
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
                pendingSmsAction = {
                    scope.launch {
                        showSendResult(action())
                    }
                }
                smsPermissionLauncher.launch(BusinessSmsPermissions.sendSmsPermission)
            } else {
                showSendResult(action())
            }
        }
    }

    val title = when (val r = route) {
        WorkflowRoute.Categories -> stringResource(R.string.workflow_title_categories)
        is WorkflowRoute.Processes -> r.categoryName
        is WorkflowRoute.Bills -> r.processName
        is WorkflowRoute.BillDetail -> stringResource(R.string.workflow_bill_detail_title)
        is WorkflowRoute.CreateProcess -> stringResource(R.string.workflow_create_process)
        is WorkflowRoute.CreateBill -> stringResource(R.string.workflow_create_bill)
    }

    val showBack = route !is WorkflowRoute.Categories
    val showFab = route is WorkflowRoute.Processes || route is WorkflowRoute.Bills

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                            is WorkflowRoute.Bills -> viewModel.openCreateBill(r.processId)
                            else -> Unit
                        }
                    },
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        when (val r = route) {
            WorkflowRoute.Categories -> CategoriesList(
                categories = categories,
                onCategoryClick = viewModel::openCategory,
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.Processes -> ProcessesList(
                processes = processes,
                onProcessClick = viewModel::openProcess,
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.Bills -> BillsList(
                bills = bills,
                onBillClick = { viewModel.openBill(it.id) },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.BillDetail -> BillDetailScreen(
                debts = debts,
                selectedDebtIds = selectedDebtIds,
                messageBody = messageBody,
                onToggleDebt = viewModel::toggleDebtSelection,
                onSelectAll = { viewModel.selectAllDebts(debts.map { it.debt.id }) },
                onClearSelection = viewModel::clearDebtSelection,
                onMessageChange = viewModel::updateMessageBody,
                onAiGenerate = { viewModel.generateMessageForFirstSelected(r.billId) },
                onMarkPaid = viewModel::markDebtPaid,
                onSendWhatsApp = {
                    scope.launch {
                        showSendResult(viewModel.sendReminders(context, r.billId, MessagingChannel.WHATSAPP))
                    }
                },
                onSendSms = {
                    scope.launch {
                        val settings = app.preferencesStore.businessBranchSettings.first()
                        sendWithSmsPermission(settings.smsDirectSendEnabled) {
                            viewModel.sendReminders(
                                context,
                                r.billId,
                                MessagingChannel.SMS,
                                settings.smsDirectSendEnabled,
                            )
                        }
                    }
                },
                onSendPush = {
                    scope.launch {
                        showSendResult(viewModel.sendPushReminders(context, r.billId))
                    }
                },
                onSendAll = {
                    scope.launch {
                        val settings = app.preferencesStore.businessBranchSettings.first()
                        sendWithSmsPermission(settings.smsDirectSendEnabled) {
                            viewModel.sendAllChannels(
                                context,
                                r.billId,
                                settings.smsDirectSendEnabled,
                                includePush = true,
                            )
                        }
                    }
                },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.CreateProcess -> CreateProcessForm(
                onCreate = { name, description ->
                    viewModel.createProcess(r.categoryId, name, description) { processId ->
                        val process = WorkflowProcess(id = processId, categoryId = r.categoryId, name = name)
                        viewModel.openProcess(process)
                    }
                },
                modifier = Modifier.padding(padding),
            )
            is WorkflowRoute.CreateBill -> CreateBillForm(
                onCreate = { title, totalCents, description, period, participants ->
                    viewModel.createBill(
                        processId = r.processId,
                        title = title,
                        totalAmountCents = totalCents,
                        description = description,
                        periodLabel = period,
                        participants = participants,
                    ) { billId -> viewModel.openBill(billId) }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun CategoriesList(
    categories: List<WorkflowCategory>,
    onCategoryClick: (WorkflowCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.workflow_categories_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        items(categories, key = { it.id }) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(category) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ProcessesList(
    processes: List<WorkflowProcess>,
    onProcessClick: (WorkflowProcess) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (processes.isEmpty()) {
        Text(
            text = stringResource(R.string.workflow_empty_processes),
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(processes, key = { it.id }) { process ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProcessClick(process) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(process.name, style = MaterialTheme.typography.titleMedium)
                    if (process.description.isNotBlank()) {
                        Text(
                            process.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillsList(
    bills: List<WorkflowBill>,
    onBillClick: (WorkflowBill) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bills.isEmpty()) {
        Text(
            text = stringResource(R.string.workflow_empty_bills),
            modifier = modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(bills, key = { it.id }) { bill ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBillClick(bill) },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(bill.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Formatters.currencyFromCents(bill.totalAmountCents),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (bill.periodLabel.isNotBlank()) {
                        Text(bill.periodLabel, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun BillDetailScreen(
    debts: List<WorkflowDebtWithParticipant>,
    selectedDebtIds: Set<Long>,
    messageBody: String,
    onToggleDebt: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onMessageChange: (String) -> Unit,
    onAiGenerate: () -> Unit,
    onMarkPaid: (Long) -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendSms: () -> Unit,
    onSendPush: () -> Unit,
    onSendAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalCents = debts.sumOf { it.debt.amountCents }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.workflow_total_bill, Formatters.currencyFromCents(totalCents)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) { Text(stringResource(R.string.workflow_select_all)) }
                TextButton(onClick = onClearSelection) { Text(stringResource(R.string.workflow_clear_selection)) }
            }
        }
        items(debts, key = { it.debt.id }) { item ->
            DebtRow(
                item = item,
                selected = item.debt.id in selectedDebtIds,
                onToggle = { onToggleDebt(item.debt.id) },
                onMarkPaid = { onMarkPaid(item.debt.id) },
            )
        }
        item {
            OutlinedTextField(
                value = messageBody,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.workflow_message_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAiGenerate) {
                    Text(stringResource(R.string.workflow_ai_generate))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onSendWhatsApp,
                    enabled = selectedDebtIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.workflow_send_whatsapp))
                }
                OutlinedButton(
                    onClick = onSendSms,
                    enabled = selectedDebtIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.workflow_send_sms))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onSendPush,
                    enabled = selectedDebtIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.workflow_send_push))
                }
                Button(
                    onClick = onSendAll,
                    enabled = selectedDebtIds.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.workflow_send_all))
                }
            }
        }
    }
}

@Composable
private fun DebtRow(
    item: WorkflowDebtWithParticipant,
    selected: Boolean,
    onToggle: () -> Unit,
    onMarkPaid: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Text(item.participant.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    Formatters.currencyFromCents(item.debt.remainingAmountCents),
                    fontWeight = FontWeight.SemiBold,
                )
                if (item.participant.phone.isBlank()) {
                    Text(
                        stringResource(R.string.debt_remind_need_phone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    when (item.debt.status) {
                        WorkflowDebtStatus.PAID -> stringResource(R.string.workflow_status_paid)
                        WorkflowDebtStatus.PARTIAL -> stringResource(R.string.workflow_status_partial)
                        WorkflowDebtStatus.OPEN -> stringResource(R.string.workflow_status_open)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (item.debt.status != WorkflowDebtStatus.PAID) {
                TextButton(onClick = onMarkPaid) {
                    Text(stringResource(R.string.workflow_mark_paid))
                }
            }
        }
    }
}

@Composable
private fun CreateProcessForm(
    onCreate: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.workflow_process_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.workflow_process_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Button(
            onClick = { onCreate(name, description) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.workflow_create))
        }
    }
}

private data class ParticipantRow(
    val name: String,
    val phone: String,
    val email: String,
    val contactUri: String?,
    val contactLookupKey: String?,
    val amountInput: String,
)

@Composable
private fun CreateBillForm(
    onCreate: (String, Long, String, String, List<WorkflowParticipantDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var totalInput by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("") }
    val participants = remember { mutableStateListOf<ParticipantRow>() }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            ContactResolver.resolve(context, uri)?.let { contact ->
                val info = ContactInfoResolver.resolve(context, uri)
                participants.add(
                    ParticipantRow(
                        name = contact.displayName,
                        phone = info?.phone.orEmpty(),
                        email = info?.email.orEmpty(),
                        contactUri = uri.toString(),
                        contactLookupKey = contact.lookupKey,
                        amountInput = "",
                    ),
                )
            }
        }
    }

    val contactPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactPicker.launch(null)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.workflow_bill_title)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = totalInput,
            onValueChange = { totalInput = it },
            label = { Text(stringResource(R.string.workflow_bill_total)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = period,
            onValueChange = { period = it },
            label = { Text(stringResource(R.string.workflow_bill_period)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.workflow_bill_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    if (BusinessSmsPermissions.canReadContacts(context)) {
                        contactPicker.launch(null)
                    } else {
                        contactPermission.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
            ) {
                Icon(Icons.Outlined.Contacts, contentDescription = null)
                Text(stringResource(R.string.workflow_add_participant))
            }
            TextButton(
                onClick = {
                    val totalCents = MoneyMath.toCents(totalInput.toDoubleOrNull() ?: 0.0)
                    if (participants.isEmpty() || totalCents <= 0) return@TextButton
                    val shares = splitEqually(totalCents, participants.size)
                    participants.indices.forEach { index ->
                        val row = participants[index]
                        participants[index] = row.copy(
                            amountInput = MoneyMath.fromCents(shares[index]).let { v ->
                                if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
                            },
                        )
                    }
                },
            ) {
                Text(stringResource(R.string.workflow_split_equally))
            }
        }
        participants.forEachIndexed { index, row ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(row.name, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = row.amountInput,
                        onValueChange = { value ->
                            participants[index] = row.copy(amountInput = value)
                        },
                        label = { Text(stringResource(R.string.workflow_participant_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Button(
            onClick = {
                val totalCents = MoneyMath.toCents(totalInput.toDoubleOrNull() ?: 0.0)
                val drafts = participants.mapNotNull { row ->
                    val cents = MoneyMath.toCents(row.amountInput.toDoubleOrNull() ?: 0.0)
                    if (cents <= 0) return@mapNotNull null
                    WorkflowParticipantDraft(
                        name = row.name,
                        phone = row.phone,
                        email = row.email,
                        contactUri = row.contactUri,
                        contactLookupKey = row.contactLookupKey,
                        amountCents = cents,
                    )
                }
                onCreate(title, totalCents, description, period, drafts)
            },
            enabled = title.isNotBlank() && participants.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.workflow_create_bill))
        }
    }
}

private fun splitEqually(totalCents: Long, count: Int): List<Long> {
    if (count <= 0) return emptyList()
    val base = totalCents / count
    val remainder = (totalCents % count).toInt()
    return List(count) { index -> base + if (index < remainder) 1L else 0L }
}

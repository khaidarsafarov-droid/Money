package com.example.money.ui.business.workflow

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.contacts.ContactResolver
import com.example.money.domain.model.business.workflow.WorkflowBill
import com.example.money.domain.model.business.workflow.WorkflowCategory
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.domain.model.business.workflow.WorkflowProcess
import com.example.money.messaging.ContactInfoResolver
import com.example.money.ui.business.theme.BusinessSpacing
import com.example.money.ui.util.Formatters

@Composable
fun WorkflowCategoriesScreen(
    categories: List<WorkflowCategory>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long) -> Unit,
    onContinue: () -> Unit,
    onOpenCommunicationCenter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = stringResource(R.string.workflow_ux_categories_title),
                subtitle = stringResource(R.string.workflow_ux_categories_subtitle),
            )
        }
        items(categories, key = { it.id }) { category ->
            val selected = category.id == selectedCategoryId
            WorkflowCategoryCard(
                category = category,
                onClick = { onSelectCategory(category.id) },
                modifier = Modifier.then(
                    if (selected) Modifier.padding(2.dp) else Modifier,
                ),
            )
        }
        item {
            Button(
                onClick = onContinue,
                enabled = selectedCategoryId != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.workflow_ux_continue))
            }
        }
        item {
            TextButton(onClick = onOpenCommunicationCenter, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workflow_ux_open_communication_center))
            }
        }
    }
}

@Composable
fun WorkflowProcessesScreen(
    categoryName: String,
    processes: List<WorkflowProcess>,
    onProcessClick: (WorkflowProcess) -> Unit,
    onCreateProcess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = stringResource(R.string.workflow_ux_branch_title, categoryName),
                subtitle = stringResource(R.string.workflow_ux_branch_subtitle),
            )
        }
        if (processes.isEmpty()) {
            item {
                WorkflowEmptyState(
                    message = stringResource(R.string.workflow_empty_processes),
                    actionLabel = stringResource(R.string.workflow_create_process),
                    onAction = onCreateProcess,
                )
            }
        } else {
            items(processes, key = { it.id }) { process ->
                WorkflowListCard(
                    title = process.name,
                    subtitle = process.description,
                    leadingIcon = Icons.Outlined.Tune,
                    onClick = { onProcessClick(process) },
                )
            }
        }
        item {
            FilledTonalButton(onClick = onCreateProcess, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workflow_ux_new_process))
            }
        }
    }
}

@Composable
fun WorkflowBillsScreen(
    processName: String,
    bills: List<WorkflowBill>,
    onBillClick: (WorkflowBill) -> Unit,
    onCreateBill: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = processName,
                subtitle = stringResource(R.string.workflow_ux_bills_subtitle),
            )
        }
        if (bills.isEmpty()) {
            item {
                WorkflowEmptyState(
                    message = stringResource(R.string.workflow_empty_bills),
                    actionLabel = stringResource(R.string.workflow_create_bill),
                    onAction = onCreateBill,
                )
            }
        } else {
            items(bills, key = { it.id }) { bill ->
                WorkflowListCard(
                    title = bill.title,
                    subtitle = bill.periodLabel.ifBlank { bill.description },
                    trailing = Formatters.currencyFromCents(bill.totalAmountCents),
                    onClick = { onBillClick(bill) },
                )
            }
        }
        item {
            FilledTonalButton(onClick = onCreateBill, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.workflow_create_bill))
            }
        }
    }
}

@Composable
fun WorkflowCreateProcessScreen(
    onCreate: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        WorkflowHeroHeader(
            title = stringResource(R.string.workflow_ux_new_process_title),
            subtitle = stringResource(R.string.workflow_ux_new_process_subtitle),
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.workflow_process_name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.workflow_ux_description_optional)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
        Button(
            onClick = { onCreate(name, description) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.workflow_ux_create_process))
        }
    }
}

@Composable
fun WorkflowCreateBillInfoScreen(
    draft: CreateBillDraft?,
    onDraftChange: (CreateBillDraft) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = draft ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.md),
    ) {
        WorkflowHeroHeader(
            title = stringResource(R.string.workflow_ux_bill_title),
            subtitle = stringResource(R.string.workflow_ux_bill_subtitle),
        )
        OutlinedTextField(
            value = current.title,
            onValueChange = { onDraftChange(current.copy(title = it)) },
            label = { Text(stringResource(R.string.workflow_bill_title)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = current.totalInput,
            onValueChange = { onDraftChange(current.copy(totalInput = it)) },
            label = { Text(stringResource(R.string.workflow_bill_total)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = current.description,
            onValueChange = { onDraftChange(current.copy(description = it)) },
            label = { Text(stringResource(R.string.workflow_bill_description)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )
        OutlinedTextField(
            value = current.periodLabel,
            onValueChange = { onDraftChange(current.copy(periodLabel = it)) },
            label = { Text(stringResource(R.string.workflow_bill_period)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onContinue,
            enabled = current.title.isNotBlank() && current.totalInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.workflow_ux_continue))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowCreateBillParticipantsScreen(
    draft: CreateBillDraft?,
    onDraftChange: (CreateBillDraft) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val current = draft ?: return
    var searchQuery by remember { mutableStateOf("") }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            ContactResolver.resolve(context, uri)?.let { contact ->
                val info = ContactInfoResolver.resolve(context, uri)
                onDraftChange(
                    current.copy(
                        participants = current.participants + ParticipantDraftRow(
                            name = contact.displayName,
                            phone = info?.phone.orEmpty(),
                            email = info?.email.orEmpty(),
                            contactUri = uri.toString(),
                            contactLookupKey = contact.lookupKey,
                        ),
                    ),
                )
            }
        }
    }
    val contactPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) contactPicker.launch(null)
    }

    val filtered = current.participants.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = stringResource(R.string.workflow_ux_participants_title),
                subtitle = stringResource(R.string.workflow_ux_participants_subtitle),
            )
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.workflow_ux_search)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
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
                        onDraftChange(current.copy(participants = current.participants.map { it.copy(selected = true) }))
                    },
                ) {
                    Text(stringResource(R.string.workflow_select_all))
                }
            }
        }
        items(filtered, key = { "${it.contactUri}-${it.name}" }) { row ->
            val index = current.participants.indexOfFirst { it.contactUri == row.contactUri && it.name == row.name }
            if (index < 0) return@items
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = row.selected,
                        onCheckedChange = { checked ->
                            val updated = current.participants.toMutableList()
                            updated[index] = row.copy(selected = checked)
                            onDraftChange(current.copy(participants = updated))
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.name, fontWeight = FontWeight.SemiBold)
                        if (row.phone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(row.phone, style = MaterialTheme.typography.bodySmall)
                                Icon(
                                    Icons.Outlined.Store,
                                    contentDescription = stringResource(R.string.messaging_channel_whatsapp),
                                    modifier = Modifier.padding(start = 8.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = onContinue,
                enabled = current.participants.any { it.selected },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.workflow_ux_add_participants))
            }
        }
    }
}

@Composable
fun WorkflowCreateBillDistributionScreen(
    draft: CreateBillDraft?,
    onDraftChange: (CreateBillDraft) -> Unit,
    onSplitEqually: () -> Unit,
    onAiDistribute: () -> Unit,
    onReset: () -> Unit,
    onCreateDebts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = draft ?: return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        WorkflowHeroHeader(
            title = stringResource(R.string.workflow_ux_distribution_title),
            subtitle = stringResource(R.string.workflow_ux_distribution_subtitle),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSplitEqually) { Text(stringResource(R.string.workflow_ux_split_evenly)) }
            TextButton(onClick = onAiDistribute) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Text(stringResource(R.string.workflow_ux_ai_distribute))
            }
            TextButton(onClick = onReset) { Text(stringResource(R.string.workflow_ux_reset)) }
        }
        current.participants.filter { it.selected }.forEachIndexed { index, row ->
            val globalIndex = current.participants.indexOf(row)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(row.name, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = row.amountInput,
                        onValueChange = { value ->
                            val updated = current.participants.toMutableList()
                            updated[globalIndex] = row.copy(amountInput = value)
                            onDraftChange(current.copy(participants = updated))
                        },
                        label = { Text(stringResource(R.string.workflow_participant_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Button(
            onClick = onCreateDebts,
            enabled = current.toParticipantDrafts().isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.workflow_ux_create_debts))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowDebtListScreen(
    bill: WorkflowBill?,
    debts: List<WorkflowDebtWithParticipant>,
    stats: WorkflowDebtStats,
    selectedDebtIds: Set<Long>,
    debtFilter: WorkflowDebtFilter,
    onFilterChange: (WorkflowDebtFilter) -> Unit,
    onToggleDebt: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onMarkPaid: (Long) -> Unit,
    onSendReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = stringResource(R.string.workflow_ux_debt_list_title),
                subtitle = stringResource(
                    R.string.workflow_total_bill,
                    Formatters.currencyFromCents(bill?.totalAmountCents ?: stats.totalCents),
                ),
            )
        }
        item { WorkflowBillSummaryCard(bill = bill, stats = stats) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = debtFilter == WorkflowDebtFilter.ALL,
                    onClick = { onFilterChange(WorkflowDebtFilter.ALL) },
                    label = { Text(stringResource(R.string.workflow_ux_filter_all)) },
                )
                FilterChip(
                    selected = debtFilter == WorkflowDebtFilter.UNPAID,
                    onClick = { onFilterChange(WorkflowDebtFilter.UNPAID) },
                    label = { Text(stringResource(R.string.workflow_ux_filter_unpaid)) },
                )
                FilterChip(
                    selected = debtFilter == WorkflowDebtFilter.PAID,
                    onClick = { onFilterChange(WorkflowDebtFilter.PAID) },
                    label = { Text(stringResource(R.string.workflow_ux_filter_paid)) },
                )
            }
        }
        item {
            TextButton(onClick = onSelectAll) { Text(stringResource(R.string.workflow_select_all)) }
        }
        items(debts, key = { it.debt.id }) { item ->
            WorkflowDebtRow(
                item = item,
                selected = item.debt.id in selectedDebtIds,
                onToggle = { onToggleDebt(item.debt.id) },
                onMarkPaid = { onMarkPaid(item.debt.id) },
            )
        }
        item {
            Button(
                onClick = onSendReminders,
                enabled = selectedDebtIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.workflow_ux_send_reminders))
            }
        }
    }
}

@Composable
fun WorkflowMessageComposerScreen(
    messageBody: String,
    selectedCount: Int,
    onMessageChange: (String) -> Unit,
    onAiImprove: () -> Unit,
    onAiShorten: () -> Unit,
    onAiFormal: () -> Unit,
    onSendWhatsApp: () -> Unit,
    onSendSms: () -> Unit,
    onSendPush: () -> Unit,
    onSendAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = stringResource(R.string.workflow_ux_message_title),
                subtitle = stringResource(R.string.workflow_ux_message_subtitle),
            )
        }
        item {
            WorkflowCommunicationHub(
                messageBody = messageBody,
                selectedCount = selectedCount,
                onMessageChange = onMessageChange,
                onAiGenerate = onAiImprove,
                onSendWhatsApp = onSendWhatsApp,
                onSendSms = onSendSms,
                onSendPush = onSendPush,
                onSendAll = onSendAll,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAiImprove) { Text(stringResource(R.string.workflow_ux_ai_improve)) }
                TextButton(onClick = onAiShorten) { Text(stringResource(R.string.workflow_ux_ai_shorten)) }
                TextButton(onClick = onAiFormal) { Text(stringResource(R.string.workflow_ux_ai_formal)) }
            }
        }
    }
}

@Composable
fun WorkflowCommunicationCenterScreen(
    onOpenSmsTab: () -> Unit,
    onOpenAutomation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(BusinessSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BusinessSpacing.sm),
    ) {
        item {
            WorkflowHeroHeader(
                title = stringResource(R.string.workflow_ux_comm_center_title),
                subtitle = stringResource(R.string.workflow_ux_comm_center_subtitle),
            )
        }
        item {
            WorkflowHubTile(
                title = stringResource(R.string.messaging_channel_whatsapp),
                subtitle = stringResource(R.string.workflow_ux_hub_whatsapp),
                icon = Icons.Outlined.Store,
                onClick = onOpenSmsTab,
            )
        }
        item {
            WorkflowHubTile(
                title = stringResource(R.string.messaging_channel_sms),
                subtitle = stringResource(R.string.workflow_ux_hub_sms),
                icon = Icons.Outlined.Sms,
                onClick = onOpenSmsTab,
            )
        }
        item {
            WorkflowHubTile(
                title = stringResource(R.string.workflow_ux_hub_push_title),
                subtitle = stringResource(R.string.workflow_ux_hub_push),
                icon = Icons.Outlined.Notifications,
                onClick = onOpenSmsTab,
            )
        }
        item {
            WorkflowHubTile(
                title = stringResource(R.string.business_automation_title),
                subtitle = stringResource(R.string.workflow_ux_hub_automation),
                icon = Icons.Outlined.Tune,
                onClick = onOpenAutomation,
            )
        }
        item {
            WorkflowHubTile(
                title = stringResource(R.string.workflow_ux_hub_history_title),
                subtitle = stringResource(R.string.workflow_ux_hub_history),
                icon = Icons.Outlined.History,
                onClick = onOpenSmsTab,
            )
        }
        item {
            WorkflowHubTile(
                title = stringResource(R.string.workflow_ux_hub_templates_title),
                subtitle = stringResource(R.string.workflow_ux_hub_templates),
                icon = Icons.Outlined.AutoAwesome,
                onClick = onOpenSmsTab,
            )
        }
    }
}

@Composable
private fun WorkflowHubTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    WorkflowListCard(
        title = title,
        subtitle = subtitle,
        leadingIcon = icon,
        onClick = onClick,
    )
}

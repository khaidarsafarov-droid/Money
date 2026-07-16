package com.example.money.ui.business.sms

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
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.money.R
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.MessagingContact
import com.example.money.messaging.UniversalMessageSender
import com.example.money.messaging.ContactInfoResolver
import com.example.money.contacts.ContactResolver
import com.example.money.domain.model.business.BusinessClient
import com.example.money.domain.model.business.BusinessSmsLog
import com.example.money.domain.model.business.BusinessSmsRecipient
import com.example.money.domain.model.business.BusinessSmsSendResult
import com.example.money.domain.model.business.BusinessSmsStatus
import com.example.money.domain.model.business.BusinessSmsTemplate
import com.example.money.domain.model.business.BusinessSmsTemplateId
import com.example.money.domain.util.ai.BusinessSmsTone
import com.example.money.settings.BusinessBranchSettings
import com.example.money.ui.business.designsystem.HeaderPrimary
import com.example.money.ui.business.designsystem.HistoryRow
import com.example.money.ui.business.designsystem.TemplateItem
import com.example.money.ui.business.sms.BusinessSmsPanel
import com.example.money.ui.util.Formatters
import kotlinx.coroutines.launch

private fun BusinessSmsPanel.titleRes(): Int = when (this) {
    BusinessSmsPanel.COMPOSE -> R.string.business_sms_panel_compose
    BusinessSmsPanel.BULK -> R.string.business_sms_panel_bulk
    BusinessSmsPanel.TEMPLATES -> R.string.business_sms_panel_templates
    BusinessSmsPanel.HISTORY -> R.string.business_sms_panel_history
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessSmsScreen(
    clients: List<BusinessClient>,
    templates: List<BusinessSmsTemplate>,
    smsHistory: List<BusinessSmsLog>,
    settings: BusinessBranchSettings,
    onSendSms: suspend (
        recipients: List<BusinessSmsRecipient>,
        body: String,
        templateId: BusinessSmsTemplateId?,
        useDirectSend: Boolean,
    ) -> BusinessSmsSendResult,
    onSendBulk: suspend (
        clientIds: List<Long>,
        body: String,
        templateId: BusinessSmsTemplateId?,
        useDirectSend: Boolean,
    ) -> BusinessSmsSendResult,
    onRenderTemplate: suspend (BusinessSmsTemplateId, Long?) -> String,
    onAiGenerate: suspend (BusinessSmsTemplateId, Long?, BusinessSmsTone) -> String,
    initialPanel: BusinessSmsPanel? = null,
    onInitialPanelConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPanel by remember { mutableStateOf(BusinessSmsPanel.COMPOSE) }

    LaunchedEffect(initialPanel) {
        val panel = initialPanel ?: return@LaunchedEffect
        selectedPanel = panel
        kotlinx.coroutines.yield()
        onInitialPanelConsumed()
    }
    var messageBody by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf("") }
    var selectedClientId by remember { mutableStateOf<Long?>(null) }
    var selectedTemplateId by remember { mutableStateOf<BusinessSmsTemplateId?>(null) }
    var selectedBulkClientIds by remember { mutableStateOf(setOf<Long>()) }
    var aiTone by remember { mutableStateOf(BusinessSmsTone.FRIENDLY) }
    var pendingSend by remember { mutableStateOf(false) }
    var pendingBulkSend by remember { mutableStateOf(false) }
    var awaitingBulkPermission by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri != null) {
            ContactResolver.resolve(context, uri)?.let { contact ->
                recipientName = contact.displayName
                ContactInfoResolver.resolve(context, uri)?.let { info ->
                    if (info.phone.isNotBlank()) phone = info.phone
                    if (info.email.isNotBlank()) email = info.email
                }
            }
        }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            if (awaitingBulkPermission) {
                pendingBulkSend = true
                awaitingBulkPermission = false
            } else {
                pendingSend = true
            }
        } else {
            awaitingBulkPermission = false
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.business_sms_permission_denied))
            }
        }
    }

    LaunchedEffect(pendingSend) {
        if (!pendingSend) return@LaunchedEffect
        pendingSend = false
        val recipients = buildRecipients(phone, recipientName, selectedClientId)
        if (recipients.isEmpty() || messageBody.isBlank()) return@LaunchedEffect
        val result = onSendSms(recipients, messageBody, selectedTemplateId, settings.smsDirectSendEnabled)
        snackbarHostState.showSnackbar(formatSendResult(context, result))
    }

    LaunchedEffect(pendingBulkSend) {
        if (!pendingBulkSend) return@LaunchedEffect
        pendingBulkSend = false
        if (selectedBulkClientIds.isEmpty() || messageBody.isBlank()) return@LaunchedEffect
        val result = onSendBulk(
            selectedBulkClientIds.toList(),
            messageBody,
            selectedTemplateId,
            settings.smsDirectSendEnabled,
        )
        snackbarHostState.showSnackbar(formatSendResult(context, result))
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedPanel.ordinal) {
            BusinessSmsPanel.entries.forEach { panel ->
                Tab(
                    selected = selectedPanel == panel,
                    onClick = { selectedPanel = panel },
                    text = { Text(stringResource(panel.titleRes())) },
                )
            }
        }

        when (selectedPanel) {
            BusinessSmsPanel.COMPOSE -> SmsComposePanel(
                clients = clients,
                phone = phone,
                email = email,
                recipientName = recipientName,
                messageBody = messageBody,
                selectedClientId = selectedClientId,
                selectedTemplateId = selectedTemplateId,
                templates = templates,
                useDirectSend = settings.smsDirectSendEnabled,
                onPhoneChange = { phone = it },
                onRecipientNameChange = { recipientName = it },
                onMessageChange = { messageBody = it },
                onClientSelected = { client ->
                    selectedClientId = client.id
                    recipientName = client.name
                    phone = client.phone
                    email = client.email
                },
                onTemplateSelected = { templateId ->
                    selectedTemplateId = templateId
                    scope.launch {
                        messageBody = onRenderTemplate(templateId, selectedClientId)
                    }
                },
                onPickContact = { contactPickerLauncher.launch(null) },
                onAiGenerate = {
                    val templateId = selectedTemplateId ?: BusinessSmsTemplateId.CUSTOM
                    scope.launch {
                        messageBody = onAiGenerate(templateId, selectedClientId, aiTone)
                    }
                },
                aiTone = aiTone,
                onToneChange = { aiTone = it },
                onSend = {
                    if (settings.smsDirectSendEnabled && !BusinessSmsPermissions.canSendDirect(context)) {
                        smsPermissionLauncher.launch(BusinessSmsPermissions.sendSmsPermission)
                    } else {
                        pendingSend = true
                    }
                },
            )
            BusinessSmsPanel.BULK -> SmsBulkPanel(
                clients = clients.filter { it.phone.isNotBlank() },
                selectedClientIds = selectedBulkClientIds,
                messageBody = messageBody,
                templates = templates,
                onToggleClient = { clientId ->
                    selectedBulkClientIds = if (clientId in selectedBulkClientIds) {
                        selectedBulkClientIds - clientId
                    } else {
                        selectedBulkClientIds + clientId
                    }
                },
                onSelectAll = {
                    selectedBulkClientIds = clients.filter { it.phone.isNotBlank() }.map { it.id }.toSet()
                },
                onClearSelection = { selectedBulkClientIds = emptySet() },
                onMessageChange = { messageBody = it },
                onTemplateSelected = { templateId ->
                    selectedTemplateId = templateId
                    scope.launch {
                        messageBody = onRenderTemplate(templateId, null)
                    }
                },
                onSend = {
                    scope.launch {
                        if (selectedBulkClientIds.isEmpty() || messageBody.isBlank()) return@launch
                        if (settings.smsDirectSendEnabled && !BusinessSmsPermissions.canSendDirect(context)) {
                            awaitingBulkPermission = true
                            smsPermissionLauncher.launch(BusinessSmsPermissions.sendSmsPermission)
                            return@launch
                        }
                        val result = onSendBulk(
                            selectedBulkClientIds.toList(),
                            messageBody,
                            selectedTemplateId,
                            settings.smsDirectSendEnabled,
                        )
                        snackbarHostState.showSnackbar(formatSendResult(context, result))
                    }
                },
            )
            BusinessSmsPanel.TEMPLATES -> SmsTemplatesPanel(
                templates = templates,
                onTemplateSelected = { templateId ->
                    selectedTemplateId = templateId
                    selectedPanel = BusinessSmsPanel.COMPOSE
                    scope.launch {
                        messageBody = onRenderTemplate(templateId, selectedClientId)
                    }
                },
            )
            BusinessSmsPanel.HISTORY -> SmsHistoryPanel(logs = smsHistory)
        }

        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun SmsComposePanel(
    clients: List<BusinessClient>,
    phone: String,
    email: String,
    recipientName: String,
    messageBody: String,
    selectedClientId: Long?,
    selectedTemplateId: BusinessSmsTemplateId?,
    templates: List<BusinessSmsTemplate>,
    useDirectSend: Boolean,
    onPhoneChange: (String) -> Unit,
    onRecipientNameChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onClientSelected: (BusinessClient) -> Unit,
    onTemplateSelected: (BusinessSmsTemplateId) -> Unit,
    onPickContact: () -> Unit,
    onAiGenerate: () -> Unit,
    aiTone: BusinessSmsTone,
    onToneChange: (BusinessSmsTone) -> Unit,
    onSend: () -> Unit,
) {
    val context = LocalContext.current
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onPickContact()
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.business_sms_compose_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            Text(
                text = if (useDirectSend) {
                    stringResource(R.string.business_sms_mode_direct)
                } else {
                    stringResource(R.string.business_sms_mode_app)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = recipientName,
                    onValueChange = onRecipientNameChange,
                    label = { Text(stringResource(R.string.business_field_name)) },
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    if (BusinessSmsPermissions.canReadContacts(context)) {
                        onPickContact()
                    } else {
                        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                }) {
                    Icon(Icons.Outlined.Contacts, contentDescription = stringResource(R.string.business_sms_pick_contact))
                }
            }
        }
        item {
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.business_field_phone)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (clients.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.business_sms_select_client),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            items(clients.filter { it.phone.isNotBlank() }) { client ->
                FilterChip(
                    selected = selectedClientId == client.id,
                    onClick = { onClientSelected(client) },
                    label = { Text(client.name) },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                templates.take(4).forEach { template ->
                    FilterChip(
                        selected = selectedTemplateId == template.id,
                        onClick = { onTemplateSelected(template.id) },
                        label = { Text(stringResource(template.id.titleRes())) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BusinessSmsTone.entries.forEach { tone ->
                    FilterChip(
                        selected = aiTone == tone,
                        onClick = { onToneChange(tone) },
                        label = { Text(stringResource(tone.labelRes())) },
                    )
                }
                TextButton(onClick = onAiGenerate) {
                    Text(stringResource(R.string.business_sms_ai_generate))
                }
            }
        }
        item {
            OutlinedTextField(
                value = messageBody,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.business_sms_message)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
        }
        item {
            TextButton(
                onClick = onSend,
                enabled = phone.isNotBlank() && messageBody.isNotBlank(),
            ) {
                Icon(Icons.Outlined.Send, contentDescription = null)
                Text(stringResource(R.string.business_sms_send))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val contact = MessagingContact(phone = phone, email = email, name = recipientName)
                val payload = MessagePayload(body = messageBody)
                listOf(
                    MessagingChannel.WHATSAPP,
                    MessagingChannel.TELEGRAM,
                    MessagingChannel.EMAIL,
                ).forEach { channel ->
                    if (UniversalMessageSender.canSend(channel, contact, context)) {
                        TextButton(
                            onClick = {
                                UniversalMessageSender.send(
                                    context,
                                    channel,
                                    contact,
                                    payload,
                                    useDirectSend,
                                )
                            },
                            enabled = messageBody.isNotBlank(),
                        ) {
                            Text(stringResource(channel.labelRes()))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsBulkPanel(
    clients: List<BusinessClient>,
    selectedClientIds: Set<Long>,
    messageBody: String,
    templates: List<BusinessSmsTemplate>,
    onToggleClient: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onMessageChange: (String) -> Unit,
    onTemplateSelected: (BusinessSmsTemplateId) -> Unit,
    onSend: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.business_sms_bulk_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSelectAll) { Text(stringResource(R.string.business_sms_select_all)) }
                TextButton(onClick = onClearSelection) { Text(stringResource(R.string.business_sms_clear_selection)) }
            }
        }
        items(clients) { client ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleClient(client.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = client.id in selectedClientIds,
                    onCheckedChange = { onToggleClient(client.id) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(client.name, fontWeight = FontWeight.Medium)
                    Text(client.phone, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                templates.take(3).forEach { template ->
                    FilterChip(
                        selected = false,
                        onClick = { onTemplateSelected(template.id) },
                        label = { Text(stringResource(template.id.titleRes())) },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = messageBody,
                onValueChange = onMessageChange,
                label = { Text(stringResource(R.string.business_sms_message)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
        }
        item {
            TextButton(
                onClick = onSend,
                enabled = selectedClientIds.isNotEmpty() && messageBody.isNotBlank(),
            ) {
                Text(stringResource(R.string.business_sms_send_bulk, selectedClientIds.size))
            }
        }
    }
}

@Composable
private fun SmsTemplatesPanel(
    templates: List<BusinessSmsTemplate>,
    onTemplateSelected: (BusinessSmsTemplateId) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderPrimary(
                title = stringResource(R.string.business_ds_templates_title),
                subtitle = stringResource(R.string.business_ds_templates_subtitle),
            )
        }
        items(templates) { template ->
            TemplateItem(
                title = stringResource(template.id.titleRes()),
                preview = template.bodyPattern,
                onClick = { onTemplateSelected(template.id) },
            )
        }
    }
}

@Composable
private fun SmsHistoryPanel(logs: List<BusinessSmsLog>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            HeaderPrimary(
                title = stringResource(R.string.business_ds_history_title),
                subtitle = stringResource(R.string.business_ds_history_subtitle),
            )
        }
        if (logs.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.business_sms_history_empty),
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        } else {
            items(logs) { log ->
                HistoryRow(
                    channel = log.channel.ifBlank { stringResource(R.string.messaging_channel_sms) },
                    recipient = log.recipientName.ifBlank { log.recipientPhone },
                    body = log.body,
                    status = stringResource(log.status.labelRes()),
                    dateLabel = Formatters.shortDate(log.createdAt),
                )
            }
        }
    }
}

private fun buildRecipients(
    phone: String,
    name: String,
    clientId: Long?,
): List<BusinessSmsRecipient> {
    val trimmed = phone.trim()
    if (trimmed.isBlank()) return emptyList()
    return listOf(BusinessSmsRecipient(phone = trimmed, name = name.trim(), clientId = clientId))
}

private fun formatSendResult(context: android.content.Context, result: BusinessSmsSendResult): String =
    context.getString(
        R.string.business_sms_send_result,
        result.sentCount,
        result.openedAppCount,
        result.failedCount,
    )

fun BusinessSmsTemplateId.titleRes(): Int = when (this) {
    BusinessSmsTemplateId.DEBT_REMINDER -> R.string.business_sms_template_debt_reminder
    BusinessSmsTemplateId.INVOICE_STATUS -> R.string.business_sms_template_invoice_status
    BusinessSmsTemplateId.NEW_PRODUCT -> R.string.business_sms_template_new_product
    BusinessSmsTemplateId.PROMOTION -> R.string.business_sms_template_promotion
    BusinessSmsTemplateId.PROJECT_UPDATE -> R.string.business_sms_template_project_update
    BusinessSmsTemplateId.DELIVERY_UPDATE -> R.string.business_sms_template_delivery_update
    BusinessSmsTemplateId.MEETING_REMINDER -> R.string.business_sms_template_meeting_reminder
    BusinessSmsTemplateId.SUBSCRIPTION_REMINDER -> R.string.business_sms_template_subscription_reminder
    BusinessSmsTemplateId.PAYMENT_RECEIPT -> R.string.business_sms_template_payment_receipt
    BusinessSmsTemplateId.LARGE_TRANSACTION -> R.string.business_sms_template_large_transaction
    BusinessSmsTemplateId.CUSTOM -> R.string.business_sms_template_custom
}

private fun BusinessSmsTone.labelRes(): Int = when (this) {
    BusinessSmsTone.FORMAL -> R.string.business_sms_tone_formal
    BusinessSmsTone.FRIENDLY -> R.string.business_sms_tone_friendly
    BusinessSmsTone.URGENT -> R.string.business_sms_tone_urgent
}

private fun BusinessSmsStatus.labelRes(): Int = when (this) {
    BusinessSmsStatus.SENT -> R.string.business_sms_status_sent
    BusinessSmsStatus.FAILED -> R.string.business_sms_status_failed
    BusinessSmsStatus.PENDING -> R.string.business_sms_status_pending
    BusinessSmsStatus.OPENED_APP -> R.string.business_sms_status_opened_app
}

private fun MessagingChannel.labelRes(): Int = when (this) {
    MessagingChannel.SMS -> R.string.messaging_channel_sms
    MessagingChannel.WHATSAPP -> R.string.messaging_channel_whatsapp
    MessagingChannel.TELEGRAM -> R.string.messaging_channel_telegram
    MessagingChannel.EMAIL -> R.string.messaging_channel_email
}

package com.example.money.ui.business.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.RecurringBusinessRule
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.BusinessBranchSettings
import com.example.money.ui.business.designsystem.BusinessScreenHeader
import com.example.money.ui.util.Formatters

@Composable
fun BusinessAutomationSettings(
    settings: BusinessBranchSettings,
    recurringRules: List<RecurringBusinessRule>,
    onAutomationChange: ((BusinessBranchSettings) -> BusinessBranchSettings) -> Unit,
    onAddRule: (RecurringBusinessRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onRunNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddRule by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            BusinessScreenHeader(
                title = stringResource(R.string.business_automation_title),
                subtitle = stringResource(R.string.business_ds_automation_subtitle),
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_automation_invoice_reminders),
                checked = settings.invoiceRemindersEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(invoiceRemindersEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_automation_low_cash),
                checked = settings.lowCashAlertsEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(lowCashAlertsEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_automation_recurring_tx),
                checked = settings.recurringTransactionsEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(recurringTransactionsEnabled = enabled) }
                },
            )
        }

        item {
            Text(
                text = stringResource(R.string.business_sms_automation_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_sms_automation_enabled),
                checked = settings.smsAutomationEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(smsAutomationEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_sms_overdue_enabled),
                checked = settings.smsOverdueInvoiceEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(smsOverdueInvoiceEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_sms_direct_send),
                checked = settings.smsDirectSendEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(smsDirectSendEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_whatsapp_automation),
                checked = settings.whatsappAutomationEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(whatsappAutomationEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_telegram_automation),
                checked = settings.telegramAutomationEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(telegramAutomationEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_email_automation),
                checked = settings.emailAutomationEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(emailAutomationEnabled = enabled) }
                },
            )
        }

        item {
            Text(
                text = stringResource(R.string.business_workflow_automation_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_workflow_debt_automation),
                checked = settings.workflowDebtAutomationEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(workflowDebtAutomationEnabled = enabled) }
                },
            )
        }

        item {
            AutomationToggleRow(
                label = stringResource(R.string.business_workflow_debt_push),
                checked = settings.workflowDebtPushEnabled,
                onCheckedChange = { enabled ->
                    onAutomationChange { it.copy(workflowDebtPushEnabled = enabled) }
                },
            )
        }

        item {
            TextButton(onClick = onRunNow) {
                Text(stringResource(R.string.business_automation_run_now))
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.business_automation_recurring_rules),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { showAddRule = true }) {
                    Text(stringResource(R.string.business_automation_add_rule))
                }
            }
        }

        if (recurringRules.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.business_automation_no_rules),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(recurringRules, key = { it.id }) { rule ->
                RecurringRuleRow(
                    rule = rule,
                    onToggle = { onToggleRule(rule.id, it) },
                    onDelete = { onDeleteRule(rule.id) },
                )
            }
        }
    }

    if (showAddRule) {
        AddRecurringRuleDialog(
            onDismiss = { showAddRule = false },
            onAdd = { rule ->
                onAddRule(rule)
                showAddRule = false
            },
        )
    }
}

@Composable
private fun AutomationToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RecurringRuleRow(
    rule: RecurringBusinessRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rule.label, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${Formatters.currencyFromCents(rule.amountCents)} · ${rule.type.name} · every ${rule.intervalDays}d",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.action_delete))
            }
        }
    }
}

@Composable
private fun AddRecurringRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (RecurringBusinessRule) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Services") }
    var interval by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.business_automation_add_rule)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.business_field_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.business_field_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.business_field_category)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = interval,
                    onValueChange = { interval = it },
                    label = { Text(stringResource(R.string.business_automation_interval_days)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedAmount = amount.replace(',', '.').toDoubleOrNull() ?: return@TextButton
                    val parsedInterval = interval.toIntOrNull() ?: 30
                    if (label.isNotBlank() && parsedAmount > 0) {
                        onAdd(
                            RecurringBusinessRule(
                                label = label.trim(),
                                type = BusinessTransactionType.EXPENSE,
                                amountCents = MoneyMath.toCents(parsedAmount),
                                category = category.trim(),
                                intervalDays = parsedInterval.coerceAtLeast(1),
                            ),
                        )
                    }
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

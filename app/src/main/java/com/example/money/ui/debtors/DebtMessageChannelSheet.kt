package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.messaging.MessagingChannel
import com.example.money.settings.PersonalBranchSettings

@Composable
fun DebtMessageChannelSheet(
    debtor: Debtor,
    settings: PersonalBranchSettings,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val options = DebtReminderShare.channelOptions(context, debtor)
    val hasEnabledChannel = options.any { it.enabled }
    val contact = DebtReminderShare.resolveContact(context, debtor)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_remind_chooser_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (contact.phone.isBlank() && contact.email.isBlank()) {
                    Text(
                        text = stringResource(R.string.debt_remind_add_contact_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                options.forEach { option ->
                    TextButton(
                        onClick = {
                            DebtReminderShare.send(
                                context = context,
                                debtor = debtor,
                                channel = option.channel,
                                useDirectSms = settings.debtDirectSmsEnabled,
                            )
                            onDismiss()
                        },
                        enabled = option.enabled,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(option.channel.label())
                            if (!option.enabled && option.disabledReasonRes != null) {
                                Text(
                                    text = stringResource(option.disabledReasonRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (!hasEnabledChannel) {
                    Text(
                        text = stringResource(R.string.debt_remind_no_contact),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun MessagingChannel.label(): String = stringResource(
    when (this) {
        MessagingChannel.SMS -> R.string.messaging_channel_sms
        MessagingChannel.WHATSAPP -> R.string.messaging_channel_whatsapp
        MessagingChannel.TELEGRAM -> R.string.messaging_channel_telegram
        MessagingChannel.EMAIL -> R.string.messaging_channel_email
    },
)

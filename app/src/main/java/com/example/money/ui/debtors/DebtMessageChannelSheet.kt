package com.example.money.ui.debtors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
    val channels = DebtReminderShare.availableChannels(context, debtor)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debt_remind_chooser_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (channels.isEmpty()) {
                    Text(stringResource(R.string.debt_remind_no_contact))
                } else {
                    channels.forEach { channel ->
                        TextButton(
                            onClick = {
                                DebtReminderShare.send(
                                    context = context,
                                    debtor = debtor,
                                    channel = channel,
                                    useDirectSms = settings.debtDirectSmsEnabled,
                                )
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(channel.label())
                        }
                    }
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

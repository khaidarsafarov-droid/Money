package com.example.money.ui.debtors

import android.content.Context
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.messaging.ContactInfoResolver
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.UniversalMessageSender
import com.example.money.ui.util.Formatters

data class DebtRemindChannelOption(
    val channel: MessagingChannel,
    val enabled: Boolean,
    val disabledReasonRes: Int? = null,
)

object DebtReminderShare {

    fun buildMessage(context: Context, debtor: Debtor): String {
        val remaining = Formatters.currencyFromCents(debtor.remainingAmountCents, debtor.currency)
        val dueDate = Formatters.shortDate(debtor.dateDue)
        return when (debtor.dueStatus()) {
            DebtDueStatus.OVERDUE -> context.getString(
                R.string.debt_remind_message_overdue,
                debtor.name,
                remaining,
                dueDate,
            )
            else -> context.getString(
                R.string.debt_remind_message,
                debtor.name,
                remaining,
                dueDate,
            )
        }
    }

    fun buildSubject(context: Context, debtor: Debtor): String =
        context.getString(R.string.debt_remind_subject, debtor.name)

    fun resolveContact(context: Context, debtor: Debtor) =
        ContactInfoResolver.resolveFromStored(
            phoneField = debtor.phone,
            emailField = debtor.email,
            contactUri = debtor.contactUri,
            context = context,
        ).copy(name = debtor.name)

    fun send(
        context: Context,
        debtor: Debtor,
        channel: MessagingChannel,
        useDirectSms: Boolean = false,
    ): Boolean {
        val contact = resolveContact(context, debtor)
        val payload = MessagePayload(
            subject = buildSubject(context, debtor),
            body = buildMessage(context, debtor),
        )
        if (!UniversalMessageSender.canSend(channel, contact, context)) return false
        return UniversalMessageSender.send(context, channel, contact, payload, useDirectSms) !=
            com.example.money.messaging.MessageSendResult.FAILED
    }

    fun availableChannels(context: Context, debtor: Debtor): List<MessagingChannel> =
        channelOptions(context, debtor)
            .filter { it.enabled }
            .map { it.channel }

    fun channelOptions(context: Context, debtor: Debtor): List<DebtRemindChannelOption> {
        val contact = resolveContact(context, debtor)
        return MessagingChannel.entries.map { channel ->
            val enabled = UniversalMessageSender.canSend(channel, contact, context)
            val disabledReasonRes = when {
                enabled -> null
                channel == MessagingChannel.SMS || channel == MessagingChannel.WHATSAPP -> {
                    if (contact.phone.isBlank()) R.string.debt_remind_need_phone else R.string.debt_remind_channel_unavailable
                }
                channel == MessagingChannel.EMAIL -> {
                    if (contact.email.isBlank()) R.string.debt_remind_need_email else R.string.debt_remind_channel_unavailable
                }
                channel == MessagingChannel.TELEGRAM -> R.string.debt_remind_telegram_unavailable
                else -> R.string.debt_remind_channel_unavailable
            }
            DebtRemindChannelOption(channel, enabled, disabledReasonRes)
        }
    }
}

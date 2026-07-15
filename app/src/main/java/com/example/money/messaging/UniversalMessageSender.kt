package com.example.money.messaging

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.money.business.sms.BusinessSmsPermissions
import com.example.money.business.sms.BusinessSmsSender
import com.example.money.business.sms.PhoneNormalizer
import com.example.money.domain.model.business.BusinessSmsStatus

enum class MessageSendResult {
    SENT,
    OPENED_APP,
    FAILED,
}

object UniversalMessageSender {

    fun canSend(
        channel: MessagingChannel,
        contact: MessagingContact,
        context: Context? = null,
    ): Boolean = when (channel) {
        MessagingChannel.SMS -> contact.phone.isNotBlank()
        MessagingChannel.WHATSAPP -> contact.phone.isNotBlank()
        MessagingChannel.TELEGRAM -> isTelegramAvailable(context)
        MessagingChannel.EMAIL -> contact.email.isNotBlank()
    }

    fun send(
        context: Context,
        channel: MessagingChannel,
        contact: MessagingContact,
        payload: MessagePayload,
        useDirectSms: Boolean = false,
    ): MessageSendResult {
        return when (channel) {
            MessagingChannel.SMS -> sendSms(context, contact.phone, payload.body, useDirectSms)
            MessagingChannel.WHATSAPP -> openWhatsApp(context, contact.phone, payload.body)
            MessagingChannel.TELEGRAM -> openTelegram(context, payload.body)
            MessagingChannel.EMAIL -> openEmail(context, contact.email, payload.subject, payload.body)
        }
    }

    fun createLaunchIntent(
        context: Context,
        channel: MessagingChannel,
        contact: MessagingContact,
        payload: MessagePayload,
    ): Intent? = when (channel) {
        MessagingChannel.SMS -> createSmsIntent(contact.phone, payload.body)
        MessagingChannel.WHATSAPP -> createWhatsAppIntent(contact.phone, payload.body)
        MessagingChannel.TELEGRAM -> createTelegramIntent(payload.body)
        MessagingChannel.EMAIL -> createEmailIntent(contact.email, payload.subject, payload.body)
    }?.takeIf { intent ->
        intent.resolveActivity(context.packageManager) != null ||
            channel == MessagingChannel.TELEGRAM && isTelegramAvailable(context)
    }

    private fun isTelegramAvailable(context: Context?): Boolean {
        if (context == null) return false
        val packageManager = context.packageManager
        return createTelegramIntent("").resolveActivity(packageManager) != null ||
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://t.me/share/url"),
            ).resolveActivity(packageManager) != null
    }

    private fun sendSms(
        context: Context,
        phone: String,
        body: String,
        useDirectSms: Boolean,
    ): MessageSendResult {
        if (useDirectSms && BusinessSmsPermissions.canSendDirect(context)) {
            return when (BusinessSmsSender.sendDirect(context, phone, body)) {
                BusinessSmsStatus.SENT -> MessageSendResult.SENT
                BusinessSmsStatus.OPENED_APP -> MessageSendResult.OPENED_APP
                BusinessSmsStatus.FAILED, BusinessSmsStatus.PENDING -> MessageSendResult.FAILED
            }
        }
        return if (BusinessSmsSender.openSmsApp(context, phone, body)) {
            MessageSendResult.OPENED_APP
        } else {
            MessageSendResult.FAILED
        }
    }

    private fun openWhatsApp(context: Context, phone: String, body: String): MessageSendResult {
        val intent = createWhatsAppIntent(phone, body) ?: return MessageSendResult.FAILED
        return launch(context, intent)
    }

    private fun openTelegram(context: Context, body: String): MessageSendResult {
        val telegramIntent = createTelegramIntent(body)
        if (telegramIntent.resolveActivity(context.packageManager) != null) {
            return launch(context, telegramIntent)
        }
        val shareIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?text=${Uri.encode(body)}"))
        return launch(context, shareIntent)
    }

    private fun openEmail(
        context: Context,
        email: String,
        subject: String,
        body: String,
    ): MessageSendResult {
        val intent = createEmailIntent(email, subject, body) ?: return MessageSendResult.FAILED
        return launch(context, intent)
    }

    private fun launch(context: Context, intent: Intent): MessageSendResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            MessageSendResult.OPENED_APP
        } catch (_: Exception) {
            MessageSendResult.FAILED
        }
    }

    private fun createSmsIntent(phone: String, body: String): Intent? {
        val normalized = PhoneNormalizer.normalize(phone) ?: return null
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$normalized")).apply {
            putExtra("sms_body", body)
        }
    }

    private fun createWhatsAppIntent(phone: String, body: String): Intent? {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return null
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$digits?text=${Uri.encode(body)}"),
        )
    }

    private fun createTelegramIntent(body: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
            setPackage("org.telegram.messenger")
        }

    private fun createEmailIntent(email: String, subject: String, body: String): Intent? {
        if (email.isBlank()) return null
        val uri = Uri.parse(
            "mailto:${Uri.encode(email)}" +
                "?subject=${Uri.encode(subject)}" +
                "&body=${Uri.encode(body)}",
        )
        return Intent(Intent.ACTION_SENDTO, uri)
    }
}

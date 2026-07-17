package com.example.money.ui.debtors

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.MessagingContact
import com.example.money.messaging.UniversalMessageSender

object DebtContactActions {

    fun dialPhone(context: Context, phone: String) {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        if (normalized.isBlank()) return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalized"))
        context.startActivity(intent)
    }

    fun openWhatsApp(context: Context, phone: String, debtorName: String) {
        if (phone.isBlank()) return
        UniversalMessageSender.send(
            context = context,
            channel = MessagingChannel.WHATSAPP,
            contact = MessagingContact(phone = phone, email = ""),
            payload = MessagePayload(
                subject = "",
                body = context.getString(
                    com.example.money.R.string.debt_whatsapp_default_body,
                    debtorName,
                ),
            ),
        )
    }
}

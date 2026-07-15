package com.example.money.settings

import com.example.money.messaging.MessagingChannel

data class PersonalBranchSettings(
    val debtNotificationsEnabled: Boolean = true,
    val debtAdvanceRemind1Day: Boolean = true,
    val debtAdvanceRemind3Days: Boolean = true,
    val debtAutoMessageEnabled: Boolean = false,
    val debtAutoMessageSms: Boolean = true,
    val debtAutoMessageWhatsapp: Boolean = true,
    val debtAutoMessageTelegram: Boolean = false,
    val debtAutoMessageEmail: Boolean = true,
    val debtDirectSmsEnabled: Boolean = false,
) {
    fun enabledAutoChannels(): List<MessagingChannel> = buildList {
        if (debtAutoMessageSms) add(MessagingChannel.SMS)
        if (debtAutoMessageWhatsapp) add(MessagingChannel.WHATSAPP)
        if (debtAutoMessageTelegram) add(MessagingChannel.TELEGRAM)
        if (debtAutoMessageEmail) add(MessagingChannel.EMAIL)
    }
}

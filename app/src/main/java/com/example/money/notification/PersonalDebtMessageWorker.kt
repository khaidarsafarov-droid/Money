package com.example.money.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.money.MoneyApplication
import com.example.money.data.mapper.toDomain
import com.example.money.domain.model.DebtType
import com.example.money.messaging.MessagePayload
import com.example.money.messaging.MessagingChannel
import com.example.money.messaging.UniversalMessageSender
import com.example.money.ui.debtors.DebtReminderShare
import kotlinx.coroutines.flow.first

class PersonalDebtMessageWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as MoneyApplication
        val settings = app.preferencesStore.personalBranchSettings.first()
        if (!settings.debtAutoMessageEnabled) return Result.success()

        val debtors = app.debtorRepository.getActiveDebtors().first()
        val now = System.currentTimeMillis()
        debtors
            .filter { !it.isPaid && it.dateDue < now && it.debtType == DebtType.CREDITOR }
            .forEach { debtor ->
                val contact = DebtReminderShare.resolveContact(applicationContext, debtor)
                val payload = MessagePayload(
                    subject = DebtReminderShare.buildSubject(applicationContext, debtor),
                    body = DebtReminderShare.buildMessage(applicationContext, debtor),
                )
                settings.enabledAutoChannels().forEach { channel ->
                    if (!UniversalMessageSender.canSend(channel, contact)) return@forEach
                    when (channel) {
                        MessagingChannel.SMS -> {
                            UniversalMessageSender.send(
                                applicationContext,
                                channel,
                                contact,
                                payload,
                                settings.debtDirectSmsEnabled,
                            )
                        }
                        else -> {
                            DebtNotificationHelper.showMessagingAction(
                                context = applicationContext,
                                debtor = debtor,
                                channel = channel,
                                title = applicationContext.getString(
                                    com.example.money.R.string.notification_debt_auto_message_title,
                                    debtor.name,
                                ),
                            )
                        }
                    }
                }
            }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "personal_debt_auto_message"

        fun uniqueWorkName(): String = UNIQUE_WORK_NAME
    }
}

package com.example.money.business.push

import android.content.Context
import com.example.money.domain.model.business.workflow.WorkflowDebtWithParticipant
import com.example.money.notification.BusinessNotificationHelper

/**
 * Routes business push notifications locally today and exposes a remote hook for FCM.
 */
object BusinessPushDelivery {

    fun sendWorkflowDebtReminder(
        context: Context,
        debt: WorkflowDebtWithParticipant,
        billTitle: String,
        body: String,
    ): Boolean = BusinessNotificationHelper.showWorkflowDebtReminder(
        context = context,
        debt = debt,
        billTitle = billTitle,
        body = body,
    )

    suspend fun sendRemote(
        context: Context,
        title: String,
        body: String,
    ): Boolean {
        val token = FcmTokenStore.getToken(context) ?: return false
        if (token.isBlank()) return false
        // Remote delivery is enabled once Firebase Messaging is configured.
        return false
    }
}

package com.example.money.domain.model.business

enum class BusinessSmsStatus {
    SENT,
    FAILED,
    PENDING,
    OPENED_APP,
}

enum class BusinessSmsTemplateId {
    DEBT_REMINDER,
    INVOICE_STATUS,
    NEW_PRODUCT,
    PROMOTION,
    PROJECT_UPDATE,
    DELIVERY_UPDATE,
    MEETING_REMINDER,
    SUBSCRIPTION_REMINDER,
    PAYMENT_RECEIPT,
    LARGE_TRANSACTION,
    CUSTOM,
}

data class BusinessSmsRecipient(
    val phone: String,
    val name: String = "",
    val clientId: Long? = null,
)

data class BusinessSmsLog(
    val id: Long = 0,
    val recipientPhone: String,
    val recipientName: String = "",
    val clientId: Long? = null,
    val body: String,
    val templateId: String? = null,
    val status: BusinessSmsStatus = BusinessSmsStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
)

data class BusinessSmsTemplate(
    val id: BusinessSmsTemplateId,
    val titleResKey: String,
    val bodyPattern: String,
)

data class BusinessSmsSendRequest(
    val recipients: List<BusinessSmsRecipient>,
    val body: String,
    val templateId: BusinessSmsTemplateId? = null,
    val useDirectSend: Boolean = false,
)

data class BusinessSmsSendResult(
    val sentCount: Int,
    val failedCount: Int,
    val openedAppCount: Int,
)

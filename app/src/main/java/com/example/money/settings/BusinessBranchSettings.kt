package com.example.money.settings

import com.example.money.domain.model.FinanceBranch
import com.example.money.messaging.MessagingChannel
import com.example.money.domain.model.business.BusinessDashboardView
import com.example.money.domain.model.business.BusinessFilterPreset
import com.example.money.domain.model.business.BusinessTransactionType
import com.example.money.domain.model.business.InvoiceStatus
import com.example.money.domain.model.business.RecurringBusinessRule
import com.example.money.domain.model.business.RecurringBusinessRuleCodec
import com.example.money.domain.model.business.SavedFilterPreset
import org.json.JSONArray
import org.json.JSONObject

data class BusinessBranchSettings(
    val activeBranch: FinanceBranch = FinanceBranch.PERSONAL,
    val businessName: String = "",
    val dashboardView: BusinessDashboardView = BusinessDashboardView.FINANCIAL,
    val savedFilterPresetsJson: String = "[]",
    val customCategoriesJson: String = "[]",
    val invoiceRemindersEnabled: Boolean = true,
    val lowCashAlertsEnabled: Boolean = true,
    val recurringTransactionsEnabled: Boolean = false,
    val invoiceReminderHour: Int = 9,
    val lowCashThresholdCents: Long = 50_000,
    val recurringRulesJson: String = "[]",
    val smsAutomationEnabled: Boolean = false,
    val smsOverdueInvoiceEnabled: Boolean = true,
    val smsDirectSendEnabled: Boolean = false,
    val whatsappAutomationEnabled: Boolean = true,
    val telegramAutomationEnabled: Boolean = false,
    val emailAutomationEnabled: Boolean = true,
) {
    fun savedFilterPresets(): List<SavedFilterPreset> = decodeSavedFilterPresets(savedFilterPresetsJson)

    fun enabledOverdueAutomationChannels(): List<MessagingChannel> = buildList {
        if (smsAutomationEnabled && smsOverdueInvoiceEnabled) add(MessagingChannel.SMS)
        if (whatsappAutomationEnabled) add(MessagingChannel.WHATSAPP)
        if (telegramAutomationEnabled) add(MessagingChannel.TELEGRAM)
        if (emailAutomationEnabled) add(MessagingChannel.EMAIL)
    }

    fun hasOverdueInvoiceAutomation(): Boolean = enabledOverdueAutomationChannels().isNotEmpty()

    fun customCategories(): List<String> = decodeStringList(customCategoriesJson)

    fun recurringRules(): List<RecurringBusinessRule> = RecurringBusinessRuleCodec.decode(recurringRulesJson)

    companion object {
        fun encodeSavedFilterPresets(presets: List<SavedFilterPreset>): String {
            val array = JSONArray()
            presets.forEach { preset ->
                array.put(
                    JSONObject().apply {
                        put("name", preset.name)
                        preset.clientId?.let { put("clientId", it) }
                        preset.projectId?.let { put("projectId", it) }
                        preset.category?.let { put("category", it) }
                        preset.transactionType?.let { put("transactionType", it.name) }
                        preset.invoiceStatus?.let { put("invoiceStatus", it.name) }
                        put("periodPreset", preset.periodPreset.name)
                    },
                )
            }
            return array.toString()
        }

        fun decodeSavedFilterPresets(json: String): List<SavedFilterPreset> = runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    add(
                        SavedFilterPreset(
                            name = obj.getString("name"),
                            clientId = obj.optLong("clientId").takeIf { obj.has("clientId") && it > 0 },
                            projectId = obj.optLong("projectId").takeIf { obj.has("projectId") && it > 0 },
                            category = obj.optString("category").takeIf { it.isNotBlank() },
                            transactionType = obj.optString("transactionType").takeIf { it.isNotBlank() }
                                ?.let { runCatching { BusinessTransactionType.valueOf(it) }.getOrNull() },
                            invoiceStatus = obj.optString("invoiceStatus").takeIf { it.isNotBlank() }
                                ?.let { runCatching { InvoiceStatus.valueOf(it) }.getOrNull() },
                            periodPreset = runCatching {
                                BusinessFilterPreset.valueOf(obj.optString("periodPreset", BusinessFilterPreset.THIS_MONTH.name))
                            }.getOrDefault(BusinessFilterPreset.THIS_MONTH),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())

        fun encodeStringList(items: List<String>): String {
            val array = JSONArray()
            items.forEach { array.put(it) }
            return array.toString()
        }

        fun decodeStringList(json: String): List<String> = runCatching {
            val array = JSONArray(json)
            buildList {
                for (index in 0 until array.length()) {
                    add(array.getString(index))
                }
            }
        }.getOrDefault(emptyList())
    }
}

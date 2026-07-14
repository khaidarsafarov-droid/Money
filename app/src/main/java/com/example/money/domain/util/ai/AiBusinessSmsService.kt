package com.example.money.domain.util.ai

import com.example.money.business.sms.BusinessSmsTemplateContext
import com.example.money.business.sms.BusinessSmsTemplates
import com.example.money.domain.model.business.BusinessSmsTemplateId

enum class BusinessSmsTone {
    FORMAL,
    FRIENDLY,
    URGENT,
}

object AiBusinessSmsService {

    fun generate(
        templateId: BusinessSmsTemplateId,
        context: BusinessSmsTemplateContext,
        tone: BusinessSmsTone = BusinessSmsTone.FRIENDLY,
        languageHint: String = "en",
    ): String {
        val base = BusinessSmsTemplates.render(templateId, context)
        if (base.isBlank()) return ""

        val toned = when (tone) {
            BusinessSmsTone.FORMAL -> base.replace("Hello", "Dear").replace("Friendly reminder", "Reminder")
            BusinessSmsTone.FRIENDLY -> base
            BusinessSmsTone.URGENT -> "URGENT: $base"
        }

        return if (languageHint.equals("ru", ignoreCase = true)) {
            translateHeuristic(toned)
        } else {
            toned
        }
    }

    fun personalize(body: String, clientName: String): String =
        body.replace("{client}", clientName).replace("Client", clientName)

    private fun translateHeuristic(text: String): String = text
        .replace("Hello", "Здравствуйте")
        .replace("Friendly reminder", "Напоминание")
        .replace("invoice status", "статус счёта")
        .replace("Amount", "Сумма")
        .replace("Due", "Срок")
        .replace("Thank you", "Спасибо")
}

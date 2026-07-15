package com.example.money.domain.util.ai

import com.example.money.domain.model.business.workflow.WorkflowReminderContext
import com.example.money.domain.util.MoneyMath

object AiWorkflowMessageService {

    fun generate(
        context: WorkflowReminderContext,
        languageHint: String = "ru",
    ): String {
        val amount = formatUsd(context.amountCents)
        val total = formatUsd(context.totalAmountCents)
        val base = if (languageHint.equals("ru", ignoreCase = true)) {
            buildString {
                appendLine("Привет, ${context.participantName}!")
                if (context.periodLabel.isNotBlank()) {
                    appendLine("${context.billTitle} (${context.periodLabel}).")
                } else {
                    appendLine(context.billTitle + ".")
                }
                if (context.billDescription.isNotBlank()) {
                    appendLine(context.billDescription)
                }
                appendLine("Твоя часть: $amount.")
                appendLine("Общий счёт: $total.")
                append("Спасибо!")
            }
        } else {
            buildString {
                appendLine("Hi ${context.participantName}!")
                appendLine("${context.billTitle}${if (context.periodLabel.isNotBlank()) " (${context.periodLabel})" else ""}.")
                if (context.billDescription.isNotBlank()) appendLine(context.billDescription)
                appendLine("Your share: $amount.")
                appendLine("Total bill: $total.")
                append("Thank you!")
            }
        }
        return base.trim()
    }

    private fun formatUsd(cents: Long): String {
        val dollars = MoneyMath.fromCents(cents)
        return if (dollars % 1.0 == 0.0) {
            "$${dollars.toLong()}"
        } else {
            "$" + "%.2f".format(dollars)
        }
    }
}

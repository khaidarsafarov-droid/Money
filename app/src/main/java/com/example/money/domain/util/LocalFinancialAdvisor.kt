package com.example.money.domain.util

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtType
import com.example.money.settings.AutopilotStrategy
import java.util.Locale
import kotlin.math.max

/**
 * On-device financial consultant. No network, no API keys —
 * rule-based intent routing over local finance engines.
 */
object LocalFinancialAdvisor {

    data class AdviceRequest(
        val rawText: String = "",
        val promptId: String? = null,
    )

    data class AdviceContext(
        val debts: List<Debtor>,
        val totalAccumulatedCents: Long = 0L,
        val remainingToGoalCents: Long = 0L,
        val extraMonthlyCents: Long = 5_000L,
        val payoffStrategy: AutopilotStrategy = AutopilotStrategy.AVALANCHE,
        val now: Long = System.currentTimeMillis(),
        val locale: Locale = Locale.getDefault(),
    )

    fun resolvePromptId(request: AdviceRequest): String {
        request.promptId?.takeIf { it.isNotBlank() }?.let { return it }
        return classifyIntent(request.rawText)
    }

    fun classifyIntent(text: String): String {
        val t = text.trim().lowercase(Locale.getDefault())
        if (t.isEmpty()) return "overview"
        return when {
            t.contains("мотив") || t.contains("поддерж") || t.contains("motiv") ||
                t.contains("encourage") -> "motivation"
            t.contains("процент") || t.contains("лавин") || t.contains("avalanche") ||
                t.contains("interest") -> "which_debt_first"
            t.contains("перв") || t.contains("с чего") || t.contains("начать") ||
                t.contains("first") || t.contains("start") || t.contains("snowball") ||
                t.contains("закрыть") -> "which_debt_first"
            t.contains("50") || t.contains("лишн") || t.contains("extra") ||
                t.contains("больш") || t.contains("what if") || t.contains("что если") -> "what_if_extra"
            t.contains("просроч") || t.contains("overdue") || t.contains("urgent") ||
                t.contains("срочн") -> "overdue"
            t.contains("цел") || t.contains("goal") || t.contains("копить") ||
                t.contains("накоп") -> "goals"
            t.contains("бюджет") || t.contains("расход") || t.contains("budget") ||
                t.contains("expense") || t.contains("deposit") || t.contains("пополн") -> "budget"
            t.contains("доход") || t.contains("работ") || t.contains("income") ||
                t.contains("заработ") || t.contains("фриланс") -> "income"
            t.contains("автопилот") || t.contains("autopilot") || t.contains("микро") -> "autopilot"
            t.contains("мне должны") || t.contains("owed") || t.contains("creditor") -> "owes_me"
            t.contains("я должен") || t.contains("i owe") || t.contains("debtor") -> "i_owe"
            t.contains("свобод") || t.contains("freedom") || t.contains("когда") -> "freedom"
            else -> "overview"
        }
    }

    fun buildReply(request: AdviceRequest, context: AdviceContext): String {
        val promptId = resolvePromptId(request)
        val active = context.debts.filter { !it.isPaid && it.remainingAmountCents > 0L }
        val en = context.locale.language.startsWith("en", ignoreCase = true)

        if (active.isEmpty() && promptId !in setOf("goals", "budget", "income", "overview", "motivation")) {
            return if (en) {
                "You have no active debts — that’s already a win. Focus on goals or ask about your budget."
            } else {
                "У вас нет активных долгов — это уже победа. Можно сфокусироваться на целях или бюджете."
            }
        }

        return when (promptId) {
            "which_debt_first" -> whichDebtFirstAdvice(active, context.payoffStrategy, en)
            "what_if_extra" -> DebtAdvisorTemplates.buildReply(
                promptId = "what_if_extra",
                debts = context.debts,
                extraMonthlyCents = context.extraMonthlyCents,
                now = context.now,
            )
            "motivation" -> DebtAdvisorTemplates.buildReply(
                promptId = "motivation",
                debts = context.debts,
                extraMonthlyCents = context.extraMonthlyCents,
                now = context.now,
            )
            "overdue" -> overdueAdvice(active, en, context.now)
            "goals" -> goalsAdvice(context, en)
            "budget" -> budgetAdvice(context, en)
            "income" -> incomeAdvice(active, context, en)
            "autopilot" -> autopilotAdvice(active, context.payoffStrategy, en)
            "owes_me" -> owesMeAdvice(context.debts, en)
            "i_owe" -> iOweAdvice(context.debts, en)
            "freedom" -> freedomAdvice(active, context, en)
            else -> overviewAdvice(active, context, en)
        }
    }

    private fun overdueAdvice(active: List<Debtor>, en: Boolean, now: Long): String {
        val overdue = active.filter { DebtDueDateUtils.isOverdue(it.dateDue, now) }
        if (overdue.isEmpty()) {
            return if (en) "No overdue debts right now. Stay on schedule."
            else "Сейчас нет просроченных долгов. Держите темп."
        }
        val names = overdue.take(3).joinToString(", ") { "«${it.name}»" }
        val total = overdue.sumOf { it.remainingAmountCents } / 100.0
        return if (en) {
            "Overdue: $names. About $${total.toInt()} at risk. Prioritize a payment or a reminder today."
        } else {
            "Просрочено: $names. Под угрозой около $${total.toInt()}. Сделайте платёж или напомните сегодня."
        }
    }

    private fun goalsAdvice(context: AdviceContext, en: Boolean): String {
        val saved = context.totalAccumulatedCents / 100.0
        val left = context.remainingToGoalCents / 100.0
        return if (en) {
            "Saved toward goals: $${saved.toInt()}. Still needed: $${left.toInt()}. Move a small transfer this week."
        } else {
            "В целях уже $${saved.toInt()}. До целей осталось $${left.toInt()}. Сделайте небольшой перевод на этой неделе."
        }
    }

    private fun budgetAdvice(context: AdviceContext, en: Boolean): String {
        val goalNet = context.totalAccumulatedCents / 100.0
        val debtRemainder = context.debts
            .filter { !it.isPaid && it.remainingAmountCents > 0L }
            .sumOf { it.remainingAmountCents } / 100.0
        return if (en) {
            "Goal money $${goalNet.toInt()} · debt remaining $${debtRemainder.toInt()}. Put extra money on the nearest debt or goal."
        } else {
            "В целях $${goalNet.toInt()} · остаток долгов $${debtRemainder.toInt()}. Лишние деньги направьте в ближайший долг или цель."
        }
    }

    private fun incomeAdvice(active: List<Debtor>, context: AdviceContext, en: Boolean): String {
        val projection = DebtFreedomEngine.calculateFreedomProjection(
            debts = active,
            extraMonthlyCents = 10_000L,
            now = context.now,
        )
        val saved = projection.monthsSavedWithExtra ?: 0
        return if (en) {
            "An extra $100/mo could shorten debt freedom by ~$saved months. Open Income ideas for freelance options."
        } else {
            "Лишние $100/мес могут сократить путь к свободе примерно на $saved мес. Откройте «Идеи дохода»."
        }
    }

    private fun whichDebtFirstAdvice(
        active: List<Debtor>,
        strategy: AutopilotStrategy,
        en: Boolean,
    ): String {
        val target = DebtAutopilotEngine.pickTargetDebt(active, strategy)
            ?: return if (en) "Add an \"I owe\" debt first." else "Сначала добавьте долг «Я должен»."
        return when (strategy) {
            AutopilotStrategy.AVALANCHE -> {
                val rate = target.interestRateBps / 100.0
                if (en) {
                    "Avalanche: start with «${target.name}» (highest rate ${"%.1f".format(rate)}%)."
                } else {
                    "Стратегия «Лавина»: начните с «${target.name}» — ставка ${"%.1f".format(rate)}%."
                }
            }
            AutopilotStrategy.SNOWBALL -> {
                if (en) {
                    "Snowball: start with «${target.name}» — smallest remaining balance for a quick win."
                } else {
                    "Стратегия «Снежный ком»: начните с «${target.name}» — самый маленький остаток."
                }
            }
        }
    }

    private fun autopilotAdvice(active: List<Debtor>, strategy: AutopilotStrategy, en: Boolean): String {
        val target = DebtAutopilotEngine.pickTargetDebt(active, strategy)
        return if (target == null) {
            if (en) "Enable Autopilot in settings to get daily micro-payment reminders."
            else "Включите Автопилот в настройках — будут ежедневные микро-напоминания."
        } else if (en) {
            "Autopilot would nudge «${target.name}» first (${strategy.name.lowercase()}). Turn it on in Settings → Autopilot."
        } else {
            "Автопилот начнёт с «${target.name}» (${if (strategy == AutopilotStrategy.AVALANCHE) "лавина" else "снежный ком"}). Включите в Настройки → Автопилот."
        }
    }

    private fun owesMeAdvice(debts: List<Debtor>, en: Boolean): String {
        val list = debts.filter { it.debtType == DebtType.CREDITOR && !it.isPaid }
        val total = list.sumOf { it.remainingAmountCents } / 100.0
        return if (en) {
            "${list.size} people owe you ~$${total.toInt()}. Use Remind or WhatsApp from the debt menu."
        } else {
            "Вам должны ${list.size} чел. · ~$${total.toInt()}. Напомните через меню долга (WhatsApp / звонок)."
        }
    }

    private fun iOweAdvice(debts: List<Debtor>, en: Boolean): String {
        val list = debts.filter { it.debtType == DebtType.DEBTOR && !it.isPaid }
        val total = list.sumOf { it.remainingAmountCents } / 100.0
        return if (en) {
            "You owe ~$${total.toInt()} across ${list.size} debts. Open Forecast to see your freedom date."
        } else {
            "Вы должны ~$${total.toInt()} по ${list.size} долг(ам). Откройте «Прогноз», чтобы увидеть дату свободы."
        }
    }

    private fun freedomAdvice(active: List<Debtor>, context: AdviceContext, en: Boolean): String {
        val projection = DebtFreedomEngine.calculateFreedomProjection(
            debts = active,
            extraMonthlyCents = 0L,
            now = context.now,
        )
        val date = projection.freedomDateMillis?.let { DebtFreedomEngine.isoDateLabel(it) }
        return if (date == null) {
            if (en) "Not enough payment history yet for a freedom date. Log a few payments first."
            else "Пока мало данных для даты свободы. Внесите несколько платежей."
        } else if (en) {
            "At your current pace, debt freedom is around $date. Ask «what if +$50» to see acceleration."
        } else {
            "При текущем темпе свобода около $date. Спросите «что если +$50», чтобы увидеть ускорение."
        }
    }

    private fun overviewAdvice(active: List<Debtor>, context: AdviceContext, en: Boolean): String {
        val owedToMe = context.debts.filter { it.debtType == DebtType.CREDITOR && !it.isPaid }
            .sumOf { it.remainingAmountCents } / 100.0
        val iOwe = active.filter { it.debtType == DebtType.DEBTOR }
            .sumOf { it.remainingAmountCents } / 100.0
        val overdue = active.count { DebtDueDateUtils.isOverdue(it.dateDue, context.now) }
        return if (en) {
            "Snapshot: owed to you $${owedToMe.toInt()} · you owe $${iOwe.toInt()} · overdue $overdue. Ask: where to start, budget, or goals."
        } else {
            "Сводка: вам должны $${owedToMe.toInt()} · вы должны $${iOwe.toInt()} · просрочено $overdue. Спросите: с чего начать, бюджет или цели."
        }
    }

    fun userFacingPromptLabel(promptId: String, rawText: String, en: Boolean): String {
        if (rawText.isNotBlank()) return rawText.trim()
        return when (promptId) {
            "which_debt_first" -> if (en) "Where should I start?" else "С чего начать?"
            "what_if_extra" -> if (en) "What if I pay +$50?" else "Что если +$50?"
            "motivation" -> if (en) "Motivate me" else "Дай мотивацию"
            "overdue" -> if (en) "Any overdue debts?" else "Есть просрочки?"
            "goals" -> if (en) "How are my goals?" else "Как мои цели?"
            "budget" -> if (en) "Budget health?" else "Как бюджет?"
            "income" -> if (en) "Need more income?" else "Нужен доход?"
            "autopilot" -> if (en) "About Autopilot" else "Про автопилот"
            "freedom" -> if (en) "When am I free?" else "Когда свобода?"
            "overview" -> if (en) "Give me an overview" else "Дай сводку"
            else -> promptId
        }
    }
}

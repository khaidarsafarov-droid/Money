package com.example.money.domain.util

import com.example.money.domain.model.DashboardGoalMetrics
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtType
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.FinancialPsychotype
import kotlin.math.ceil
import kotlin.math.max

object GoalPsychologyEngines {

    private const val MS_PER_DAY = 86_400_000L
    private const val MS_PER_MONTH = 30L * MS_PER_DAY

    data class PayoffPlanItem(
        val debtorId: Long,
        val name: String,
        val remainingCents: Long,
        val projectedCloseDateMs: Long?,
        val progressPercent: Int,
        val hasPaymentData: Boolean,
    )

    fun determinePsychotype(answers: List<Char>): FinancialPsychotype {
        if (answers.isEmpty()) return FinancialPsychotype.BALANCED

        val counts = mutableMapOf(
            FinancialPsychotype.SPENDER to 0,
            FinancialPsychotype.SAVER to 0,
            FinancialPsychotype.INDIFFERENT to 0,
            FinancialPsychotype.PLAYER to 0,
            FinancialPsychotype.BALANCED to 0,
        )

        answers.forEach { answer ->
            when (answer.lowercaseChar()) {
                'a' -> counts[FinancialPsychotype.SPENDER] = counts.getValue(FinancialPsychotype.SPENDER) + 1
                'b' -> counts[FinancialPsychotype.SAVER] = counts.getValue(FinancialPsychotype.SAVER) + 1
                'c' -> counts.getValue(FinancialPsychotype.INDIFFERENT).let {
                    counts[FinancialPsychotype.INDIFFERENT] = it + 1
                }
                'd' -> counts.getValue(FinancialPsychotype.PLAYER).let {
                    counts[FinancialPsychotype.PLAYER] = it + 1
                }
            }
        }

        val max = counts.values.maxOrNull() ?: 0
        val winners = counts.filterValues { it == max }.keys
        return if (winners.size == 1) winners.first() else FinancialPsychotype.BALANCED
    }

    fun psychotypeLabel(type: FinancialPsychotype): String = when (type) {
        FinancialPsychotype.SPENDER -> "Транжира"
        FinancialPsychotype.SAVER -> "Эконом"
        FinancialPsychotype.INDIFFERENT -> "Равнодушный"
        FinancialPsychotype.PLAYER -> "Игрок"
        FinancialPsychotype.BALANCED -> "Сбалансированный"
    }

    fun psychotypeTagline(type: FinancialPsychotype): String = when (type) {
        FinancialPsychotype.SPENDER -> "Деньги любят счёт — и уют 🔥"
        FinancialPsychotype.SAVER -> "Деньги — это энергия, дайте ей течь"
        FinancialPsychotype.INDIFFERENT -> "План важнее эмоций"
        FinancialPsychotype.PLAYER -> "Каждый платёж — очко к победе"
        FinancialPsychotype.BALANCED -> "Шаг за шагом к свободе"
    }

    fun psychotypeOnboardingDescription(type: FinancialPsychotype): String = when (type) {
        FinancialPsychotype.SPENDER -> "Вы любите тратить — это даёт вам энергию. Давайте направим её в нужное русло."
        FinancialPsychotype.SAVER -> "Вы умеете копить — это ваша суперсила. Научимся тратить с удовольствием."
        FinancialPsychotype.INDIFFERENT -> "Деньги не ваша страсть, но контроль над ними даст вам свободу."
        FinancialPsychotype.PLAYER -> "Риск — ваше топливо. Используем его для роста, а не для потерь."
        FinancialPsychotype.BALANCED -> "Вы на верном пути — сохраняйте баланс."
    }

    fun affirmationForFear(
        fearId: String,
        progressPercent: Int,
        displayName: String? = null,
        goalText: String? = null,
    ): String {
        val base = when (fearId) {
            "wont_escape" -> "Каждый платёж приближает вас к свободе. Вы уже сделали первый шаг."
            "earn_more_than_parents" -> "Ваш успех — это благодарность, а не предательство."
            "ashamed_of_debt" -> "Долги — это опыт, а не приговор. Вы справитесь."
            "money_corrupts" -> "Вы решаете, каким человеком быть — деньги лишь инструмент."
            "rich_are_bad" -> "Финансовая свобода может служить вам и близким."
            "afraid_of_numbers" -> if (progressPercent > 0) {
                "Сегодня посмотрите на свой прогресс. Вы стали ближе на $progressPercent%."
            } else {
                "Сегодня посмотрите на свой прогресс — вы на правильном пути."
            }
            else -> "Каждый шаг к свободе имеет значение."
        }
        val who = displayName?.trim()?.takeIf { it.length >= 2 } ?: return base
        val goal = goalText?.trim()?.takeIf { it.isNotEmpty() }
        return if (goal != null) {
            "$who, вы справитесь! $base Ваша цель — $goal."
        } else {
            "$who, вы справитесь! $base"
        }
    }

    fun buildPayoffPlan(
        debtors: List<Debtor>,
        strategy: AutopilotStrategy,
        now: Long = System.currentTimeMillis(),
    ): List<PayoffPlanItem> {
        val active = debtors.filter { it.debtType == DebtType.DEBTOR && !it.isPaid && it.remainingAmountCents > 0L }
        val sorted = when (strategy) {
            AutopilotStrategy.SNOWBALL -> active.sortedBy { it.remainingAmountCents }
            AutopilotStrategy.AVALANCHE -> active.sortedWith(
                compareByDescending<Debtor> { it.interestRateBps }
                    .thenByDescending { it.remainingAmountCents },
            )
        }

        return sorted.map { debtor ->
            val monthly = estimateMonthly(debtor, now)
            val projected = if (monthly != null && monthly > 0L) {
                val months = ceil(debtor.remainingAmountCents.toDouble() / monthly).toInt()
                val paceDate = addMonths(now, months)
                if (debtor.dateDue > now) minOf(debtor.dateDue, paceDate) else paceDate
            } else {
                null
            }
            val progress = if (debtor.targetAmountCents > 0L) {
                ((debtor.currentAmountCents.toDouble() / debtor.targetAmountCents) * 100).toInt().coerceAtMost(100)
            } else {
                0
            }
            PayoffPlanItem(
                debtorId = debtor.id,
                name = debtor.name,
                remainingCents = debtor.remainingAmountCents,
                projectedCloseDateMs = projected,
                progressPercent = progress,
                hasPaymentData = monthly != null,
            )
        }
    }

    fun freedomSummary(
        debtors: List<Debtor>,
        extraMonthlyCents: Long = 0L,
        now: Long = System.currentTimeMillis(),
    ): DebtFreedomEngine.FreedomProjection =
        DebtFreedomEngine.calculateFreedomProjection(
            debts = debtors,
            debtType = DebtType.DEBTOR,
            extraMonthlyCents = extraMonthlyCents,
            now = now,
        )

    fun totalDebtProgressPercent(debtors: List<Debtor>): Int {
        val iOwe = debtors.filter { it.debtType == DebtType.DEBTOR }
        val total = iOwe.sumOf { it.targetAmountCents }
        if (total <= 0L) return 0
        val paid = iOwe.sumOf { it.currentAmountCents }
        return ((paid.toDouble() / total) * 100).toInt().coerceAtMost(100)
    }

    /**
     * One home score: paid "I owe" cents plus goal savings, over those debt
     * targets plus the goal pool. Creditor balances stay out of the score.
     */
    fun planProgressPercent(debtors: List<Debtor>, goalMetrics: DashboardGoalMetrics): Int {
        val iOwe = debtors.filter { it.debtType == DebtType.DEBTOR }
        val paid = iOwe.sumOf { it.currentAmountCents } + goalMetrics.totalAccumulatedCents
        val pool = iOwe.sumOf { it.targetAmountCents } +
            goalMetrics.totalAccumulatedCents +
            goalMetrics.totalRemainingToGoalCents
        if (pool <= 0L) return 0
        return ((paid.toDouble() / pool) * 100).toInt().coerceIn(0, 100)
    }

    fun daysUntilPayday(paydayDayOfMonth: Int, now: Long = System.currentTimeMillis()): Int {
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val target = (calendar.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.DAY_OF_MONTH, paydayDayOfMonth.coerceIn(1, 28))
            set(java.util.Calendar.HOUR_OF_DAY, 12)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(java.util.Calendar.MONTH, 1)
            }
        }
        return max(0, ceil((target.timeInMillis - now).toDouble() / MS_PER_DAY).toInt())
    }

    private fun estimateMonthly(debtor: Debtor, now: Long): Long? {
        val recent = debtor.paymentHistory.filter { it.paidAt >= now - 30L * MS_PER_DAY }
        if (recent.isEmpty()) return null
        val total = recent.sumOf { it.amountCents }
        val oldest = recent.minOf { it.paidAt }
        val days = max(1.0, (now - oldest).toDouble() / MS_PER_DAY)
        return ceil((total / days) * 30.0).toLong()
    }

    private fun addMonths(fromMillis: Long, months: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = fromMillis
            add(java.util.Calendar.MONTH, months)
        }
        return calendar.timeInMillis
    }
}

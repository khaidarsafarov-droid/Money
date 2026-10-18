package com.example.money.domain.util

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtType
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.AutopilotStrategy
import kotlin.math.ceil
import kotlin.math.max

object DebtAutopilotEngine {

    data class MicroPaymentPlan(
        val debtorId: Long,
        val debtorName: String,
        val amountCents: Long,
    )

    fun pickTargetDebt(debts: List<Debtor>, strategy: AutopilotStrategy): Debtor? {
        val active = debts.filter {
            it.debtType == DebtType.DEBTOR && !it.isPaid && it.remainingAmountCents > 0L
        }
        if (active.isEmpty()) return null
        return when (strategy) {
            AutopilotStrategy.SNOWBALL -> active.minByOrNull { it.remainingAmountCents }
            AutopilotStrategy.AVALANCHE -> active.sortedWith(
                compareByDescending<Debtor> { it.interestRateBps }
                    .thenBy { it.dateDue }
                    .thenByDescending { it.remainingAmountCents },
            ).firstOrNull()
        }
    }

    fun planMicroPayment(debts: List<Debtor>, settings: AutopilotSettings): MicroPaymentPlan? {
        if (!settings.enabled) return null
        val target = pickTargetDebt(debts, settings.strategy) ?: return null
        val amount = minOf(
            microPaymentAmountCents(settings),
            target.remainingAmountCents,
        )
        if (amount <= 0L) return null
        return MicroPaymentPlan(
            debtorId = target.id,
            debtorName = target.name,
            amountCents = amount,
        )
    }

    private fun microPaymentAmountCents(settings: AutopilotSettings): Long {
        val base = max(100L, settings.dailyAmountCents)
        return if (settings.roundPaymentsToTen) roundPaymentToTenCents(base) else base
    }

    fun roundPaymentToTenCents(amountCents: Long): Long {
        if (amountCents <= 0L) return amountCents
        val dollars = ceil(amountCents / 100.0).toLong()
        val roundedDollars = ((dollars + 9) / 10) * 10
        return roundedDollars * 100
    }
}

object DebtAdvisorTemplates {

    fun buildReply(
        promptId: String,
        debts: List<Debtor>,
        extraMonthlyCents: Long = 5_000L,
        now: Long = System.currentTimeMillis(),
    ): String {
        val active = debts.filter { !it.isPaid && it.remainingAmountCents > 0L }
        if (active.isEmpty()) {
            return "У вас нет активных долгов — это уже победа. Можно сфокусироваться на целях."
        }

        return when (promptId) {
            "which_debt_first" -> {
                val hasHighRate = active.any { it.interestRateBps > 0 }
                val avalanche = DebtAutopilotEngine.pickTargetDebt(active, AutopilotStrategy.AVALANCHE)
                val snowball = DebtAutopilotEngine.pickTargetDebt(active, AutopilotStrategy.SNOWBALL)
                if (hasHighRate && avalanche != null) {
                    val rate = avalanche.interestRateBps / 100.0
                    "Стратегия «Лавина»: начните с «${avalanche.name}» — самая высокая ставка (${String.format("%.1f", rate)}%). Закрыв «плохие» долги, вы сэкономите на процентах."
                } else {
                    val target = snowball ?: return "Сначала добавьте долг в приложение."
                    "Начните с «${target.name}» — это самый маленький остаток. Быстрая победа даст энергию закрыть остальные."
                }
            }
            "what_if_extra" -> {
                val boosted = DebtFreedomEngine.calculateFreedomProjection(
                    debts = active,
                    extraMonthlyCents = extraMonthlyCents,
                    now = now,
                )
                val extra = extraMonthlyCents / 100.0
                val saved = boosted.monthsSavedWithExtra ?: 0
                if (saved > 0) {
                    "+$${extra.toInt()}/мес сократит путь примерно на $saved мес. Это ощутимый сдвиг без героических усилий."
                } else {
                    "+$${extra.toInt()}/мес ускорит погашение. Попробуйте один месяц — и посмотрите, как сдвинется дата."
                }
            }
            "motivation" -> {
                val projection = DebtFreedomEngine.calculateFreedomProjection(active, now = now)
                val closed = debts.count { it.isPaid }
                val date = projection.freedomDateMillis?.let { DebtFreedomEngine.isoDateLabel(it) }
                if (date == null) {
                    "Вы уже закрыли $closed долг(ов). Продолжайте — каждый платёж приближает цель."
                } else {
                    "При текущем темпе долги закроются $date. Каждый платёж сдвигает эту дату ближе."
                }
            }
            else -> "Спросите про долги — я подскажу следующий шаг."
        }
    }
}

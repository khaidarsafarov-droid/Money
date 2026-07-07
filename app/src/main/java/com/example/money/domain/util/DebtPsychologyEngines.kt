package com.example.money.domain.util

import com.example.money.domain.model.Debtor
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.AutopilotStrategy
import com.example.money.settings.GamificationBadges
import com.example.money.settings.GamificationState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max

object DebtGamificationEngine {

    fun isoDateFromMillis(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()

    fun applyPayment(
        previous: GamificationState,
        paidAtMillis: Long,
        dateDueMillis: Long,
        currentCents: Long,
        targetCents: Long,
        paymentCount: Int,
        now: Long = System.currentTimeMillis(),
    ): GamificationState {
        val streak = computeStreak(previous, paidAtMillis, dateDueMillis, now)
        val badges = mutableSetOf<String>()
        badges.addAll(previous.unlockedBadges)
        if (paymentCount >= 1) badges.add(GamificationBadges.FIRST_STEP)

        val milestones = DebtFreedomEngine.computeMilestones(currentCents, targetCents)
        if (milestones.milestones.any { it.percent >= 50 && it.reached }) {
            badges.add(GamificationBadges.HALF_WAY)
        }
        if (milestones.milestones.any { it.percent == 100 && it.reached }) {
            badges.add(GamificationBadges.FREEDOM)
        }
        if (streak >= 3) badges.add(GamificationBadges.STREAK_3)

        val lastWin = when {
            badges.contains(GamificationBadges.FREEDOM) -> "Долг закрыт — отличная победа!"
            milestones.nextMilestone != null ->
                "До вехи ${milestones.nextMilestone.percent}% осталось немного. Вы справляетесь."
            else -> "Серия платежей: $streak. Так держать."
        }

        return GamificationState(
            streakCount = streak,
            lastPaymentIsoDate = isoDateFromMillis(paidAtMillis),
            unlockedBadges = badges,
            lastWinMessage = lastWin,
        )
    }

    fun badgeLabel(badgeId: String): String = when (badgeId) {
        GamificationBadges.FIRST_STEP -> "Первый шаг"
        GamificationBadges.HALF_WAY -> "Полпути"
        GamificationBadges.FREEDOM -> "Свобода"
        GamificationBadges.STREAK_3 -> "Серия ×3"
        else -> badgeId
    }

    private fun computeStreak(
        previous: GamificationState,
        paidAtMillis: Long,
        dateDueMillis: Long,
        now: Long,
    ): Int {
        if (paidAtMillis > dateDueMillis && dateDueMillis > 0L) return 1
        val paymentDay = isoDateFromMillis(paidAtMillis)
        val last = previous.lastPaymentIsoDate ?: return 1
        if (paymentDay == last) return previous.streakCount
        val gap = ChronoUnit.DAYS.between(LocalDate.parse(last), LocalDate.parse(paymentDay))
        return if (gap == 1L) previous.streakCount + 1 else 1
    }
}

object DebtAutopilotEngine {

    data class MicroPaymentPlan(
        val debtorId: Long,
        val debtorName: String,
        val amountCents: Long,
    )

    fun pickTargetDebt(debts: List<Debtor>, strategy: AutopilotStrategy): Debtor? {
        val active = debts.filter { !it.isPaid && it.remainingAmountCents > 0L }
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
                    "+$${extra.toInt()}/мес ускорит погашение. Попробуйте один месяц — и посмотрите, как изменится дата свободы."
                }
            }
            "motivation" -> {
                val projection = DebtFreedomEngine.calculateFreedomProjection(active, now = now)
                val closed = debts.count { it.isPaid }
                val date = projection.freedomDateMillis?.let { DebtFreedomEngine.isoDateLabel(it) }
                if (date == null) {
                    "Вы уже закрыли $closed долг(ов). Продолжайте — каждый платёж приближает свободу."
                } else {
                    "Вы на пути к свободе. При текущем темпе цель — $date. Каждый платёж сдвигает эту дату ближе."
                }
            }
            else -> "Спросите про долги — я подскажу следующий шаг."
        }
    }
}

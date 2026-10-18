package com.example.money.domain.util

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtType
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object GoalChallengeEngines {

    const val BADGE_FIRST_PAYMENT = "first_payment"
    const val BADGE_SNOWBALL = "snowball"
    const val BADGE_HERO = "hero"
    const val BADGE_FREEDOM = "freedom"

    enum class ChallengeTemplateId(val stored: String) {
        CLOSE_DEBT_14D("close_debt_14d"),
        NO_COFFEE_MONTH("no_coffee_month"),
        THREE_EARLY_PAYMENTS("three_early_payments"),
        ;

        companion object {
            fun fromStored(value: String): ChallengeTemplateId? =
                entries.firstOrNull { it.stored == value }
        }
    }

    data class ChallengeTemplate(
        val id: ChallengeTemplateId,
        val title: String,
        val description: String,
        val requiresDebtPick: Boolean,
        val targetDays: Int? = null,
    )

    data class ActiveChallenge(
        val id: String,
        val templateId: ChallengeTemplateId,
        val debtId: Long? = null,
        val debtName: String? = null,
        val startedAtMs: Long,
        val targetDays: Int? = null,
        val progressDays: Int = 0,
        val completed: Boolean = false,
    )

    val templates: List<ChallengeTemplate> = listOf(
        ChallengeTemplate(
            id = ChallengeTemplateId.CLOSE_DEBT_14D,
            title = "Закрыть долг за 14 дней",
            description = "Выберите долг и поставьте срок",
            requiresDebtPick = true,
            targetDays = 14,
        ),
        ChallengeTemplate(
            id = ChallengeTemplateId.NO_COFFEE_MONTH,
            title = "Сэкономить \$50 на кофе за месяц",
            description = "Отмечайте каждый день без лишних трат на кофе",
            requiresDebtPick = false,
            targetDays = 30,
        ),
        ChallengeTemplate(
            id = ChallengeTemplateId.THREE_EARLY_PAYMENTS,
            title = "Внести 3 платежа досрочно",
            description = "Отслеживается по вашим оплатам",
            requiresDebtPick = false,
        ),
    )

    fun templateFor(id: ChallengeTemplateId): ChallengeTemplate =
        templates.first { it.id == id }

    fun parseChallenges(json: String): List<ActiveChallenge> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val templateId = ChallengeTemplateId.fromStored(obj.getString("templateId")) ?: continue
                    add(
                        ActiveChallenge(
                            id = obj.getString("id"),
                            templateId = templateId,
                            debtId = obj.optLong("debtId").takeIf { obj.has("debtId") && !obj.isNull("debtId") },
                            debtName = obj.optString("debtName").takeIf { it.isNotBlank() },
                            startedAtMs = obj.getLong("startedAtMs"),
                            targetDays = obj.optInt("targetDays").takeIf { obj.has("targetDays") },
                            progressDays = obj.optInt("progressDays"),
                            completed = obj.optBoolean("completed"),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun encodeChallenges(challenges: List<ActiveChallenge>): String {
        val array = JSONArray()
        challenges.forEach { challenge ->
            array.put(
                JSONObject()
                    .put("id", challenge.id)
                    .put("templateId", challenge.templateId.stored)
                    .put("startedAtMs", challenge.startedAtMs)
                    .put("progressDays", challenge.progressDays)
                    .put("completed", challenge.completed)
                    .apply {
                        challenge.debtId?.let { put("debtId", it) }
                        challenge.debtName?.let { put("debtName", it) }
                        challenge.targetDays?.let { put("targetDays", it) }
                    },
            )
        }
        return array.toString()
    }

    fun createChallenge(
        templateId: ChallengeTemplateId,
        debtId: Long? = null,
        debtName: String? = null,
        now: Long = System.currentTimeMillis(),
    ): ActiveChallenge {
        val template = templateFor(templateId)
        return ActiveChallenge(
            id = UUID.randomUUID().toString(),
            templateId = templateId,
            debtId = debtId,
            debtName = debtName,
            startedAtMs = now,
            targetDays = template.targetDays,
        )
    }

    fun syncPaymentChallengeProgress(
        challenges: List<ActiveChallenge>,
        debtors: List<Debtor>,
        now: Long = System.currentTimeMillis(),
    ): List<ActiveChallenge> {
        val paymentsSince = countPaymentsSince(
            debtors = debtors,
            sinceMs = challenges
                .filter { it.templateId == ChallengeTemplateId.THREE_EARLY_PAYMENTS && !it.completed }
                .minOfOrNull { it.startedAtMs } ?: now,
        )
        return challenges.mapNotNull { challenge ->
            when {
                challenge.completed -> challenge
                challenge.templateId == ChallengeTemplateId.THREE_EARLY_PAYMENTS -> {
                    val progress = paymentsSince.coerceAtMost(3)
                    challenge.copy(
                        progressDays = progress,
                        completed = progress >= 3,
                    )
                }
                challenge.templateId == ChallengeTemplateId.CLOSE_DEBT_14D -> {
                    val targetDays = challenge.targetDays ?: 14
                    val deadlineMs = challenge.startedAtMs + targetDays * DAY_MS
                    val debt = challenge.debtId?.let { id -> debtors.find { it.id == id } }
                    when {
                        debt == null || debt.isPaid -> challenge.copy(
                            progressDays = targetDays,
                            completed = true,
                        )
                        now > deadlineMs -> null // failed — drop from active list
                        else -> {
                            val elapsedDays = ((now - challenge.startedAtMs) / DAY_MS)
                                .toInt()
                                .coerceIn(0, targetDays)
                            challenge.copy(progressDays = elapsedDays)
                        }
                    }
                }
                else -> challenge
            }
        }
    }

    private const val DAY_MS = 24L * 60L * 60L * 1000L

    fun computeGoalBadges(debtors: List<Debtor>): Set<String> {
        val iOwe = debtors.filter { it.debtType == DebtType.DEBTOR }
        val paymentCount = iOwe.sumOf { it.paymentHistory.size }
        val anyClosed = iOwe.any { it.isPaid }
        val progress = GoalPsychologyEngines.totalDebtProgressPercent(debtors)
        val allClosed = iOwe.isNotEmpty() && iOwe.all { it.isPaid }

        val badges = mutableSetOf<String>()
        if (paymentCount >= 1) badges.add(BADGE_FIRST_PAYMENT)
        if (anyClosed) badges.add(BADGE_SNOWBALL)
        if (progress >= 50) badges.add(BADGE_HERO)
        if (allClosed) badges.add(BADGE_FREEDOM)
        return badges
    }

    fun badgeLabel(badgeId: String): String = when (badgeId) {
        BADGE_FIRST_PAYMENT -> "Первый платёж"
        BADGE_SNOWBALL -> "Снежный ком"
        BADGE_HERO -> "Герой"
        BADGE_FREEDOM -> "Долги закрыты"
        else -> badgeId
    }

    fun luckCategoryLabel(category: String): String = when (category) {
        "UNEXPECTED_INCOME" -> "Неожиданный доход"
        "SAVINGS" -> "Экономия"
        "LUCK" -> "Удача"
        "GIFT" -> "Подарок"
        else -> "Другое"
    }

    private fun countPaymentsSince(debtors: List<Debtor>, sinceMs: Long): Int =
        debtors
            .filter { it.debtType == DebtType.DEBTOR }
            .sumOf { debtor -> debtor.paymentHistory.count { it.paidAt >= sinceMs } }
}

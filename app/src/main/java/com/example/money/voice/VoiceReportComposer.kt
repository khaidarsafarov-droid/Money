package com.example.money.voice

import android.content.Context
import com.example.money.R
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.util.DebtAdvisorTemplates
import com.example.money.domain.util.MoneyMath

object VoiceReportComposer {

    fun buildSnapshot(goals: List<SavingsGoal>): VoiceReportSnapshot {
        val activeGoals = goals.filter { !it.isCompleted && it.targetAmountCents > 0L }
        val completedGoals = goals.count { it.isCompleted }
        val totalCurrent = activeGoals.sumOf { it.currentAmountCents }
        val totalTarget = activeGoals.sumOf { it.targetAmountCents }
        val remaining = MoneyMath.remainingCents(totalTarget, totalCurrent)
        val fraction = MoneyMath.progressFraction(totalTarget, totalCurrent)
        val percent = (fraction * 100f).toInt().coerceIn(0, 100)

        return VoiceReportSnapshot(
            activeGoalCount = activeGoals.size,
            completedGoalCount = completedGoals,
            totalCurrentCents = totalCurrent,
            totalTargetCents = totalTarget,
            remainingCents = remaining,
            progressPercent = percent,
        )
    }

    fun composeMessage(
        context: Context,
        snapshot: VoiceReportSnapshot,
        language: VoiceLanguage = VoiceLanguage.RU,
    ): String {
        val localizedContext = VoiceLocaleContext.wrap(context, language)
        if (snapshot.activeGoalCount == 0 && snapshot.completedGoalCount == 0) {
            return localizedContext.getString(R.string.voice_report_no_goals)
        }
        if (snapshot.activeGoalCount == 0) {
            return localizedContext.getString(
                R.string.voice_report_all_complete,
                snapshot.completedGoalCount,
            )
        }

        val saved = VoiceReportSpeechFormatter.formatAmountForSpeech(
            snapshot.totalCurrentCents,
            language,
        )
        val target = VoiceReportSpeechFormatter.formatAmountForSpeech(
            snapshot.totalTargetCents,
            language,
        )
        val remaining = VoiceReportSpeechFormatter.formatAmountForSpeech(
            snapshot.remainingCents,
            language,
        )

        val goalsMessage = VoiceReportSpeechFormatter.prepareForSpeech(
            localizedContext.getString(
                R.string.voice_report_summary,
                snapshot.progressPercent,
                saved,
                target,
                remaining,
            ),
            language,
        )

        return goalsMessage
    }

    fun composeDebtAdvice(
        context: Context,
        debtors: List<Debtor>,
        language: VoiceLanguage = VoiceLanguage.RU,
    ): String? {
        val activeIOwe = debtors.filter {
            it.debtType == DebtType.DEBTOR && !it.isPaid && it.remainingAmountCents > 0L
        }
        if (activeIOwe.isEmpty()) return null

        val localizedContext = VoiceLocaleContext.wrap(context, language)
        val advice = DebtAdvisorTemplates.buildReply("motivation", debtors)
        return localizedContext.getString(R.string.voice_report_debt_advice, advice)
    }
}

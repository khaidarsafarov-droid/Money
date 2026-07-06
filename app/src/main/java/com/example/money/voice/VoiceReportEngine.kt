package com.example.money.voice

import android.content.Context
import com.example.money.MoneyApplication
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.domain.model.DebtType
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.GoalRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale

enum class VoiceReportOutcome {
    Spoken,
    SkippedDisabled,
    SkippedAppOpenedToday,
    SkippedAlreadyReported,
    SkippedSilent,
    SkippedDnd,
    SkippedNoGoals,
    Failed,
}

class VoiceReportEngine(
    private val context: Context,
    private val preferencesStore: MoneyPreferencesStore,
    private val goalRepository: GoalRepository,
    private val debtorRepository: DebtorRepository,
    private val speaker: VoiceReportSpeaker,
) {
    constructor(app: MoneyApplication) : this(
        context = app,
        preferencesStore = app.preferencesStore,
        goalRepository = app.goalRepository,
        debtorRepository = app.debtorRepository,
        speaker = VoiceReportSpeaker(
            app,
            VoiceReportAudioPolicy(app),
        ),
    )

    suspend fun runScheduledReport(): VoiceReportOutcome {
        val settings = preferencesStore.voiceReportSettings.first()
        if (!settings.enabled) return VoiceReportOutcome.SkippedDisabled

        val today = todayIso()
        if (settings.lastAppOpenDate == today) {
            return VoiceReportOutcome.SkippedAppOpenedToday
        }
        if (settings.lastVoiceReportDate == today) {
            return VoiceReportOutcome.SkippedAlreadyReported
        }

        return deliverReport(settings, markReportDate = true)
    }

    suspend fun runTestReport(): VoiceReportSpeaker.SpeakResult {
        val settings = preferencesStore.voiceReportSettings.first()
        val testConfig = settings.playbackConfig.copy(forceInSilentMode = true)
        return speakCurrentSnapshot(settings, testConfig)
    }

    private suspend fun speakCurrentSnapshot(
        settings: VoiceReportSettings,
        playbackConfig: VoicePlaybackConfig = settings.playbackConfig,
    ): VoiceReportSpeaker.SpeakResult {
        val goals = goalRepository.observeAllGoals().first()
        val debtors = debtorRepository.getAllDebtors().first()
        val snapshot = VoiceReportComposer.buildSnapshot(goals)
        var message = VoiceReportComposer.composeMessage(
            context = context,
            snapshot = snapshot,
            language = settings.language,
        )
        val hasActiveIOwe = debtors.any {
            it.debtType == DebtType.DEBTOR && !it.isPaid && it.remainingAmountCents > 0L
        }
        if (hasActiveIOwe) {
            VoiceReportComposer.composeDebtAdvice(context, debtors, settings.language)?.let { advice ->
                message = "$message $advice"
            }
        }
        return speaker.speak(message, playbackConfig)
    }

    private suspend fun deliverReport(
        settings: VoiceReportSettings,
        markReportDate: Boolean,
    ): VoiceReportOutcome {
        return when (speakCurrentSnapshot(settings)) {
            VoiceReportSpeaker.SpeakResult.Success -> {
                if (markReportDate) {
                    preferencesStore.markVoiceReportDelivered(todayIso())
                }
                VoiceReportOutcome.Spoken
            }
            VoiceReportSpeaker.SpeakResult.BlockedSilent -> VoiceReportOutcome.SkippedSilent
            VoiceReportSpeaker.SpeakResult.BlockedDnd -> VoiceReportOutcome.SkippedDnd
            VoiceReportSpeaker.SpeakResult.AudioFocusDenied,
            VoiceReportSpeaker.SpeakResult.TtsUnavailable,
            VoiceReportSpeaker.SpeakResult.TtsError,
            -> VoiceReportOutcome.Failed
        }
    }

    private fun todayIso(): String {
        val cal = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }
}

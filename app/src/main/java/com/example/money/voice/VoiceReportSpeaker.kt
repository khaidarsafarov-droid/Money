package com.example.money.voice

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class VoiceReportSpeaker(
    context: Context,
    private val audioPolicy: VoiceReportAudioPolicy,
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun speak(
        text: String,
        config: VoicePlaybackConfig,
    ): SpeakResult = withContext(Dispatchers.Main) {
        when (audioPolicy.blockedReason(config.forceInSilentMode)) {
            VoiceAudioBlockReason.SILENT_MODE -> return@withContext SpeakResult.BlockedSilent
            VoiceAudioBlockReason.DO_NOT_DISTURB -> return@withContext SpeakResult.BlockedDnd
            null -> Unit
        }

        val speechText = VoiceReportSpeechFormatter.prepareForSpeech(text, config.language)
        if (speechText.isBlank()) {
            return@withContext SpeakResult.TtsError
        }

        suspendCancellableCoroutine { continuation ->
            val resumed = AtomicBoolean(false)
            fun finish(result: SpeakResult) {
                if (resumed.compareAndSet(false, true)) {
                    continuation.resume(result)
                }
            }

            var focusRequest: android.media.AudioFocusRequest? = null
            var tts: TextToSpeech? = null
            var volumeSnapshot: StreamVolumeSnapshot? = null
            var timeoutRunnable: Runnable? = null

            fun cleanup(engine: TextToSpeech?) {
                timeoutRunnable?.let { mainHandler.removeCallbacks(it) }
                timeoutRunnable = null
                engine?.stop()
                engine?.shutdown()
                audioPolicy.restoreVolume(volumeSnapshot)
                audioPolicy.abandonSpeechFocus(focusRequest)
            }

            fun scheduleCompletionTimeout(engine: TextToSpeech) {
                if (timeoutRunnable != null) return
                val runnable = Runnable {
                    if (resumed.get()) return@Runnable
                    cleanup(engine)
                    finish(SpeakResult.TtsError)
                }
                timeoutRunnable = runnable
                mainHandler.postDelayed(runnable, SPEAK_TIMEOUT_MS)
            }

            continuation.invokeOnCancellation {
                cleanup(tts)
            }

            audioPolicy.prepareAudioRoute()
            volumeSnapshot = audioPolicy.ensureAudibleVolume(config.forceInSilentMode)

            focusRequest = audioPolicy.requestSpeechFocus { focusChange ->
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                    focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                ) {
                    tts?.stop()
                }
            }

            tts = TextToSpeech(appContext) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    cleanup(null)
                    finish(SpeakResult.TtsUnavailable)
                    return@TextToSpeech
                }

                val engine = tts ?: run {
                    cleanup(null)
                    finish(SpeakResult.TtsUnavailable)
                    return@TextToSpeech
                }

                configureEngine(engine, config.language.toLocale(), config.gender)
                engine.setSpeechRate(0.95f)

                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit

                    override fun onDone(utteranceId: String?) {
                        if (utteranceId != UTTERANCE_ID) return
                        cleanup(engine)
                        finish(SpeakResult.Success)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (utteranceId != UTTERANCE_ID) return
                        cleanup(engine)
                        finish(SpeakResult.TtsError)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (utteranceId != UTTERANCE_ID) return
                        cleanup(engine)
                        finish(SpeakResult.TtsError)
                    }
                })

                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, audioPolicy.playbackStreamType)
                }
                val queued = engine.speak(
                    speechText,
                    TextToSpeech.QUEUE_FLUSH,
                    params,
                    UTTERANCE_ID,
                )
                if (queued == TextToSpeech.ERROR) {
                    cleanup(engine)
                    finish(SpeakResult.TtsError)
                    return@TextToSpeech
                }

                scheduleCompletionTimeout(engine)
            }
        }
    }

    private fun configureEngine(
        engine: TextToSpeech,
        preferredLocale: Locale,
        gender: VoiceGender,
    ) {
        applyLocale(engine, preferredLocale)
        applyVoice(engine, preferredLocale, gender)
    }

    private fun applyLocale(engine: TextToSpeech, preferredLocale: Locale) {
        when (engine.setLanguage(preferredLocale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE,
            -> return
        }

        val fallbacks = listOf(
            VoiceLanguage.EN.toLocale(),
            Locale.getDefault(),
            Locale.US,
            preferredLocale,
        ).distinctBy { "${it.language}-${it.country}" }

        for (locale in fallbacks) {
            val result = engine.setLanguage(locale)
            if (result >= TextToSpeech.LANG_AVAILABLE) {
                return
            }
        }
    }

    private fun applyVoice(engine: TextToSpeech, locale: Locale, gender: VoiceGender) {
        val systemVoices = engine.voices ?: return
        if (systemVoices.isEmpty()) return

        val candidates = systemVoices.map { voice ->
            VoiceSelector.VoiceCandidate(
                name = voice.name,
                localeLanguage = voice.locale?.language,
                features = voice.features ?: emptySet(),
            )
        }
        val picked = VoiceSelector.pickBest(candidates, locale, gender) ?: return
        val selected = systemVoices.firstOrNull { it.name == picked.name } ?: return
        engine.voice = selected
    }

    enum class SpeakResult {
        Success,
        BlockedSilent,
        BlockedDnd,
        AudioFocusDenied,
        TtsUnavailable,
        TtsError,
    }

    private companion object {
        const val UTTERANCE_ID = "money_voice_report"
        const val SPEAK_TIMEOUT_MS = 45_000L
    }
}

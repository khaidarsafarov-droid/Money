package com.example.money.voice

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService

enum class VoiceAudioBlockReason {
    SILENT_MODE,
    DO_NOT_DISTURB,
}

data class StreamVolumeSnapshot(
    val streamType: Int,
    val previousVolume: Int,
)

class VoiceReportAudioPolicy(private val context: Context) {

    private val audioManager = context.getSystemService<AudioManager>()
    private val notificationManager = context.getSystemService<NotificationManager>()

    val playbackStreamType: Int = AudioManager.STREAM_ALARM

    fun blockedReason(forceInSilentMode: Boolean): VoiceAudioBlockReason? {
        if (forceInSilentMode) return null

        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return VoiceAudioBlockReason.SILENT_MODE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val filter = notificationManager?.currentInterruptionFilter
            if (filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                return VoiceAudioBlockReason.DO_NOT_DISTURB
            }
        }
        return null
    }

    fun prepareAudioRoute() {
        val manager = audioManager ?: return
        if (!isBluetoothAudioConnected()) return
        manager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetooth = manager.availableCommunicationDevices.firstOrNull { device ->
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
            if (bluetooth != null) {
                manager.setCommunicationDevice(bluetooth)
            } else {
                manager.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            manager.isSpeakerphoneOn = false
        }
    }

    fun ensureAudibleVolume(forceInSilentMode: Boolean): StreamVolumeSnapshot? {
        if (!forceInSilentMode) return null
        val manager = audioManager ?: return null

        val current = manager.getStreamVolume(playbackStreamType)
        val max = manager.getStreamMaxVolume(playbackStreamType)
        val target = (max * TARGET_VOLUME_RATIO).toInt().coerceIn(1, max)

        if (current >= target) return null

        manager.setStreamVolume(playbackStreamType, target, 0)
        return StreamVolumeSnapshot(playbackStreamType, current)
    }

    fun restoreVolume(snapshot: StreamVolumeSnapshot?) {
        if (snapshot == null) return
        audioManager?.setStreamVolume(snapshot.streamType, snapshot.previousVolume, 0)
    }

    fun requestSpeechFocus(onFocusChange: (Int) -> Unit): AudioFocusRequest? {
        val manager = audioManager ?: return null
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setLegacyStreamType(playbackStreamType)
            .build()

        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attributes)
            .setAcceptsDelayedFocusGain(false)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(onFocusChange)
            .build()

        val granted = manager.requestAudioFocus(request)
        return when (granted) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED,
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED,
            -> request
            else -> request
        }
    }

    fun abandonSpeechFocus(request: AudioFocusRequest?) {
        if (request == null) return
        audioManager?.abandonAudioFocusRequest(request)
    }

    @Suppress("DEPRECATION")
    fun isBluetoothAudioConnected(): Boolean {
        val manager = audioManager ?: return false
        return manager.isBluetoothA2dpOn || manager.isBluetoothScoOn
    }

    private companion object {
        const val TARGET_VOLUME_RATIO = 0.75f
    }
}

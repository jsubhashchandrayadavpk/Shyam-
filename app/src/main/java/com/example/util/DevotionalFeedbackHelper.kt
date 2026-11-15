package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class DevotionalFeedbackHelper(private val context: Context) {

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun triggerTapFeedback(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (vibrationEnabled) {
            vibratePulse(durationMs = 24L, amplitude = 90)
        }
        if (soundEnabled) {
            audioScope.launch {
                playSoftChime(frequencyHz = 660.0, durationMs = 70)
            }
        }
    }

    fun triggerMalaCompletedFeedback(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        if (vibrationEnabled) {
            vibrateDoublePulse()
        }
        if (soundEnabled) {
            audioScope.launch {
                playSoftChime(frequencyHz = 528.0, durationMs = 110)
                playSoftChime(frequencyHz = 660.0, durationMs = 110)
                playSoftChime(frequencyHz = 792.0, durationMs = 200)
            }
        }
    }

    private fun getVibrator(): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun vibratePulse(durationMs: Long, amplitude: Int) {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (_: Throwable) {
            // Safe no-op on test/restricted devices
        }
    }

    private fun vibrateDoublePulse() {
        val vibrator = getVibrator() ?: return
        if (!vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 60L, 70L, 110L)
                val amplitudes = intArrayOf(0, 140, 0, 200)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 60L, 70L, 110L), -1)
            }
        } catch (_: Throwable) {
            // Safe no-op on test/restricted devices
        }
    }

    private fun playSoftChime(frequencyHz: Double, durationMs: Int) {
        var track: AudioTrack? = null
        try {
            val sampleRate = 22050
            val numSamples = (sampleRate * durationMs) / 1000
            if (numSamples <= 0) return
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-t * 18.0) * (1.0 - exp(-t * 250.0))
                val wave = 0.7 * sin(2.0 * PI * frequencyHz * t) +
                    0.3 * sin(2.0 * PI * (frequencyHz * 2.0) * t)
                samples[i] = (wave * envelope * 0.25 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val bufferSizeBytes = numSamples * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(maxOf(bufferSizeBytes, minBuf))
                .build()

            track.write(samples, 0, numSamples)
            track.play()
            Thread.sleep(durationMs.toLong() + 15L)
        } catch (_: Throwable) {
            // Safe no-op in Robolectric or muted audio environments
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }
}

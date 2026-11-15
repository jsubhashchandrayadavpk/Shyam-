package com.example.reminder

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

class HindiVoiceSpeaker(private val context: Context) {

    fun speakHindi(text: String, onFinished: (() -> Unit)? = null) {
        var tts: TextToSpeech? = null
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status != TextToSpeech.SUCCESS) {
                    try {
                        tts?.shutdown()
                    } catch (_: Throwable) {
                    }
                    onFinished?.invoke()
                    return@TextToSpeech
                }

                try {
                    val engine = tts ?: return@TextToSpeech
                    val hiLocale = Locale.Builder().setLanguage("hi").setRegion("IN").build()
                    val langResult = engine.setLanguage(hiLocale)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        engine.language = Locale.getDefault()
                    }
                    engine.setSpeechRate(0.92f)
                    engine.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    val utteranceId = UUID.randomUUID().toString()
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) {
                            try {
                                engine.shutdown()
                            } catch (_: Throwable) {
                            }
                            onFinished?.invoke()
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(id: String?) {
                            try {
                                engine.shutdown()
                            } catch (_: Throwable) {
                            }
                            onFinished?.invoke()
                        }
                    })
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                } catch (_: Throwable) {
                    try {
                        tts?.shutdown()
                    } catch (_: Throwable) {
                    }
                    onFinished?.invoke()
                }
            }
        } catch (_: Throwable) {
            onFinished?.invoke()
        }
    }
}

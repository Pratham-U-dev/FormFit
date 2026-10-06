package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

object TtsCoach : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isTtsEnabled: Boolean = true
    private var lastSpokenText: String = ""
    private var lastSpokenTimestamp: Long = 0L

    fun init(context: Context) {
        if (tts == null) {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                Log.e("TtsCoach", "Failed to initialize TextToSpeech", e)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
                isInitialized = true
                Log.d("TtsCoach", "TextToSpeech successfully initialized")
            } catch (e: Exception) {
                Log.e("TtsCoach", "Error configuring TTS language", e)
            }
        } else {
            Log.e("TtsCoach", "TextToSpeech init status error: $status")
        }
    }

    fun speak(text: String, isUrgent: Boolean = false, minIntervalMs: Long = 2800L) {
        if (!isTtsEnabled || !isInitialized || text.isBlank()) return

        val now = System.currentTimeMillis()
        val cleanText = text.trim()

        // Avoid repeating the exact identical advice within 4 seconds unless flagged as urgent
        if (!isUrgent && cleanText.equals(lastSpokenText, ignoreCase = true) && (now - lastSpokenTimestamp) < 4000L) {
            return
        }

        // Throttle rapid consecutive speech to avoid auditory confusion during movement
        if (!isUrgent && (now - lastSpokenTimestamp) < minIntervalMs) {
            return
        }

        lastSpokenText = cleanText
        lastSpokenTimestamp = now

        try {
            val queueMode = if (isUrgent) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(cleanText, queueMode, null, "TTS_ALERT_${now}")
        } catch (e: Exception) {
            Log.e("TtsCoach", "TTS speak failed", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TtsCoach", "TTS stop failed", e)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e("TtsCoach", "TTS shutdown failed", e)
        }
    }
}

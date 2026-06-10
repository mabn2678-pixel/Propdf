package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.GERMAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TtsManager", "German language is not supported on this device.")
                } else {
                    isInitialized = true
                }
            } else {
                Log.e("TtsManager", "TextToSpeech initialization failed.")
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f, repeatTimes: Int = 1) {
        if (!isInitialized) {
            Log.e("TtsManager", "TTS not initialized yet")
            return
        }
        
        // Clean text formatting: Der Hund -> speak differently from plain text
        val cleanedText = text.trim()
        
        tts?.apply {
            setSpeechRate(speed)
            setPitch(1.0f)
            
            // Repeat implementation
            for (i in 0 until repeatTimes) {
                speak(cleanedText, TextToSpeech.QUEUE_ADD, null, "${System.currentTimeMillis()}_$i")
            }
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

package com.careerops.mobile.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale

object VoiceDictationManager {
    fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer")
        }
    }

    fun parseResult(
        resultCode: Int,
        data: Intent?
    ): String? {
        if (resultCode != Activity.RESULT_OK || data == null) return null
        val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (text.isNullOrBlank()) return null
        return text
    }
}

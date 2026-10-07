package org.ivanivorontsov.boec.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Russian voice cues. Works offline if a Russian TTS voice is installed (Google/RuStore TTS etc.). */
class Speaker(ctx: Context, private val enabled: () -> Boolean) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(ctx.applicationContext, this)
    var ready = false; private set
    var missingVoice = false; private set

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val r = tts.setLanguage(Locale("ru", "RU"))
        missingVoice = r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED
        ready = !missingVoice
        tts.setSpeechRate(1.05f)
    }

    fun say(text: String, flush: Boolean = true) {
        if (!ready || !enabled()) return
        tts.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun shutdown() { runCatching { tts.stop(); tts.shutdown() } }
}

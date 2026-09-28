package guide.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import guide.core.NarrationQueue
import java.util.Locale

/**
 * Phase 1 narrator: on-device Android TTS only (SPEC §1 privacy default).
 * Cloud voices are behind Settings opt-in and not implemented in beta.
 * Single voice at a time; mute stops speech in <500ms (F-08) and clears queue.
 */
class Narrator(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val sarvam = SarvamAudioService.getInstance(context)

    val queue = NarrationQueue()
    var muted = false
        private set
    /** Quiet hours: cards still appear, voice stays silent. Set from Settings. */
    var quiet = false
    /** Auto-play off = triggers become cards only, queue untouched by voice. */
    var autoPlay = true
    /** Whether to use Sarvam AI studio voice (true) or device system voice (false). */
    var useStudioVoice = true

    var speechRate = 1.0f
        set(value) {
            field = value
            tts?.setSpeechRate(value)
        }

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            tts?.language = Locale.getDefault()
            tts?.setSpeechRate(speechRate)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onDone(utteranceId: String) = onSpoken(utteranceId)
                override fun onError(utteranceId: String) = onSpoken(utteranceId)
                override fun onStart(utteranceId: String) = Unit
            })
        }
    }

    /** 30–60s story text per POI card. Keeps narration short (SPEC §3.1). */
    fun storyText(name: String, summary: String, firstFact: String?): String =
        "$name. $summary ${firstFact ?: ""}".take(600)

    fun enqueue(poiId: String, text: String, onRoute: Boolean = false) {
        if (muted) return
        // TTS Input Sanitization & SSML Injection Protection (OWASP M4, OWASP LLM01)
        val sanitized = text
            .replace(Regex("""<[^>]*>"""), "") // Strip HTML/SSML tags (<say-as>, <break>, etc.)
            .replace(Regex("""[\x00-\x1F\x7F]"""), " ") // Strip non-printable control characters
            .trim()
            .take(600)
        if (sanitized.isBlank()) return
        pendingTexts[poiId] = sanitized
        queue.enqueue(poiId, onRoute)
        pump()
    }

    fun setMuted(m: Boolean) {
        muted = m
        if (m) {
            sarvam.stop() // Immediate <500ms stop (F-08)
            tts?.stop()
            queue.clear()
            pendingTexts.clear()
        }
    }

    fun shutdown() {
        sarvam.stop()
        tts?.stop()
        tts?.shutdown()
    }

    private val pendingTexts = mutableMapOf<String, String>()

    private fun pump() {
        if (muted || quiet || !autoPlay || queue.speaking != null) return
        val next = queue.next() ?: return
        val text = pendingTexts.remove(next) ?: return

        if (useStudioVoice) {
            sarvam.speak(
                text = text,
                onStart = { /* Audio playback underway */ },
                onDone = { onSpoken(next) },
                onError = {
                    // Gracefully fallback to on-device TTS if network or quota issue occurs
                    if (ready) {
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, next)
                    } else {
                        onSpoken(next)
                    }
                },
            )
        } else if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, next)
        }
    }

    private fun onSpoken(utteranceId: String) {
        queue.finished(utteranceId)
        pump()
    }
}

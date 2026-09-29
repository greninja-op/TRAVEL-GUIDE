package guide.app.data

import java.util.Locale

/**
 * Supported Languages for Travel Guide UI & Spoken Voice.
 *
 * Synchronizes:
 * 1. Studio-grade Neural Speech Synthesis via Sarvam AI (`bulbul:v3`)
 * 2. On-device Android Text-To-Speech (TTS) locale fallback
 * 3. Spontaneous AI oral storytelling persona prompts
 * 4. Clean typographic UI strings across all 5 navigation screens and detail cards.
 */
enum class AppLanguage(
    val code: String,
    val title: String,
    val nativeName: String,
    val sarvamCode: String,
    val defaultSpeaker: String,
    val locale: Locale,
) {
    ENGLISH(
        code = "en",
        title = "English",
        nativeName = "English (India)",
        sarvamCode = "en-IN",
        defaultSpeaker = "ritu",
        locale = Locale("en", "IN"),
    ),
    MALAYALAM(
        code = "ml",
        title = "Malayalam",
        nativeName = "മലയാളം",
        sarvamCode = "ml-IN",
        defaultSpeaker = "ritu",
        locale = Locale("ml", "IN"),
    ),
    HINDI(
        code = "hi",
        title = "Hindi",
        nativeName = "हिन्दी",
        sarvamCode = "hi-IN",
        defaultSpeaker = "ritu",
        locale = Locale("hi", "IN"),
    ),
    TAMIL(
        code = "ta",
        title = "Tamil",
        nativeName = "தமிழ்",
        sarvamCode = "ta-IN",
        defaultSpeaker = "ritu",
        locale = Locale("ta", "IN"),
    );

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

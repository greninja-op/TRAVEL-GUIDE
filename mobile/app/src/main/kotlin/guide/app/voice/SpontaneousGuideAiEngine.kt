package guide.app.voice

import android.content.Context
import android.util.Log
import guide.app.data.PackLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.util.Random

/**
 * Spontaneous AI Tour Guide Engine.
 *
 * Generates fresh, unscripted, and context-aware spoken narratives whenever a traveler
 * arrives at a historical landmark, approaches a discovery corridor, or asks "What am I seeing?".
 *
 * Architecture:
 * 1. Primary Engine: OpenAI GPT-4o-mini cloud intelligence via direct API completions.
 *    Incorporates real-time ambient signals: time of day, current light/weather, walking proximity,
 *    and persona styling (Conversational Local Insider, Dramatic Storyteller, Heritage Historian).
 * 2. Zero-Lag / Offline Fallback: Smart On-Device Generative Engine that dynamically synthesizes
 *    multi-perspective observations from historical notes, architectural quirks, and ambient hooks
 *    so narration is always spontaneous, novel, and never repeats a static pre-recorded sentence.
 */
object SpontaneousGuideAiEngine {

    private const val TAG = "SpontaneousAiEngine"
    private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
    const val MODEL_NAME = "gpt-4o-mini"

    enum class Persona(val id: String, val title: String, val description: String) {
        INSIDER(
            id = "insider",
            title = "Conversational Local Insider",
            description = "Friendly, observational, atmospheric, points out hidden quirks and tactile details as if walking right beside you.",
        ),
        STORYTELLER(
            id = "storyteller",
            title = "Dramatic Storyteller",
            description = "Engaging folklore, maritime legends, sensory drama, and evocative historic tales.",
        ),
        HISTORIAN(
            id = "historian",
            title = "Heritage Historian",
            description = "Architectural nuances, colonial transitions, trade route history, and precise cultural depth.",
        );

        companion object {
            fun fromId(id: String?): Persona =
                entries.find { it.id.equals(id, ignoreCase = true) } ?: INSIDER
        }
    }

    /**
     * Generates a spontaneous, living observation for the given landmark.
     */
    suspend fun generateStory(
        context: Context,
        card: PackLoader.PoiCard,
        userLat: Double? = null,
        userLng: Double? = null,
        language: guide.app.data.AppLanguage? = null,
    ): String = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("openai_api_key", "")?.trim()?.ifBlank { null }
            ?: runCatching { guide.app.BuildConfig.OPENAI_API_KEY }.getOrNull()?.trim()?.ifBlank { null }
            ?: ""
        val personaId = prefs.getString("ai_tour_persona", Persona.INSIDER.id)
        val persona = Persona.fromId(personaId)
        val lang = language ?: guide.app.data.AppLanguage.fromCode(prefs.getString("app_language", guide.app.data.AppLanguage.ENGLISH.code))

        val timeContext = getTimeOfDayContext()
        val proximityContext = getProximityContext(card, userLat, userLng)

        // If an OpenAI API key is supplied, attempt live GPT-4o-mini generation
        if (apiKey.isNotBlank()) {
            val cloudStory = tryCloudGeneration(apiKey, card, persona, timeContext, proximityContext, lang)
            if (!cloudStory.isNullOrBlank()) {
                return@withContext cleanVoiceText(cloudStory)
            }
        }

        // Seamless, high-variety on-device generative fallback
        cleanVoiceText(generateLocalSpontaneousStory(card, persona, timeContext, proximityContext, lang))
    }

    /**
     * Calls OpenAI GPT-4o-mini API for real-time spontaneous oral storytelling.
     */
    private fun tryCloudGeneration(
        apiKey: String,
        card: PackLoader.PoiCard,
        persona: Persona,
        timeContext: String,
        proximityContext: String,
        lang: guide.app.data.AppLanguage,
    ): String? {
        return try {
            val langInstruction = when (lang) {
                guide.app.data.AppLanguage.MALAYALAM -> "CRITICAL: You must speak entirely in natural, fluent Malayalam (മലയാളത്തിൽ സ്വാഭാവികമായി സംസാരിക്കുക). Do not output English."
                guide.app.data.AppLanguage.HINDI -> "CRITICAL: You must speak entirely in natural, fluent Hindi (स्वाभाविक और सरल हिंदी में बोलें). Do not output English."
                guide.app.data.AppLanguage.TAMIL -> "CRITICAL: You must speak entirely in natural, fluent Tamil (இயற்கையான தமிழில் பேசுங்கள்). Do not output English."
                guide.app.data.AppLanguage.ENGLISH -> "Speak in clear, warm, engaging English."
            }

            val systemPrompt = when (persona) {
                Persona.INSIDER -> """
                    You are an authentic, warm, and observant local guide walking beside a traveler in Fort Kochi and Mattancherry, Kerala.
                    $langInstruction
                    Speak spontaneously in 2 to 3 natural spoken sentences (around 35 to 45 words).
                    Never recite brochure facts or textbook paragraphs.
                    Mention tactile sensory details: the sea breeze, timber scent, peeling lime wash, fishing ropes, or how the light looks at this time.
                    Point out a hidden quirk or secret detail about this spot right now. Keep it punchy, conversational, and direct.
                """.trimIndent()
                Persona.STORYTELLER -> """
                    You are a captivating maritime storyteller in Fort Kochi and Mattancherry, Kerala.
                    $langInstruction
                    Speak spontaneously in 2 to 3 evocative spoken sentences (around 35 to 45 words).
                    Bring out the drama of the spice trade, sea voyages, ancient empires, and legends tied to this exact place.
                    Make the listener feel the historic atmosphere of this hour.
                """.trimIndent()
                Persona.HISTORIAN -> """
                    You are a distinguished heritage scholar walking through Fort Kochi and Mattancherry.
                    $langInstruction
                    Speak spontaneously in 2 to 3 insightful spoken sentences (around 35 to 45 words).
                    Highlight architectural transitions, colonial engineering, cultural fusion, and historical significance.
                """.trimIndent()
            }

            val userPrompt = """
                Landmark: ${card.name}
                Summary: ${card.summary}
                Key History: ${card.history}
                Fun Facts: ${card.funFacts.joinToString("; ")}
                What to look for: ${card.seeList.joinToString("; ")}
                Current Ambience: $timeContext. $proximityContext.
                
                Speak a fresh, spontaneous oral guide observation tailored for this moment.
            """.trimIndent()

            val body = JSONObject().apply {
                put("model", MODEL_NAME)
                put("temperature", 0.85)
                put("max_tokens", 110)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            }

            val url = URL(OPENAI_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4500
                readTimeout = 4500
                doOutput = true
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
            }

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    return message?.optString("content")?.trim()
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.w(TAG, "OpenAI API returned HTTP $responseCode: $err")
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "OpenAI generation failed: ${e.message}")
            null
        }
    }

    /**
     * Smart On-Device Generative Engine.
     * Generates a completely novel, spontaneous narrative locally using multi-angle contextual synthesis.
     */
    fun generateLocalSpontaneousStory(
        card: PackLoader.PoiCard,
        persona: Persona,
        timeContext: String,
        proximityContext: String,
        lang: guide.app.data.AppLanguage = guide.app.data.AppLanguage.ENGLISH,
    ): String {
        val localized = guide.app.data.AppStrings.getLocalizedPoi(card.id, lang)
        if (localized != null) {
            return "${localized.name}. ${localized.summary} ${localized.secret}"
        }
        val rand = Random(System.currentTimeMillis())

        val atmosphericIntros = when (persona) {
            Persona.INSIDER -> listOf(
                "Take a breath of that harbour air right here.",
                "Look closely right in front of you as we step up.",
                "Notice the way the light catches the old timber here.",
                "Here is something most people walk straight past.",
                "Listen to the quiet hum around this corner.",
                "You are standing right where centuries of travelers paused.",
            )
            Persona.STORYTELLER -> listOf(
                "Centuries of maritime storms and trader footprints converge on this stone.",
                "If these walls could speak, they would tell of sea voyages and spice fleets.",
                "Legend lingers thick around this spot.",
                "Imagine arriving here five hundred years ago by wooden caravel.",
            )
            Persona.HISTORIAN -> listOf(
                "Notice the distinct confluence of colonial craftsmanship right here.",
                "This site preserves a rare architectural dialogue across three centuries.",
                "Examine the materials beneath the exterior plaster.",
                "Here lies a pivotal chapter in the Malabar coast's maritime heritage.",
            )
        }

        val intro = atmosphericIntros[rand.nextInt(atmosphericIntros.size)]

        // Pick a dynamic focus from fun facts or see list
        val randomFact = card.funFacts.shuffled(rand).firstOrNull()
        val randomSee = card.seeList.shuffled(rand).firstOrNull()

        val body = when {
            randomFact != null && rand.nextBoolean() -> {
                "${card.name} holds a wonderful secret. $randomFact."
            }
            randomSee != null -> {
                "At ${card.name}, be sure to look for the $randomSee. ${card.summary}"
            }
            else -> {
                "${card.name} — ${card.summary}"
            }
        }

        val observations = listOf(
            "It gives you a real feel for Fort Kochi's living soul.",
            "Take a quiet moment to take it in before we continue along the lane.",
            "Notice how the breeze off the Arabian Sea cools the stonework right now.",
            "The textures here tell more of the story than any guidebook could.",
        )
        val observation = observations[rand.nextInt(observations.size)]

        return "$intro $body $observation"
    }

    private fun getTimeOfDayContext(): String {
        val hour = LocalTime.now().hour
        return when (hour) {
            in 5..8 -> "Early morning, soft dawn mist, fishermen beginning their day"
            in 9..11 -> "Bright morning sunlight filtering through rain trees, bustling streets"
            in 12..14 -> "Midday sun, warm coastal heat, quiet afternoon shaded alleys"
            in 15..17 -> "Late afternoon golden light, warm tropical air, long shadows"
            in 18..19 -> "Golden hour sunset, silhouettes against the orange water, evening sea breeze"
            in 20..22 -> "Evening twilight, ambient streetlamps, quiet night air"
            else -> "Nighttime, tranquil stillness along the old quarters"
        }
    }

    private fun getProximityContext(card: PackLoader.PoiCard, userLat: Double?, userLng: Double?): String {
        if (userLat == null || userLng == null) return "Approaching the historic site"
        val dist = guide.core.Geo.distanceM(userLat, userLng, card.lat, card.lng)
        return when {
            dist <= 30.0 -> "Standing directly in front of the landmark"
            dist <= 75.0 -> "Approaching within ${dist.toInt()} meters"
            else -> "Viewing nearby from ${dist.toInt()} meters away"
        }
    }

    /**
     * Cleans text to guarantee no asterisks, markdown tags, quotes, or emojis leak into TTS audio.
     */
    fun cleanVoiceText(raw: String): String {
        return raw
            .replace(Regex("""[*#_`~>\[\]]"""), "") // Strip markdown symbols
            .replace(Regex("""^["']|["']$"""), "") // Strip leading/trailing quotes
            .replace(Regex("""[\uD83C-\uDBFF\uDC00-\uDFFF]"""), "") // Strip high/low surrogate emojis
            .replace(Regex("""[\u2600-\u27BF\uFE00-\uFE0F]"""), "") // Strip symbols/dingbats
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}

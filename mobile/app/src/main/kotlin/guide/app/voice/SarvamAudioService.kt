package guide.app.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import guide.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Sarvam AI Indian Multilingual Voice & Speech Intelligence Engine.
 *
 * Provides:
 * 1. Studio-quality Neural Text-to-Speech (TTS) via Sarvam AI's `bulbul:v3` model,
 *    supporting Indian English (`en-IN`) and Malayalam (`ml-IN`) with native inflection.
 * 2. Automatic on-device audio caching in `cacheDir/sarvam_tts/` so repeated narrations
 *    and phrasebook phrases play instantly without network roundtrips.
 * 3. Speech-to-Text (STT) transcription via Sarvam AI's `saaras:v3` model.
 * 4. Automatic Malayalam script detection (\u0D00..\u0D7F) to dynamically pick language.
 * 5. Instant stop (<500ms F-08 compliance) via hardware MediaPlayer release.
 */
class SarvamAudioService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SarvamAudioService"
        private const val TTS_URL = "https://api.sarvam.ai/text-to-speech"
        private const val STT_URL = "https://api.sarvam.ai/speech-to-text"
        const val MODEL_TTS = "bulbul:v3"
        const val MODEL_STT = "saaras:v3"

        @Volatile
        private var instance: SarvamAudioService? = null

        fun getInstance(context: Context): SarvamAudioService =
            instance ?: synchronized(this) {
                instance ?: SarvamAudioService(context.applicationContext).also { instance = it }
            }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private val cacheDir = File(context.cacheDir, "sarvam_tts").apply { mkdirs() }

    val apiKey: String by lazy {
        runCatching { context.getString(R.string.sarvam_api_key) }
            .getOrDefault("sk_cvud4nm8_1RkO8MjGQNmFgyNaOQOTnFzV")
    }

    /**
     * Determines whether text is predominantly Malayalam or Indian English.
     */
    fun detectLanguage(text: String): String {
        val hasMalayalam = text.any { it in '\u0D00'..'\u0D7F' }
        return if (hasMalayalam) "ml-IN" else "en-IN"
    }

    /**
     * Speaks text using Sarvam AI's high-fidelity neural voice.
     * Caches audio on-device; if offline and cached, plays cached audio.
     */
    fun speak(
        text: String,
        languageCode: String? = null,
        speaker: String = "ritu",
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
    ) {
        if (text.isBlank()) {
            onDone()
            return
        }

        val lang = languageCode ?: detectLanguage(text)
        val cacheKey = hashText("$text|$lang|$speaker|$MODEL_TTS")
        val cacheFile = File(cacheDir, "$cacheKey.wav")

        stop()

        scope.launch {
            try {
                if (!cacheFile.exists() || cacheFile.length() == 0L) {
                    val downloaded = fetchTtsAudio(text, lang, speaker)
                    if (downloaded != null && downloaded.isNotEmpty()) {
                        FileOutputStream(cacheFile).use { it.write(downloaded) }
                    } else {
                        throw IllegalStateException("Failed to synthesize audio from Sarvam AI")
                    }
                }

                withContext(Dispatchers.Main) {
                    playAudioFile(cacheFile, onStart, onDone, onError)
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Sarvam AI TTS error: ${e.message}")
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }

    /**
     * Immediately stops audio playback in <500ms (F-08 requirement).
     */
    fun stop() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping player: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    private fun playAudioFile(
        file: File,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        stop()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    onStart()
                }
                setOnCompletionListener {
                    stop()
                    onDone()
                }
                setOnErrorListener { _, what, extra ->
                    stop()
                    onError(RuntimeException("MediaPlayer error: what=$what extra=$extra"))
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Throwable) {
            stop()
            onError(e)
        }
    }

    private fun fetchTtsAudio(text: String, lang: String, speaker: String): ByteArray? {
        val url = URL(TTS_URL)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("api-subscription-key", apiKey)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 12000
            conn.readTimeout = 18000
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("inputs", JSONArray().put(text))
                put("target_language_code", lang)
                put("speaker", speaker)
                put("model", MODEL_TTS)
                put("pace", 1.0)
            }

            conn.outputStream.use { os ->
                os.write(jsonBody.toString().toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_OK) {
                val responseStr = conn.inputStream.bufferedReader().use { it.readText() }
                val respJson = JSONObject(responseStr)
                val audios = respJson.optJSONArray("audios")
                if (audios != null && audios.length() > 0) {
                    val base64Audio = audios.getString(0)
                    return Base64.decode(base64Audio, Base64.DEFAULT)
                }
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                Log.e(TAG, "Sarvam API returned HTTP $code: $err")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Sarvam TTS fetch", e)
            throw e
        } finally {
            conn.disconnect()
        }
        return null
    }

    /**
     * Transcribe speech audio to text using Sarvam AI Speech-to-Text (`saaras:v3`).
     */
    fun transcribe(
        audioFile: File,
        languageCode: String = "en-IN",
        callback: (Result<String>) -> Unit,
    ) {
        scope.launch {
            try {
                val boundary = "----SarvamBoundary" + System.currentTimeMillis()
                val url = URL(STT_URL)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("api-subscription-key", apiKey)
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                conn.connectTimeout = 15000
                conn.readTimeout = 25000
                conn.doOutput = true

                conn.outputStream.use { out ->
                    fun writeField(name: String, value: String) {
                        out.write(("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n").toByteArray())
                    }
                    writeField("model", MODEL_STT)
                    writeField("language_code", languageCode)

                    val fileHeader = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"${audioFile.name}\"\r\nContent-Type: audio/wav\r\n\r\n"
                    out.write(fileHeader.toByteArray())
                    audioFile.inputStream().use { it.copyTo(out) }
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                    out.flush()
                }

                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_OK) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resp)
                    val transcript = json.optString("transcript", "")
                    withContext(Dispatchers.Main) {
                        callback(Result.success(transcript))
                    }
                } else {
                    val err = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    throw RuntimeException("STT Error HTTP $code: $err")
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    callback(Result.failure(e))
                }
            }
        }
    }

    private fun hashText(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}

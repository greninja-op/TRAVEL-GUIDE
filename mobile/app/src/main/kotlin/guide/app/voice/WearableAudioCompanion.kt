package guide.app.voice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.util.Log

/**
 * Wearable & Bluetooth Audio Companion Controller.
 *
 * Provides complete hands-free hardware button and wearable device support:
 * 1. Bluetooth Earbud / Headset Control via Android [MediaSession]:
 *    - Single tap: Play / Pause story
 *    - Double tap (Skip Next): On-demand "What am I seeing?" spontaneous AI tour observation
 *    - Triple tap (Skip Previous): Replay last spoken landmark story
 *    - Long press / Stop: Silence guide
 * 2. Wearables & Smartwatch Integration (Wear OS, Apple Watch, Garmin, Car Bluetooth):
 *    - Publishes active landmark metadata (Name, Summary, Tour State) directly to smartwatch screens
 *    - Accepts media controls from wearable display interfaces
 * 3. Soft Arrival Audio Chime:
 *    - Synthesizes a gentle, 280ms dual-tone acoustic chime (587Hz -> 880Hz) before speech
 *      so travelers never miss the first few words while walking through street noise.
 * 4. Audio Becoming Noisy Safety:
 *    - Instantly pauses audio when Bluetooth earphones disconnect or are removed, preventing
 *      unexpected speaker output in public spaces.
 */
class WearableAudioCompanion(
    private val context: Context,
    private val onPlayRequested: () -> Unit,
    private val onPauseRequested: () -> Unit,
    private val onWhatAmISeeingRequested: () -> Unit,
    private val onReplayRequested: () -> Unit,
) {
    companion object {
        private const val TAG = "WearableAudioCompanion"
    }

    private var mediaSession: MediaSession? = null
    private var noisyReceiver: BroadcastReceiver? = null
    private var lastSpokenTitle: String = "Travel Guide Audio Companion"
    private var isPlayingState: Boolean = false

    init {
        setupMediaSession()
        setupNoisyReceiver()
    }

    private fun setupMediaSession() {
        try {
            val session = MediaSession(context, "TravelGuideAudioSession").apply {
                val stateBuilder = PlaybackState.Builder()
                    .setActions(
                        PlaybackState.ACTION_PLAY or
                        PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackState.ACTION_STOP
                    )
                    .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)

                setPlaybackState(stateBuilder.build())

                setCallback(object : MediaSession.Callback() {
                    override fun onPlay() {
                        Log.d(TAG, "Bluetooth / Wearable onPlay received")
                        onPlayRequested()
                    }

                    override fun onPause() {
                        Log.d(TAG, "Bluetooth / Wearable onPause received")
                        onPauseRequested()
                    }

                    override fun onSkipToNext() {
                        // Double tap on wireless earbuds -> Trigger What Am I Seeing!
                        Log.d(TAG, "Bluetooth / Wearable onSkipToNext -> Triggering What Am I Seeing")
                        onWhatAmISeeingRequested()
                    }

                    override fun onSkipToPrevious() {
                        // Triple tap on wireless earbuds -> Replay last story
                        Log.d(TAG, "Bluetooth / Wearable onSkipToPrevious -> Replaying last story")
                        onReplayRequested()
                    }

                    override fun onStop() {
                        Log.d(TAG, "Bluetooth / Wearable onStop received")
                        onPauseRequested()
                    }
                })

                isActive = true
            }
            mediaSession = session
            updateMetadata(lastSpokenTitle, "Ready for spontaneous audio guidance")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize MediaSession: ${e.message}")
        }
    }

    private fun setupNoisyReceiver() {
        noisyReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    val prefs = context?.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
                    val autoPause = prefs?.getBoolean("earbuds_autopause_enabled", true) ?: true
                    if (autoPause) {
                        Log.i(TAG, "Earphones disconnected: auto-pausing spoken guidance")
                        onPauseRequested()
                    }
                }
            }
        }
        try {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(noisyReceiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register noisy receiver: ${e.message}")
        }
    }

    /**
     * Updates the lock screen and wearable smartwatch media view.
     */
    fun updateMetadata(landmarkTitle: String, subtitle: String = "Spontaneous Oral Heritage Guide") {
        lastSpokenTitle = landmarkTitle
        try {
            val metadata = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, landmarkTitle)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Travel Guide AI Companion")
                .putString(MediaMetadata.METADATA_KEY_ALBUM, subtitle)
                .build()
            mediaSession?.setMetadata(metadata)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update MediaMetadata: ${e.message}")
        }
    }

    /**
     * Synchronizes playback state with smartwatch and Bluetooth AVRCP controllers.
     */
    fun setPlaybackState(isPlaying: Boolean) {
        isPlayingState = isPlaying
        try {
            val state = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
            val stateBuilder = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP
                )
                .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)

            mediaSession?.setPlaybackState(stateBuilder.build())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update PlaybackState: ${e.message}")
        }
    }

    /**
     * Plays a high-fidelity, subtle arrival cue tone before the guide begins speaking.
     */
    fun playArrivalChime() {
        val prefs = context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
        val chimeEnabled = prefs.getBoolean("earbuds_chime_enabled", true)
        if (!chimeEnabled) return

        NavigationChime.play()
    }

    fun release() {
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaSession: ${e.message}")
        }

        try {
            noisyReceiver?.let { context.unregisterReceiver(it) }
            noisyReceiver = null
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering noisy receiver: ${e.message}")
        }
    }
}

/**
 * Procedural Dual-Tone Acoustic Chime Generator.
 * Synthesizes a clean 280ms acoustic chime (D5 587Hz -> A5 880Hz) directly into PCM 16-bit.
 */
object NavigationChime {
    private val pcmData: ByteArray by lazy {
        val sampleRate = 44100
        val durationMs = 280
        val numSamples = (sampleRate * durationMs / 1000)
        val buffer = ShortArray(numSamples)

        val f1 = 587.33 // D5
        val f2 = 880.00 // A5
        val split = (sampleRate * 90 / 1000) // First tone 90ms

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val sample = if (i < split) {
                val envelope = Math.sin(Math.PI * i / split)
                Math.sin(2.0 * Math.PI * f1 * t) * envelope * 0.35
            } else {
                val decay = Math.exp(-6.5 * (i - split) / (numSamples - split))
                Math.sin(2.0 * Math.PI * f2 * t) * decay * 0.45
            }
            buffer[i] = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
        }

        val bytes = ByteArray(numSamples * 2)
        for (i in 0 until numSamples) {
            bytes[i * 2] = (buffer[i].toInt() and 0xFF).toByte()
            bytes[i * 2 + 1] = ((buffer[i].toInt() shr 8) and 0xFF).toByte()
        }
        bytes
    }

    fun play() {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(44100)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmData, 0, pcmData.size)
            track.setNotificationMarkerPosition(pcmData.size / 2)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    try {
                        t?.stop()
                        t?.release()
                    } catch (_: Exception) {}
                }
                override fun onPeriodicNotification(t: AudioTrack?) = Unit
            })
            track.play()
        } catch (_: Exception) {
            // Non-critical fallback
        }
    }
}

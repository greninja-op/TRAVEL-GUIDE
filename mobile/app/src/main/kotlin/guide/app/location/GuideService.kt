package guide.app.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import guide.app.MainActivity
import guide.app.data.GuideDb
import guide.app.data.PackLoader
import guide.app.data.VisitEntity
import guide.app.power.BatteryProfile
import guide.app.voice.Narrator
import guide.core.GuideEngine
import guide.core.Poi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Phase 1 foreground location service. Persistent notification whenever
 * tracking is live (SPEC §1: visible indicator, never suppressible).
 * Mute action in the notification stops the voice instantly (F-08).
 *
 * Real loop: LocationTracker -> GuideEngine -> Narrator + Room visits.
 */
class GuideService : Service() {

    companion object {
        const val CHANNEL = "guide_tracking"
        const val CHANNEL_SILENT = "guide_silent"
        const val NOTIF_ID = 41
        const val ACTION_MUTE = "guide.app.MUTE"
        const val ACTION_UNMUTE = "guide.app.UNMUTE"
        const val ACTION_STOP = "guide.app.STOP"
        const val ACTION_PROFILE = "guide.app.PROFILE_CHANGED"
        const val ACTION_QUIET_ON = "guide.app.QUIET_ON"
        const val ACTION_QUIET_OFF = "guide.app.QUIET_OFF"
        const val ACTION_AUTOPLAY_ON = "guide.app.AUTOPLAY_ON"
        const val ACTION_AUTOPLAY_OFF = "guide.app.AUTOPLAY_OFF"
        const val ACTION_RATE = "guide.app.RATE"
        const val ACTION_SPEAK = "guide.app.SPEAK"
        const val ACTION_NOTIF_ON = "guide.app.NOTIF_ON"
        const val ACTION_NOTIF_OFF = "guide.app.NOTIF_OFF"
        const val EXTRA_PROFILE = "guide.app.PROFILE"
        const val EXTRA_RATE = "guide.app.RATE_VALUE"
        const val EXTRA_PHRASE = "guide.app.PHRASE_VALUE"
        var running = false
            private set

        /** Start tracking. Caller must already hold location permission. */
        fun start(context: Context, profile: BatteryProfile = BatteryProfile.BALANCED) {
            val i = Intent(context, GuideService::class.java)
                .putExtra(EXTRA_PROFILE, profile.name)
            ContextCompat.startForegroundService(context, i)
        }

        /**
         * Real working notifications toggle.
         * When enabled: posts live rich notifications with interactive controls & POI alerts.
         * When disabled: suppresses all drawer notifications and silences background service.
         */
        fun setNotificationsEnabled(context: Context, enabled: Boolean) {
            send(context, if (enabled) ACTION_NOTIF_ON else ACTION_NOTIF_OFF)
        }

        /**
         * Mute/unmute the live voice (F-08: under 500ms, no service restart).
         * Sends the intent directly; if the service isn't running this is a
         * harmless no-op — muted is UI state the user can still toggle.
         */
        fun setMuted(context: Context, muted: Boolean) {
            if (!running) return
            val action = if (muted) ACTION_MUTE else ACTION_UNMUTE
            context.startService(
                Intent(context, GuideService::class.java).setAction(action),
            )
        }

        /** Change battery profile live — restarts the tracker at the new rate. */
        fun setProfile(context: Context, profile: BatteryProfile) {
            send(context, ACTION_PROFILE) { putExtra(EXTRA_PROFILE, profile.name) }
        }

        /** Quiet hours (SPEC: cards still appear, the voice stays off). */
        fun setQuiet(context: Context, quiet: Boolean) {
            send(context, if (quiet) ACTION_QUIET_ON else ACTION_QUIET_OFF)
        }

        /** Whether a reached stop starts speaking without being asked. */
        fun setAutoPlay(context: Context, autoPlay: Boolean) {
            send(context, if (autoPlay) ACTION_AUTOPLAY_ON else ACTION_AUTOPLAY_OFF)
        }

        /** Playback rate for the narration voice. */
        fun setSpeechRate(context: Context, rate: Float) {
            send(context, ACTION_RATE) { putExtra(EXTRA_RATE, rate) }
        }

        /** Speak an arbitrary phrase (the phrasebook's "Say it for me" or audio preview). */
        fun speak(context: Context, phrase: String) {
            if (running) {
                send(context, ACTION_SPEAK) { putExtra(EXTRA_PHRASE, phrase) }
            } else {
                guide.app.voice.SarvamAudioService.getInstance(context).speak(phrase)
            }
        }

        /**
         * Deliver one of the actions above. No-op when the service is not
         * running — the caller still owns the UI state, so a setting changed
         * before tracking starts is not lost, it simply applies on start.
         */
        private fun send(
            context: Context,
            action: String,
            extras: Intent.() -> Unit = {},
        ) {
            if (!running) return
            context.startService(
                Intent(context, GuideService::class.java).setAction(action).apply(extras),
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Guide tracking", NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SILENT, "Silent background tracking", NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            },
        )
    }

    private var tracker: LocationTracker? = null
    private var narrator: Narrator? = null
    private var engine: GuideEngine? = null
    private var notificationsEnabled: Boolean = true

    /** Battery profile the tracker is currently armed with. */
    private var activeProfile: BatteryProfile = BatteryProfile.BALANCED
    private val arrivals = mutableMapOf<String, Long>() // poiId -> arrivedAtS
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_NOTIF_ON -> {
                notificationsEnabled = true
                startForeground(NOTIF_ID, notification(muted = narrator?.muted ?: false))
                return START_STICKY
            }
            ACTION_NOTIF_OFF -> {
                notificationsEnabled = false
                startForeground(NOTIF_ID, silentNotification())
                val nm = getSystemService(NotificationManager::class.java)
                nm.cancel(9901)
                return START_STICKY
            }
            ACTION_MUTE -> {
                narrator?.setMuted(true)
                if (notificationsEnabled) {
                    startForeground(NOTIF_ID, notification(muted = true))
                } else {
                    startForeground(NOTIF_ID, silentNotification())
                }
                return START_STICKY // keeps tracking (cards only) while voice is off
            }
            ACTION_UNMUTE -> {
                narrator?.setMuted(false)
                if (notificationsEnabled) {
                    startForeground(NOTIF_ID, notification(muted = false))
                } else {
                    startForeground(NOTIF_ID, silentNotification())
                }
                return START_STICKY
            }
            ACTION_PROFILE -> {
                // Re-arm the tracker at the new rate so the choice takes effect
                // now, rather than at the next app start.
                val p = intent.getStringExtra(EXTRA_PROFILE)
                    ?.let { name -> runCatching { BatteryProfile.valueOf(name) }.getOrNull() }
                if (p != null) {
                    tracker?.stop()
                    tracker = null
                    startTracking(p)
                }
                return START_STICKY
            }
            ACTION_QUIET_ON -> { narrator?.quiet = true; return START_STICKY }
            ACTION_QUIET_OFF -> { narrator?.quiet = false; return START_STICKY }
            ACTION_AUTOPLAY_ON -> { narrator?.autoPlay = true; return START_STICKY }
            ACTION_AUTOPLAY_OFF -> { narrator?.autoPlay = false; return START_STICKY }
            ACTION_RATE -> {
                narrator?.speechRate = intent.getFloatExtra(EXTRA_RATE, 1.0f)
                return START_STICKY
            }
            ACTION_SPEAK -> {
                // Phrasebook: speak on demand, never automatically. Uses the
                // same Narrator so mute and rate apply to it too.
                val phrase = intent.getStringExtra(EXTRA_PHRASE).orEmpty()
                if (phrase.isNotBlank()) {
                    narrator?.enqueue("phrase-${phrase.hashCode()}", phrase)
                }
                return START_STICKY
            }
            ACTION_STOP -> {
                running = false
                tracker?.stop()
                narrator?.shutdown()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        running = true
        notificationsEnabled = getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
            .getBoolean("notifications_enabled", true)
        // Honour the profile the caller asked for (the Settings choice) rather
        // than always starting at BALANCED.
        intent?.getStringExtra(EXTRA_PROFILE)
            ?.let { n -> runCatching { BatteryProfile.valueOf(n) }.getOrNull() }
            ?.let { activeProfile = it }
        if (notificationsEnabled) {
            startForeground(NOTIF_ID, notification(muted = false))
        } else {
            startForeground(NOTIF_ID, silentNotification())
        }
        startTracking(activeProfile)
        return START_STICKY
    }

    /** Tracker -> engine -> narrator + visits. Caller guarantees permission. */
    private fun startTracking(profile: BatteryProfile = activeProfile) {
        if (tracker != null) return
        val (version, cards) = PackLoader.load(this)
        val pois = cards.map { c ->
            Poi(
                id = c.id, name = c.name, lat = c.lat, lng = c.lng,
                radiusM = c.radiusM, category = "", layer = c.layer,
                summary = c.summary, history = c.history,
                funFacts = c.funFacts, seeList = c.seeList,
                // "09:00–17:00" (en dash) -> Hours, so closed-skip logic works.
                hours = c.hours?.split("–")?.takeIf { it.size == 2 }
                    ?.let { guide.core.Hours(it[0], it[1]) },
                sources = c.sources, packVersion = version,
            )
        }
        val pack = guide.core.Pack("live", "live", version, pois, emptyList())
        engine = GuideEngine(pack)
        narrator = Narrator(this)
        val db = GuideDb.getInstance(this)
        val byId = cards.associateBy { it.id }
        tracker = LocationTracker(this).also { t ->
            t.start(profile) { fix ->
                val update = engine?.onFix(fix.lat, fix.lng, fix.speedMps, fix.atS) ?: return@start
                for (a in update.arrived) arrivals[a.id] = fix.atS
                for (d in update.departed) {
                    val arrivedAt = arrivals.remove(d.id) ?: fix.atS
                    scope.launch {
                        db.dao().insertVisit(
                            VisitEntity(poiId = d.id, arrivedAt = arrivedAt,
                                leftAt = fix.atS, packVersion = version),
                        )
                    }
                }
                for (p in update.spoke) {
                    val card = byId[p.id] ?: continue
                    currentCard = card
                    updateDynamicNotification(muted = narrator?.muted ?: false)
                    val n = narrator ?: continue
                    scope.launch {
                        val story = guide.app.voice.SpontaneousGuideAiEngine.generateStory(
                            context = this@GuideService,
                            card = card,
                            userLat = fix.lat,
                            userLng = fix.lng,
                        )
                        n.enqueue(p.id, story, onRoute = true)
                    }
                }

                // If Google Maps Navigation Companion is active, prioritize POIs along the route corridor
                val corridorIds = guide.app.navigation.MapsCompanionState.corridorPoiIds
                if (corridorIds.isNotEmpty()) {
                    for (cId in corridorIds) {
                        if (cId in arrivals) continue
                        val cCard = byId[cId] ?: continue
                        val radius = (cCard.radiusM.toDouble()).takeIf { it > 0.0 } ?: 60.0
                        val dist = guide.core.Geo.distanceM(fix.lat, fix.lng, cCard.lat, cCard.lng)
                        if (dist <= radius * 1.25) {
                            arrivals[cId] = fix.atS
                            currentCard = cCard
                            updateDynamicNotification(muted = narrator?.muted ?: false)
                            val n = narrator ?: continue
                            scope.launch {
                                val story = guide.app.voice.SpontaneousGuideAiEngine.generateStory(
                                    context = this@GuideService,
                                    card = cCard,
                                    userLat = fix.lat,
                                    userLng = fix.lng,
                                )
                                n.enqueue(cId, story, onRoute = true)
                            }
                        }
                    }
                }
            }
        }
    }

    private var currentCard: PackLoader.PoiCard? = null

    private fun updateDynamicNotification(muted: Boolean) {
        if (!notificationsEnabled) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, notification(muted))
    }

    private fun silentNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_SILENT)
            .setSmallIcon(guide.app.R.mipmap.ic_launcher)
            .setContentTitle("Travel Guide Active")
            .setContentText("Background tour service active (drawer notifications muted)")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setNotificationSilent()
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun notification(muted: Boolean): Notification {
        val targetPoiId = currentCard?.id
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!targetPoiId.isNullOrEmpty()) {
                putExtra("route", "poi/$targetPoiId")
            }
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val muteAction = if (muted) ACTION_UNMUTE else ACTION_MUTE
        val muteIntent = Intent(this, GuideService::class.java).setAction(muteAction)
        val mutePendingIntent = PendingIntent.getService(
            this, 1, muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val mapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("route", "map")
        }
        val mapPendingIntent = PendingIntent.getActivity(
            this, 2, mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val card = currentCard
        val title = card?.name ?: "Exploring Fort Kochi"
        val messageText = if (card != null) {
            "Near ${card.name} • ${card.summary}"
        } else {
            "Exploring Fort Kochi heritage quarter • 24 cultural stops along your path."
        }
        val statusSummary = if (muted) "Audio Muted • Tap to Resume" else "Spoken Tour Active"

        val drawableId = guide.app.ui.PoiImageResolver.getDrawableForPoi(card?.id)
        val heroBitmap = runCatching {
            android.graphics.BitmapFactory.decodeResource(resources, drawableId)
        }.getOrNull()

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(guide.app.R.drawable.ic_stat_notification)
            .setColor(0xFFFF5A36.toInt()) // Brand Sunset Coral
            .setContentTitle(title)
            .setContentText(messageText)
            .setSubText("Travel Guide")
            .setContentIntent(openPendingIntent)
            .addAction(
                if (muted) android.R.drawable.ic_media_play else android.R.drawable.ic_lock_silent_mode,
                if (muted) "Unmute Audio" else "Mute Audio",
                mutePendingIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_compass,
                "Explore Place",
                openPendingIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_mapmode,
                "Open Map",
                mapPendingIntent,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (heroBitmap != null) {
            builder.setLargeIcon(heroBitmap)
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(heroBitmap)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
                    .setBigContentTitle(title)
                    .setSummaryText(messageText),
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .setSummaryText(statusSummary)
                    .bigText(messageText),
            )
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

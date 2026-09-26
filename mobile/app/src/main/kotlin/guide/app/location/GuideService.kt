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
        const val NOTIF_ID = 41
        const val ACTION_MUTE = "guide.app.MUTE"
        const val ACTION_UNMUTE = "guide.app.UNMUTE"
        const val ACTION_STOP = "guide.app.STOP"
        const val EXTRA_PROFILE = "guide.app.PROFILE"
        var running = false
            private set

        /** Start tracking. Caller must already hold location permission. */
        fun start(context: Context, profile: BatteryProfile = BatteryProfile.BALANCED) {
            val i = Intent(context, GuideService::class.java)
                .putExtra(EXTRA_PROFILE, profile.name)
            ContextCompat.startForegroundService(context, i)
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
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Guide tracking", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private var tracker: LocationTracker? = null
    private var narrator: Narrator? = null
    private var engine: GuideEngine? = null
    private val arrivals = mutableMapOf<String, Long>() // poiId -> arrivedAtS
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_MUTE -> {
                narrator?.setMuted(true)
                startForeground(NOTIF_ID, notification(muted = true))
                return START_STICKY // keeps tracking (cards only) while voice is off
            }
            ACTION_UNMUTE -> {
                narrator?.setMuted(false)
                startForeground(NOTIF_ID, notification(muted = false))
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
        startForeground(NOTIF_ID, notification(muted = false))
        startTracking()
        return START_STICKY
    }

    /** Tracker -> engine -> narrator + visits. Caller guarantees permission. */
    private fun startTracking() {
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
        val db = Room.databaseBuilder(this, GuideDb::class.java, "guide.db").build()
        val byId = cards.associateBy { it.id }
        tracker = LocationTracker(this).also { t ->
            t.start(BatteryProfile.BALANCED) { fix ->
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
                    val n = narrator ?: continue
                    n.enqueue(p.id, n.storyText(
                        card.name, card.summary, card.funFacts.firstOrNull(),
                    ), onRoute = true)
                }
            }
        }
    }

    private fun notification(muted: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val mute = PendingIntent.getService(
            this, 1, Intent(this, GuideService::class.java).setAction(ACTION_MUTE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle(if (muted) "Travel Guide (muted)" else "Travel Guide is narrating")
            .setContentText("Tracking your walk for nearby stories")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_lock_silent_mode, "Mute", mute)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

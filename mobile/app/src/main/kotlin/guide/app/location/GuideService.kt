package guide.app.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import guide.app.MainActivity

/**
 * Phase 1 foreground location service. Persistent notification whenever
 * tracking is live (SPEC §1: visible indicator, never suppressible).
 * Mute action in the notification stops the voice instantly (F-08).
 */
class GuideService : Service() {

    companion object {
        const val CHANNEL = "guide_tracking"
        const val NOTIF_ID = 41
        const val ACTION_MUTE = "guide.app.MUTE"
        const val ACTION_STOP = "guide.app.STOP"
        var running = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Guide tracking", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                running = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_MUTE -> {
                // Mute is delivered to Narrator via broadcast in the full wiring;
                // service keeps tracking (cards only) while voice is off.
            }
        }
        running = true
        startForeground(NOTIF_ID, notification(muted = false))
        // FusedLocationProvider updates + TriggerState feed attach here.
        return START_STICKY
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

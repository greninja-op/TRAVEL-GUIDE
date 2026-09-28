package guide.app.navigation

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import guide.core.Geo
import java.util.Locale

/**
 * Origin of an external navigation session detected by Travel Guide.
 */
enum class CompanionSource {
    GOOGLE_MAPS,
    SHARED_INTENT,
    SIMULATED,
    ACCESSIBILITY,
}

/**
 * Snapshot of an active navigation session captured from Google Maps (or external map app).
 */
data class NavigationCompanionSession(
    val destinationName: String,
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val etaOrDistance: String? = null,
    val nextManeuver: String? = null,
    val source: CompanionSource = CompanionSource.GOOGLE_MAPS,
    val detectedAtMs: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
)

/**
 * Singleton state holder for Google Maps navigation companion integration.
 * Accessible from Compose UI, GuideService engine, and NotificationListenerService.
 */
object MapsCompanionState {
    var currentSession by mutableStateOf<NavigationCompanionSession?>(null)
        private set

    /** POI IDs identified to be along the active navigation route corridor. */
    var corridorPoiIds by mutableStateOf<Set<String>>(emptySet())
        private set

    /**
     * Called when a navigation session is detected or updated from Google Maps.
     */
    fun onNavStarted(
        destinationName: String,
        destinationLat: Double? = null,
        destinationLng: Double? = null,
        etaOrDistance: String? = null,
        nextManeuver: String? = null,
        source: CompanionSource = CompanionSource.GOOGLE_MAPS,
    ) {
        currentSession = NavigationCompanionSession(
            destinationName = destinationName,
            destinationLat = destinationLat,
            destinationLng = destinationLng,
            etaOrDistance = etaOrDistance,
            nextManeuver = nextManeuver,
            source = source,
            isActive = true,
        )
    }

    fun onNavUpdated(
        etaOrDistance: String? = null,
        nextManeuver: String? = null,
    ) {
        val s = currentSession ?: return
        currentSession = s.copy(
            etaOrDistance = etaOrDistance ?: s.etaOrDistance,
            nextManeuver = nextManeuver ?: s.nextManeuver,
        )
    }

    fun onNavEnded() {
        currentSession = null
        corridorPoiIds = emptySet()
    }

    /**
     * Compute and record the POIs that lie within a corridor along the path from
     * [originLat],[originLng] to the destination.
     *
     * A POI is in the corridor if:
     * 1. Its distance to origin + distance to dest is <= 1.3x straight-line distance, OR
     * 2. It is within [maxCrossTrackM] (e.g. 350m) of the path segment.
     */
    fun updateCorridorPois(
        originLat: Double,
        originLng: Double,
        destLat: Double,
        destLng: Double,
        allPois: List<CorridorCandidate>,
        maxCrossTrackM: Double = 400.0,
    ) {
        val directDist = Geo.distanceM(originLat, originLng, destLat, destLng)
        val matched = allPois.filter { poi ->
            val toOrigin = Geo.distanceM(originLat, originLng, poi.lat, poi.lng)
            val toDest = Geo.distanceM(destLat, destLng, poi.lat, poi.lng)
            // Elliptical corridor check
            if (directDist > 100.0 && (toOrigin + toDest) <= (directDist * 1.35 + maxCrossTrackM)) {
                true
            } else {
                toDest <= maxCrossTrackM || toOrigin <= maxCrossTrackM
            }
        }.map { it.id }.toSet()

        corridorPoiIds = matched
    }

    /**
     * Checks if the Android Notification Access permission is granted to this app.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val cn = ComponentName(context, MapsCompanionService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(cn.flattenToString())
    }
}

data class CorridorCandidate(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
)

/**
 * Background NotificationListenerService that observes turn-by-turn navigation
 * updates posted by Google Maps (`com.google.android.apps.maps`).
 *
 * When navigation starts in Google Maps:
 * - Google Maps posts an ongoing notification with turn directions & ETA.
 * - This service extracts the destination, maneuvers, and ETA.
 * - It pre-arms Travel Guide so stories along that corridor are pre-cached and ready!
 */
class MapsCompanionService : NotificationListenerService() {

    companion object {
        const val GOOGLE_MAPS_PKG = "com.google.android.apps.maps"
        private val ETA_REGEX = Regex("""\d+\s*(?:hr|min|hour|m|km|mi)""", RegexOption.IGNORE_CASE)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val pkg = sbn?.packageName ?: return
        // Strict package isolation: Drop all notifications not originating exactly from Google Maps
        if (pkg != GOOGLE_MAPS_PKG) return

        val n = sbn.notification ?: return
        val extras = n.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        // Check if this notification represents active navigation.
        // Google Maps uses ongoing notifications with ETA/distance and turn instructions.
        val combined = "$title $text $subText $bigText"
        val hasEta = ETA_REGEX.containsMatchIn(combined)
        val hasNavCue = combined.contains("toward", ignoreCase = true) ||
            combined.contains("navigating", ignoreCase = true) ||
            combined.contains("head", ignoreCase = true) ||
            combined.contains("turn", ignoreCase = true) ||
            combined.contains("exit", ignoreCase = true) ||
            combined.contains("destination", ignoreCase = true) ||
            (n.flags and Notification.FLAG_ONGOING_EVENT) != 0

        if (hasNavCue || hasEta) {
            // Determine likely destination and maneuver
            var destName = subText.ifBlank { "" }
            var maneuver = title
            var eta = text

            if (destName.isBlank()) {
                // If subText is blank, parse title/text
                if (title.contains("to ", ignoreCase = true)) {
                    destName = title.substringAfter("to ", "").trim()
                } else if (text.contains("to ", ignoreCase = true)) {
                    destName = text.substringAfter("to ", "").trim()
                } else if (title.isNotBlank()) {
                    destName = title
                } else {
                    destName = "Active Navigation Route"
                }
            }

            // Security sanitization (clamp length, filter control characters)
            val cleanDest = destName.replace(Regex("""[^\w\s.,'#\-]"""), " ").trim().take(80)
            val cleanManeuver = maneuver.replace(Regex("""[^\w\s.,'#\-]"""), " ").trim().take(100)
            val cleanEta = eta.replace(Regex("""[^\w\s.,'#\-•]"""), " ").trim().take(40)

            if (MapsCompanionState.currentSession == null) {
                MapsCompanionState.onNavStarted(
                    destinationName = cleanDest.ifBlank { "Active Navigation Route" },
                    etaOrDistance = cleanEta.ifBlank { null },
                    nextManeuver = cleanManeuver.ifBlank { null },
                    source = CompanionSource.GOOGLE_MAPS,
                )
            } else {
                MapsCompanionState.onNavUpdated(
                    etaOrDistance = cleanEta.ifBlank { null },
                    nextManeuver = cleanManeuver.ifBlank { null },
                )
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg == GOOGLE_MAPS_PKG) {
            // When Google Maps removes its ongoing navigation notification, the route ended.
            MapsCompanionState.onNavEnded()
        }
    }
}

package guide.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import guide.app.power.BatteryProfile

/**
 * Fused location feed honoring the user's battery profile (SPEC §1.3).
 * Caller applies TriggerState; quiet hours + speed gate live in shared-core.
 */
class LocationTracker(context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    data class Fix(val lat: Double, val lng: Double, val atS: Long, val speedMps: Double)

    @SuppressLint("MissingPermission") // caller checks permissions first (ConsentScreen)
    fun start(profile: BatteryProfile, onFix: (Fix) -> Unit) {
        stop()
        val req = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            profile.intervalS * 1000,
        ).setMinUpdateDistanceMeters(profile.minDisplacementM).build()
        callback = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                val l: Location = r.lastLocation ?: return
                onFix(Fix(l.latitude, l.longitude, l.time / 1000, l.speed.toDouble()))
            }
        }
        client.requestLocationUpdates(req, callback!!, Looper.getMainLooper())
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }
}

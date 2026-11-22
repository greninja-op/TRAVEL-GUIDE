package guide.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import guide.app.power.BatteryProfile
import kotlin.math.abs

/**
 * Location feed honoring the user's battery profile.
 *
 * Google Play Services' fused provider is preferred because it is the most
 * power-efficient and accurate. Some devices (or a broken/disabled Play
 * services install) never answer that request. The platform
 * [LocationManager] providers are registered alongside it as a fallback, and a
 * recent last-known fix is delivered immediately so the map is not blank while
 * GPS settles.
 */
class LocationTracker(context: Context) {
    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)
    private val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var callback: LocationCallback? = null
    private var platformListener: LocationListener? = null

    private var lastDeliveredElapsed = 0L
    private var lastLat = Double.NaN
    private var lastLng = Double.NaN

    data class Fix(val lat: Double, val lng: Double, val atS: Long, val speedMps: Double)

    @SuppressLint("MissingPermission") // caller checks permissions first (PermissionGate)
    fun start(profile: BatteryProfile, onFix: (Fix) -> Unit) {
        stop()

        val intervalMs = profile.intervalS * 1000L
        val minDistance = profile.minDisplacementM

        // 1) Fused provider — best case.
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMs,
        ).setMinUpdateDistanceMeters(minDistance).build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { deliver(it, onFix) }
            }
        }
        runCatching {
            client.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
        }.onFailure {
            Log.w(TAG, "Fused location unavailable: ${it.message}")
        }

        // 2) Platform providers — the fallback when Play services cannot serve
        // location (seen on devices with a disabled/broken GMS install).
        platformListener = object : LocationListener {
            override fun onLocationChanged(location: Location) = deliver(location, onFix)

            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        val now = System.currentTimeMillis()
        for (provider in PROVIDERS) {
            runCatching {
                if (!manager.isProviderEnabled(provider)) return@runCatching
                manager.requestLocationUpdates(
                    provider,
                    intervalMs,
                    minDistance,
                    platformListener!!,
                    Looper.getMainLooper(),
                )
                // A recent last-known fix paints the map immediately while the
                // first live update is acquired.
                manager.getLastKnownLocation(provider)
                    ?.takeIf { it.time > 0 && now - it.time < LAST_KNOWN_MAX_AGE_MS }
                    ?.let { deliver(it, onFix) }
            }.onFailure {
                Log.w(TAG, "Platform $provider unavailable: ${it.message}")
            }
        }
    }

    private fun deliver(location: Location, onFix: (Fix) -> Unit) {
        val elapsed = SystemClock.elapsedRealtime()
        if (elapsed - lastDeliveredElapsed < DEDUPE_WINDOW_MS &&
            !lastLat.isNaN() && !lastLng.isNaN() &&
            abs(location.latitude - lastLat) < 0.00005 &&
            abs(location.longitude - lastLng) < 0.00005
        ) {
            return
        }

        lastDeliveredElapsed = elapsed
        lastLat = location.latitude
        lastLng = location.longitude
        onFix(
            Fix(
                lat = location.latitude,
                lng = location.longitude,
                atS = location.time / 1000,
                speedMps = location.speed.toDouble(),
            ),
        )
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
        platformListener?.let { manager.removeUpdates(it) }
        platformListener = null
        lastDeliveredElapsed = 0L
        lastLat = Double.NaN
        lastLng = Double.NaN
    }

    private companion object {
        const val TAG = "LocationTracker"
        const val DEDUPE_WINDOW_MS = 1_200L
        const val LAST_KNOWN_MAX_AGE_MS = 15 * 60 * 1000L
        val PROVIDERS = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    }
}

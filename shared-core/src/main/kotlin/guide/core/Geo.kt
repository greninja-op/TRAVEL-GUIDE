package guide.core

import kotlin.math.*

/** Pure geo math. No Android imports. */
object Geo {
    private const val EARTH_M = 6_371_000.0

    fun distanceM(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_M * asin(sqrt(a))
    }

    fun isInside(lat: Double, lng: Double, poi: Poi): Boolean =
        distanceM(lat, lng, poi.lat, poi.lng) <= poi.radiusM

    /** Bearing degrees 0..360 from point to POI. */
    fun bearingTo(lat: Double, lng: Double, poi: Poi): Double {
        val dLng = Math.toRadians(poi.lng - lng)
        val y = sin(dLng) * cos(Math.toRadians(poi.lat))
        val x = cos(Math.toRadians(lat)) * sin(Math.toRadians(poi.lat)) -
            sin(Math.toRadians(lat)) * cos(Math.toRadians(poi.lat)) * cos(dLng)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }
}

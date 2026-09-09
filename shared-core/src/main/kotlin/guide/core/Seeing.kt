package guide.core

/** "What am I seeing?" (SPEC §3.1): nearest POI within 1.5x radius,
 *  boosted when the user's heading points at it (±35°). */
object Seeing {
    fun pick(
        lat: Double,
        lng: Double,
        headingDeg: Double?,
        pois: List<Poi>,
    ): Poi? {
        var best: Poi? = null
        var bestScore = Double.MAX_VALUE
        for (poi in pois) {
            val d = Geo.distanceM(lat, lng, poi.lat, poi.lng)
            if (d > poi.radiusM * 1.5) continue
            var score = d
            if (headingDeg != null) {
                val bearing = Geo.bearingTo(lat, lng, poi)
                var diff = (bearing - headingDeg) % 360
                if (diff > 180) diff -= 360
                if (diff < -180) diff += 360
                if (kotlin.math.abs(diff) <= 35) score *= 0.5 // facing it: wins ties
            }
            if (score < bestScore) {
                bestScore = score
                best = poi
            }
        }
        return best
    }
}

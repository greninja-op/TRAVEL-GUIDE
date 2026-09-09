package guide.core

/** Phase 1 trigger rules. Mirrored by tools/simulate_triggers.py + test_queue.py. */
data class TriggerConfig(
    val dwellS: Long = 8,
    val cooldownS: Long = 1800,
    val maxSpeakSpeedMps: Double = 8.0,
)

data class Fix(val lat: Double, val lng: Double, val atS: Long, val speedMps: Double)

class TriggerState(val config: TriggerConfig = TriggerConfig()) {
    private val insideSince = mutableMapOf<String, Long>()
    private val lastSpoken = mutableMapOf<String, Long>()

    /**
     * Feed one location fix. Returns POI ids that newly qualify for speech,
     * in caller-provided priority order (on-route first). One fix can qualify
     * several overlapping POIs — NarrationQueue serializes the voice.
     */
    fun onFix(fix: Fix, candidates: List<Poi>): List<String> {
        val out = mutableListOf<String>()
        for (poi in candidates) {
            val inside = Geo.isInside(fix.lat, fix.lng, poi)
            if (!inside) {
                insideSince.remove(poi.id)
                continue
            }
            val since = insideSince.getOrPut(poi.id) { fix.atS }
            val dwelled = fix.atS - since
            val cooled = (fix.atS - (lastSpoken[poi.id] ?: -config.cooldownS)) >= config.cooldownS
            val fast = fix.speedMps > config.maxSpeakSpeedMps
            if (dwelled >= config.dwellS && cooled && !fast) {
                out += poi.id
                lastSpoken[poi.id] = fix.atS
                insideSince.remove(poi.id) // require re-dwell after speaking
            }
        }
        return out
    }
}

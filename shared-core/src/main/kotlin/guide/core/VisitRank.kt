package guide.core

/**
 * Don't-miss / skip ranking (SPEC Phase 2): already-visited POIs sink,
 * closed POIs sink (exteriors still narrate), chosen layer filters.
 * Wander mode consumes this ordering; routes keep their fixed order.
 */
object VisitRank {
    fun rank(
        nearby: List<Pair<Poi, Double>>,
        visited: Set<String>,
        nowHHMM: String? = null,
        layer: String? = null,
    ): List<Poi> {
        var list = nearby
        if (layer != null) list = list.filter { it.first.layer == layer }
        return list.sortedWith(
            compareBy(
                { it.first.id in visited }, // unvisited first
                { poi -> // closed sinks (open-status computed when clock known)
                    if (nowHHMM != null && poi.first.hours != null &&
                        !Itineraries.isOpen(poi.first.hours, nowHHMM)
                    ) 1 else 0
                },
                { it.second }, // then nearest
            ),
        ).map { it.first }
    }
}

package guide.core

/**
 * The product loop in one pure class (no Android imports): location fix in,
 * speak/card/arrive/depart events out. GuideService feeds it; tools mirror it.
 *
 * Candidates are ordered remaining-route-stops first so TriggerState returns
 * speech in on-route priority (NarrationQueue then serializes the voice).
 */
class GuideEngine(
    val pack: Pack,
    val routeOrder: List<String> = emptyList(),
    val triggers: TriggerState = TriggerState(),
) {
    data class Update(
        val spoke: List<Poi>,
        val cards: List<Poi>,
        val arrived: List<Poi>,
        val departed: List<Poi>,
        val offRoute: Boolean,
    )

    private val byId = pack.pois.associateBy { it.id }
    private val inside = mutableSetOf<String>()
    val visited = mutableSetOf<String>()
    private val openVisits = mutableMapOf<String, Long>() // poiId -> arrivedAtS

    fun onFix(lat: Double, lng: Double, speedMps: Double, atS: Long): Update {
        // Order: remaining route stops first, then everything else by distance.
        val remaining = routeOrder.filter { it in byId && it !in visited }
        val rest = pack.pois
            .filter { it.id !in remaining.toSet() }
            .sortedBy { Geo.distanceM(lat, lng, it.lat, it.lng) }
        val ordered = (remaining.mapNotNull { byId[it] } + rest)

        val qualified = triggers.onFix(TriggerState.Fix(lat, lng, atS, speedMps), ordered)

        val nowInside = pack.pois.filter { Geo.isInside(lat, lng, it) }.map { it.id }.toSet()
        val arrived = (nowInside - inside).mapNotNull { byId[it] }
        val departed = (inside - nowInside).mapNotNull { byId[it] }
        for (a in arrived) openVisits[a.id] = atS
        for (d in departed) openVisits.remove(d.id)
        inside.clear()
        inside.addAll(nowInside)
        visited.addAll(arrived.map { it.id })

        val stillRemaining = routeOrder.filter { it in byId && it !in visited }
        val offRoute = routeOrder.isNotEmpty() && stillRemaining.isNotEmpty() &&
            stillRemaining.all { id ->
                val p = byId[id] ?: return@all true
                Geo.distanceM(lat, lng, p.lat, p.lng) > 100.0
            }

        val cards = nowInside.mapNotNull { byId[it] }
        return Update(
            spoke = qualified.mapNotNull { byId[it] },
            cards = cards,
            arrived = arrived,
            departed = departed,
            offRoute = offRoute,
        )
    }

    fun openVisitCount(): Int = openVisits.size
}

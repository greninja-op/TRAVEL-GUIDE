package guide.core

/** Day-plan builder (SPEC §3.2 itineraries): 2h / half-day / full-day.
 *  Picks route POIs fitting the budget at ~9 min per stop + walk time,
 *  skipping closed POIs when hours are known. Pure logic, no clock. */
object Itineraries {
    data class Plan(val routeId: String, val stops: List<String>, val estMinutes: Int)

    private const val MIN_PER_STOP = 9

    fun plan(route: Route, pois: Map<String, Poi>, budgetMin: Int, nowHHMM: String? = null): Plan {
        val stops = mutableListOf<String>()
        var used = 0
        for (id in route.orderedPoiIds) {
            val poi = pois[id] ?: continue
            if (nowHHMM != null && poi.hours != null && !isOpen(poi.hours, nowHHMM)) continue
            if (used + MIN_PER_STOP > budgetMin) break
            stops += id
            used += MIN_PER_STOP
        }
        return Plan(route.id, stops, used)
    }

    fun isOpen(hours: Hours, nowHHMM: String): Boolean =
        if (hours.open <= hours.close) nowHHMM in hours.open..hours.close
        else nowHHMM >= hours.open || nowHHMM <= hours.close // overnight, just in case
}

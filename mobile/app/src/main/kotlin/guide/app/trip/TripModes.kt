package guide.app.trip

/**
 * Phase 1 trip modes. Walk = fixed route order; Wander = nearest-first.
 * Both feed NarrationQueue with the right priority flag.
 */
object TripModes {
    /** Next unvisited POI on the route order, or null when done. */
    fun nextOnRoute(routeOrder: List<String>, visited: Set<String>): String? =
        routeOrder.firstOrNull { it !in visited }

    /** True when the user strayed >strayM from every remaining route stop. */
    fun isOffRoute(
        remaining: List<Pair<String, Double>>,
        strayM: Double = 100.0,
    ): Boolean = remaining.isNotEmpty() && remaining.all { it.second > strayM }

    /** Wander ordering: nearest first by distance metres. */
    fun wanderOrder(nearby: List<Pair<String, Double>>): List<String> =
        nearby.sortedBy { it.second }.map { it.first }
}

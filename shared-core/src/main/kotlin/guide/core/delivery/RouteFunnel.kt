package guide.core.delivery

/**
 * The route funnel — docs/ARCHITECTURE.md §3.3.
 *
 * A wrong guess is survived by a *hierarchy* of plausible paths, not by a wider
 * circle: alternatives share most of their length, so their union costs
 * 1.2–1.6× one route rather than 3×.
 */

enum class RouteLevel(val weight: Double, val lanes: Set<Lane>) {
    /** The route the traveller actually chose. The only level that gets audio. */
    PRIMARY(1.00, setOf(Lane.INDEX, Lane.TEXT, Lane.AUDIO)),

    /** Router alternatives for the same origin → destination. */
    ALTERNATIVE(0.45, setOf(Lane.INDEX, Lane.TEXT)),

    /** Roads leaving the next few junctions. Index only: enough for an instant card. */
    JUNCTION(0.25, setOf(Lane.INDEX)),

    /** The widened net: coarse coverage, cheapest content only. */
    WIDE(0.10, setOf(Lane.INDEX)),
}

data class RouteCandidate(
    val level: RouteLevel,
    val points: List<MapPoint>,
    val bufferM: Double,
    val label: String = level.name,
)

enum class TravelMode { WALKING, DRIVING }

object RouteFunnel {

    /** Corridor half-width: what "the route" means when deciding which cells are in. */
    fun corridorBufferM(mode: TravelMode): Double =
        if (mode == TravelMode.WALKING) 250.0 else 500.0

    /** The last-resort net width (L3). Deliberately coarse. */
    fun wideBufferM(mode: TravelMode): Double =
        if (mode == TravelMode.WALKING) 1_500.0 else 2_500.0

    /** How far a branch is followed past its junction. */
    const val JUNCTION_REACH_M = 400.0

    fun primary(points: List<MapPoint>, mode: TravelMode): RouteCandidate =
        RouteCandidate(RouteLevel.PRIMARY, points, corridorBufferM(mode), "primary")

    fun alternatives(paths: List<List<MapPoint>>, mode: TravelMode): List<RouteCandidate> =
        paths.mapIndexed { i, path ->
            RouteCandidate(RouteLevel.ALTERNATIVE, path, corridorBufferM(mode), "alt-$i")
        }

    fun junctions(branches: List<List<MapPoint>>, mode: TravelMode): List<RouteCandidate> =
        branches.mapIndexed { i, branch ->
            RouteCandidate(RouteLevel.JUNCTION, branch, 120.0, "junction-$i")
        }

    fun wide(points: List<MapPoint>, mode: TravelMode): RouteCandidate =
        RouteCandidate(RouteLevel.WIDE, points, wideBufferM(mode), "net")

    /**
     * Assemble the funnel. Order matters only for readability — the planner
     * scores by level weight, not by list position.
     */
    fun build(
        primary: List<MapPoint>,
        mode: TravelMode,
        alternatives: List<List<MapPoint>> = emptyList(),
        junctions: List<List<MapPoint>> = emptyList(),
        includeWideNet: Boolean = false,
    ): List<RouteCandidate> {
        if (primary.isEmpty()) return emptyList()
        val out = mutableListOf(primary(primary, mode))
        out += alternatives(alternatives.filter { it.size >= 2 }, mode)
        out += junctions(junctions.filter { it.size >= 2 }, mode)
        if (includeWideNet) out += wide(primary, mode)
        return out
    }

    /**
     * Free roam: no route, so predict forward from the heading.
     *
     * A single straight candidate is enough — the planner buffers it anyway,
     * and a wrong prediction here costs a card that arrives from the network,
     * not a blank screen.
     */
    fun freeRoam(position: MapPoint, bearingDeg: Double?, mode: TravelMode): RouteCandidate {
        val reach = if (mode == TravelMode.WALKING) 500.0 else 3_000.0
        val heading = bearingDeg ?: 0.0
        val end = PathMath.destination(position, heading, reach)
        return RouteCandidate(
            level = RouteLevel.PRIMARY,
            points = listOf(position, end),
            bufferM = corridorBufferM(mode),
            label = "roam",
        )
    }

    /** Branches leaving a junction: short stubs in the given bearings. */
    fun branchesFrom(junction: MapPoint, bearingsDeg: List<Double>, reachM: Double = JUNCTION_REACH_M): List<List<MapPoint>> =
        bearingsDeg.map { bearing ->
            listOf(junction, PathMath.destination(junction, bearing, reachM))
        }
}

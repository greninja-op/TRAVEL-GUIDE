package guide.core.delivery

import guide.core.Geo

/**
 * Turns a route funnel into a budgeted fetch plan — docs/ARCHITECTURE.md §3.2–3.3.
 *
 * The output is ranked cells with the lanes each cell needs. The planner never
 * talks to a network and never touches a file, so its behaviour is fully
 * testable.
 */
object PrefetchPlanner {

    data class Limits(
        /** Hard cap on cells in a plan. 800 cells ≈ a full city walk at z16. */
        val maxCells: Int = 800,
        /** Audio is the expensive lane, so it is capped separately. */
        val maxAudioCells: Int = 32,
        /** How far ahead the audio window reaches while walking. */
        val audioWindowWalkingM: Double = 700.0,
        /** Driving needs a longer window; you cover ground faster than you can buffer. */
        val audioWindowDrivingM: Double = 5_000.0,
        /** Everything behind the traveller stays playable — re-listening is free. */
        val behindWindowM: Double = 0.0,
        /** Half-life of the proximity score, in metres. */
        val proximityScaleM: Double = 500.0,
    )

    data class CellScore(
        val cell: Cell,
        val score: Double,
        val lanes: Set<Lane>,
    )

    data class Plan(val cells: List<CellScore>) {
        fun forLane(lane: Lane): List<Cell> = cells.filter { lane in it.lanes }.map { it.cell }
        val index: List<Cell> get() = forLane(Lane.INDEX)
        val text: List<Cell> get() = forLane(Lane.TEXT)
        val audio: List<Cell> get() = forLane(Lane.AUDIO)
        val isEmpty: Boolean get() = cells.isEmpty()
    }

    fun plan(
        candidates: List<RouteCandidate>,
        position: MapPoint?,
        mode: TravelMode,
        limits: Limits = Limits(),
    ): Plan {
        if (candidates.isEmpty()) return Plan(emptyList())

        // Primary first: its cells are the ones that can earn the audio lane.
        val ordered = candidates.sortedByDescending { it.level.weight }
        val primary = ordered.firstOrNull { it.level == RouteLevel.PRIMARY }
        val alongM = if (primary != null && position != null) {
            PathMath.project(primary.points, position).distanceAlongM
        } else {
            null
        }
        val audioWindowM = when (mode) {
            TravelMode.WALKING -> limits.audioWindowWalkingM
            TravelMode.DRIVING -> limits.audioWindowDrivingM
        }

        val accumulated = LinkedHashMap<Cell, MutableCell>()

        for (candidate in ordered) {
            val cells = Cells.cellsAlong(candidate.points, candidate.bufferM)
            for (cell in cells) {
                val entry = accumulated.getOrPut(cell) { MutableCell(cell) }
                entry.lanes += candidate.level.lanes
                entry.score += candidate.level.weight * proximityWeight(cell, position, limits.proximityScaleM)

                if (candidate.level == RouteLevel.PRIMARY && Lane.AUDIO in entry.lanes) {
                    if (!withinAudioWindow(candidate, cell, alongM, audioWindowM, limits.behindWindowM)) {
                        entry.lanes -= Lane.AUDIO
                    }
                }
            }
        }

        val ranked = accumulated.values
            .map { CellScore(it.cell, it.score, it.lanes.toSet()) }
            .sortedWith(compareByDescending<CellScore> { it.score }.thenBy { it.cell.key })
            .take(limits.maxCells)

        val keepAudio = ranked
            .filter { Lane.AUDIO in it.lanes }
            .take(limits.maxAudioCells)
            .map { it.cell }
            .toSet()

        val trimmed = ranked.map { cellScore ->
            if (Lane.AUDIO in cellScore.lanes && cellScore.cell !in keepAudio) {
                cellScore.copy(lanes = cellScore.lanes - Lane.AUDIO)
            } else {
                cellScore
            }
        }
        return Plan(trimmed)
    }

    /**
     * Is this cell close enough along the route to deserve audio now?
     *
     * Everything behind counts (it is already paid for and re-listening is
     * free); ahead stops at the window. With no fix yet, the first window's
     * worth from the start of the route is what gets warmed.
     */
    private fun withinAudioWindow(
        primary: RouteCandidate,
        cell: Cell,
        alongM: Double?,
        windowM: Double,
        behindM: Double,
    ): Boolean {
        val cellAlong = PathMath.project(primary.points, Cells.center(cell)).distanceAlongM
        return if (alongM == null) {
            cellAlong <= windowM
        } else {
            cellAlong <= alongM + windowM && cellAlong >= alongM - windowM - behindM
        }
    }

    private fun proximityWeight(cell: Cell, position: MapPoint?, scaleM: Double): Double {
        if (position == null) return 1.0
        val c = Cells.center(cell)
        val distance = Geo.distanceM(position.lat, position.lng, c.lat, c.lng)
        return 1.0 / (1.0 + distance / scaleM)
    }

    private class MutableCell(val cell: Cell) {
        var score: Double = 0.0
        val lanes: MutableSet<Lane> = mutableSetOf()
    }
}

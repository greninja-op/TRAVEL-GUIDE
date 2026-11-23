package guide.core.delivery

/**
 * Watches the actual path instead of trusting the plan — docs/ARCHITECTURE.md §3.4.
 *
 * Two rules make this usable on a phone rather than a simulator:
 *  - it only counts as off-route while *moving* (a fix indoors is not a turn),
 *  - it debounces replans, so a jittery fix cannot cause a fetch storm.
 */
class DeviationMonitor(
    private val enterOffsetM: Double = 80.0,
    private val exitOffsetM: Double = 50.0,
    private val enterAfterMs: Long = 15_000L,
    private val minReplanGapMs: Long = 60_000L,
    private val promoteAfterMs: Long = 120_000L,
    private val minSpeedMps: Double = 1.0,
) {

    enum class State {
        ON_ROUTE,
        /** Sustained offset: replan from the real position. */
        OFF_ROUTE,
        /** Offset persisted: the observed path becomes the new primary route. */
        PROMOTED,
    }

    data class Reading(
        val state: State,
        val offsetM: Double,
        /** The caller should plan a fresh corridor from the current position. */
        val replan: Boolean,
        /** The caller should make the observed path the new primary route. */
        val promote: Boolean,
    )

    private var state = State.ON_ROUTE
    /** -1 means "no deviation in progress"; 0 is a legal clock value. */
    private var offStartedAtMs = -1L
    private var lastReplanAtMs = Long.MIN_VALUE / 2

    fun reset() {
        state = State.ON_ROUTE
        offStartedAtMs = -1L
        lastReplanAtMs = Long.MIN_VALUE / 2
    }

    val currentState: State get() = state

    fun reading(
        position: MapPoint,
        path: List<MapPoint>,
        speedMps: Double,
        nowMs: Long,
    ): Reading {
        if (path.size < 2) return Reading(state, Double.MAX_VALUE, false, false)

        val offset = PathMath.project(path, position).offsetM

        // Standing still proves nothing: a bad fix at a red light is not a turn.
        if (speedMps < minSpeedMps) {
            return Reading(state, offset, false, false)
        }

        return when (state) {
            State.ON_ROUTE -> {
                if (offset > enterOffsetM) {
                    if (offStartedAtMs < 0L) offStartedAtMs = nowMs
                    val sustained = nowMs - offStartedAtMs >= enterAfterMs
                    val quiet = nowMs - lastReplanAtMs >= minReplanGapMs
                    if (sustained && quiet) {
                        state = State.OFF_ROUTE
                        lastReplanAtMs = nowMs
                        Reading(state, offset, replan = true, promote = false)
                    } else {
                        Reading(state, offset, false, false)
                    }
                } else {
                    offStartedAtMs = -1L
                    Reading(state, offset, false, false)
                }
            }

            State.OFF_ROUTE, State.PROMOTED -> {
                if (offset <= exitOffsetM) {
                    state = State.ON_ROUTE
                    offStartedAtMs = -1L
                    return Reading(state, offset, false, false)
                }
                if (state == State.OFF_ROUTE && nowMs - offStartedAtMs >= promoteAfterMs) {
                    state = State.PROMOTED
                    lastReplanAtMs = nowMs
                    return Reading(state, offset, replan = true, promote = true)
                }
                if (nowMs - lastReplanAtMs >= minReplanGapMs) {
                    lastReplanAtMs = nowMs
                    return Reading(state, offset, replan = true, promote = false)
                }
                Reading(state, offset, false, false)
            }
        }
    }
}

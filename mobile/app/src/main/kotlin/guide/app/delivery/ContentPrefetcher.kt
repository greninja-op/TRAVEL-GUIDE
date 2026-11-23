package guide.app.delivery

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import guide.core.Geo
import guide.core.delivery.BundleStore
import guide.core.delivery.ContentSource
import guide.core.delivery.DeviationMonitor
import guide.core.delivery.EventRecord
import guide.core.delivery.MapPoint
import guide.core.delivery.PrefetchCoordinator
import guide.core.delivery.PrefetchPlanner
import guide.core.delivery.RouteCandidate
import guide.core.delivery.RouteFunnel
import guide.core.delivery.TravelMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The phone-side prefetch loop — docs/ARCHITECTURE.md §3.
 *
 * Every fix answers one question: "what is the traveller about to reach, and is
 * it already here?" If yes, nothing happens. If not, one bounded request brings
 * the next slice in, on an IO thread, never on the trigger path.
 *
 * This class owns no UI. It exposes snapshot state so a screen can show
 * "offline guide ready · 2.1 MB" without reaching into the engine.
 */
class ContentPrefetcher(
    context: Context,
    private val source: ContentSource,
    private val language: String,
    private val mode: TravelMode = TravelMode.WALKING,
    private val routeProvider: RouteProvider = NoRouteProvider,
    storeRoot: File = File(context.filesDir, "content"),
) {

    private val store = BundleStore(storeRoot)
    private val coordinator = PrefetchCoordinator(store, source, language)
    private val deviation = DeviationMonitor()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val running = AtomicBoolean(false)

    private var route: List<MapPoint> = emptyList()
    private var alternatives: List<List<MapPoint>> = emptyList()
    private var junctions: List<List<MapPoint>> = emptyList()
    private var lastFix: MapPoint? = null
    private var lastPlanAtMs = 0L
    private var lastEventsAtMs = 0L

    /** Live events for the current corridor, refreshed on a TTL (never invented). */
    var liveEvents by mutableStateOf<List<EventRecord>>(emptyList())
        private set

    var lastSyncedAtMs by mutableStateOf(0L)
        private set

    var plannedCells by mutableStateOf(0)
        private set

    var cachedBytes by mutableStateOf(0L)
        private set

    /**
     * Hand the active route (in-app walk or Google Maps companion path).
     * Cheap to call on every fix: an unchanged route is ignored.
     */
    fun setRoute(points: List<MapPoint>, nowMs: Long = System.currentTimeMillis()) {
        val next = if (points.size >= 2) points else emptyList()
        if (next == route) return
        route = next
        alternatives = emptyList()
        junctions = if (next.isEmpty()) emptyList() else RouteProvider.junctionBranches(next)
        lastPlanAtMs = 0L
        lastFix?.let { refresh(it, null, nowMs, promoted = false) }
    }

    fun onFix(
        lat: Double,
        lng: Double,
        speedMps: Double,
        bearingDeg: Double? = null,
        nowMs: Long = System.currentTimeMillis(),
    ) {
        val position = MapPoint(lat, lng)
        val previous = lastFix
        lastFix = position

        val reading = if (route.size >= 2) {
            deviation.reading(position, route, speedMps, nowMs)
        } else {
            null
        }

        val movedFar = previous == null ||
            Geo.distanceM(previous.lat, previous.lng, lat, lng) >= MIN_MOVE_M
        val due = nowMs - lastPlanAtMs >= MAX_QUIET_MS

        if (!movedFar && !due && reading?.replan != true) return
        if (!running.compareAndSet(false, true)) return

        lastPlanAtMs = nowMs
        refresh(position, bearingDeg, nowMs, promoted = reading?.promote == true)
    }

    private fun refresh(position: MapPoint, bearing: Double?, nowMs: Long, promoted: Boolean) {
        scope.launch {
            try {
                if (route.size >= 2 && !promoted && alternatives.isEmpty()) {
                    alternatives = runCatching {
                        routeProvider.alternatives(route.first(), route.last(), mode)
                    }.getOrDefault(emptyList())
                }

                val candidates: List<RouteCandidate> = when {
                    // Off-route for good, or no route at all: predict forward.
                    promoted || route.size < 2 -> listOf(RouteFunnel.freeRoam(position, bearing, mode))
                    else -> RouteFunnel.build(
                        route,
                        mode,
                        alternatives = alternatives,
                        junctions = junctions,
                    )
                }

                val plan = PrefetchPlanner.plan(candidates, position, mode)
                plannedCells = plan.cells.size

                coordinator.run(plan, nowMs)

                cachedBytes = store.totalBytes()
                lastSyncedAtMs = nowMs
                maybeFetchEvents(plan, nowMs)
            } catch (_: Exception) {
                // Prefetch is an optimisation; a failure must never surface as a crash
                // or block narration.
            } finally {
                running.set(false)
            }
        }
    }

    /**
     * Events are fetched early for the whole corridor, then held on a TTL so
     * "tomorrow" cannot silently become stale.
     */
    private fun maybeFetchEvents(plan: PrefetchPlanner.Plan, nowMs: Long) {
        if (plan.index.isEmpty()) return
        if (nowMs - lastEventsAtMs < EVENTS_TTL_MS) return

        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val cells = plan.index.take(MAX_EVENT_CELLS)

        liveEvents = runCatching { source.events(cells, today, tomorrow) }.getOrDefault(emptyList())
        lastEventsAtMs = nowMs
    }

    fun shutdown() {
        scope.cancel()
    }

    private companion object {
        /** Re-plan only after real movement; a jittery fix is not progress. */
        const val MIN_MOVE_M = 50.0

        /** Even standing still, refresh at most this often (events, corrections). */
        const val MAX_QUIET_MS = 45_000L

        const val EVENTS_TTL_MS = 15 * 60 * 1000L
        const val MAX_EVENT_CELLS = 200
    }
}

package guide.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import guide.app.map.MapPins
import guide.app.ui.NearbyRow
import guide.app.ui.PackRow
import guide.app.ui.Trio
import guide.app.ui.VisitRow
import guide.core.Geo
import guide.core.Itineraries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The one place that turns on-device data into what the screens render.
 *
 * Before this existed, `MainActivity` passed `emptyList()` / `{}` to every
 * screen, so the UI could only ever show its empty states — the engine was
 * built and tested but nothing read from it. This class is the seam.
 *
 * Design choices worth stating:
 *  - It holds **Compose state** (`mutableStateOf`) rather than exposing Flows:
 *    the app has no ViewModel layer and adding one now would be a bigger change
 *    than this wiring task warrants. State that genuinely needs to survive a
 *    process restart lives in Room, not here.
 *  - It reads the pack **once** at construction from assets (a bounded, small
 *    JSON), so screens never do I/O in composition.
 *  - It owns the Room database, because visits and notes are the only durable
 *    user data the app has (SPEC §1.5: they stay on the device).
 */
class AppState(context: Context) {

    private val db: GuideDb = Room.databaseBuilder(
        context.applicationContext, GuideDb::class.java, "guide.db",
    ).build()

    private val scope = CoroutineScope(Dispatchers.IO)

    // ---- Pack (the content itself) ----------------------------------------
    val packVersion: String
    val cards: List<PackLoader.PoiCard>
    private val byId: Map<String, PackLoader.PoiCard>
    private val order: List<String>

    // ---- UI state ----------------------------------------------------------
    /** Visits from Room, newest first. */
    var visits by mutableStateOf<List<VisitRow>>(emptyList())
        private set

    /** Notes keyed by POI id — merged into [visits] as they load. */
    private var notes: Map<String, String> = emptyMap()

    /**
     * Distance-sorted POIs for the Nearby list. Sorted by real distance when
     * there is a fix; falls back to the pack's own order (the walking route)
     * so the list is never empty just because location is not yet available.
     */
    var nearby by mutableStateOf<List<NearbyRow>>(emptyList())
        private set

    /** The last fix, or null when we have none yet. */
    var lastLat: Double? = null
        private set
    var lastLng: Double? = null
        private set

    /** POI ids the user has already heard — drives visited pin state. */
    var visitedIds by mutableStateOf<Set<String>>(emptySet())
        private set

    /** Pack inventory for the offline-packs screen. */
    val packs: List<PackRow>

    /** Routes from the pack, as the Routes screen wants them. */
    val routes: List<Trio>

    init {
        val (version, loaded) = PackLoader.load(context)
        packVersion = version
        cards = loaded
        byId = loaded.associateBy { it.id }

        // The Heritage Loop is the pack's primary walking route; its stop order
        // is what "next stop" and the distance fallback both follow.
        order = loaded.map { it.id }

        val sizeBytes = runCatching {
            context.assets.open("packs/fort-kochi-walk-v1.json").available().toLong()
        }.getOrDefault(0L)
        packs = listOf(
            PackRow(
                cityId = "Fort Kochi & Mattancherry",
                version = version,
                bytes = sizeBytes,
                bundled = true,
            ),
        )

        routes = listOf(
            Trio(
                name = "Fort Kochi Heritage Loop",
                detail = "${loaded.size} stops · about 2.5 hours on foot",
            ),
        )

        refreshNearby()
        loadVisits()
    }

    /** Resolve a POI card by id — used by the detail screen and the sheet. */
    fun card(id: String): PackLoader.PoiCard? = byId[id]

    /** Display name for a POI id, falling back to the id so nothing renders blank. */
    fun name(id: String): String = byId[id]?.name ?: id

    /** Pins for the map, in pack order, with visited state applied. */
    fun pins(): List<MapPins.Pin> = order.mapNotNull { id ->
        byId[id]?.let { c ->
            MapPins.Pin(
                id = c.id, name = c.name, lat = c.lat, lng = c.lng,
                visited = c.id in visitedIds,
            )
        }
    }

    /** Route polyline: the pack order, as coordinates. */
    fun routePoints(): List<org.maplibre.android.geometry.LatLng> =
        order.mapNotNull { id -> byId[id]?.let { org.maplibre.android.geometry.LatLng(it.lat, it.lng) } }

    // ---- Location ----------------------------------------------------------

    fun onFix(lat: Double, lng: Double) {
        lastLat = lat
        lastLng = lng
        refreshNearby()
        updateCompanionCorridor()
    }

    private fun refreshNearby() {
        val lat = lastLat
        val lng = lastLng
        val sorted = if (lat != null && lng != null) {
            cards.sortedBy { Geo.distanceM(lat, lng, it.lat, it.lng) }
        } else {
            cards
        }
        nearby = sorted.map { c ->
            val detail = if (lat != null && lng != null) {
                val d = Geo.distanceM(lat, lng, c.lat, c.lng)
                when {
                    d < 1000 -> "${d.toInt()} m away"
                    else -> String.format(Locale.US, "%.1f km away", d / 1000)
                }
            } else {
                "Distance unknown — no GPS fix yet"
            }
            NearbyRow(id = c.id, name = c.name, detail = detail, layer = c.layer)
        }
    }

    // ---- Google Maps Navigation Companion -----------------------------------

    /**
     * Compute corridor POIs between user location and destination when an external
     * Google Maps navigation session is active.
     */
    fun updateCompanionCorridor() {
        val session = guide.app.navigation.MapsCompanionState.currentSession ?: return
        val originLat = lastLat ?: 9.9656
        val originLng = lastLng ?: 76.2423

        // Resolve destination coordinates: either explicit lat/lng or best-match POI card
        val destCoords = if (session.destinationLat != null && session.destinationLng != null) {
            Pair(session.destinationLat, session.destinationLng)
        } else {
            val matchedCard = cards.find {
                it.name.contains(session.destinationName, ignoreCase = true) ||
                    session.destinationName.contains(it.name, ignoreCase = true)
            } ?: cards.firstOrNull()
            matchedCard?.let { Pair(it.lat, it.lng) }
        }

        if (destCoords != null) {
            guide.app.navigation.MapsCompanionState.updateCorridorPois(
                originLat = originLat,
                originLng = originLng,
                destLat = destCoords.first,
                destLng = destCoords.second,
                allPois = cards.map {
                    guide.app.navigation.CorridorCandidate(it.id, it.name, it.lat, it.lng)
                },
                maxCrossTrackM = 500.0,
            )
        }
    }

    /** POIs identified along the current Google Maps navigation route corridor. */
    val corridorPois: List<PackLoader.PoiCard>
        get() = cards.filter { it.id in guide.app.navigation.MapsCompanionState.corridorPoiIds }

    /**
     * Test / demo simulator for the Google Maps Companion feature.
     * Activates a simulated route to a chosen destination so the user can test the
     * pre-loaded corridor stories without leaving their desk.
     */
    fun simulateCompanionSession(destinationPoiId: String) {
        val target = card(destinationPoiId) ?: cards.firstOrNull() ?: return
        guide.app.navigation.MapsCompanionState.onNavStarted(
            destinationName = target.name,
            destinationLat = target.lat,
            destinationLng = target.lng,
            etaOrDistance = "15 min (3.8 km) · Google Maps",
            nextManeuver = "In 350m turn right onto River Road",
            source = guide.app.navigation.CompanionSource.SIMULATED,
        )
        updateCompanionCorridor()
    }

    fun clearCompanionSession() {
        guide.app.navigation.MapsCompanionState.onNavEnded()
    }

    // ---- Visits & notes ----------------------------------------------------

    fun loadVisits() {
        scope.launch {
            val raw = db.dao().visits()
            val n = raw.mapNotNull { v -> db.dao().note(v.poiId)?.let { it.poiId to it.text } }.toMap()
            notes = n
            visitedIds = raw.map { it.poiId }.toSet()
            visits = raw.map { v ->
                VisitRow(
                    poiId = v.poiId,
                    name = name(v.poiId),
                    whenText = formatWhen(v.arrivedAt),
                    note = n[v.poiId],
                )
            }
        }
    }

    fun saveNote(poiId: String, text: String) {
        scope.launch {
            db.dao().upsertNote(
                NoteEntity(poiId = poiId, text = text, updatedAt = System.currentTimeMillis()),
            )
            loadVisits()
        }
    }

    private fun formatWhen(epochSeconds: Long): String {
        // Visits are stored in epoch SECONDS (the engine's time base), not ms.
        val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
        return fmt.format(Date(epochSeconds * 1000))
    }
}

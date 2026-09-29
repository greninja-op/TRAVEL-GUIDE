package guide.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.room.Room
import guide.app.location.GuideService
import guide.app.map.MapPins
import guide.app.ui.NearbyRow
import guide.app.ui.PackRow
import guide.app.ui.Trio
import guide.app.ui.VisitRow
import guide.core.Geo
import guide.core.Itineraries
import guide.app.security.CryptoVault
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import guide.app.extras.Events
import guide.app.extras.LocalEvent

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
class AppState(private val context: Context) {

    private val db: GuideDb = GuideDb.getInstance(context)

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

    /** Notes keyed by POI id — personal reflections and photos. */
    var poiNotes by mutableStateOf<Map<String, PoiNote>>(emptyMap())
        private set
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

    /** Cached pins for the map to prevent spurious recomposition loops. */
    var cachedPins by mutableStateOf<List<MapPins.Pin>>(emptyList())
        private set

    /** Active cultural events and seasonal festivals for Fort Kochi. */
    val allEvents: List<LocalEvent>
    var activeEvents by mutableStateOf<List<LocalEvent>>(emptyList())
        private set

    fun eventForPoi(poiId: String): LocalEvent? =
        activeEvents.find { it.poiId == poiId }

    /** Active UI and Spoken Voice Language. */
    var appLanguage by mutableStateOf(
        AppLanguage.fromCode(
            context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
                .getString("app_language", AppLanguage.ENGLISH.code)
        )
    )
        private set

    fun setLanguage(lang: AppLanguage) {
        appLanguage = lang
        context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", lang.code)
            .apply()

        // Update GuideService & Sarvam Indic language in real-time
        GuideService.setLanguage(context, lang.code)
        refreshNearby()
    }

    /** Pack inventory for the offline-packs screen. */
    val packs: List<PackRow>

    /** Routes from the pack, as the Routes screen wants them. */
    val routes: List<Trio>

    private val cachedRoutePoints: List<com.google.android.gms.maps.model.LatLng>

    init {
        val (version, loaded) = PackLoader.load(context)
        packVersion = version
        cards = loaded
        byId = loaded.associateBy { it.id }

        // The Heritage Loop is the pack's primary walking route; its stop order
        // is what "next stop" and the distance fallback both follow.
        order = loaded.map { it.id }
        cachedRoutePoints = guide.app.map.RoadGeometry.HERITAGE_LOOP

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

        allEvents = Events.loadFromAssets(context)
        activeEvents = Events.forDate(allEvents, java.time.LocalDate.now().toString())

        recomputePins()
        refreshNearby()
        loadVisits()

        guide.app.navigation.MapsCompanionState.onNavStateChanged = {
            updateCompanionCorridor()
        }
    }

    private fun recomputePins() {
        val eventPoiIds = activeEvents.map { it.poiId }.toSet()
        cachedPins = order.mapNotNull { id ->
            byId[id]?.let { c ->
                MapPins.Pin(
                    id = c.id, name = c.name, lat = c.lat, lng = c.lng,
                    visited = c.id in visitedIds,
                    hasEvent = c.id in eventPoiIds,
                )
            }
        }
    }

    /** Resolve a POI card by id — used by the detail screen and the sheet. */
    fun card(id: String): PackLoader.PoiCard? = byId[id]

    /** Display name for a POI id, falling back to the id so nothing renders blank. */
    fun name(id: String): String = byId[id]?.name ?: id

    /** Pins for the map, in pack order, with visited state applied (stable reference). */
    fun pins(): List<MapPins.Pin> = cachedPins

    /** Whether the user manually started a walking tour route without Google Maps. */
    var isManualRouteActive by mutableStateOf(false)

    /** Dynamically fetched road-snapped polyline coordinates hugging real streets. */
    var activeRoutePoints by mutableStateOf<List<com.google.android.gms.maps.model.LatLng>>(emptyList())
        private set

    /**
     * Route polyline: shown strictly while active navigation is underway
     * (either from Google Maps companion session or a manual walking plan).
     * When navigation is canceled or stopped, this returns emptyList() so the route
     * polyline disappears, while preserving all map pins, GPS location, and places!
     */
    fun routePoints(): List<com.google.android.gms.maps.model.LatLng> {
        val hasActiveNav = guide.app.navigation.MapsCompanionState.currentSession != null
        return when {
            hasActiveNav -> activeRoutePoints
            isManualRouteActive -> cachedRoutePoints
            else -> emptyList()
        }
    }

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
                    d < 1000 -> AppStrings.distanceMeters(appLanguage, d.toInt())
                    else -> AppStrings.distanceKm(appLanguage, String.format(Locale.US, "%.1f", d / 1000))
                }
            } else {
                AppStrings.distanceGpsWaiting(appLanguage)
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
        val session = guide.app.navigation.MapsCompanionState.currentSession
        if (session == null) {
            activeRoutePoints = emptyList()
            return
        }
        val originLat = lastLat ?: 9.9675
        val originLng = lastLng ?: 76.2442

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
            val origin = com.google.android.gms.maps.model.LatLng(originLat, originLng)
            val dest = com.google.android.gms.maps.model.LatLng(destCoords.first, destCoords.second)

            scope.launch {
                val routeResult = guide.app.map.RoadRouter.fetchRoute(origin, dest, mode = "driving")
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    activeRoutePoints = routeResult.points

                    // Filter corridor POIs: only places that lie within 350m of the actual road line segments
                    val matchedIds = cards.filter { card ->
                        guide.app.map.RoadRouter.isPoiAlongRoad(
                            poiLat = card.lat,
                            poiLng = card.lng,
                            roadPolyline = routeResult.points,
                            thresholdMeters = 350.0,
                        )
                    }.map { it.id }.toSet()

                    guide.app.navigation.MapsCompanionState.setCorridorPois(matchedIds)
                }
            }
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
    fun simulateCompanionSession(destinationPoiId: String, customName: String? = null) {
        val target = card(destinationPoiId)
        val name = customName ?: target?.name ?: "Destination from Google Maps"
        val lat = target?.lat ?: lastLat ?: 9.9656
        val lng = target?.lng ?: lastLng ?: 76.2423
        guide.app.navigation.MapsCompanionState.onNavStarted(
            destinationName = name,
            destinationLat = lat,
            destinationLng = lng,
            etaOrDistance = "12 min (3.2 km) · Google Maps",
            nextManeuver = "In 250m turn right onto Bazaar Road",
            source = guide.app.navigation.CompanionSource.SIMULATED,
        )
        updateCompanionCorridor()
    }

    fun clearCompanionSession() {
        guide.app.navigation.MapsCompanionState.onNavEnded()
        activeRoutePoints = emptyList()
    }

    fun clearAllData() {
        scope.launch {
            db.clearAllTables()
            loadVisits()
        }
    }

    // ---- Visits & notes ----------------------------------------------------

    fun loadVisits() {
        scope.launch {
            val raw = db.dao().visits()
            val allNotesList = db.dao().allNotes()
            val notesMap = allNotesList.associate { entity ->
                entity.poiId to PoiNote(
                    poiId = entity.poiId,
                    text = CryptoVault.decrypt(entity.text),
                    photoUri = entity.photoUri,
                    updatedAt = entity.updatedAt,
                )
            }
            poiNotes = notesMap
            notes = notesMap.mapValues { it.value.text }
            visitedIds = raw.map { it.poiId }.toSet()
            recomputePins()
            visits = raw.map { v ->
                VisitRow(
                    poiId = v.poiId,
                    name = name(v.poiId),
                    whenText = formatWhen(v.arrivedAt),
                    note = notesMap[v.poiId]?.text,
                    photoUri = notesMap[v.poiId]?.photoUri,
                )
            }
        }
    }

    fun getNote(poiId: String): PoiNote? = poiNotes[poiId]

    fun saveNote(poiId: String, text: String, photoUri: String? = null) {
        scope.launch {
            val encryptedText = CryptoVault.encrypt(text)
            db.dao().upsertNote(
                NoteEntity(
                    poiId = poiId,
                    text = encryptedText,
                    photoUri = photoUri ?: poiNotes[poiId]?.photoUri,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            loadVisits()
        }
    }

    fun deleteNote(poiId: String) {
        scope.launch {
            db.dao().deleteNote(poiId)
            loadVisits()
        }
    }

    private fun formatWhen(epochSeconds: Long): String {
        // Visits are stored in epoch SECONDS (the engine's time base), not ms.
        val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
        return fmt.format(Date(epochSeconds * 1000))
    }
}

/** Traveler note attached to a specific POI with optional photo and timestamp. */
data class PoiNote(
    val poiId: String,
    val text: String,
    val photoUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
)

package guide.core.delivery

/**
 * The network seam — docs/ARCHITECTURE.md §5.
 *
 * The engine never holds a socket: the Android layer implements [ContentSource]
 * over HTTP, and [FixtureContentSource] implements it over an in-memory table so
 * the corridor → request → store loop can be tested end to end on the JVM.
 */
data class EventRecord(
    val id: String,
    val poiId: String?,
    val title: String,
    /** ISO-8601 date (`2026-11-23`) or an ISO instant when the time is known. */
    val startsAt: String,
    val endsAt: String? = null,
    val sourceUrl: String? = null,
    val fetchedAtMs: Long = 0L,
)

data class PrefetchRequest(
    val cells: List<Cell>,
    val language: String,
    val voice: String,
    /** Cells whose audio is wanted now (the rolling window). */
    val wantAudio: Set<Cell> = emptySet(),
    /** `poiId/lane` → content hash the phone already holds. */
    val have: Map<String, String> = emptyMap(),
)

data class PrefetchItem(
    val poiId: String,
    val cell: Cell,
    val hash: String,
    val language: String,
    /** Lanes still missing on the phone for this POI. */
    val lanes: Set<Lane>,
    val textUrl: String? = null,
    val audioUrl: String? = null,
)

data class PrefetchResponse(
    val items: List<PrefetchItem> = emptyList(),
    val events: List<EventRecord> = emptyList(),
    val serverTimeMs: Long = 0L,
)

interface ContentSource {
    fun prefetch(request: PrefetchRequest): PrefetchResponse

    /** Live events for a set of cells — one call for a whole route. */
    fun events(cells: List<Cell>, fromIso: String, toIso: String): List<EventRecord>

    fun fetchText(url: String): ByteArray
    fun fetchAudio(url: String): ByteArray
}

/**
 * Deterministic in-memory source.
 *
 * Every cell "contains" [poisPerCell] POIs whose hash is stable per cell, so a
 * test can assert that a second pass fetches nothing.
 */
class FixtureContentSource(
    private val poisPerCell: Int = 2,
    private val textBytesPerPoi: Int = 120,
    private val audioBytesPerPoi: Int = 400,
) : ContentSource {

    private val bundles = HashMap<String, ByteArray>()

    override fun prefetch(request: PrefetchRequest): PrefetchResponse {
        val items = LinkedHashMap<String, PrefetchItem>()
        for (cell in request.cells) {
            for (i in 1..poisPerCell) {
                val poiId = "poi-${cell.x}-${cell.y}-$i"
                val hash = "h${cell.z}${cell.x}x${cell.y}n$i"

                val wanted = linkedSetOf<Lane>()
                if (request.have["$poiId/${Lane.TEXT}"] != hash) wanted += Lane.TEXT
                if (cell in request.wantAudio && request.have["$poiId/${Lane.AUDIO}"] != hash) {
                    wanted += Lane.AUDIO
                }
                if (wanted.isEmpty()) continue

                val textUrl = if (Lane.TEXT in wanted) "https://cdn.test/text/$hash.json" else null
                val audioUrl = if (Lane.AUDIO in wanted) "https://cdn.test/audio/$hash.opus" else null
                textUrl?.let { bundles.putIfAbsent(it, ByteArray(textBytesPerPoi)) }
                audioUrl?.let { bundles.putIfAbsent(it, ByteArray(audioBytesPerPoi)) }

                items[poiId] = PrefetchItem(
                    poiId = poiId,
                    cell = cell,
                    hash = hash,
                    language = request.language,
                    lanes = wanted,
                    textUrl = textUrl,
                    audioUrl = audioUrl,
                )
            }
        }
        return PrefetchResponse(items = items.values.toList(), serverTimeMs = 1_000L)
    }

    override fun events(cells: List<Cell>, fromIso: String, toIso: String): List<EventRecord> =
        cells.take(1).map {
            EventRecord(
                id = "event-${it.x}-${it.y}",
                poiId = null,
                title = "Parish fete",
                startsAt = fromIso,
                endsAt = toIso,
                sourceUrl = "https://events.test/${it.key}",
                fetchedAtMs = 1_000L,
            )
        }

    override fun fetchText(url: String): ByteArray = bundles[url] ?: ByteArray(0)
    override fun fetchAudio(url: String): ByteArray = bundles[url] ?: ByteArray(0)
}

/**
 * Drives one plan to completion: ask only for what is missing, then store what
 * came back.
 *
 * This is the whole content path minus the socket and the UI — which is what
 * makes "nothing on the trigger path waits on the network" checkable in a test.
 */
class PrefetchCoordinator(
    private val store: BundleStore,
    private val source: ContentSource,
    private val language: String,
    private val voice: String = "",
) {

    data class Result(
        val requestedCells: Int,
        val itemsOffered: Int,
        val textWritten: Int,
        val audioWritten: Int,
        val bytesWritten: Long,
        val events: List<EventRecord>,
    )

    fun run(plan: PrefetchPlanner.Plan, nowMs: Long): Result {
        // One request covers every cell in the plan. The server filters by the
        // per-lane hashes we already hold, so a warm corridor costs a few bytes.
        val cells = (plan.index + plan.text + plan.audio).distinct()
        val audioCells = plan.audio.toSet()

        val response = source.prefetch(
            PrefetchRequest(
                cells = cells,
                language = language,
                voice = voice,
                wantAudio = audioCells,
                have = store.knownHashes(),
            ),
        )

        var textWritten = 0
        var audioWritten = 0
        var bytes = 0L

        for (item in response.items) {
            if (Lane.TEXT in item.lanes && item.textUrl != null &&
                !store.has(item.poiId, item.hash, Lane.TEXT, language)
            ) {
                val payload = source.fetchText(item.textUrl)
                if (payload.isNotEmpty()) {
                    store.put(item.poiId, item.hash, Lane.TEXT, language, payload, nowMs)
                    textWritten++
                    bytes += payload.size
                }
            }
            if (Lane.AUDIO in item.lanes && item.audioUrl != null && item.cell in audioCells &&
                !store.has(item.poiId, item.hash, Lane.AUDIO, language, voice)
            ) {
                val payload = source.fetchAudio(item.audioUrl)
                if (payload.isNotEmpty()) {
                    store.put(item.poiId, item.hash, Lane.AUDIO, language, payload, nowMs, voice = voice)
                    audioWritten++
                    bytes += payload.size
                }
            }
        }

        return Result(
            requestedCells = cells.size,
            itemsOffered = response.items.size,
            textWritten = textWritten,
            audioWritten = audioWritten,
            bytesWritten = bytes,
            events = response.events,
        )
    }
}

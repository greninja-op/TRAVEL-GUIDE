package guide.app.delivery

import guide.core.delivery.Cell
import guide.core.delivery.ContentSource
import guide.core.delivery.EventRecord
import guide.core.delivery.Lane
import guide.core.delivery.PrefetchItem
import guide.core.delivery.PrefetchRequest
import guide.core.delivery.PrefetchResponse
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * HTTP implementation of the content contract in docs/ARCHITECTURE.md §5.
 *
 * The engine supplies the corridor and the hashes it already holds; this class
 * only moves bytes. Nothing here is called from the trigger path — it runs on
 * an IO dispatcher ahead of the traveller.
 */
class HttpContentSource(
    baseUrl: String,
    private val apiKey: String? = null,
) : ContentSource {

    private val base = baseUrl.trim().trimEnd('/')

    override fun prefetch(request: PrefetchRequest): PrefetchResponse {
        if (base.isEmpty()) return PrefetchResponse()

        val payload = JSONObject().apply {
            put("cells", JSONArray(request.cells.map { it.key }))
            put("language", request.language)
            put("voice", request.voice)
            put("wantAudio", JSONArray(request.wantAudio.map { it.key }))
            put("have", JSONObject(request.have))
        }

        val root = postJson("$base/v1/prefetch", payload.toString()) ?: return PrefetchResponse()

        val items = mutableListOf<PrefetchItem>()
        val itemsArray = root.optJSONArray("items") ?: JSONArray()
        for (i in 0 until itemsArray.length()) {
            val o = itemsArray.optJSONObject(i) ?: continue
            val cell = Cell.parse(o.optString("cell")) ?: continue
            val lanes = mutableSetOf<Lane>()
            val laneArray = o.optJSONArray("lanes") ?: JSONArray()
            for (j in 0 until laneArray.length()) {
                when (laneArray.optString(j).uppercase()) {
                    "INDEX" -> lanes += Lane.INDEX
                    "TEXT" -> lanes += Lane.TEXT
                    "AUDIO" -> lanes += Lane.AUDIO
                }
            }
            val poiId = o.optString("poiId").ifBlank { o.optString("poi_id") }
            val hash = o.optString("hash")
            if (poiId.isBlank() || hash.isBlank() || lanes.isEmpty()) continue
            items += PrefetchItem(
                poiId = poiId,
                cell = cell,
                hash = hash,
                language = o.optString("language", request.language),
                lanes = lanes,
                textUrl = o.optString("textUrl", "").ifBlank { null },
                audioUrl = o.optString("audioUrl", "").ifBlank { null },
            )
        }

        return PrefetchResponse(
            items = items,
            events = parseEvents(root.optJSONArray("events")),
            serverTimeMs = root.optLong("serverTimeMs", 0L),
        )
    }

    override fun events(cells: List<Cell>, fromIso: String, toIso: String): List<EventRecord> {
        if (base.isEmpty() || cells.isEmpty()) return emptyList()
        val query = cells.joinToString(",") { it.key }
        val root = getJson("$base/v1/events?cells=$query&from=$fromIso&to=$toIso") ?: return emptyList()
        return parseEvents(root.optJSONArray("events") ?: root.optJSONArray("data") ?: return emptyList())
    }

    override fun fetchText(url: String): ByteArray = getBytes(url) ?: ByteArray(0)

    override fun fetchAudio(url: String): ByteArray = getBytes(url) ?: ByteArray(0)

    // ---- transport ------------------------------------------------------

    private fun postJson(url: String, body: String): JSONObject? {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(url).apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return null
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) null else JSONObject(text)
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun getJson(url: String): JSONObject? {
        val text = getBytes(url)?.toString(Charsets.UTF_8) ?: return null
        return runCatching { JSONObject(text) }.getOrNull()
    }

    private fun getBytes(url: String): ByteArray? {
        var connection: HttpURLConnection? = null
        return try {
            connection = open(url)
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 8_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TravelGuide/1.0 (Android)")
            if (!apiKey.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $apiKey")
            }
        }

    private fun parseEvents(array: JSONArray?): List<EventRecord> {
        if (array == null) return emptyList()
        val out = mutableListOf<EventRecord>()
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("id").ifBlank { continue }
            val title = o.optString("title").ifBlank { continue }
            out += EventRecord(
                id = id,
                poiId = o.optString("poiId").ifBlank { null },
                title = title,
                startsAt = o.optString("startsAt"),
                endsAt = o.optString("endsAt").ifBlank { null },
                sourceUrl = o.optString("sourceUrl").ifBlank { null },
                fetchedAtMs = o.optLong("fetchedAtMs", System.currentTimeMillis()),
            )
        }
        return out
    }
}

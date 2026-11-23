package guide.app.delivery

import guide.core.delivery.MapPoint
import guide.core.delivery.RouteFunnel
import guide.core.delivery.TravelMode
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Supplies the L1 (alternatives) and L2 (junction fan) levels of the route
 * funnel — docs/ARCHITECTURE.md §3.3.
 *
 * The primary route is already known; this only answers "which other plausible
 * roads could the traveller take", so a wrong guess costs a card, not a blank
 * screen.
 */
interface RouteProvider {

    /** Alternative paths between the same two points, best first. May be empty. */
    fun alternatives(origin: MapPoint, destination: MapPoint, mode: TravelMode): List<List<MapPoint>>

    companion object {

        /** Turns sharper than this are treated as a junction worth fanning. */
        const val JUNCTION_TURN_DEG = 25.0

        /**
         * Short branches at the sharpest turns of the route.
         *
         * This is a geometric approximation, not road data: it exists so a
         * branch's *index* is local before the traveller commits to it. Real
         * geometry replaces it when the routing service returns it.
         */
        fun junctionBranches(route: List<MapPoint>, maxJunctions: Int = 3): List<List<MapPoint>> {
            if (route.size < 3) return emptyList()
            val out = mutableListOf<List<MapPoint>>()
            for (i in 1 until route.size - 1) {
                if (out.size >= maxJunctions * 2) break
                val incoming = bearing(route[i - 1], route[i])
                val outgoing = bearing(route[i], route[i + 1])
                if (abs(shortestTurn(incoming, outgoing)) < JUNCTION_TURN_DEG) continue
                out += RouteFunnel.branchesFrom(
                    route[i],
                    listOf(normalize(outgoing + 90.0), normalize(outgoing - 90.0)),
                )
            }
            return out
        }

        fun bearing(a: MapPoint, b: MapPoint): Double {
            val dLng = Math.toRadians(b.lng - a.lng)
            val y = sin(dLng) * cos(Math.toRadians(b.lat))
            val x = cos(Math.toRadians(a.lat)) * sin(Math.toRadians(b.lat)) -
                sin(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * cos(dLng)
            return normalize(Math.toDegrees(atan2(y, x)))
        }

        fun normalize(deg: Double): Double {
            var d = deg % 360.0
            if (d < 0) d += 360.0
            return d
        }

        private fun shortestTurn(from: Double, to: Double): Double {
            var diff = normalize(to - from)
            if (diff > 180.0) diff -= 360.0
            return diff
        }
    }
}

/** No routing service: the funnel degrades to the primary route only. */
object NoRouteProvider : RouteProvider {
    override fun alternatives(origin: MapPoint, destination: MapPoint, mode: TravelMode): List<List<MapPoint>> =
        emptyList()
}

/**
 * OSRM alternatives, the same service `RoadRouter` already uses for geometry.
 *
 * Returns every route after the first: the first is what the traveller is
 * already following, and duplicating it would just double the plan.
 */
class OsrmRouteProvider(
    private val endpoint: String = "https://router.project-osrm.org",
) : RouteProvider {

    override fun alternatives(origin: MapPoint, destination: MapPoint, mode: TravelMode): List<List<MapPoint>> {
        val profile = if (mode == TravelMode.WALKING) "walking" else "driving"
        val url = "$endpoint/route/v1/$profile/" +
            "${origin.lng},${origin.lat};${destination.lng},${destination.lat}" +
            "?alternatives=true&overview=full&geometries=geojson"

        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 5_000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "TravelGuide/1.0 (Android)")
            }
            if (connection.responseCode !in 200..299) return emptyList()
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            parseAlternatives(text)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        /** Parse an OSRM `route` response into paths, dropping the primary. */
        fun parseAlternatives(json: String): List<List<MapPoint>> {
            val root = runCatching { JSONObject(json) }.getOrNull() ?: return emptyList()
            val routes = root.optJSONArray("routes") ?: return emptyList()

            val paths = mutableListOf<List<MapPoint>>()
            for (i in 0 until routes.length()) {
                val geometry = routes.optJSONObject(i)?.optJSONObject("geometry") ?: continue
                val coords = geometry.optJSONArray("coordinates") ?: continue
                val points = mutableListOf<MapPoint>()
                for (j in 0 until coords.length()) {
                    val pair = coords.optJSONArray(j) ?: continue
                    if (pair.length() < 2) continue
                    points += MapPoint(lat = pair.getDouble(1), lng = pair.getDouble(0))
                }
                if (points.size >= 2) paths += points
            }
            return paths.drop(1)
        }
    }
}

package guide.core.delivery

import guide.core.Geo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Cell addressing.
 *
 * The world is tiled at zoom 16 (≈ 610 m at the equator, ~530 m in Kerala).
 * A cell is the unit of requesting, caching and invalidating content, so the
 * phone can ask for "the next few hundred metres" instead of a city or a
 * planet — docs/ARCHITECTURE.md §3.1.
 */
data class Cell(val z: Int, val x: Int, val y: Int) {
    val key: String get() = "$z/$x/$y"

    companion object {
        /** Parse a `z/x/y` key. Returns null for anything malformed or out of range. */
        fun parse(key: String): Cell? {
            val parts = key.split('/')
            if (parts.size != 3) return null
            val z = parts[0].toIntOrNull() ?: return null
            if (z !in 0..22) return null
            val n = 1 shl z
            val x = parts[1].toIntOrNull() ?: return null
            val y = parts[2].toIntOrNull() ?: return null
            if (x !in 0 until n || y !in 0 until n) return null
            return Cell(z, x, y)
        }
    }
}

data class CellBounds(
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double,
) {
    fun contains(lat: Double, lng: Double): Boolean =
        lat in minLat..maxLat && lng in minLng..maxLng
}

object Cells {
    /** Prefetch cell zoom. z16 ≈ 610 m wide at the equator. */
    const val ZOOM = 16

    private const val M_PER_DEG_LAT = 110_540.0
    private const val M_PER_DEG_LNG = 111_320.0
    private const val MAX_MERCATOR_LAT = 85.05112878

    fun cellFor(lat: Double, lng: Double, z: Int = ZOOM): Cell {
        val n = 1 shl z
        val clampedLat = lat.coerceIn(-MAX_MERCATOR_LAT, MAX_MERCATOR_LAT)
        val x = floor((lng + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)

        val latRad = Math.toRadians(clampedLat)
        val y = floor(
            (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n,
        ).toInt().coerceIn(0, n - 1)

        return Cell(z, x, y)
    }

    fun bounds(cell: Cell): CellBounds {
        val n = 1 shl cell.z
        val minLng = cell.x.toDouble() / n * 360.0 - 180.0
        val maxLng = (cell.x + 1).toDouble() / n * 360.0 - 180.0
        val maxLat = latitudeAt(cell.y.toDouble(), n)
        val minLat = latitudeAt((cell.y + 1).toDouble(), n)
        return CellBounds(minLat, minLng, maxLat, maxLng)
    }

    fun center(cell: Cell): MapPoint {
        val b = bounds(cell)
        return MapPoint((b.minLat + b.maxLat) / 2.0, (b.minLng + b.maxLng) / 2.0)
    }

    /** Approximate cell width in metres at [lat]. */
    fun sizeMeters(lat: Double, z: Int = ZOOM): Double {
        val n = 1 shl z
        return 360.0 / n * M_PER_DEG_LNG * abs(cos(Math.toRadians(lat)))
    }

    /**
     * Every cell a buffered path touches.
     *
     * The corridor is stroked rather than bounded: the centre line is sampled at
     * a third of a cell, and so are parallel offset lines out to [bufferM]. That
     * keeps a 250 m walking corridor from pulling in cells 900 m away (which a
     * ring test around each sample would do), while a 1.5 km net still fills a
     * band several cells wide.
     */
    fun cellsAlong(
        path: List<MapPoint>,
        bufferM: Double,
        z: Int = ZOOM,
        maxCells: Int = 2_000,
    ): Set<Cell> {
        if (path.isEmpty()) return emptySet()

        val size = sizeMeters(path[0].lat, z).coerceAtLeast(1.0)
        val step = (size / 3.0).coerceAtLeast(10.0)
        val out = LinkedHashSet<Cell>()

        // 0, then ±k·size up to the buffer (clamped, so the last offset lands on the edge).
        val offsets = LinkedHashSet<Double>()
        offsets += 0.0
        val rings = ceil(bufferM / size).toInt().coerceAtLeast(0)
        for (k in 1..rings) {
            val d = minOf(k * size, bufferM)
            offsets += d
            offsets += -d
        }

        for (i in 0 until path.size - 1) {
            val a = path[i]
            val b = path[i + 1]
            val segmentM = Geo.distanceM(a.lat, a.lng, b.lat, b.lng)
            if (segmentM <= 0.0) continue
            val steps = ceil(segmentM / step).toInt().coerceAtLeast(1)

            // Unit direction and its left normal, in metres, converted to degrees.
            val kx = cos(Math.toRadians(a.lat)) * M_PER_DEG_LNG
            val ux = (b.lng - a.lng) * kx / segmentM
            val uy = (b.lat - a.lat) * M_PER_DEG_LAT / segmentM
            val nx = -uy
            val ny = ux

            for (offset in offsets) {
                val dLat = ny * offset / M_PER_DEG_LAT
                val dLng = nx * offset / kx
                for (s in 0..steps) {
                    val t = s.toDouble() / steps
                    val lat = a.lat + (b.lat - a.lat) * t + dLat
                    val lng = a.lng + (b.lng - a.lng) * t + dLng
                    out += cellFor(lat, lng, z)
                    if (out.size >= maxCells) return out
                }
            }
        }
        return out
    }

    private fun latitudeAt(tileY: Double, n: Int): Double {
        val y = PI * (1.0 - 2.0 * tileY / n)
        return Math.toDegrees(atan(sinh(y)))
    }
}

/** Path geometry: how far along a path a point is, and how far off it. */
object PathMath {

    data class Projection(val distanceAlongM: Double, val offsetM: Double, val segmentIndex: Int)

    private const val M_PER_DEG_LAT = 110_540.0
    private const val M_PER_DEG_LNG = 111_320.0

    /** Offset of [p] from [path], plus how far along the path the closest point is. */
    fun project(path: List<MapPoint>, p: MapPoint): Projection {
        if (path.isEmpty()) return Projection(0.0, Double.MAX_VALUE, -1)
        if (path.size == 1) {
            return Projection(0.0, Geo.distanceM(p.lat, p.lng, path[0].lat, path[0].lng), 0)
        }

        var walked = 0.0
        var best = Projection(0.0, Double.MAX_VALUE, 0)
        for (i in 0 until path.size - 1) {
            val a = path[i]
            val b = path[i + 1]
            val segmentM = Geo.distanceM(a.lat, a.lng, b.lat, b.lng)
            val (t, offset) = projectOnSegment(p, a, b)
            if (offset < best.offsetM) {
                best = Projection(walked + t * segmentM, offset, i)
            }
            walked += segmentM
        }
        return best
    }

    fun distanceToPathM(path: List<MapPoint>, p: MapPoint): Double =
        project(path, p).offsetM

    /** Great-circle destination from [from], [bearingDeg] clockwise from north. */
    fun destination(from: MapPoint, bearingDeg: Double, distanceM: Double): MapPoint {
        val earthM = 6_371_000.0
        val angular = distanceM / earthM
        val bearing = Math.toRadians(bearingDeg)
        val lat1 = Math.toRadians(from.lat)
        val lng1 = Math.toRadians(from.lng)

        val lat2 = kotlin.math.asin(
            sin(lat1) * cos(angular) + cos(lat1) * sin(angular) * cos(bearing),
        )
        val lng2 = lng1 + kotlin.math.atan2(
            sin(bearing) * sin(angular) * cos(lat1),
            cos(angular) - sin(lat1) * sin(lat2),
        )
        return MapPoint(
            Math.toDegrees(lat2),
            ((Math.toDegrees(lng2) + 540.0) % 360.0) - 180.0,
        )
    }

    /** (fraction along the segment, perpendicular distance in metres). */
    private fun projectOnSegment(p: MapPoint, a: MapPoint, b: MapPoint): Pair<Double, Double> {
        val kx = cos(Math.toRadians(a.lat)) * M_PER_DEG_LNG
        val ky = M_PER_DEG_LAT

        val bx = (b.lng - a.lng) * kx
        val by = (b.lat - a.lat) * ky
        val px = (p.lng - a.lng) * kx
        val py = (p.lat - a.lat) * ky

        val lengthSq = bx * bx + by * by
        val t = if (lengthSq <= 0.0) 0.0 else ((px * bx + py * by) / lengthSq).coerceIn(0.0, 1.0)

        val ex = px - t * bx
        val ey = py - t * by
        return t to sqrt(ex * ex + ey * ey)
    }
}

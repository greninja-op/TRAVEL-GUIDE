package guide.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** F-07 (what am I seeing?) + geo math + visited/closed reranking. */
class SeeingTest {
    // ~111m north and ~132m east of the user; radius 100 so both are
    // within the 1.5x pickup cutoff (150m).
    private val north = poi("north", 9.001, 76.0, radiusM = 100.0)
    private val east = poi("east", 9.0, 76.0012, radiusM = 100.0)

    @Test
    fun `facing boost beats a nearer poi (F-07)`() {
        val pick = Seeing.pick(9.0, 76.0, headingDeg = 90.0, listOf(north, east))
        assertEquals("east", pick?.id)
    }

    @Test
    fun `without heading, nearest wins`() {
        assertEquals("north", Seeing.pick(9.0, 76.0, null, listOf(north, east))?.id)
    }

    @Test
    fun `ignores poi beyond 1-5x radius`() {
        val far = poi("far", 9.002, 76.0) // ~223m, cutoff 90m
        assertNull(Seeing.pick(9.0, 76.0, null, listOf(far)))
    }
}

class GeoTest {
    @Test
    fun `one degree of latitude is about 111 km`() {
        assertEquals(111195.0, Geo.distanceM(0.0, 0.0, 1.0, 0.0), absoluteTolerance = 200.0)
    }

    @Test
    fun `bearing north is 0, east is 90`() {
        assertEquals(0.0, Geo.bearingTo(9.0, 76.0, poi("n", 9.01, 76.0)), absoluteTolerance = 0.5)
        assertEquals(90.0, Geo.bearingTo(9.0, 76.0, poi("e", 9.0, 76.01)), absoluteTolerance = 0.5)
    }
}

class VisitRankTest {
    @Test
    fun `visited sink, closed sink, then nearest`() {
        val near = poi("near", 9.0, 76.0)
        val was = poi("was", 9.0, 76.0005)
        val closed = poi("closed", 9.0, 76.0002, hours = Hours("09:00", "10:00"))
        val ranked = VisitRank.rank(
            listOf(near to 50.0, was to 55.0, closed to 30.0),
            visited = setOf("was"),
            nowHHMM = "13:00",
        )
        assertEquals(listOf("near", "closed", "was"), ranked.map { it.id })
    }

    @Test
    fun `layer filter keeps only the chosen layer`() {
        val food = poi("f", 9.0, 76.0, layer = "food")
        val heritage = poi("h", 9.0, 76.0)
        val ranked = VisitRank.rank(
            listOf(food to 10.0, heritage to 20.0),
            visited = emptySet(),
            layer = "food",
        )
        assertTrue(ranked.map { it.id } == listOf("f"))
    }
}

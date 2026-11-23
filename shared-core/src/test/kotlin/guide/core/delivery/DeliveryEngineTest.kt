package guide.core.delivery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Cells, projection, the route funnel and the planner (docs/ARCHITECTURE.md §3). */
class DeliveryEngineTest {

    private val walkingPath = listOf(
        MapPoint(9.9600, 76.2400),
        MapPoint(9.9700, 76.2400),
    )

    @Test
    fun `cell addressing round-trips and bounds contain their point`() {
        val origin = Cells.cellFor(0.0, 0.0)
        assertEquals(Cell(16, 32768, 32768), origin)
        assertTrue(Cells.bounds(origin).contains(0.0, 0.0))

        val kochi = MapPoint(9.9658, 76.2423)
        val cell = Cells.cellFor(kochi.lat, kochi.lng)
        assertTrue(Cells.bounds(cell).contains(kochi.lat, kochi.lng))
        assertTrue(Cells.sizeMeters(kochi.lat) in 550.0..650.0)
        assertEquals(cell, Cell.parse(cell.key))
    }

    @Test
    fun `cell key parser rejects malformed keys`() {
        assertEquals(null, Cell.parse("16/1"))
        assertEquals(null, Cell.parse("x/1/2"))
        assertEquals(null, Cell.parse("16/999999/1"))
        assertEquals(null, Cell.parse("16/1/2/3"))
    }

    @Test
    fun `a narrow corridor stays narrow and a wide net fills the band`() {
        val narrow = Cells.cellsAlong(walkingPath, 250.0)
        val wide = Cells.cellsAlong(walkingPath, 1_500.0)

        assertTrue(narrow.isNotEmpty())
        assertTrue(wide.size > narrow.size)

        // ~700 m east of the line: outside a 250 m walking corridor, inside a 1.5 km net.
        val east = Cells.cellFor(9.9650, 76.2455)
        assertFalse(east in narrow, "a 250 m corridor must not reach 700 m east")
        assertTrue(east in wide, "a 1.5 km net should reach 700 m east")
    }

    @Test
    fun `projection reports offset and distance along the path`() {
        val onPath = PathMath.project(walkingPath, MapPoint(9.9650, 76.2400))
        assertTrue(onPath.offsetM < 5.0, "expected on-path, got ${onPath.offsetM}")
        assertTrue(onPath.distanceAlongM in 500.0..620.0, "expected ~553 m, got ${onPath.distanceAlongM}")

        val offPath = PathMath.project(walkingPath, MapPoint(9.9650, 76.2409))
        assertTrue(offPath.offsetM in 80.0..120.0, "expected ~99 m, got ${offPath.offsetM}")
    }

    @Test
    fun `funnel covers the whole path in lane A but keeps audio on the primary`() {
        val longPath = listOf(MapPoint(9.9600, 76.2400), MapPoint(10.0050, 76.2400))
        val funnel = RouteFunnel.build(longPath, TravelMode.WALKING)
        val plan = PrefetchPlanner.plan(funnel, MapPoint(9.9600, 76.2400), TravelMode.WALKING)

        val farCell = Cells.cellFor(10.0050, 76.2400)
        assertTrue(farCell in plan.index, "lane A must cover the whole route")
        assertFalse(farCell in plan.audio, "audio must stay inside the walking window")
        assertTrue(plan.audio.size <= PrefetchPlanner.Limits().maxAudioCells)
    }

    @Test
    fun `junction branches get the index lane only`() {
        val junction = MapPoint(9.9650, 76.2400)
        val branch = RouteFunnel.branchesFrom(junction, listOf(90.0), reachM = 1_100.0)
        val funnel = RouteFunnel.build(
            walkingPath,
            TravelMode.WALKING,
            junctions = branch,
        )
        val plan = PrefetchPlanner.plan(funnel, MapPoint(9.9600, 76.2400), TravelMode.WALKING)

        val branchCell = Cells.cellFor(9.9650, 76.2480)
        val scored = plan.cells.firstOrNull { it.cell == branchCell }
        assertTrue(scored != null, "the branch cell must be planned")
        assertEquals(setOf(Lane.INDEX), scored.lanes, "a junction branch earns no text or audio")
    }

    @Test
    fun `alternatives earn text but never audio`() {
        val alternative = listOf(
            MapPoint(9.9600, 76.2400),
            MapPoint(9.9650, 76.2450),
            MapPoint(9.9700, 76.2400),
        )
        val funnel = RouteFunnel.build(walkingPath, TravelMode.WALKING, alternatives = listOf(alternative))
        val plan = PrefetchPlanner.plan(funnel, MapPoint(9.9600, 76.2400), TravelMode.WALKING)

        val altCell = Cells.cellFor(9.9650, 76.2460)
        val scored = plan.cells.firstOrNull { it.cell == altCell }
        assertTrue(scored != null, "the alternative's cell must be planned")
        assertTrue(Lane.TEXT in scored.lanes)
        assertFalse(Lane.AUDIO in scored.lanes, "only the chosen route may carry audio")
    }

    @Test
    fun `shared cells outrank cells only one route reaches`() {
        val alternative = listOf(
            MapPoint(9.9600, 76.2400),
            MapPoint(9.9650, 76.2450),
            MapPoint(9.9700, 76.2400),
        )
        val funnel = RouteFunnel.build(walkingPath, TravelMode.WALKING, alternatives = listOf(alternative))
        val plan = PrefetchPlanner.plan(funnel, MapPoint(9.9600, 76.2400), TravelMode.WALKING)

        val shared = plan.cells.first { it.cell == Cells.cellFor(9.9600, 76.2400) }
        val altOnly = plan.cells.first { it.cell == Cells.cellFor(9.9650, 76.2460) }
        assertTrue(shared.score > altOnly.score, "the deduplicated shared cell should score higher")
    }

    @Test
    fun `free roam builds a corridor from the heading`() {
        val candidate = RouteFunnel.freeRoam(MapPoint(9.9650, 76.2400), 90.0, TravelMode.WALKING)
        assertEquals(RouteLevel.PRIMARY, candidate.level)
        assertEquals(2, candidate.points.size)
        assertTrue(candidate.points[1].lng > candidate.points[0].lng, "bearing 90 should point east")
    }
}

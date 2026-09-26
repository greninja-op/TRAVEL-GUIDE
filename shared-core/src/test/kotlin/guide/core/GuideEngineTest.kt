package guide.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ports tools/test_engine.py onto the REAL Kotlin engine (the Python file
 * mirrored the logic; these tests exercise it). Scenario coords identical
 * to the Python suite so results stay comparable.
 */
class GuideEngineTest {
    private val a = poi("a", 9.0, 76.0)
    private val b = poi("b", 9.001, 76.0)

    @Test
    fun `dwell then speak, arrival recorded on enter (F-01)`() {
        val e = GuideEngine(pack(a, b), routeOrder = listOf("a", "b"))
        val u1 = e.onFix(9.0, 76.0, speedMps = 0.0, atS = 0)
        assertEquals(emptyList(), u1.spoke.map { it.id }, "first fix only starts dwell")
        assertEquals(listOf("a"), u1.arrived.map { it.id })
        val u2 = e.onFix(9.0, 76.0, speedMps = 0.0, atS = 8)
        assertEquals(listOf("a"), u2.spoke.map { it.id }, "dwell complete -> speaks")
    }

    @Test
    fun `departure recorded on exit (F-03)`() {
        val e = GuideEngine(pack(a, b), routeOrder = listOf("a", "b"))
        e.onFix(9.0, 76.0, 0.0, 0)
        val u = e.onFix(9.005, 76.005, 0.0, 16) // ~785m away: outside both radii
        assertEquals(listOf("a"), u.departed.map { it.id })
        assertEquals(0, e.openVisitCount())
    }

    @Test
    fun `cooldown blocks re-speak, card still shows (F-04)`() {
        val e = GuideEngine(pack(a, b), routeOrder = listOf("a", "b"))
        e.onFix(9.0, 76.0, 0.0, 0)
        e.onFix(9.0, 76.0, 0.0, 8) // speaks a at t=8
        e.onFix(9.005, 76.005, 0.0, 16) // leave
        e.onFix(9.0, 76.0, 0.0, 17) // re-enter, dwell restarts
        val u = e.onFix(9.0, 76.0, 0.0, 25) // dwell done, 17s << 1800s cooldown
        assertEquals(emptyList(), u.spoke.map { it.id })
        assertEquals(listOf("a"), u.cards.map { it.id })
    }

    @Test
    fun `off-route detected when far from remaining stops (F-06)`() {
        val e = GuideEngine(pack(a, b), routeOrder = listOf("a", "b"))
        e.onFix(9.0, 76.0, 0.0, 0) // visits a
        val u = e.onFix(9.05, 76.05, 0.0, 24) // >5km from b
        assertTrue(u.offRoute)
    }

    @Test
    fun `on-route stop narrated before nearer wander poi (priority order)`() {
        val routePoi = poi("route", 9.0, 76.0)
        val wanderPoi = poi("wander", 9.0, 76.0001) // ~11m closer pair, overlapping radii
        val e = GuideEngine(
            pack(routePoi, wanderPoi.copy(radiusM = 200.0)),
            routeOrder = listOf("route"),
        )
        e.onFix(9.0, 76.0, 0.0, 0)
        val u = e.onFix(9.0, 76.0, 0.0, 8)
        assertEquals(listOf("route", "wander"), u.spoke.map { it.id })
    }
}

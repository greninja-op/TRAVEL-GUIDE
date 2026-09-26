package guide.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Ports tools/test_itinerary.py onto the real Itineraries + QuietHours. */
class ItinerariesTest {
    private val a = poi("a", 9.0, 76.0)
    private val b = poi("b", 9.0, 76.0, hours = Hours("09:00", "17:00"))
    private val c = poi("c", 9.0, 76.0, hours = Hours("09:00", "12:00"))
    private val route = Route("r", "Test Walk", listOf("a", "b", "c"), 30)
    private val byId = listOf(a, b, c).associateBy { it.id }

    @Test
    fun `2h plan fits all open stops`() {
        val plan = Itineraries.plan(route, byId, budgetMin = 120, nowHHMM = "10:00")
        assertEquals(listOf("a", "b", "c"), plan.stops)
        assertEquals(27, plan.estMinutes)
    }

    @Test
    fun `closed poi skipped at 13-00 (c closes at 12)`() {
        val plan = Itineraries.plan(route, byId, budgetMin = 120, nowHHMM = "13:00")
        assertEquals(listOf("a", "b"), plan.stops)
    }

    @Test
    fun `tight budget truncates and never exceeds`() {
        val plan = Itineraries.plan(route, byId, budgetMin = 18, nowHHMM = "10:00")
        assertEquals(listOf("a", "b"), plan.stops)
        assertTrue(plan.estMinutes <= 18)
    }

    @Test
    fun `no clock given - hours ignored`() {
        val plan = Itineraries.plan(route, byId, budgetMin = 120)
        assertEquals(listOf("a", "b", "c"), plan.stops)
    }
}

class QuietHoursTest {
    @Test
    fun `overnight window quiets 22-00 to 07-00`() {
        val w = QuietHours.Window("22:00", "07:00")
        assertFalse(QuietHours.isQuiet("21:59", w))
        assertTrue(QuietHours.isQuiet("22:00", w))
        assertTrue(QuietHours.isQuiet("06:59", w))
        assertFalse(QuietHours.isQuiet("07:01", w))
    }

    @Test
    fun `same-day window`() {
        val w = QuietHours.Window("13:00", "14:00")
        assertTrue(QuietHours.isQuiet("13:30", w))
        assertFalse(QuietHours.isQuiet("14:30", w))
    }

    @Test
    fun `disabled or missing window never quiet`() {
        assertFalse(QuietHours.isQuiet("23:00", QuietHours.Window("22:00", "07:00", enabled = false)))
        assertFalse(QuietHours.isQuiet("23:00", null))
    }
}

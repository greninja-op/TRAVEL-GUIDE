package guide.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Ports tools/test_queue.py onto the real NarrationQueue + TriggerState. */
class NarrationQueueTest {
    @Test
    fun `F-02 one voice at a time, second plays after first`() {
        val q = NarrationQueue()
        q.enqueue("a")
        q.enqueue("b")
        assertEquals("a", q.next())
        assertEquals("a", q.speaking)
        q.finished("a")
        assertEquals("b", q.next())
    }

    @Test
    fun `dedupes same poi enqueued twice`() {
        val q = NarrationQueue()
        q.enqueue("x")
        q.enqueue("x")
        assertEquals(listOf("x"), q.pending())
    }

    @Test
    fun `on-route priority jumps the queue`() {
        val q = NarrationQueue()
        q.enqueue("wander-poi")
        q.enqueue("route-poi", onRoute = true)
        assertEquals("route-poi", q.next())
    }

    @Test
    fun `F-08 mute clears queue and speaking state`() {
        val q = NarrationQueue()
        q.enqueue("a")
        q.next()
        q.enqueue("b")
        q.clear()
        assertTrue(q.pending().isEmpty())
        assertNull(q.speaking)
    }
}

class TriggerStateTest {
    private val p = poi("p", 9.0, 76.0)
    private val candidates = listOf(p)

    @Test
    fun `speaks only after dwell completes`() {
        val t = TriggerState()
        assertEquals(emptyList(), t.onFix(Fix(9.0, 76.0, 0, 0.0), candidates))
        assertEquals(listOf("p"), t.onFix(Fix(9.0, 76.0, 8, 0.0), candidates))
    }

    @Test
    fun `F-04 cooldown blocks within 30 min and expires at 30 min`() {
        val t = TriggerState()
        t.onFix(Fix(9.0, 76.0, 0, 0.0), candidates)
        t.onFix(Fix(9.0, 76.0, 8, 0.0), candidates) // spoke at t=8
        t.onFix(Fix(9.1, 76.0, 100, 0.0), candidates) // exit
        t.onFix(Fix(9.0, 76.0, 300, 0.0), candidates) // re-enter, dwell restarts
        assertEquals(emptyList(), t.onFix(Fix(9.0, 76.0, 308, 0.0), candidates), "5 min later: still cooling")
        t.onFix(Fix(9.1, 76.0, 1700, 0.0), candidates) // exit again
        t.onFix(Fix(9.0, 76.0, 1800, 0.0), candidates) // re-enter
        assertEquals(listOf("p"), t.onFix(Fix(9.0, 76.0, 1808, 0.0), candidates), "cooldown expired")
    }

    @Test
    fun `speed gate blocks narration above max speak speed`() {
        val t = TriggerState()
        t.onFix(Fix(9.0, 76.0, 0, 12.0), candidates)
        assertEquals(emptyList(), t.onFix(Fix(9.0, 76.0, 8, 12.0), candidates), "moving too fast")
        assertEquals(listOf("p"), t.onFix(Fix(9.0, 76.0, 16, 0.0), candidates), "slowed down -> speaks")
    }
}

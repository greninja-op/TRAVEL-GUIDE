package guide.core.delivery

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Deviation rules, the bundle store and the end-to-end prefetch loop. */
class DeliveryStoreTest {

    private val path = listOf(MapPoint(9.9600, 76.2400), MapPoint(9.9700, 76.2400))

    private fun tempStore(maxBytes: Long): BundleStore {
        val dir = Files.createTempDirectory("travel-guide-store").toFile()
        return BundleStore(File(dir, "store"), maxBytes)
    }

    @Test
    fun `on-device store writes, reloads and reports size`() {
        val store = tempStore(10_000_000)
        store.put("poi-1", "h1", Lane.TEXT, "en", ByteArray(120), nowMs = 1_000)
        store.put("poi-1", "h1", Lane.AUDIO, "en", ByteArray(400), nowMs = 2_000)

        assertEquals(520, store.totalBytes())
        assertTrue(store.has("poi-1", "h1", Lane.TEXT, "en"))
        assertTrue(store.has("poi-1", "h1", Lane.AUDIO, "en"))
        assertEquals("h1", store.knownHashes()["poi-1/TEXT"])
        assertEquals(120, store.read("poi-1", "h1", Lane.TEXT, "en")!!.size)

        // A fresh instance over the same directory reloads the manifest.
        val reloaded = BundleStore(store.root, 10_000_000)
        assertTrue(reloaded.has("poi-1", "h1", Lane.TEXT, "en"))
    }

    @Test
    fun `eviction removes audio before text`() {
        val store = tempStore(500)
        store.put("a", "h1", Lane.TEXT, "en", ByteArray(100), nowMs = 1_000)
        store.put("a", "h1", Lane.AUDIO, "en", ByteArray(300), nowMs = 2_000)
        store.put("b", "h2", Lane.TEXT, "en", ByteArray(100), nowMs = 3_000)
        store.put("b", "h2", Lane.AUDIO, "en", ByteArray(300), nowMs = 4_000)

        assertEquals(800, store.totalBytes())
        val victims = store.evict()

        assertEquals(1, victims.size, "one eviction is enough to get back under budget")
        assertEquals(Lane.AUDIO, victims[0].lane, "audio pays first")
        assertEquals("a", victims[0].poiId)
        assertTrue(store.has("a", "h1", Lane.TEXT, "en"), "the oldest text must survive")
        assertEquals(500, store.totalBytes())
    }

    @Test
    fun `pinned entries are never evicted`() {
        val store = tempStore(400)
        store.put("pinned", "hp", Lane.AUDIO, "en", ByteArray(300), nowMs = 0, pinned = true)
        store.put("a", "h1", Lane.TEXT, "en", ByteArray(100), nowMs = 5_000)
        store.put("a", "h1", Lane.AUDIO, "en", ByteArray(300), nowMs = 6_000)

        val victims = store.evict()

        assertEquals(listOf("a"), victims.map { it.poiId })
        assertTrue(store.has("pinned", "hp", Lane.AUDIO, "en"))
    }

    @Test
    fun `deviation needs sustained movement before it replans`() {
        val monitor = DeviationMonitor()
        val off = MapPoint(9.9650, 76.2409) // ~99 m east of the path

        val first = monitor.reading(off, path, speedMps = 1.4, nowMs = 0)
        assertEquals(DeviationMonitor.State.ON_ROUTE, first.state)
        assertFalse(first.replan, "one fix is never enough")

        val early = monitor.reading(off, path, speedMps = 1.4, nowMs = 10_000)
        assertFalse(early.replan)

        val sustained = monitor.reading(off, path, speedMps = 1.4, nowMs = 16_000)
        assertEquals(DeviationMonitor.State.OFF_ROUTE, sustained.state)
        assertTrue(sustained.replan)

        val tooSoon = monitor.reading(off, path, speedMps = 1.4, nowMs = 30_000)
        assertFalse(tooSoon.replan, "replans are debounced to one a minute")

        val later = monitor.reading(off, path, speedMps = 1.4, nowMs = 80_000)
        assertTrue(later.replan)
    }

    @Test
    fun `standing still never triggers a replan`() {
        val monitor = DeviationMonitor()
        val off = MapPoint(9.9650, 76.2409)
        var replans = 0
        for (t in 0L..120_000L step 5_000L) {
            if (monitor.reading(off, path, speedMps = 0.0, nowMs = t).replan) replans++
        }
        assertEquals(0, replans)
    }

    @Test
    fun `rejoining clears the deviation and a long detour promotes the observed path`() {
        val monitor = DeviationMonitor()
        val off = MapPoint(9.9650, 76.2409)
        val on = MapPoint(9.9650, 76.2400)

        monitor.reading(off, path, 1.4, 0)
        assertTrue(monitor.reading(off, path, 1.4, 16_000).replan)

        val rejoin = monitor.reading(on, path, 1.4, 20_000)
        assertEquals(DeviationMonitor.State.ON_ROUTE, rejoin.state)
        assertFalse(rejoin.replan)

        monitor.reading(off, path, 1.4, 100_000)
        assertTrue(monitor.reading(off, path, 1.4, 116_000).replan)

        val promoted = monitor.reading(off, path, 1.4, 250_000)
        assertEquals(DeviationMonitor.State.PROMOTED, promoted.state)
        assertTrue(promoted.promote, "a two-minute detour becomes the new route")
    }

    @Test
    fun `prefetch loop fetches once and then asks for nothing`() {
        val store = tempStore(10_000_000)
        val source = FixtureContentSource()
        val coordinator = PrefetchCoordinator(store, source, language = "en")

        val funnel = RouteFunnel.build(path, TravelMode.WALKING)
        val plan = PrefetchPlanner.plan(funnel, MapPoint(9.9600, 76.2400), TravelMode.WALKING)

        val first = coordinator.run(plan, nowMs = 1_000)
        assertTrue(first.itemsOffered > 0, "a cold corridor must offer content")
        assertTrue(first.textWritten > 0, "lane B must be written")
        assertTrue(first.audioWritten > 0, "lane C must be written for the window")
        assertTrue(store.totalBytes() > 0)

        val second = coordinator.run(plan, nowMs = 2_000)
        assertEquals(0, second.itemsOffered, "a warm corridor should cost nothing")
        assertEquals(0, second.textWritten)
        assertEquals(0, second.audioWritten)
    }

    @Test
    fun `audio is only fetched for cells inside the window`() {
        val store = tempStore(10_000_000)
        val source = FixtureContentSource()
        val coordinator = PrefetchCoordinator(store, source, language = "en")

        val longPath = listOf(MapPoint(9.9600, 76.2400), MapPoint(10.0050, 76.2400))
        val funnel = RouteFunnel.build(longPath, TravelMode.WALKING)
        val plan = PrefetchPlanner.plan(funnel, MapPoint(9.9600, 76.2400), TravelMode.WALKING)

        val result = coordinator.run(plan, nowMs = 1_000)
        assertTrue(result.audioWritten > 0, "the window must warm some audio")
        assertTrue(result.textWritten > result.audioWritten, "text covers far more than audio")
    }
}

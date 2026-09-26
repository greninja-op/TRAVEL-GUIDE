package guide.app.packs

/** Phase 3 offline packs manager: download / delete / sizes, LRU cap. */
object PackManager {
    data class PackInfo(val cityId: String, val version: String, val bytes: Long)

    const val MAX_CACHED_BYTES = 500L * 1024 * 1024 // 500 MB device cap

    /** Evict oldest packs until under cap. Returns evicted city ids. */
    fun evictForBudget(
        installed: List<PackInfo>,
        lastUsed: Map<String, Long>,
        incomingBytes: Long,
    ): List<String> {
        var total = installed.sumOf { it.bytes } + incomingBytes
        if (total <= MAX_CACHED_BYTES) return emptyList()
        val evicted = mutableListOf<String>()
        for (p in installed.sortedBy { lastUsed[it.cityId] ?: 0 }) {
            if (total <= MAX_CACHED_BYTES) break
            // Never evict the bundled v1 test-city pack.
            if (p.cityId == "kochi-fort-kochi") continue
            total -= p.bytes
            evicted += p.cityId
        }
        return evicted
    }
}

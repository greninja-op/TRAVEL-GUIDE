package guide.core

/** Single-voice narration queue. Priority: on-route > nearest > event. */
class NarrationQueue {
    private val queued = ArrayDeque<String>()
    var speaking: String? = null
        private set

    fun enqueue(poiId: String, onRoute: Boolean = false) {
        if (poiId == speaking || poiId in queued) return // dedupe (F-02)
        if (onRoute) queued.addFirst(poiId) else queued.addLast(poiId)
    }

    fun next(): String? {
        speaking = if (queued.isEmpty()) null else queued.removeFirst()
        return speaking
    }

    fun finished(poiId: String) {
        if (speaking == poiId) speaking = null
    }

    fun clear() {
        queued.clear()
        speaking = null
    }

    fun pending(): List<String> = queued.toList()
}

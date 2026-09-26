package guide.core

/** Quiet-hours gate: no narration starts inside the user's quiet window.
 *  Cards still appear; the voice stays silent. Overnight windows supported. */
object QuietHours {
    data class Window(val startHHMM: String, val endHHMM: String, val enabled: Boolean = true)

    fun isQuiet(nowHHMM: String, window: Window?): Boolean {
        if (window == null || !window.enabled) return false
        return if (window.startHHMM <= window.endHHMM) {
            nowHHMM in window.startHHMM..window.endHHMM
        } else {
            nowHHMM >= window.startHHMM || nowHHMM <= window.endHHMM
        }
    }
}

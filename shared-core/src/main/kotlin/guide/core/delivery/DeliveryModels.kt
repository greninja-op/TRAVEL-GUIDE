package guide.core.delivery

/**
 * The engine side of content delivery (docs/ARCHITECTURE.md §3).
 *
 * Everything here is pure Kotlin + `java.io`: no Android imports, so the
 * corridor logic, the store and the deviation rules are all testable on the
 * JVM without a device or a network.
 */

/** A plain coordinate — the engine never sees an Android `Location` or `LatLng`. */
data class MapPoint(val lat: Double, val lng: Double)

/**
 * The three prefetch lanes.
 *
 * - [INDEX] is cheap and covers the whole route: every POI the corridor
 *   touches. It is what makes a card appear instantly, even off-route.
 * - [TEXT] is small and also covers the whole route: the story itself.
 * - [AUDIO] is the only heavy lane, so it follows the traveller rather than
 *   the route.
 */
enum class Lane { INDEX, TEXT, AUDIO }

package guide.app.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Pins + route polyline on the map.
 *
 * REWRITTEN 2026-09-26 against docs/UI-AUDIT-2026-09-26.md. Three things were
 * wrong and are now structural, not patched:
 *
 * 1. NON-IDEMPOTENT (audit defect 2). `render()` called `addSource`/`addLayer`
 *    unconditionally, so the second invocation threw on the duplicate id — and
 *    the map re-renders on every state change, so the second call was the normal
 *    path, not an edge case. It now always REMOVES every app-owned object first
 *    ([clear]) and re-adds; add and update are the same operation, which means
 *    there is no "already added" branch to get wrong.
 *
 * 2. TINY HIT AREAS (audit defect 3). `circleRadius(7f)` is ~14px — a third of
 *    the 44dp minimum. Pins are now SymbolLayers using registered raster icons
 *    whose bitmap INCLUDES 10px of transparent padding on every side, sized so
 *    the padded quad is 44dp at baseline density. See [MapStyle.ICON_SIZE_PX].
 *
 * 3. NO PIN STATES (audit defect 4). Four states, each differing in SHAPE and
 *    SIZE as well as colour (SPEC §5: never colour alone):
 *
 *      state    | glyph          | ring           | size
 *      ---------|----------------|----------------|----------------
 *      default  | pin, hollow    | primary        | 1.00  (33dp)
 *      next     | pin, hollow    | primary        | 1.16  (38dp) + label
 *      active   | pin, filled    | white          | 1.16  (38dp) + label
 *      visited  | small solid dot| none           | 0.78  (26dp) + 55% alpha
 *
 *    "Timeless colours" is respected: the palette comes from [MapStyle]'s
 *    palettes, which are built from [guide.app.ui.theme.GuideTokens] — nothing
 *    here re-types a hex.
 */
object MapPins {

    /** State of a place on the map. Exhaustive by design — no boolean soup. */
    enum class State {
        /** POI the walk has not reached yet. */
        DEFAULT,

        /** The next stop on the active route — the guide's next utterance. */
        NEXT,

        /** Currently narrating. The ONE accent state on the map. */
        ACTIVE,

        /** Already heard. Quiet by design — presence, not emphasis. */
        VISITED,
    }

    /**
     * One place on the map.
     *
     * @param visited retained from the previous signature so existing callers
     *   keep compiling; it is only consulted when [state] is left null, so a
     *   caller predating pin states still gets sensible behaviour.
     */
    data class Pin(
        val id: String,
        val name: String,
        val lat: Double,
        val lng: Double,
        val visited: Boolean = false,
    )

    /** Emitted when a pin is tapped. */
    fun interface PinTapListener {
        fun onPinTap(id: String)
    }

    /** Corner rounding of the label chip, in dp — concentric with the pin. */
    private const val CHIP_CORNER_DP = 6f

    /**
     * Icon scale per state. These multiply a bitmap already sized to hit 44dp at
     * 1.0, so the values are the state's size difference — the shape half of
     * "never colour alone". VISITED drops to 0.78 because a heard place should
     * read as quiet, and the icon's own glyph is smaller still.
     */
    private const val PIN_SCALE_DEFAULT = 0.90f

    /** Next-up and currently-narrating: equal size, different glyph + colour. */
    private const val PIN_SCALE_FOCUS = 1.10f

    /** Already heard — present for orientation, never competing for the eye. */
    private const val PIN_SCALE_VISITED = 0.70f

    /**
     * REMOVED 2026-09-26 — there is no `EMPTY_FONT_STACK` and no map-layer label.
     *
     * A `text-field` symbol can only render if the style declares a `glyphs`
     * URL, and this style deliberately ships none (offline-first: MapLibre would
     * fetch glyph PBFs over the network). The previous revision kept the label
     * layers with an unresolvable font stack to keep them "inert" — but an inert
     * label still reserves collision space, so it silently pushed real markers
     * around the map in exchange for nothing visible.
     *
     * The active pin's NAME now renders as a Compose overlay above the map
     * (see `MapScreen`'s selected-pin chip). That is what Google/Apple Maps do
     * for a selected marker, it costs no network, and it gets real Inter type
     * instead of a map glyph. Marker STATES still read without any label: each
     * state differs in glyph, size AND colour, so it survives greyscale.
     */
    private const val PROP_ID = "id"
    private const val PROP_NAME = "name"

    /**
     * Draw pins + route onto [style].
     *
     * Safe to call repeatedly, with any combination of empty inputs:
     * `render(emptyList(), emptyList())` is a valid "clear the map" call — it
     * removes the app's objects and stops, rather than throwing or leaving
     * orphans (audit §3.4: screens legitimately receive no data yet).
     *
     * @param pins places to draw. Empty is fine.
     * @param route ordered route geometry. Fewer than 2 points draws no line.
     * @param activeId the pin currently narrating, if any.
     * @param nextId the next stop on the route, if any.
     * @param onPinTap invoked with the pin id on tap. No listener = pins are
     *   purely decorative and get no tap handling at all.
     * @param fitToPins move the camera to the pins. False lets the caller keep
     *   its own camera (e.g. following the walker) — the map must not yank the
     *   viewport out from under a user who is being followed.
     */
    fun render(
        context: Context,
        map: MapLibreMap,
        style: Style,
        pins: List<Pin>,
        route: List<LatLng> = emptyList(),
        activeId: String? = null,
        nextId: String? = null,
        onPinTap: PinTapListener? = null,
        fitToPins: Boolean = true,
    ) {
        // --- 1. Always start from a clean slate -----------------------------
        // This single line is what makes the function idempotent: add and
        // update become the same code path.
        clear(style)

        // --- 2. Route: casing under fill ------------------------------------
        // Two layers over one source — a wide, dark "scale" line with a bright
        // "route" line on top of it. The underside reads as an outline, which
        // is what keeps a 3px line legible over busy OSM raster at every zoom
        // without ever thickening the bright stroke. Draw order matters:
        // scale -> base -> route, and pins are added after so they sit on top.
        if (route.size >= 2) {
            style.addSource(
                GeoJsonSource(
                    MapStyle.ID_ROUTE_SCALE,
                    FeatureCollection.fromFeature(
                        Feature.fromGeometry(lineGeometry(route)),
                    ),
                ),
            )
            style.addLayer(
                LineLayer(MapStyle.ID_ROUTE_BASE, MapStyle.ID_ROUTE_SCALE).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineColor(GuideColors.ROUTE_BASE),
                    PropertyFactory.lineWidth(9f),
                    PropertyFactory.lineOpacity(0.30f),
                ),
            )
            style.addLayer(
                LineLayer(MapStyle.ID_ROUTE_ROUTE, MapStyle.ID_ROUTE_SCALE).withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineColor(GuideColors.ROUTE_ROUTE),
                    // Slightly wider under the casing's centroid so the bright
                    // line reads as the subject, not the outline.
                    PropertyFactory.lineWidth(4.5f),
                ),
            )
        }

        if (pins.isEmpty()) {
            // Valid and common: audit §3.4 notes every screen is currently fed
            // `emptyList()`. Removing our objects and returning is the honest
            // behaviour — no crash, no orphaned layer, no blank-looking state
            // that is actually a duplicate-id exception.
            return
        }

        // --- 3. Register the pin icons (idempotent by name) ------------------
        // Style.addImage replaces an existing image with the same id on the
        // current stable MapLibre — and because every object is cleared and
        // re-added together, re-registering cannot desync layers from images.
        val defaultBitmap = Raster.drawPin(
            sizePx = MapStyle.ICON_SIZE_PX,
            edge = MapStyle.PIN_EDGE_PX,
            pad = MapStyle.PIN_PAD_PX,
            palette = MapStyle.defaultPinPalette(),
            filled = false,
            chipCornerPx = CHIP_CORNER_DP * MapStyle.ICON_DENSITY,
        )
        val activeBitmap = Raster.drawPin(
            sizePx = MapStyle.ICON_SIZE_PX,
            edge = MapStyle.PIN_EDGE_PX,
            pad = MapStyle.PIN_PAD_PX,
            palette = MapStyle.activePinPalette(),
            filled = true,
            chipCornerPx = CHIP_CORNER_DP * MapStyle.ICON_DENSITY,
        )
        style.addImage(MapStyle.ICON_PIN_DEFAULT, defaultBitmap)
        style.addImage(MapStyle.ICON_PIN_ACTIVE, activeBitmap)

        // --- 4. One source + one layer per state -----------------------------
        // `iconImage` is a single value per layer, so states are expressed as
        // separate layers. Order = paint order, back to front.
        val byState: Map<State, List<Pin>> = pins.groupBy { pinState(it, activeId, nextId) }

        addPinLayer(style, State.VISITED, byState[State.VISITED], MapStyle.ID_PIN_VISITED)
        addPinLayer(style, State.DEFAULT, byState[State.DEFAULT], MapStyle.ID_PIN_DEFAULT)
        addPinLayer(style, State.NEXT, byState[State.NEXT], MapStyle.ID_PIN_NEXT)
        addPinLayer(style, State.ACTIVE, byState[State.ACTIVE], MapStyle.ID_PIN_ACTIVE)

        // --- 5. Tap handling -------------------------------------------------
        if (onPinTap != null) {
            map.addOnMapClickListener { latLng ->
                // Nearest pin within a generous radius. The hit test is done in
                // METRES, not by reading back MapLibre's projected symbol quads:
                // queryRenderedFeatures would give the visual bounds, but it
                // needs a screen point, and converting a touch LatLng to a
                // symbol's padded quad is exactly the kind of style-spec
                // assumption that breaks silently. A distance test is honest
                // about what it is and cannot silently stop matching.
                val hit = pins.minByOrNull { distanceMeters(latLng, it) } ?: return@addOnMapClickListener false
                if (distanceMeters(latLng, hit) <= TAP_RADIUS_M) {
                    onPinTap.onPinTap(hit.id)
                    true
                } else {
                    false
                }
            }
        }

        // --- 6. Camera -------------------------------------------------------
        if (fitToPins) {
            fitCamera(map, pins, route)
        }
    }

    /**
     * Remove every object this app owns from [style]. Idempotent and safe to
     * call on a style that has none of them.
     *
     * Sources are removed AFTER their layers (a source cannot be removed while
     * a layer still references it), and images last.
     */
    fun clear(style: Style) {
        for (id in MapStyle.OWNED_LAYER_IDS) {
            style.getLayer(id)?.let { style.removeLayer(it) }
        }
        for (id in MapStyle.OWNED_SOURCE_IDS) {
            style.getSource(id)?.let { style.removeSource(it) }
        }
    }

    /**
     * Move the camera to frame [pins] (and [route] when it extends beyond them).
     *
     * Called from [render], which runs on the MapLibre callback thread, so this
     * uses the synchronous `moveCamera`. The ANIMATED camera work (audit defect
     * 5) lives in `MapScreen`, driven by user actions on the Compose thread —
     * animation spec and duration are UI concerns, not style concerns.
     */
    fun fitCamera(map: MapLibreMap, pins: List<Pin>, route: List<LatLng> = emptyList()) {
        val points = buildList {
            pins.forEach { add(LatLng(it.lat, it.lng)) }
            route.forEach { add(it) }
        }
        if (points.isEmpty()) return
        // A single point has a degenerate bounds: `newLatLngBounds` cannot
        // compute a zoom from zero area and throws. Zoom in on it instead.
        if (points.size == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), FOCUS_ZOOM))
            return
        }
        val bounds = LatLngBounds.Builder().apply { points.forEach { include(it) } }.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX))
    }

    // -----------------------------------------------------------------------
    // Internals
    // -----------------------------------------------------------------------

    /**
     * Tap radius. 44dp is the minimum TARGET size; a finger aiming at a 33dp
     * pin lands within roughly this of the marker centre, and pin spacing in
     * Fort Kochi (pack v1.1.0) is far wider than 90m, so a generous radius
     * costs no accuracy. [minByOrNull] already guarantees the nearest pin wins.
     */
    private const val TAP_RADIUS_M = 90.0

    private const val BOUNDS_PADDING_PX = 120

    /** Matches the map's offline pack range (12–16) — a single POI wants context. */
    private const val FOCUS_ZOOM = 16.0

    private fun pinState(pin: Pin, activeId: String?, nextId: String?): State = when (pin.id) {
        activeId -> State.ACTIVE
        nextId -> State.NEXT
        else -> if (pin.visited) State.VISITED else State.DEFAULT
    }

    /**
     * Add one state's source + symbol layer.
     *
     * Visibility is filtered by layer existence rather than by pushing an empty
     * collection: MapLibre renders nothing for an empty FeatureCollection, and
     * skipping the layer entirely keeps `queryRenderedFeatures` and the tap
     * listener honest — a layer that exists but is empty is a trap for readers.
     */
    private fun addPinLayer(style: Style, state: State, pins: List<Pin>?, sourceId: String) {
        if (pins.isNullOrEmpty()) return

        style.addSource(GeoJsonSource(sourceId, pinFeatures(pins)))

        val active = state == State.ACTIVE
        val next = state == State.NEXT
        val visited = state == State.VISITED

        val iconSize = when (state) {
            State.ACTIVE, State.NEXT -> PIN_SCALE_FOCUS
            State.DEFAULT -> PIN_SCALE_DEFAULT
            State.VISITED -> PIN_SCALE_VISITED
        }

        val layer = SymbolLayer(sourceId, sourceId).withProperties(
            PropertyFactory.iconImage(
                if (active) MapStyle.ICON_PIN_ACTIVE else MapStyle.ICON_PIN_DEFAULT,
            ),
            PropertyFactory.iconSize(iconSize),
            // Anchor the marker at its point, not its centre — the padded quad
            // means the glyph's tip is BELOW the quad centre, so the pin would
            // otherwise float off its own location.
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            // Visited pins sit back without vanishing; the walk still needs to
            // read them for orientation ("I've done that one").
            PropertyFactory.iconOpacity(if (visited) 0.55f else 1f),
            // DEFAULT places all pins like a real map. The non-overlap rule is
            // applied only to the two states that carry a LABEL, which is where
            // collision actually hurts readability.
            PropertyFactory.iconAllowOverlap(visited),
            PropertyFactory.iconIgnorePlacement(!(active || next)),
        )

        // No label is added here — see the PROP_ID/PROP_NAME comment above.
        // Marker state is carried by glyph + size + colour, all of which render
        // without a glyphs URL. The focused pin's name is drawn as a Compose
        // overlay by MapScreen instead.

        style.addLayer(layer)
    }

    private fun pinFeatures(pins: List<Pin>): FeatureCollection =
        FeatureCollection.fromFeatures(
            pins.map { p ->
                Feature.fromGeometry(
                    Point.fromLngLat(p.lng, p.lat),
                    JsonObject().apply {
                        addProperty(PROP_ID, p.id)
                        addProperty(PROP_NAME, p.name)
                    },
                )
            },
        )

    private fun lineGeometry(route: List<LatLng>): LineString =
        LineString.fromLngLats(route.map { Point.fromLngLat(it.longitude, it.latitude) })

    /** Equirectangular distance in metres — adequate well below a kilometre. */
    private fun distanceMeters(a: LatLng, b: Pin): Double {
        val earthRadiusM = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.latitude)
        val dLng = Math.toRadians(b.lng - a.longitude) *
            Math.cos(Math.toRadians((a.latitude + b.lat) / 2.0))
        return earthRadiusM * Math.sqrt(dLat * dLat + dLng * dLng)
    }
}

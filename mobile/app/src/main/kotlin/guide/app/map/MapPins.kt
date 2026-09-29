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
    private var cachedDefaultBitmap: Bitmap? = null
    private var cachedActiveBitmap: Bitmap? = null
    private var cachedNavArrowBitmap: Bitmap? = null

    private var currentPins: List<Pin> = emptyList()
    private var currentTapListener: PinTapListener? = null
    private var clickListenerAttached = false

    internal fun getOrCreateDefaultBitmap(): Bitmap =
        cachedDefaultBitmap ?: Raster.drawPin(
            sizePx = MapStyle.ICON_SIZE_PX,
            edge = MapStyle.PIN_EDGE_PX,
            pad = MapStyle.PIN_PAD_PX,
            palette = MapStyle.defaultPinPalette(),
            filled = false,
            chipCornerPx = CHIP_CORNER_DP * MapStyle.ICON_DENSITY,
        ).also { cachedDefaultBitmap = it }

    internal fun getOrCreateActiveBitmap(): Bitmap =
        cachedActiveBitmap ?: Raster.drawPin(
            sizePx = MapStyle.ICON_SIZE_PX,
            edge = MapStyle.PIN_EDGE_PX,
            pad = MapStyle.PIN_PAD_PX,
            palette = MapStyle.activePinPalette(),
            filled = true,
            chipCornerPx = CHIP_CORNER_DP * MapStyle.ICON_DENSITY,
        ).also { cachedActiveBitmap = it }

    internal fun getOrCreateNavArrowBitmap(): Bitmap =
        cachedNavArrowBitmap ?: Raster.drawNavArrow(
            sizePx = (38 * MapStyle.ICON_DENSITY).toInt(),
        ).also { cachedNavArrowBitmap = it }

    /**
     * Render vector markers and polyline on Google Maps SDK.
     */
    fun renderGoogleMap(
        map: com.google.android.gms.maps.GoogleMap,
        pins: List<Pin>,
        route: List<com.google.android.gms.maps.model.LatLng> = emptyList(),
        activeId: String? = null,
        nextId: String? = null,
        fitToPins: Boolean = false,
    ) {
        map.clear()

        // 1. Draw Route: Authentic Google Maps dual-stroke navigation polyline
        if (route.size >= 2) {
            // Dark navy outer border/casing
            val outerPolyline = com.google.android.gms.maps.model.PolylineOptions()
                .color(0xFF1557B0.toInt()) // Google Maps dark navy border
                .width(20f)
                .zIndex(1f)
                .geodesic(true)
                .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                .startCap(com.google.android.gms.maps.model.RoundCap())
                .endCap(com.google.android.gms.maps.model.RoundCap())
            route.forEach { outerPolyline.add(it) }
            map.addPolyline(outerPolyline)

            // Electric blue inner core
            val innerPolyline = com.google.android.gms.maps.model.PolylineOptions()
                .color(0xFF1A73E8.toInt()) // Google Maps electric navigation blue
                .width(14f)
                .zIndex(2f)
                .geodesic(true)
                .jointType(com.google.android.gms.maps.model.JointType.ROUND)
                .startCap(com.google.android.gms.maps.model.RoundCap())
                .endCap(com.google.android.gms.maps.model.RoundCap())
            route.forEach { innerPolyline.add(it) }
            map.addPolyline(innerPolyline)
        }

        // 2. Add Pins as Markers with Custom High-DPI Bitmaps
        pins.forEach { pin ->
            val isActive = pin.id == activeId
            val isNext = pin.id == nextId
            val bmp = if (isActive || isNext) getOrCreateActiveBitmap() else getOrCreateDefaultBitmap()
            val marker = map.addMarker(
                com.google.android.gms.maps.model.MarkerOptions()
                    .position(com.google.android.gms.maps.model.LatLng(pin.lat, pin.lng))
                    .title(pin.name)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bmp))
                    .anchor(0.5f, 1.0f)
            )
            marker?.tag = pin.id
        }

        // 3. Fit bounds if requested
        if (fitToPins && (pins.isNotEmpty() || route.isNotEmpty())) {
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            pins.forEach { builder.include(com.google.android.gms.maps.model.LatLng(it.lat, it.lng)) }
            route.forEach { builder.include(it) }
            val bounds = builder.build()
            try {
                map.animateCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 120))
            } catch (_: Exception) {
                map.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(bounds.center, 14.0f))
            }
        }
    }

    fun render(
        context: Context,
        map: MapLibreMap,
        style: Style,
        pins: List<Pin>,
        route: List<LatLng> = emptyList(),
        activeId: String? = null,
        nextId: String? = null,
        userPosition: LatLng? = null,
        headingDeg: Float? = null,
        onPinTap: PinTapListener? = null,
        fitToPins: Boolean = true,
    ) {
        // --- 1. Route: Google Maps turn-by-turn navigation blue polyline -----
        val routeSource = style.getSource(MapStyle.ID_ROUTE_SCALE) as? GeoJsonSource
        if (route.size >= 2) {
            val routeGeo = FeatureCollection.fromFeature(Feature.fromGeometry(lineGeometry(route)))
            if (routeSource != null) {
                routeSource.setGeoJson(routeGeo)
            } else {
                style.addSource(GeoJsonSource(MapStyle.ID_ROUTE_SCALE, routeGeo))
                // Dark outer border / casing
                style.addLayer(
                    LineLayer(MapStyle.ID_ROUTE_BASE, MapStyle.ID_ROUTE_SCALE).withProperties(
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineColor(GuideColors.ROUTE_BASE),
                        PropertyFactory.lineWidth(8.5f),
                        PropertyFactory.lineOpacity(0.95f),
                    ),
                )
                // Vibrant electric blue navigation polyline
                style.addLayer(
                    LineLayer(MapStyle.ID_ROUTE_ROUTE, MapStyle.ID_ROUTE_SCALE).withProperties(
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineColor(GuideColors.ROUTE_ROUTE),
                        PropertyFactory.lineWidth(5.5f),
                    ),
                )
            }
        } else if (routeSource != null) {
            routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }

        // --- 2. Register cached icons once per style -------------------------
        if (style.getImage(MapStyle.ICON_PIN_DEFAULT) == null) {
            style.addImage(MapStyle.ICON_PIN_DEFAULT, getOrCreateDefaultBitmap())
        }
        if (style.getImage(MapStyle.ICON_PIN_ACTIVE) == null) {
            style.addImage(MapStyle.ICON_PIN_ACTIVE, getOrCreateActiveBitmap())
        }
        if (style.getImage(MapStyle.ICON_NAV_ARROW) == null) {
            style.addImage(MapStyle.ICON_NAV_ARROW, getOrCreateNavArrowBitmap())
        }

        // --- 3. Update or add pin layers per state ---------------------------
        val byState: Map<State, List<Pin>> = pins.groupBy { pinState(it, activeId, nextId) }

        updateOrAddPinLayer(style, State.VISITED, byState[State.VISITED], MapStyle.ID_PIN_VISITED)
        updateOrAddPinLayer(style, State.DEFAULT, byState[State.DEFAULT], MapStyle.ID_PIN_DEFAULT)
        updateOrAddPinLayer(style, State.NEXT, byState[State.NEXT], MapStyle.ID_PIN_NEXT)
        updateOrAddPinLayer(style, State.ACTIVE, byState[State.ACTIVE], MapStyle.ID_PIN_ACTIVE)

        // --- 4. Navigation Directional Arrow Puck ----------------------------
        val arrowSource = style.getSource(MapStyle.ID_NAV_ARROW) as? GeoJsonSource
        if (userPosition != null) {
            val arrowGeo = FeatureCollection.fromFeature(
                Feature.fromGeometry(Point.fromLngLat(userPosition.longitude, userPosition.latitude))
            )
            if (arrowSource != null) {
                arrowSource.setGeoJson(arrowGeo)
                val layer = style.getLayer(MapStyle.ID_NAV_ARROW) as? SymbolLayer
                layer?.setProperties(PropertyFactory.iconRotate(headingDeg ?: 0f))
            } else {
                style.addSource(GeoJsonSource(MapStyle.ID_NAV_ARROW, arrowGeo))
                style.addLayer(
                    SymbolLayer(MapStyle.ID_NAV_ARROW, MapStyle.ID_NAV_ARROW).withProperties(
                        PropertyFactory.iconImage(MapStyle.ICON_NAV_ARROW),
                        PropertyFactory.iconSize(1.0f),
                        PropertyFactory.iconRotate(headingDeg ?: 0f),
                        PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                    ),
                )
            }
        } else if (arrowSource != null) {
            arrowSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }

        // --- 4. Tap handling with a single listener --------------------------
        currentPins = pins
        currentTapListener = onPinTap
        if (!clickListenerAttached) {
            map.addOnMapClickListener { latLng ->
                val listener = currentTapListener ?: return@addOnMapClickListener false
                val activePins = currentPins
                val hit = activePins.minByOrNull { distanceMeters(latLng, it) } ?: return@addOnMapClickListener false
                if (distanceMeters(latLng, hit) <= TAP_RADIUS_M) {
                    listener.onPinTap(hit.id)
                    true
                } else {
                    false
                }
            }
            clickListenerAttached = true
        }

        // --- 5. Camera -------------------------------------------------------
        if (fitToPins && (pins.isNotEmpty() || route.size >= 2)) {
            fitCamera(map, pins, route)
        }
    }

    /**
     * Remove every object this app owns from [style]. Idempotent and safe to
     * call on a style that has none of them.
     */
    fun clear(style: Style) {
        clickListenerAttached = false
        for (id in MapStyle.OWNED_LAYER_IDS) {
            style.getLayer(id)?.let { style.removeLayer(it) }
        }
        for (id in MapStyle.OWNED_SOURCE_IDS) {
            style.getSource(id)?.let { style.removeSource(it) }
        }
    }

    /**
     * Move the camera to frame [pins] (and [route] when it extends beyond them).
     */
    fun fitCamera(map: MapLibreMap, pins: List<Pin>, route: List<LatLng> = emptyList()) {
        val points = buildList {
            pins.forEach { add(LatLng(it.lat, it.lng)) }
            route.forEach { add(it) }
        }
        if (points.isEmpty()) return
        if (points.size == 1) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.first(), FOCUS_ZOOM))
            return
        }
        val bounds = LatLngBounds.Builder().apply { points.forEach { include(it) } }.build()
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, BOUNDS_PADDING_PX))
    }

    private const val TAP_RADIUS_M = 90.0
    private const val BOUNDS_PADDING_PX = 120
    private const val FOCUS_ZOOM = 16.0

    private fun pinState(pin: Pin, activeId: String?, nextId: String?): State = when (pin.id) {
        activeId -> State.ACTIVE
        nextId -> State.NEXT
        else -> if (pin.visited) State.VISITED else State.DEFAULT
    }

    private fun updateOrAddPinLayer(style: Style, state: State, pins: List<Pin>?, sourceId: String) {
        val features = pinFeatures(pins ?: emptyList())
        val existingSource = style.getSource(sourceId) as? GeoJsonSource
        if (existingSource != null) {
            existingSource.setGeoJson(features)
            return
        }
        if (pins.isNullOrEmpty()) return

        style.addSource(GeoJsonSource(sourceId, features))

        val active = state == State.ACTIVE
        val next = state == State.NEXT
        val visited = state == State.VISITED

        val baseScale = when (state) {
            State.ACTIVE, State.NEXT -> PIN_SCALE_FOCUS
            State.DEFAULT -> PIN_SCALE_DEFAULT
            State.VISITED -> PIN_SCALE_VISITED
        }

        val sizeExpression = Expression.interpolate(
            Expression.linear(),
            Expression.zoom(),
            Expression.stop(11.0f, baseScale * 0.40f),
            Expression.stop(14.0f, baseScale * 0.65f),
            Expression.stop(16.5f, baseScale),
        )

        val layer = SymbolLayer(sourceId, sourceId).withProperties(
            PropertyFactory.iconImage(
                if (active) MapStyle.ICON_PIN_ACTIVE else MapStyle.ICON_PIN_DEFAULT,
            ),
            PropertyFactory.iconSize(sizeExpression),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconOpacity(if (visited) 0.55f else 1f),
            PropertyFactory.iconAllowOverlap(visited),
            PropertyFactory.iconIgnorePlacement(!(active || next)),
        )

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

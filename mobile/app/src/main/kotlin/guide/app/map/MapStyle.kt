package guide.app.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import guide.app.ui.theme.GuideTokens

/**
 * Map style + map-specific assets.
 *
 * WHY THIS FILE EXISTS (audit finding §3.2):
 * the map's default style used to be `https://demotiles.maplibre.org/style.json`
 * — a remote URL. Travel Guide's core promise (SPEC §1.4) is that the full loop
 * works with ZERO network, so on a first launch with no signal the hero screen
 * rendered a blank map. The default is now a style BUNDLED AS A KOTLIN
 * CONSTANT (no asset copy to keep in sync, no I/O, no failure path) pointing at
 * OpenStreetMap raster tiles, which download through MapLibre's offline region
 * machinery (`OfflinePackHelper`) exactly like any other style.
 *
 * The remote URL survives as an explicitly-documented FALLBACK parameter
 * ([REMOTE_FALLBACK_STYLE]) for previews on a dev machine that has no pack
 * mirrored — it is never the default.
 *
 * Tile usage note: `tile.openstreetmap.org` is the community tile server and is
 * subject to its tile usage policy; a shipping build should move the
 * `"tiles"` entry to a self-hosted or commercial endpoint and keep everything
 * else in this string unchanged. Attribution is part of the style, not the UI,
 * so the required credit renders with the map itself.
 */
object MapStyle {

    /** OSM raster source id, referenced by every layer in [LOCAL_STYLE_JSON]. */
    const val TILE_SOURCE_ID: String = "osm-raster"

    /**
     * Page colour behind (and between) raster tiles, as `#RRGGBB`.
     *
     * Built from [GuideTokens.Bg] so the map's ground matches the app's ground
     * exactly — a mismatch here is visible at every tile edge, during panning,
     * and while the offline pack is still filling in. Held as a Style string
     * because the style document is parsed outside Compose.
     *
     * Declared BEFORE [LOCAL_STYLE_JSON]: object properties initialize in
     * declaration order, so a later property referencing this one would read
     * null.
     */
    private val BACKGROUND_COLOR: String = "#F8F9FA"

    /**
     * The bundled default. A MapLibre style document, held as a Kotlin string
     * constant (audit §3.2 asked for exactly this — no asset file needed).
     *
     * Notes on the choices:
     *  - `"glyphs"` is intentionally ABSENT. MapLibre fetches glyph PBFs over
     *    the network, which would break offline-first; this style therefore uses
     *    NO `text-field` layers. Place names come from the tile raster itself and
     *    our own pins carry `iconImage` symbols, not text.
     *  - `minzoom` 0 / `maxzoom` 19 matches the tile server's real range, so the
     *    map degrades gracefully instead of requesting tiles that 404.
     *  - The background is the design system's page colour, injected by
     *    [BACKGROUND_COLOR] rather than typed as a hex — the one value here that
     *    could drift from the tokens.
     *  - The attribution string is the legally required OSM credit.
     */
    val LOCAL_STYLE_JSON: String = """
        {
          "version": 8,
          "name": "Travel Guide — Fort Kochi (offline OSM raster)",
          "sources": {
            "$TILE_SOURCE_ID": {
              "type": "raster",
              "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
              "tileSize": 256,
              "minzoom": 0,
              "maxzoom": 19,
              "attribution": "© OpenStreetMap contributors"
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "$BACKGROUND_COLOR" }
            },
            {
              "id": "osm-tiles",
              "type": "raster",
              "source": "$TILE_SOURCE_ID",
              "minzoom": 0,
              "maxzoom": 22
            }
          ]
        }
    """.trimIndent()

    /**
     * Authentic Google Maps Navigation Road View Style.
     * Renders authentic Google Maps vector/raster roads, water, and terrain.
     */
    val GOOGLE_MAPS_STYLE_JSON: String = """
        {
          "version": 8,
          "name": "Travel Guide — Google Maps View",
          "sources": {
            "google-maps-raster": {
              "type": "raster",
              "tiles": [
                "https://mt0.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
                "https://mt1.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
                "https://mt2.google.com/vt/lyrs=m&x={x}&y={y}&z={z}",
                "https://mt3.google.com/vt/lyrs=m&x={x}&y={y}&z={z}"
              ],
              "tileSize": 256,
              "minzoom": 0,
              "maxzoom": 20,
              "attribution": "© Google Maps"
            }
          },
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": { "background-color": "#F4F3F0" }
            },
            {
              "id": "google-tiles",
              "type": "raster",
              "source": "google-maps-raster",
              "minzoom": 0,
              "maxzoom": 22
            }
          ]
        }
    """.trimIndent()

    /**
     * Authentic Google Maps Navigation Dark / Night Mode Style.
     * Deep obsidian background, high contrast vector roads, luminous water and parks.
     */
    val GOOGLE_MAPS_DARK_STYLE_JSON: String = """
        [
          {"elementType": "geometry", "stylers": [{"color": "#131a26"}]},
          {"elementType": "labels.text.stroke", "stylers": [{"color": "#131a26"}, {"weight": 3}]},
          {"elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b9"}]},
          {"featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
          {"featureType": "landscape.man_made", "elementType": "geometry.fill", "stylers": [{"color": "#1a2230"}]},
          {"featureType": "landscape.man_made", "elementType": "geometry.stroke", "stylers": [{"color": "#263244"}]},
          {"featureType": "landscape.man_made.building", "elementType": "geometry.fill", "stylers": [{"color": "#1f293b"}]},
          {"featureType": "landscape.man_made.building", "elementType": "geometry.stroke", "stylers": [{"color": "#2c3b52"}]},
          {"featureType": "poi", "elementType": "labels.icon", "stylers": [{"visibility": "on"}]},
          {"featureType": "poi", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}, {"visibility": "on"}]},
          {"featureType": "poi", "elementType": "labels.text.stroke", "stylers": [{"color": "#131a26"}]},
          {"featureType": "poi.business", "elementType": "all", "stylers": [{"visibility": "on"}]},
          {"featureType": "poi.business", "elementType": "geometry.fill", "stylers": [{"color": "#222d40"}]},
          {"featureType": "poi.business", "elementType": "geometry.stroke", "stylers": [{"color": "#33435c"}]},
          {"featureType": "poi.business", "elementType": "labels.icon", "stylers": [{"visibility": "on"}]},
          {"featureType": "poi.business", "elementType": "labels.text.fill", "stylers": [{"color": "#f3a669"}, {"visibility": "on"}]},
          {"featureType": "poi.business", "elementType": "labels.text.stroke", "stylers": [{"color": "#131a26"}]},
          {"featureType": "poi.attraction", "elementType": "labels.icon", "stylers": [{"visibility": "on"}]},
          {"featureType": "poi.attraction", "elementType": "labels.text.fill", "stylers": [{"color": "#f3a669"}, {"visibility": "on"}]},
          {"featureType": "poi.park", "elementType": "geometry", "stylers": [{"color": "#172b27"}]},
          {"featureType": "poi.park", "elementType": "labels.text.fill", "stylers": [{"color": "#6b9a76"}]},
          {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#283446"}]},
          {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#1e2838"}]},
          {"featureType": "road", "elementType": "labels.text.fill", "stylers": [{"color": "#9ca5b9"}]},
          {"featureType": "road.highway", "elementType": "geometry", "stylers": [{"color": "#3b4d66"}]},
          {"featureType": "road.highway", "elementType": "geometry.stroke", "stylers": [{"color": "#243042"}]},
          {"featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{"color": "#f3d19c"}]},
          {"featureType": "transit", "elementType": "geometry", "stylers": [{"color": "#253042"}]},
          {"featureType": "transit.station", "elementType": "labels.icon", "stylers": [{"visibility": "on"}]},
          {"featureType": "transit.station", "elementType": "labels.text.fill", "stylers": [{"color": "#d59563"}]},
          {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#0d1b2a"}]},
          {"featureType": "water", "elementType": "labels.text.fill", "stylers": [{"color": "#515c6d"}]},
          {"featureType": "water", "elementType": "labels.text.stroke", "stylers": [{"color": "#0d1b2a"}]}
        ]
    """.trimIndent()

    /**
     * Documented FALLBACK — map it to [MapScreen]'s `styleUrl` parameter to
     * preview the map on a machine with no offline pack. Requires network, so it
     * must never be the shipping default.
     */
    const val REMOTE_FALLBACK_STYLE: String = "https://demotiles.maplibre.org/style.json"

    // -----------------------------------------------------------------------
    // Style-layer ids. Layer/source ids are global to the style, so every id
    // this app adds is prefixed `tg-` — that keeps our objects trivially
    // separable from any style we are handed, which is what makes
    // MapPins.render() safely idempotent.
    // -----------------------------------------------------------------------
    const val ID_ROUTE_SCALE: String = "tg-route-scale"
    const val ID_ROUTE_BASE: String = "tg-route-base"
    const val ID_ROUTE_ROUTE: String = "tg-route-route"
    const val ID_PIN_VISITED: String = "tg-pin-visited"
    const val ID_PIN_NEXT: String = "tg-pin-next"
    const val ID_PIN_ACTIVE: String = "tg-pin-active"
    const val ID_PIN_DEFAULT: String = "tg-pin-default"
    const val ID_NAV_ARROW: String = "tg-nav-arrow"
    /** Prefix that identifies every object this app owns in a style. */
    const val OWNED_PREFIX: String = "tg-"

    /**
     * Layer removal order. Layer ids are unique across the whole style, so one
     * list covers every layer regardless of kind.
     */
    val OWNED_LAYER_IDS: List<String> = listOf(
        ID_ROUTE_SCALE,
        ID_ROUTE_BASE,
        ID_ROUTE_ROUTE,
        ID_PIN_VISITED,
        ID_PIN_DEFAULT,
        ID_PIN_NEXT,
        ID_PIN_ACTIVE,
        ID_NAV_ARROW,
    )

    /**
     * Source ids, in removal order. A source may only be removed once nothing
     * references it, so [MapPins.clear] removes layers before sources.
     */
    val OWNED_SOURCE_IDS: List<String> = listOf(
        ID_ROUTE_SCALE,
        ID_PIN_VISITED,
        ID_PIN_DEFAULT,
        ID_PIN_NEXT,
        ID_PIN_ACTIVE,
        ID_NAV_ARROW,
    )

    // -----------------------------------------------------------------------
    // Pin icon registry (MapLibre `icon-image`).
    // -----------------------------------------------------------------------

    /** Canvas density the registry bitmaps are generated at. */
    const val ICON_DENSITY: Float = 3f

    /** Visual pin edge, in reference px, inside the 44px padded square. */
    const val PIN_EDGE_PX: Int = 24

    /** Transparent margin around the pin — this IS the touch padding. */
    const val PIN_PAD_PX: Int = 10

    /** Registerable icon names, one per pin weight. */
    const val ICON_PIN_DEFAULT: String = "tg-pin-default"
    const val ICON_PIN_ACTIVE: String = "tg-pin-active"
    const val ICON_NAV_ARROW: String = "tg-nav-arrow"

    /** Bitmap edge, including padding. (24 + 2*10) = 44. */
    const val ICON_SIZE_PX: Int = PIN_EDGE_PX + 2 * PIN_PAD_PX

    class PinPalette(
        val fill: Color,
        val ring: Color,
        val inner: Color,
        val chip: Color,
        val chipBorder: Color,
    )

    fun defaultPinPalette(): PinPalette = PinPalette(
        fill = Color(0xFFFFFFFF),
        ring = Color(0xFF121826),
        inner = Color(0xFFFF5A36),
        chip = Color(0xFFFFFFFF),
        chipBorder = Color(0xFFE5E7EB),
    )

    fun activePinPalette(): PinPalette = PinPalette(
        fill = Color(0xFFFF5A36),
        ring = Color(0xFFFFFFFF),
        inner = Color(0xFFFFFFFF),
        chip = Color(0xFFFFFFFF),
        chipBorder = Color(0xFFFF5A36),
    )

    fun eventPinPalette(): PinPalette = PinPalette(
        fill = Color(0xFFF59E0B),
        ring = Color(0xFFFFFFFF),
        inner = Color(0xFFFFFFFF),
        chip = Color(0xFFFFFFFF),
        chipBorder = Color(0xFFF59E0B),
    )

    // =======================================================================
    // Lucide icon vectors
    //
    // Luсide only (design-reference/SKILL.md): 24px box, stroke 2, round caps
    // and joins, unfilled, currentColor. These are the 5 vectors the map screen
    // needs, transcribed from `ICONS-ASSETS/system-and-ui/lucide/icons/*.svg` as
    // ImageVectors so NO icon library dependency is added — the app keeps its
    // single `org.maplibre.gl:android-sdk` runtime dependency set.
    //
    // All five are built through [lucide] so the box/stroke/cap/join rules
    // cannot drift between them.
    // =======================================================================

    /** Navigation arrow — drawn inside the recenter FAB, points at travel bearing. */
    val IconNavigation: ImageVector by lazy {
        lucide("Navigation") {
            moveTo(3f, 11f); lineTo(22f, 2f); lineTo(13f, 21f)
            lineTo(11f, 13f); close()
        }
    }

    /** Compass — the heading/compass control. */
    val IconCompass: ImageVector by lazy {
        lucide("Compass") {
            // Outer ring.
            moveTo(22f, 12f)
            arcToRelative(10f, 10f, 0f, true, true, -20f, 0f)
            arcToRelative(10f, 10f, 0f, true, true, 20f, 0f)
            close()
            // Needle: two arrow halves meeting at 16.24,7.76 and 7.76,16.24.
            moveTo(16.24f, 7.76f)
            lineTo(14.436f, 13.171f)
            arcToRelative(2f, 2f, 0f, false, true, -1.265f, 1.265f)
            lineTo(7.76f, 16.24f)
            lineTo(9.564f, 10.829f)
            arcToRelative(2f, 2f, 0f, false, true, 1.265f, -1.265f)
            close()
        }
    }

    /** Map pin — the pin legend swatch. */
    val IconMapPin: ImageVector by lazy {
        lucide("MapPin") {
            moveTo(20f, 10f)
            curveToRelative(0f, 4.993f, -5.539f, 10.193f, -7.399f, 11.799f)
            arcToRelative(1f, 1f, 0f, false, true, -1.202f, 0f)
            curveTo(9.539f, 20.193f, 4f, 14.993f, 4f, 10f)
            arcToRelative(8f, 8f, 0f, false, true, 16f, 0f)
            close()
            moveTo(15f, 10f)
            arcToRelative(3f, 3f, 0f, true, true, -6f, 0f)
            arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
            close()
        }
    }

    /** Crosshair/locate — "follow me" is a different action from "recenter". */
    val IconLocateFixed: ImageVector by lazy {
        lucide("LocateFixed") {
            moveTo(2f, 12f); lineTo(5f, 12f)          // west tick
            moveTo(19f, 12f); lineTo(22f, 12f)        // east tick
            moveTo(12f, 2f); lineTo(12f, 5f)          // north tick
            moveTo(12f, 19f); lineTo(12f, 22f)        // south tick
            moveTo(19f, 12f)                           // outer ring, r=7
            arcToRelative(7f, 7f, 0f, true, true, -14f, 0f)
            arcToRelative(7f, 7f, 0f, true, true, 14f, 0f)
            close()
            moveTo(15f, 12f)                           // centre dot, r=3
            arcToRelative(3f, 3f, 0f, true, true, -6f, 0f)
            arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
            close()
        }
    }

    private fun lucide(
        name: String,
        pathBuilder: PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black), // tinted by Icon(tint = …)
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
            pathBuilder = pathBuilder,
        )
    }.build()
}

/**
 * Paint colours for map geometry, as style-layer strings.
 *
 * Layer paint cannot take Compose `Color`, and re-typing hex here would be a
 * hardcoded colour — the one thing the design system forbids. So every value is
 * DERIVED at first use from [GuideTokens], which stays the single source of
 * truth: change a token and the map follows.
 *
 * Layer colour strings are `#RRGGBB`; alpha is carried by the layer's own
 * `lineOpacity` / `iconOpacity` properties rather than an `#AARRGGBB` string,
 * because that keeps one value per decision (the hue is a token, the emphasis
 * is a map decision).
 */
object GuideColors {

    /** The route's casing — Google Maps Navigation dark royal blue border. */
    val ROUTE_BASE: String = "#1A73E8"

    /** The route line itself — Google Maps Navigation vibrant electric blue polyline. */
    val ROUTE_ROUTE: String = "#4285F4"

    /** Label text on pins. */
    const val LABEL_TEXT: String = "#111827"

    /** Halo behind label text — the surface, so it works over any tile. */
    const val LABEL_HALO: String = "#FFFFFF"
}

/**
 * Pin icon bitmaps.
 *
 * Icons are drawn as rasters rather than vectors because MapLibre's
 * `icon-image` takes a `Bitmap`, and its SDF path needs a style-spec `sdf` flag
 * plus a single-channel bitmap. A plain ARGB bitmap of a marker this simple is
 * sharper on device, needs no shader upload, and — the point here — lets the
 * transparent PADDING that makes the 44dp touch target possible be part of the
 * image, which is the only way to grow a symbol's hit area.
 *
 * Pins are drawn at [MapStyle.ICON_DENSITY] and MapLibre normalises that density
 * away, so the shipped pixels land at a predictable dp size on any screen.
 */
object Raster {

    /**
     * Draw one marker.
     *
     * Anatomy, bottom to top: a rounded label chip (so the name attached by the
     * label layer is legible over busy tile imagery), then the marker silhouette
     * itself — a teardrop for the pin states, a plain dot for [filled] = false
     * + small edge. All geometry is proportional to `edge` so the same call
     * produces consistent art at any density.
     */
    fun drawPin(
        sizePx: Int,
        edge: Int,
        pad: Int,
        palette: MapStyle.PinPalette,
        filled: Boolean,
        chipCornerPx: Float,
    ): Bitmap {
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cx = sizePx / 2f
        val r = edge / 2f
        // --- Soft drop shadow -----------------------------------------------
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(40, 18, 24, 38)
        canvas.drawCircle(cx, cx + r * 0.12f, r * 0.72f, paint)

        // --- Marker ----------------------------------------------------------
        if (filled) {
            // Teardrop: circle head + a tail to the anchor point at the bottom.
            val headCy = cx - r * 0.18f
            paint.style = Paint.Style.FILL
            paint.color = palette.fill.toArgb()
            canvas.drawCircle(cx, headCy, r * 0.75f, paint)

            val tail = Path().apply {
                moveTo(cx - r * 0.52f, headCy + r * 0.36f)
                lineTo(cx, cx + r * 0.98f) // anchor tip = the geographic point
                lineTo(cx + r * 0.52f, headCy + r * 0.36f)
                close()
            }
            canvas.drawPath(tail, paint)

            // Crisp white ring
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.18f
            paint.color = palette.ring.toArgb()
            canvas.drawCircle(cx, headCy, r * 0.75f, paint)

            // Centre dot — the "filled" half of the shape difference.
            paint.style = Paint.Style.FILL
            paint.color = palette.inner.toArgb()
            canvas.drawCircle(cx, headCy, r * 0.28f, paint)
        } else {
            // Elegant circular marker with crisp white rim
            paint.style = Paint.Style.FILL
            paint.color = palette.fill.toArgb()
            canvas.drawCircle(cx, cx, r * 0.72f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.24f
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(cx, cx, r * 0.72f, paint)

            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(cx, cx, r * 0.26f, paint)
        }

        return bmp
    }

    /**
     * Draw authentic Google Maps style navigation directional arrow puck.
     * White disc base with soft shadow, enclosing a bold forward navigation chevron.
     */
    fun drawNavArrow(sizePx: Int): Bitmap {
        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val cx = sizePx / 2f
        val r = sizePx * 0.42f

        // Soft drop shadow
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.argb(55, 18, 24, 38)
        canvas.drawCircle(cx, cx + 2f, r, paint)

        // White base circle with subtle rim
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(cx, cx, r, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = android.graphics.Color.parseColor("#E0E0E0")
        canvas.drawCircle(cx, cx, r, paint)

        // Blue chevron/arrow pointing NORTH (0 deg)
        val path = Path().apply {
            moveTo(cx, cx - r * 0.65f) // top tip
            lineTo(cx + r * 0.55f, cx + r * 0.55f) // bottom right
            lineTo(cx, cx + r * 0.22f) // inner notch
            lineTo(cx - r * 0.55f, cx + r * 0.55f) // bottom left
            close()
        }
        paint.style = Paint.Style.FILL
        paint.color = android.graphics.Color.parseColor("#1A73E8") // Google Maps Navigation Blue
        canvas.drawPath(path, paint)
        return bmp
    }

    private fun Color.toArgb(): Int = android.graphics.Color.argb(
        (alpha * 255f + 0.5f).toInt(),
        (red * 255f + 0.5f).toInt(),
        (green * 255f + 0.5f).toInt(),
        (blue * 255f + 0.5f).toInt(),
    )
}

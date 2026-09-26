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
    private val BACKGROUND_COLOR: String = run {
        val bg = GuideTokens.Bg
        String.format(
            "#%02X%02X%02X",
            (bg.red * 255f + 0.5f).toInt(),
            (bg.green * 255f + 0.5f).toInt(),
            (bg.blue * 255f + 0.5f).toInt(),
        )
    }

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
              "paint": { "background-color": $BACKGROUND_COLOR }
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
    )

    // -----------------------------------------------------------------------
    // Pin icon registry (MapLibre `icon-image`).
    //
    // Touch-target math, because this is the defect the audit called out
    // (circleRadius 7 -> ~14px, far under the 44dp minimum):
    // MapLibre renders a symbol at its intrinsic bitmap size scaled by
    // `iconSize`, inside a padded quad. The PAD is part of the icon and part of
    // the tap target — but it is invisible.
    //   bitmap  = (24 + 2*10) = 44px @ 1x   -> touches the 44px floor bare
    //   shipped = 44 * (drawable density = 3) at baseline, so MapLibre's
    //             density normalisation renders it at exactly 44dp on a 3x phone
    //   drawn   = 44 * 0.75 (DEFAULT_PIN_SIZE) = 33dp of visible pin
    //                       * 1.16 (ACTIVE bump)  = 38dp at the largest state
    //   target  = 44 * 0.75 = 33dp ... so DEFAULT_PIN_SIZE must not go below
    //             1.0 if this bitmap is regenerated with a smaller pad.
    // Two properties of that math matter and are load-bearing:
    //   1. `iconImage` is SINGLE-VALUED per layer, so each pin state is its own
    //      GeoJsonSource + SymbolLayer pair. Data-driven `icon-image` would need
    //      an `image`-type property or a sprite sheet; this is more layer objects
    //      and zero style-spec risk.
    //   2. `iconSize` CAN carry the state difference while `iconImage` stays
    //      uniform across the layers — that keeps ONE icon bitmap per visual
    //      weight, and the per-state shape change is the marker's own glyph
    //      (pin vs. smaller dot) drawn into the bitmap by [Raster.drawPin].
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

    /** Bitmap edge, including padding. (24 + 2*10) = 44. */
    const val ICON_SIZE_PX: Int = PIN_EDGE_PX + 2 * PIN_PAD_PX

    /**
     * Pixel-palette for the raster pin icons, resolved from the design system
     * by the caller ([MapPins] passes [GuideTokens] values in). Bitmap drawing
     * cannot read Compose Colors, so they are handed over explicitly rather than
     * re-typed as hex — that keeps the "no hardcoded colour" rule intact.
     */
    class PinPalette(
        val fill: Color,
        val ring: Color,
        val inner: Color,
        /** Tint of the label chip behind the pin glyph. */
        val chip: Color,
        val chipBorder: Color,
    )

    fun defaultPinPalette(): PinPalette = PinPalette(
        fill = GuideTokens.Surface,
        ring = GuideTokens.Primary,
        inner = GuideTokens.Primary,
        chip = GuideTokens.Surface1,
        chipBorder = GuideTokens.Border,
    )

    fun activePinPalette(): PinPalette = PinPalette(
        fill = GuideTokens.Highlight,
        ring = GuideTokens.OnPrimary,
        inner = GuideTokens.OnPrimary,
        chip = GuideTokens.Surface1,
        chipBorder = GuideTokens.Border,
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

    /** The route's casing — the design system's text colour at low opacity. */
    val ROUTE_BASE: String = hex(GuideTokens.Text)

    /** The route line itself — the warm highlight, the map's one loud colour. */
    val ROUTE_ROUTE: String = hex(GuideTokens.Highlight)

    /** Label text on pins. */
    val LABEL_TEXT: String = hex(GuideTokens.Text)

    /** Halo behind label text — the surface, so it works over any tile. */
    val LABEL_HALO: String = hex(GuideTokens.Surface)

    private fun hex(color: Color): String {
        val r = (color.red * 255f + 0.5f).toInt()
        val g = (color.green * 255f + 0.5f).toInt()
        val b = (color.blue * 255f + 0.5f).toInt()
        return String.format("#%02X%02X%02X", r, g, b)
    }
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

        // --- Label chip: a rounded plate the glyph sits on -------------------
        // Inset on one side so the chip reads as a callout, not a box.
        val chip = RectF(
            cx - r * 1.05f,
            cx - r * 1.05f,
            cx + r * 1.05f,
            cx + r * 1.05f,
        )
        paint.style = Paint.Style.FILL
        paint.color = palette.chip.toArgb()
        canvas.drawRoundRect(chip, chipCornerPx, chipCornerPx, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * (edge / 24f)
        paint.color = palette.chipBorder.toArgb()
        canvas.drawRoundRect(chip, chipCornerPx, chipCornerPx, paint)

        // --- Marker ----------------------------------------------------------
        if (filled) {
            // Teardrop: circle head + a tail to the anchor point at the bottom.
            val headCy = cx - r * 0.18f
            paint.style = Paint.Style.FILL
            paint.color = palette.fill.toArgb()
            canvas.drawCircle(cx, headCy, r * 0.72f, paint)

            val tail = Path().apply {
                moveTo(cx - r * 0.50f, headCy + r * 0.36f)
                lineTo(cx, cx + r * 0.98f) // anchor tip = the geographic point
                lineTo(cx + r * 0.50f, headCy + r * 0.36f)
                close()
            }
            canvas.drawPath(tail, paint)

            // Ring so the marker separates from the tile beneath it.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.20f
            paint.color = palette.ring.toArgb()
            canvas.drawCircle(cx, headCy, r * 0.72f, paint)

            // Centre dot — the "filled" half of the shape difference.
            paint.style = Paint.Style.FILL
            paint.color = palette.inner.toArgb()
            canvas.drawCircle(cx, headCy, r * 0.28f, paint)
        } else {
            // Hollow dot: ring only. Reads lighter and smaller than the
            // teardrop, which is exactly the point — state is legible in
            // silhouette, before colour.
            paint.style = Paint.Style.FILL
            paint.color = GuideTokens.Surface.toArgb()
            canvas.drawCircle(cx, cx, r * 0.74f, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = r * 0.30f
            paint.color = palette.ring.toArgb()
            canvas.drawCircle(cx, cx, r * 0.74f, paint)

            paint.style = Paint.Style.FILL
            paint.color = palette.inner.toArgb()
            canvas.drawCircle(cx, cx, r * 0.26f, paint)
        }

        return bmp
    }

    private fun Color.toArgb(): Int = android.graphics.Color.argb(
        (alpha * 255f + 0.5f).toInt(),
        (red * 255f + 0.5f).toInt(),
        (green * 255f + 0.5f).toInt(),
        (blue * 255f + 0.5f).toInt(),
    )
}

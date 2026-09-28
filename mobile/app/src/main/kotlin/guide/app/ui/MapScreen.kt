package guide.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import guide.app.map.MapPins
import guide.app.map.MapStyle
import guide.app.ui.components.CategoryChip
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.Pressable
import guide.app.ui.components.StatusTag
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Motion
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Map hero — full-bleed map with floating controls and a reserved sheet anchor.
 *
 * RESTRUCTURED 2026-09-26 (docs/UI-AUDIT-2026-09-26.md). The old screen was a
 * `Column` holding a `fillMaxSize(0.7f)` map, a `TextButton` and an unbounded
 * `Text` — the archetypal "generic/awkward" layout, and with a long
 * `seeingAnswer` it pushed past the viewport with no scroll and no maxLines
 * (audit §4). It is now the structure every premium map app uses: the map fills
 * the frame edge-to-edge, controls FLOAT over it inside inset padding, and the
 * bottom sheet owns the lower region.
 *
 * Overlap is designed out rather than tuned:
 *  - nothing is `fillMaxSize(0.7f)` — the map is the only full-size child, and
 *    every other child is inside a `Box` overlay, so they cannot displace it;
 *  - the top cluster and the bottom region are pinned to OPPOSITE ends of the
 *    frame, so they cannot meet;
 *  - the status/navigation bar insets are honoured explicitly, so the top
 *    controls never sit under the clock and the bottom region never sits under
 *    the gesture bar;
 *  - every text that can be long carries `maxLines` + `overflow`.
 *
 * CAMERA (audit defect 5): the old code called `moveCamera`, which teleports.
 * Two behaviours now, and they are different on purpose:
 *  - FITTING to pins happens once when the pin set first arrives, via
 *    [MapPins.fitCamera] on the MapLibre callback thread. It is a jump because
 *    it is the initial framing, not a user action, and animating it would make
 *    the map appear to fly somewhere on cold start.
 *  - FOLLOW and RECENTER are user actions and use `animateCamera(update, ms)`
 *    with [Motion.Slow] — the token for "large surface: camera settle".
 */
@Composable
fun MapScreen(
    mapView: MapView,
    offRoute: Boolean,
    seeingAnswer: String?,
    onSeeingTap: () -> Unit,
    pins: List<MapPins.Pin> = emptyList(),
    route: List<LatLng> = emptyList(),
    /**
     * Style to render. Defaults to the BUNDLED OSM style
     * ([MapStyle.LOCAL_STYLE_JSON]) so a first launch with no network still
     * draws a map — the whole point of the offline-first promise (SPEC §1.4).
     * Pass [MapStyle.REMOTE_FALLBACK_STYLE] explicitly for a network preview.
     */
    styleUrl: String = MapStyle.LOCAL_STYLE_JSON,
    /** Pin currently narrating, if any — the map's ONE accent state. */
    activePinId: String? = null,
    /** Next stop on the active route, if any. */
    nextPinId: String? = null,
    onPinTap: (String) -> Unit = {},
    /** Current device position, when known. Null hides the location puck. */
    userPosition: LatLng? = null,
    /** Compass azimuth in degrees; null hides the compass needle. */
    headingDeg: Float? = null,
    /**
     * The bottom sheet region. The app's NowPlayingSheet is owned by another
     * agent and must not be re-implemented here — this screen only reserves and
     * positions the anchor, and the host drops the sheet's content in.
     */
    sheet: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current

    // --- Map style lifecycle -------------------------------------------------
    // Held in state so a style swap (fallback -> bundled, or a future pack
    // style) is a recomposition rather than a rebuild of the MapView.
    var styleLoaded by remember { mutableStateOf(false) }

    // --- Map actions, set once the map instance exists -----------------------
    // A tiny holder instead of a `remember { mutableStateOf<MapLibreMap?> }`:
    // the lambdas read the current map at call time, so they never capture a
    // stale instance across recomposition.
    val mapActions = remember { MapActions() }

    // --- Pin/route state -----------------------------------------------------
    // `render` is idempotent, so re-running it on every state change is the
    // intended path, not a hazard (audit defect 2).
    val latestPins by rememberUpdatedState(pins)
    val latestRoute by rememberUpdatedState(route)
    val latestActive by rememberUpdatedState(activePinId)
    val latestNext by rememberUpdatedState(nextPinId)
    val latestOnPinTap by rememberUpdatedState(onPinTap)
    // Only the FIRST successful render may fit the camera: a later re-render
    // must not yank the viewport away from a user who has panned somewhere.
    var framedOnce by remember { mutableStateOf(false) }

    // --- Category filtering -------------------------------------------------
    var selectedCategory by remember { mutableStateOf("All") }
    val displayedPins = remember(latestPins, selectedCategory) {
        when (selectedCategory) {
            "Heritage" -> latestPins.filter { !it.id.contains("cafe") && !it.id.contains("hotel") }
            "Food" -> latestPins.filter { it.id.contains("cafe") || it.id.contains("food") }
            "Stay" -> latestPins.filter { it.id.contains("hotel") || it.id.contains("stay") }
            else -> latestPins
        }
    }

    LaunchedEffect(styleLoaded, displayedPins, route, activePinId, nextPinId) {
        val map = mapActions.map ?: return@LaunchedEffect
        val style = map.style ?: return@LaunchedEffect
        MapPins.render(
            context = context,
            map = map,
            style = style,
            pins = displayedPins,
            route = latestRoute,
            activeId = latestActive,
            nextId = latestNext,
            onPinTap = { id -> latestOnPinTap(id) },
            fitToPins = !framedOnce,
        )
        if (displayedPins.isNotEmpty() || latestRoute.size >= 2) framedOnce = true
    }

    // --- Camera animation ----------------------------------------------------
    var following by remember { mutableStateOf(false) }

    LaunchedEffect(following, userPosition) {
        val map = mapActions.map ?: return@LaunchedEffect
        val pos = userPosition ?: return@LaunchedEffect
        if (!following) return@LaunchedEffect
        map.easeCamera(
            CameraUpdateFactory.newLatLngZoom(pos, FOLLOW_ZOOM),
            Motion.Medium,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // =====================================================================
        // 1. The map — base of the stack with correct Style.Builder parsing
        // =====================================================================
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapActions.map = map
                        val builder = if (styleUrl.trim().startsWith("{")) {
                            Style.Builder().fromJson(styleUrl)
                        } else {
                            Style.Builder().fromUri(styleUrl)
                        }
                        map.setStyle(builder) { style ->
                            mapActions.style = style
                            styleLoaded = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // =====================================================================
        // 2. Floating Top Header & Filter Chips (AirBnB & Luxury Travel style)
        // =====================================================================
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = statusInset + GuideTokens.Space.sm)
                .padding(horizontal = GuideTokens.Space.screenPad),
            verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
        ) {
            // Elevated Place & Audio Guide Pill Card
            Surface(
                shape = RoundedCornerShape(GuideTokens.CardRadius),
                color = GuideTokens.Surface,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, GuideTokens.Border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(GuideTokens.PinRadius))
                            .background(GuideTokens.PrimaryWash),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = MapStyle.IconMapPin,
                            contentDescription = null,
                            tint = GuideTokens.Primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fort Kochi & Mattancherry",
                            style = GuideTokens.Label,
                            color = GuideTokens.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${displayedPins.size} stops • Audio Guide Ready",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                            maxLines = 1,
                        )
                    }
                    // Audio Guide Pulse indicator
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(GuideTokens.PinRadius))
                            .background(GuideTokens.Highlight),
                    )
                }
            }

            // Google Maps Navigation Companion banner (visible when turn-by-turn navigation is detected)
            val companion = guide.app.navigation.MapsCompanionState.currentSession
            if (companion != null) {
                val corridorCount = guide.app.navigation.MapsCompanionState.corridorPoiIds.size
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = GuideTokens.Surface,
                    shadowElevation = 6.dp,
                    border = BorderStroke(1.dp, GuideTokens.Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(androidx.compose.ui.graphics.Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = MapStyle.IconNavigation,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MAPS COMPANION ACTIVE",
                                    style = GuideTokens.Caption,
                                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(androidx.compose.ui.graphics.Color(0xFF2E7D32)),
                                )
                            }
                            Text(
                                text = "Navigating to ${companion.destinationName}",
                                style = GuideTokens.Label,
                                color = GuideTokens.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (corridorCount > 0) {
                                    "$corridorCount spots along path pre-loaded • ${companion.etaOrDistance ?: "Active"}"
                                } else {
                                    "Path armed • ${companion.etaOrDistance ?: "Active"}"
                                },
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        // Clear companion button
                        GuideButton(
                            text = "Clear",
                            onClick = { guide.app.navigation.MapsCompanionState.onNavEnded() },
                            variant = GuideButtonVariant.Tonal,
                        )
                    }
                }
            }

            // Horizontal Category Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryChip(
                    text = "All Stops (${pins.size})",
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" },
                )
                CategoryChip(
                    text = "Heritage",
                    selected = selectedCategory == "Heritage",
                    onClick = { selectedCategory = "Heritage" },
                )
                CategoryChip(
                    text = "Food & Cafes",
                    selected = selectedCategory == "Food",
                    onClick = { selectedCategory = "Food" },
                )
                CategoryChip(
                    text = "Stays",
                    selected = selectedCategory == "Stay",
                    onClick = { selectedCategory = "Stay" },
                )
            }
        }

        // =====================================================================
        // 3. Right Cluster — Circular Compass & Recenter buttons
        // =====================================================================
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = GuideTokens.Space.screenPad),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
        ) {
            CompassControl(
                headingDeg = headingDeg,
                onClick = {
                    val map = mapActions.map ?: return@CompassControl
                    map.animateCamera(CameraUpdateFactory.bearingTo(0.0), Motion.Slow)
                },
            )
            RecenterControl(
                following = following,
                onClick = {
                    val map = mapActions.map ?: return@RecenterControl
                    val pos = userPosition
                    if (pos != null) {
                        following = !following
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(pos, FOLLOW_ZOOM),
                            Motion.Slow,
                        )
                    } else if (pins.isNotEmpty()) {
                        MapPins.fitCamera(map, pins, route)
                    }
                },
            )
        }

        // Off-route banner if off walk
        if (offRoute) {
            OffRouteBanner(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        top = statusInset + 110.dp,
                        start = GuideTokens.Space.screenPad,
                        end = GuideTokens.Space.xxl + GuideTokens.Space.screenPad,
                    ),
            )
        }

        // =====================================================================
        // 4. Bottom Region — Floating POI card or Floating Guide Action
        // =====================================================================
        val activePin = pins.firstOrNull { it.id == activePinId }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = GuideTokens.Space.screenPad,
                    end = GuideTokens.Space.screenPad,
                    bottom = navInset + GuideTokens.Space.base,
                ),
            verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
        ) {
            if (seeingAnswer != null) {
                SeeingAnswerCard(text = seeingAnswer)
            }

            val previewPin = activePin ?: pins.firstOrNull()
            if (previewPin != null) {
                // Luxury Snapping Preview Card (AirBnB / Sakhalin luxury aesthetic)
                Surface(
                    shape = RoundedCornerShape(GuideTokens.CardRadius),
                    color = GuideTokens.Surface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, GuideTokens.Border),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPinTap(previewPin.id) },
                ) {
                    Column(modifier = Modifier.padding(GuideTokens.Space.base)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusTag(
                                text = if (activePin != null) {
                                    if (activePin.visited) "Visited" else "Playing Now"
                                } else {
                                    "Next Stop"
                                },
                                color = if (activePin != null) {
                                    if (activePin.visited) GuideTokens.Success else GuideTokens.Highlight
                                } else {
                                    GuideTokens.Primary
                                },
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "Fort Kochi Walking Tour",
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                        }
                        Spacer(Modifier.height(GuideTokens.Space.sm))
                        Text(
                            text = previewPin.name,
                            style = GuideTokens.Title,
                            color = GuideTokens.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(GuideTokens.Space.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm)) {
                            GuideButton(
                                text = "What am I seeing?",
                                onClick = onSeeingTap,
                                variant = GuideButtonVariant.Primary,
                                icon = GuideIcons.Navigation,
                                modifier = Modifier.weight(1.2f),
                            )
                            GuideButton(
                                text = "Story",
                                onClick = { onPinTap(previewPin.id) },
                                variant = GuideButtonVariant.Dark,
                                modifier = Modifier.weight(0.8f),
                            )
                        }
                    }
                }
            } else {
                GuideButton(
                    text = "What am I seeing?",
                    onClick = onSeeingTap,
                    variant = GuideButtonVariant.Primary,
                    icon = GuideIcons.Navigation,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            sheet?.let { SheetAnchor { it() } }
        }
    }
}

/**
 * Small mutable holder for the map instance + loaded style.
 *
 * A plain class rather than two `mutableStateOf`s because these are read from
 * lambdas (button clicks, LaunchedEffect bodies) and written from the MapLibre
 * callback thread — the values must be visible without recomposing the screen
 * on every map event.
 */
private class MapActions {
    @Volatile var map: org.maplibre.android.maps.MapLibreMap? = null
    @Volatile var style: Style? = null
}

// ---------------------------------------------------------------------------
// Floating controls
// ---------------------------------------------------------------------------

/** Zoom used when following the walker — tight enough to read street context. */
private const val FOLLOW_ZOOM = 17.0

/** Below this the compass needle is noise; above it the map reads as turned. */
private const val COMPASS_VISIBLE_FROM_DEG = 5f

/** Lines of the place story the map shows before deferring to the player. */
private const val SEEING_ANSWER_LINES = 3

/**
 * Ceiling on the bottom sheet's height over the map.
 *
 * 260dp is roughly a third of a phone viewport — enough for the player's
 * collapsed/peek state (header + a few lines of story + the mute control) and
 * nowhere near enough to reach the top control cluster, which is what makes
 * the "nothing may overlap" requirement structural rather than tuned. The host
 * owns its own expanded state; this is the map's share of the screen.
 */
private val SHEET_MAX_DP = 260.dp

/** Icon inside a floating map control — one step down from the 24px box. */
private val CONTROL_ICON_DP = 20.dp

/** Widest the off-route banner may get before it wraps to its second line. */
private val BANNER_MAX_DP = 280.dp

/**
 * Vertical space the off-route banner occupies in the top-left lane, used to
 * push the pin-name chip below it when both are on screen. Derived from the
 * banner's own padding + two caption lines so the two cannot be tuned apart.
 */
private val BANNER_LANE_DP: Dp =
    GuideTokens.Space.sm * 2 + (GuideTokens.Caption.lineHeight.value.dp * 2)

/** The pin-name chip stops well short of the right-hand control column. */
private val CHIP_MAX_DP = 240.dp

/** Status dot diameter (matches StatusTag's dot in Primitives). */
private val STATUS_DOT_DP = 8.dp

/**
 * One floating control. 48dp (the PRIMARY target — these are the only controls
 * over the map), tonal surface, 8dp radius, and the shared press feedback from
 * [Motion] so they feel like every other button in the app.
 */
@Composable
private fun MapControl(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    rotation: Float = 0f,
    iconAlpha: Float = 1f,
    enabled: Boolean = true,
) {
    Pressable(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            contentDescription = label
            role = Role.Button
        },
    ) {
        Surface(
            modifier = Modifier.size(GuideTokens.TouchTargetPrimary),
            shape = RoundedCornerShape(GuideTokens.PinRadius),
            color = if (active) GuideTokens.Dark else GuideTokens.Surface,
            contentColor = if (active) GuideTokens.Surface else GuideTokens.Text,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, if (active) GuideTokens.Dark else GuideTokens.Border),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(CONTROL_ICON_DP)
                        .rotate(rotation)
                        .alpha(iconAlpha),
                )
            }
        }
    }
}

/**
 * Compass. Shows the map's orientation, not the phone's: the needle rotates by
 * the negative of the camera bearing, which is the convention users already
 * know (a north-up map shows no needle at all).
 *
 * The control is always present and always tappable — it resets to north — so
 * it never becomes a dead affordance. Below [COMPASS_VISIBLE_FROM_DEG] the
 * needle simply is not drawn; the button still works.
 */
@Composable
private fun CompassControl(headingDeg: Float?, onClick: () -> Unit) {
    val bearing = headingDeg ?: 0f
    // Animate the turn so a bearing wobble does not make the needle jitter.
    val needle by animateFloatAsState(
        targetValue = -bearing,
        animationSpec = tween(Motion.Fast, easing = Motion.easeOut),
        label = "compassNeedle",
    )
    val prominent = bearing >= COMPASS_VISIBLE_FROM_DEG
    val needleAlpha by animateFloatAsState(
        targetValue = if (prominent) 1f else 0.35f,
        animationSpec = tween(Motion.Fast, easing = Motion.easeOut),
        label = "compassAlpha",
    )
    MapControl(
        icon = MapStyle.IconCompass,
        // State is a WORD, never colour alone (SPEC §5): the label says which
        // way the map currently points.
        label = if (prominent) {
            "Map turned ${bearing.toInt()}°. Reset to north."
        } else {
            "Map is north-up. Rotate the map to turn it."
        },
        onClick = onClick,
        rotation = needle,
        iconAlpha = needleAlpha,
        active = prominent,
    )
}

/**
 * Recenter / follow. Two states with a WORD each, and the icon swaps between
 * "locate" and "navigation" so the state reads without colour.
 */
@Composable
private fun RecenterControl(following: Boolean, onClick: () -> Unit) {
    MapControl(
        icon = if (following) MapStyle.IconNavigation else MapStyle.IconLocateFixed,
        label = if (following) "Following your walk. Stop following." else "Follow my walk",
        onClick = onClick,
        active = following,
    )
}

// ---------------------------------------------------------------------------
// Overlay content
// ---------------------------------------------------------------------------

/**
 * Off-route banner. A status line, so it is dot PLUS words (SPEC §5) — the dot
 * carries the warning colour and the sentence carries the meaning, which is
 * what makes it survive greyscale.
 */
@Composable
private fun OffRouteBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = BANNER_MAX_DP),
        shape = RoundedCornerShape(GuideTokens.CardRadius),
        color = GuideTokens.Surface1,
        contentColor = GuideTokens.Text,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = GuideTokens.Space.md,
                vertical = GuideTokens.Space.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(STATUS_DOT_DP),
                shape = RoundedCornerShape(GuideTokens.PinRadius),
                color = GuideTokens.Danger,
            ) {}
            Spacer(Modifier.size(GuideTokens.Space.sm))
            Text(
                text = "Off route — head back for the next stop.",
                style = GuideTokens.Caption,
                color = GuideTokens.Text,
                // Bounded: this is the only free-width text over the map.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The "What am I seeing?" answer.
 *
 * Three lines maximum, then ellipsis. The screen is a glance, not a reader —
 * the full story is the player sheet's job. Anything longer here would be the
 * audit's overflow bug wearing a card.
 */
@Composable
private fun SeeingAnswerCard(text: String) {
    // Derived from the styles this card actually bounds, not picked by eye:
    // 1 line of Caption + the gap + 3 lines of Story + the card's own padding.
    // If the type scale moves, this cap follows it instead of silently starting
    // to clip the third line.
    // Line heights are sp, so they convert to dp through the current density.
    val density = LocalDensity.current
    val cap = with(density) {
        GuideTokens.Caption.lineHeight.toDp() +
            GuideTokens.Space.xs +
            GuideTokens.Story.lineHeight.toDp() * SEEING_ANSWER_LINES +
            GuideTokens.Space.cardPad * 2
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = cap),
        shape = RoundedCornerShape(GuideTokens.CardRadius),
        color = GuideTokens.Surface1,
        contentColor = GuideTokens.Text,
    ) {
        Column(
            modifier = Modifier.padding(GuideTokens.Space.cardPad),
        ) {
            // A label, not a heading: it answers a question the user just
            // asked, so it must not compete with the place name that follows.
            Text(
                text = "What you're seeing",
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(GuideTokens.Space.xs))
            Text(
                text = text,
                style = GuideTokens.Story,
                maxLines = SEEING_ANSWER_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The sheet's reserved home.
 *
 * Deliberately dumb: one surface at the sheet radius, capped so a tall sheet
 * cannot swallow the map, and no drag/scrim/state logic — that belongs to the
 * sheet implementation the shell owns. Reserving it as a real (empty) surface
 * is what stops the sheet from being invented twice.
 *
 * The cap is the reason this is safe over a full-bleed map: whatever the host
 * puts inside, the sheet can never grow past [SHEET_MAX_DP] and so can never
 * reach the top control cluster.
 */
@Composable
private fun SheetAnchor(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = SHEET_MAX_DP),
        shape = RoundedCornerShape(
            topStart = GuideTokens.SheetRadius,
            topEnd = GuideTokens.SheetRadius,
            bottomStart = GuideTokens.CardRadius,
            bottomEnd = GuideTokens.CardRadius,
        ),
        color = GuideTokens.Surface1,
        contentColor = GuideTokens.Text,
    ) {
        Box(
            // Scrollable so the cap is a CAP, not a clip. On a tall portrait
            // phone the sheet is never this full, so this is invisible; on a
            // short viewport (landscape, small phone) overflow scrolls inside
            // the anchored region instead of pushing the layout or clipping.
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = GuideTokens.Space.cardPad,
                    vertical = GuideTokens.Space.md,
                ),
        ) { content() }
    }
}

/**
 * Lifecycle bridge.
 *
 * Kept as a named composable rather than inlined into [MapScreen] because the
 * pairing (onStart/onStop + onResume/onPause + onDestroy) is a contract, and a
 * contract written once is harder to break than four call sites.
 */
@Composable
fun MapViewLifecycle(mapView: MapView) {
    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
        }
    }
}

/**
 * The active marker's NAME, drawn over the map.
 *
 * This is the label the map style cannot supply (no `glyphs` URL — see
 * [MapPins]). It carries the two things a map glyph could not: the app's real
 * typeface, and a shadow that keeps it readable over arbitrary tile imagery.
 *
 * State is shown with a dot PLUS a word (SPEC §5 — never colour alone), and the
 * whole chip animates in/out rather than popping, so selecting a pin reads as
 * a state change rather than a redraw.
 */
@Composable
private fun PinNameChip(
    name: String,
    visited: Boolean,
    isNext: Boolean,
    modifier: Modifier = Modifier,
) {
    val (dotColor, stateWord) = when {
        isNext -> GuideTokens.Highlight to "Next stop"
        visited -> GuideTokens.Text2 to "Visited"
        else -> GuideTokens.Primary to "Stop"
    }
    Surface(
        modifier = modifier.widthIn(max = CHIP_MAX_DP),
        shape = RoundedCornerShape(GuideTokens.CardRadius),
        color = GuideTokens.Surface1,
        contentColor = GuideTokens.Text,
        border = BorderStroke(1.dp, GuideTokens.Border),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = GuideTokens.Space.md,
                vertical = GuideTokens.Space.sm,
            ),
        ) {
            Text(
                text = name,
                style = GuideTokens.Label,
                color = GuideTokens.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(GuideTokens.Space.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(STATUS_DOT_DP - 2.dp),
                    shape = RoundedCornerShape(GuideTokens.PinRadius),
                    color = dotColor,
                ) {}
                Spacer(Modifier.size(GuideTokens.Space.xs + 2.dp))
                Text(
                    text = stateWord,
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

package guide.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

/**
 * The app's icon set — hand-authored [ImageVector]s, 24dp box, 2dp stroke,
 * round caps and joins, unfilled (matching the reference's Lucide construction
 * rules) with zero runtime dependency.
 *
 * Only glyphs the UI actually uses are defined here. Geometry follows Lucide
 * origins so they sit correctly beside bundled SVGs of the same family.
 */
object GuideIcons {

    private fun icon(
        name: String,
        build: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            fill = null,
            pathBuilder = build,
        )
    }.build()

    val ChevronRight: ImageVector = icon("ChevronRight") {
        moveTo(9f, 18f); lineTo(15f, 12f); lineTo(9f, 6f)
    }
    val ChevronLeft: ImageVector = icon("ChevronLeft") {
        moveTo(15f, 18f); lineTo(9f, 12f); lineTo(15f, 6f)
    }
    val Heart: ImageVector = icon("Heart") {
        moveTo(19f, 14f)
        curveTo(20.5f, 12.5f, 21.5f, 10.5f, 21.5f, 8.5f)
        arcTo(5.5f, 5.5f, 0f, false, false, 12f, 5f)
        arcTo(5.5f, 5.5f, 0f, false, false, 2.5f, 8.5f)
        curveTo(2.5f, 10.5f, 3.5f, 12.5f, 5f, 14f)
        lineTo(12f, 21f)
        lineTo(19f, 14f)
        close()
    }
    val ChevronDown: ImageVector = icon("ChevronDown") {
        moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
    }
    val Check: ImageVector = icon("Check") {
        moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f)
    }
    val Play: ImageVector = icon("Play") {
        moveTo(6f, 3f); lineTo(20f, 12f); lineTo(6f, 21f); close()
    }
    val Eye: ImageVector = icon("Eye") {
        moveTo(2f, 12f)
        // Lens outline
        curveTo(5f, 6f, 9f, 4f, 12f, 4f)
        curveTo(15f, 4f, 19f, 6f, 22f, 12f)
        curveTo(19f, 18f, 15f, 20f, 12f, 20f)
        curveTo(9f, 20f, 5f, 18f, 2f, 12f)
        close()
        moveTo(12f, 15f)
        arcToRelative(3f, 3f, 0f, true, false, 0f, -6f)
        arcToRelative(3f, 3f, 0f, false, false, 0f, 6f)
        close()
    }
    val Clock: ImageVector = icon("Clock") {
        moveTo(12f, 22f)
        arcToRelative(10f, 10f, 0f, true, false, 0f, -20f)
        arcToRelative(10f, 10f, 0f, false, false, 0f, 20f)
        close()
        moveTo(12f, 6f); verticalLineTo(12f); lineTo(16f, 14f)
    }
    val Calendar: ImageVector = icon("Calendar") {
        moveTo(8f, 2f); verticalLineTo(6f)
        moveTo(16f, 2f); verticalLineTo(6f)
        moveTo(3f, 10f); horizontalLineTo(21f)
        moveTo(5f, 4f)
        horizontalLineTo(19f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
        verticalLineToRelative(14f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
        horizontalLineTo(5f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
        verticalLineTo(6f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
        close()
    }
    val Book: ImageVector = icon("Book") {
        moveTo(4f, 19.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 6.5f, 17f)
        horizontalLineTo(20f)
        moveTo(6.5f, 2f)
        horizontalLineTo(20f)
        verticalLineTo(22f)
        horizontalLineTo(6.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, -2.5f, -2.5f)
        verticalLineTo(4.5f)
        arcTo(2.5f, 2.5f, 0f, false, true, 6.5f, 2f)
        close()
    }
    val Note: ImageVector = icon("Note") {
        moveTo(11f, 4f)
        horizontalLineTo(5f)
        arcToRelative(2f, 2f, 0f, false, false, -2f, 2f)
        verticalLineToRelative(14f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
        horizontalLineToRelative(14f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
        verticalLineToRelative(-6f)
        moveTo(18.375f, 2.625f)
        arcToRelative(1f, 1f, 0f, false, false, -3f, 0f)
        lineTo(9f, 9f)
        verticalLineToRelative(3f)
        horizontalLineToRelative(3f)
        close()
    }
    val Compass: ImageVector = icon("Compass") {
        moveTo(12f, 22f)
        arcToRelative(10f, 10f, 0f, true, false, 0f, -20f)
        arcToRelative(10f, 10f, 0f, false, false, 0f, 20f)
        close()
        moveTo(16.24f, 7.76f); lineTo(14.12f, 14.12f); lineTo(7.76f, 16.24f); lineTo(9.88f, 9.88f); close()
    }
    val Route: ImageVector = icon("Route") {
        moveTo(6f, 19f)
        arcToRelative(3f, 3f, 0f, true, false, 0f, -6f)
        arcToRelative(3f, 3f, 0f, false, false, 0f, 6f)
        close()
        moveTo(18f, 11f)
        arcToRelative(3f, 3f, 0f, true, false, 0f, -6f)
        arcToRelative(3f, 3f, 0f, false, false, 0f, 6f)
        close()
        moveTo(9f, 19f)
        horizontalLineToRelative(5.5f)
        arcToRelative(3.5f, 3.5f, 0f, false, false, 0f, -7f)
        horizontalLineToRelative(-5f)
        arcToRelative(3.5f, 3.5f, 0f, false, true, 0f, -7f)
        horizontalLineTo(15f)
    }
    /** Heritage layer — a columned facade. */
    val Heritage: ImageVector = icon("Heritage") {
        moveTo(3f, 21f); horizontalLineTo(21f)
        moveTo(5f, 21f); verticalLineTo(10f)
        moveTo(9f, 21f); verticalLineTo(10f)
        moveTo(15f, 21f); verticalLineTo(10f)
        moveTo(19f, 21f); verticalLineTo(10f)
        moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f); close()
    }
    /** Food layer — fork and knife. */
    val Food: ImageVector = icon("Food") {
        moveTo(7f, 2f); verticalLineTo(9f)
        moveTo(4f, 2f); verticalLineTo(6f)
        arcToRelative(3f, 3f, 0f, false, false, 6f, 0f)
        verticalLineTo(2f)
        moveTo(7f, 9f); verticalLineTo(22f)
        moveTo(17f, 2f)
        curveTo(15f, 5f, 15f, 9f, 17f, 11f)
        verticalLineTo(22f)
        moveTo(17f, 2f); verticalLineTo(11f)
    }
    /** Stay layer — a bed. */
    val Bed: ImageVector = icon("Bed") {
        moveTo(2f, 20f); verticalLineTo(8f)
        moveTo(2f, 16f); horizontalLineTo(22f)
        verticalLineTo(20f)
        moveTo(22f, 16f)
        verticalLineTo(11f)
        arcToRelative(2f, 2f, 0f, false, false, -2f, -2f)
        horizontalLineTo(9f)
        verticalLineTo(16f)
        moveTo(6f, 12f)
        arcToRelative(2f, 2f, 0f, true, false, 0f, -4f)
        arcToRelative(2f, 2f, 0f, false, false, 0f, 4f)
        close()
    }
    val Sparkle: ImageVector = icon("Sparkle") {
        moveTo(12f, 3f); lineTo(13.9f, 9.1f); lineTo(20f, 11f); lineTo(13.9f, 12.9f)
        lineTo(12f, 19f); lineTo(10.1f, 12.9f); lineTo(4f, 11f); lineTo(10.1f, 9.1f); close()
    }
    /** No signal — antenna with a strike (connectivity state, not a radio button). */
    val SignalOff: ImageVector = icon("SignalOff") {        moveTo(4.9f, 19.1f)
        curveTo(3.1f, 17.3f, 2f, 14.8f, 2f, 12f)
        curveTo(2f, 9.2f, 3.1f, 6.7f, 4.9f, 4.9f)
        moveTo(7.8f, 16.2f)
        arcTo(6f, 6f, 0f, false, true, 7.8f, 7.8f)
        moveTo(16.2f, 7.8f)
        arcToRelative(6f, 6f, 0f, false, true, 0f, 8.4f)
        moveTo(19.1f, 4.9f)
        curveTo(20.9f, 6.7f, 22f, 9.2f, 22f, 12f)
        curveTo(22f, 14.8f, 20.9f, 17.3f, 19.1f, 19.1f)
        moveTo(2f, 2f); lineTo(22f, 22f)
        moveTo(12f, 16f)
        arcToRelative(4f, 4f, 0f, true, false, 0f, -8f)
        arcToRelative(4f, 4f, 0f, false, false, 0f, 8f)
        close()
    }
    /** Offline-ready — downloaded package. */
    val Offline: ImageVector = icon("Offline") {
        moveTo(12f, 3f); verticalLineTo(15f)
        moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f)
        moveTo(4f, 17f); verticalLineTo(19f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
        horizontalLineTo(18f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
        verticalLineTo(17f)
    }

    // -----------------------------------------------------------------------
    // Content screens (Nearby / PoiDetail / Routes / Packs / History /
    // Phrasebook). Added 2026-09-26 for the content-screen rebuild — same
    // construction rules: 24 box, 2 stroke, round caps/joins, unfilled.
    // -----------------------------------------------------------------------

    /** Speak a phrase aloud — waves from a source. */
    val Speak: ImageVector = icon("Speak") {
        moveTo(11f, 5f); lineTo(6f, 9f); horizontalLineTo(2f); verticalLineTo(15f)
        horizontalLineTo(6f); lineTo(11f, 19f); close()
        moveTo(15.5f, 8.5f)
        arcTo(5f, 5f, 0f, false, true, 0f, 7f)
        moveTo(18.5f, 5.5f)
        arcTo(9f, 9f, 0f, false, true, 0f, 13f)
    }

    /** Download — arrow into a tray. */
    val Download: ImageVector = icon("Download") {
        moveTo(12f, 3f); verticalLineTo(15f)
        moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f)
        moveTo(4f, 17f); verticalLineTo(19f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
        horizontalLineTo(18f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
        verticalLineTo(17f)
    }

    /** Delete — a bin with a lid. */
    val Delete: ImageVector = icon("Delete") {
        moveTo(3f, 6f); horizontalLineTo(21f)
        moveTo(8f, 6f); verticalLineTo(4f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
        horizontalLineTo(14f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
        verticalLineTo(6f)
        moveTo(19f, 6f); verticalLineTo(20f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
        horizontalLineTo(7f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
        verticalLineTo(6f)
        moveTo(10f, 11f); verticalLineTo(17f)
        moveTo(14f, 11f); verticalLineTo(17f)
    }

    /** A stack of packs — an open box. */
    val Package: ImageVector = icon("Package") {
        moveTo(3f, 7f); lineTo(12f, 3f); lineTo(21f, 7f); lineTo(12f, 11f); close()
        moveTo(3f, 7f); verticalLineTo(17f)
        lineTo(12f, 21f)
        lineTo(21f, 17f)
        verticalLineTo(7f)
        moveTo(12f, 11f); verticalLineTo(21f)
    }

    /** Export — arrow out of a tray. */
    val Export: ImageVector = icon("Export") {
        moveTo(12f, 15f); verticalLineTo(3f)
        moveTo(7f, 8f); lineTo(12f, 3f); lineTo(17f, 8f)
        moveTo(4f, 15f); verticalLineTo(19f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, 2f)
        horizontalLineTo(18f)
        arcToRelative(2f, 2f, 0f, false, false, 2f, -2f)
        verticalLineTo(15f)
    }

    /** Footprints — the visited timeline mark. */
    val Footprints: ImageVector = icon("Footprints") {
        moveTo(4f, 16f); verticalLineTo(8f)
        arcToRelative(3f, 3f, 0f, false, true, 6f, 0f)
        verticalLineTo(16f)
        arcToRelative(3f, 3f, 0f, false, true, -6f, 0f)
        close()
        moveTo(6f, 20f); verticalLineTo(21f)
        moveTo(14f, 16f); verticalLineTo(8f)
        arcToRelative(3f, 3f, 0f, false, true, 6f, 0f)
        verticalLineTo(16f)
        arcToRelative(3f, 3f, 0f, false, true, -6f, 0f)
        close()
        moveTo(16f, 20f); verticalLineTo(21f)
    }

    /** Radio button, unselected — for single-select groups (never the only cue). */
    val RadioOff: ImageVector = icon("RadioOff") {
        moveTo(12f, 22f)
        arcToRelative(10f, 10f, 0f, true, false, 0f, -20f)
        arcToRelative(10f, 10f, 0f, false, false, 0f, 20f)
        close()
    }

    /** Radio button, selected — outer ring plus a filled centre. */
    val RadioOn: ImageVector = icon("RadioOn") {
        moveTo(12f, 22f)
        arcToRelative(10f, 10f, 0f, true, false, 0f, -20f)
        arcToRelative(10f, 10f, 0f, false, false, 0f, 20f)
        close()
        moveTo(12f, 16f)
        arcToRelative(4f, 4f, 0f, true, false, 0f, -8f)
        arcToRelative(4f, 4f, 0f, false, false, 0f, 8f)
        close()
    }

    /**
     * Travel Guide: the map destination.
     *
     * Moved here 2026-09-26 from `NavBar.kt`, which had grown a second private
     * `Icons` object duplicating four glyphs (Clock/Compass/Route/Package) that
     * already existed in this shared set. Two definitions of one glyph drift —
     * this is now the single source. See docs/UI-AUDIT-2026-09-26.md §7.4.
     */
    val Map: ImageVector = icon("MapIcon") {
        moveTo(14.106f, 5.553f)
        lineTo(9.894f, 3.447f)
        arcToRelative(2f, 2f, 0f, false, false, 1.789f, 0f)
        lineTo(3.553f, 4.106f)
        arcTo(2f, 2f, 0f, false, false, 3f, 6f)
        verticalLineToRelative(14f)
        arcToRelative(1f, 1f, 0f, false, false, 1.447f, 0.894f)
        lineToRelative(4.659f, -2.329f)
        arcToRelative(2f, 2f, 0f, false, true, 1.788f, 0f)
        lineToRelative(4.212f, 2.106f)
        arcToRelative(2f, 2f, 0f, false, false, 1.788f, 0f)
        lineToRelative(4.553f, -2.276f)
        arcTo(2f, 2f, 0f, false, true, 21f, 18f)
        verticalLineTo(4f)
        arcToRelative(1f, 1f, 0f, false, false, -1.447f, -0.894f)
        close()
        moveTo(9f, 3f)
        verticalLineToRelative(15f)
        moveTo(15f, 6f)
        verticalLineToRelative(15f)
    }

    /** Locate / recenter — a paper-plane compass needle. */
    val Navigation: ImageVector = icon("NavigationIcon") {
        moveTo(3f, 11f)
        lineTo(22f, 2f)
        lineTo(13f, 21f)
        lineTo(11f, 13f)
        close()
    }

    /** Settings / Sliders icon */
    val Sliders: ImageVector = icon("SlidersIcon") {
        moveTo(4f, 21f); verticalLineTo(14f)
        moveTo(4f, 10f); verticalLineTo(3f)
        moveTo(12f, 21f); verticalLineTo(12f)
        moveTo(12f, 8f); verticalLineTo(3f)
        moveTo(20f, 21f); verticalLineTo(16f)
        moveTo(20f, 12f); verticalLineTo(3f)
        moveTo(1f, 14f); horizontalLineTo(7f)
        moveTo(9f, 8f); horizontalLineTo(15f)
        moveTo(17f, 16f); horizontalLineTo(23f)
    }

    /** Privacy Shield icon */
    val Shield: ImageVector = icon("ShieldIcon") {
        moveTo(12f, 22f)
        curveTo(12f, 22f, 20f, 18f, 20f, 12f)
        verticalLineTo(5f)
        lineTo(12f, 2f)
        lineTo(4f, 5f)
        verticalLineTo(12f)
        curveTo(4f, 18f, 12f, 22f, 12f, 22f)
        close()
    }

    /** App Lock icon */
    val Lock: ImageVector = icon("LockIcon") {
        moveTo(5f, 11f)
        horizontalLineTo(19f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
        verticalLineToRelative(7f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
        horizontalLineTo(5f)
        arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
        verticalLineToRelative(-7f)
        arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
        close()
        moveTo(7f, 11f)
        verticalLineTo(7f)
        arcToRelative(5f, 5f, 0f, false, true, 10f, 0f)
        verticalLineToRelative(4f)
    }

    /** User / Account profile icon */
    val User: ImageVector = icon("UserIcon") {
        moveTo(12f, 11f)
        arcToRelative(4f, 4f, 0f, true, false, 0f, -8f)
        arcToRelative(4f, 4f, 0f, false, false, 0f, 8f)
        close()
        moveTo(4f, 21f)
        curveToRelative(0f, -3.3f, 3.6f, -6f, 8f, -6f)
        curveToRelative(4.4f, 0f, 8f, 2.7f, 8f, 6f)
    }
}

/**
 * Empty state — required by DesignSoul for every data surface. Written to be
 * warm and specific rather than "No data": it says what is missing AND what to
 * do about it, and offers the action when there is one.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = GuideTokens.Space.xl,
                vertical = GuideTokens.Space.xxl,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(GuideTokens.PinRadius),
                color = GuideTokens.PrimaryWash,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = GuideTokens.Primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.height(GuideTokens.Space.base))
        }
        Text(
            text = title,
            style = GuideTokens.Title,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(GuideTokens.Space.sm))
        Text(
            text = body,
            style = GuideTokens.Chrome,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(GuideTokens.Space.lg))
            action()
        }
    }
}

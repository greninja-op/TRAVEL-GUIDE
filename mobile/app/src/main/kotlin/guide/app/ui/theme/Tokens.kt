package guide.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import guide.app.R

/**
 * The Travel Guide design system, as Compose values.
 *
 * Expanded 2026-09-26 (owner: "triple quality", smooth motion, premium map UI).
 * Previously this file held only 8 colors + 3 text styles + 2 radii; screens
 * hardcoded everything else and the document → code contract was nominal.
 * It now carries the full scale so NO screen hardcodes a value.
 *
 * Rules that hold everywhere (see docs/UI-AUDIT-2026-09-26.md §5):
 *  - every gap/pad comes from [Space]; nothing off-grid
 *  - every animation routes through [Motion]; no ad-hoc durations or curves
 *  - elevation is TONAL (a lighter surface reads as higher), shadow supports
 *  - status is never color alone — always dot/icon PLUS a word (SPEC §5)
 */

// ---------------------------------------------------------------------------
// Fonts — bundled as real TTFs (res/font). Before 2026-09-26 nothing was
// bundled and the app silently rendered in Roboto while the docs claimed
// Inter/Lora. Do not reference FontFamily.Default for product text.
// ---------------------------------------------------------------------------

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Serif for place stories — history/legend/fun-fact prose only. */
val Lora = FontFamily(
    Font(R.font.lora_regular, FontWeight.Normal),
    Font(R.font.lora_medium, FontWeight.Medium),
)

object GuideTokens {

    // -----------------------------------------------------------------------
    // Color — Warm luxury travel palette (inspired by AirBnB Luxe, Wanderlust)
    // -----------------------------------------------------------------------
    val Bg = Color(0xFFF8F9FA)           // Crisp, airy off-white canvas
    val Surface = Color(0xFFFFFFFF)      // Pure crisp white card/surface
    val Border = Color(0xFFE5E7EB)       // Hairline subtle boundary
    val Primary = Color(0xFFFF5A36)      // Sunset coral / warm terracotta brand accent
    val PrimaryDark = Color(0xFFE04320)  // Deepened coral for active press
    val Dark = Color(0xFF121826)         // Midnight carbon for prominent buttons/docks
    val Highlight = Color(0xFFF59E0B)    // Radiant amber/gold for audio guide & ratings
    val Success = Color(0xFF10B981)      // Emerald green for open now / active status
    val Text = Color(0xFF111827)         // Deep charcoal text for high contrast readability
    val Text2 = Color(0xFF6B7280)        // Slate grey secondary text
    val TextMuted = Color(0xFF9CA3AF)    // Tertiary/caption text
    val Danger = Color(0xFFEF4444)       // Vibrant warning red

    // -----------------------------------------------------------------------
    // Tonal elevation — higher surfaces are clean white with soft subtle elevation
    // -----------------------------------------------------------------------
    val Surface0 = Bg                   // page
    val Surface1 = Surface              // resting card
    val Surface2 = Color(0xFFF1F3F5)    // raised: sheet, active row wash
    val Surface3 = Color(0xFFE9ECEF)    // floating: nav bar, controls over map

    // Alpha state layers
    val StateHover = Color(0x0A121826)   // carbon @ 4%
    val StatePress = Color(0x14121826)   // carbon @ 8%
    val PrimaryWash = Color(0x14FF5A36)  // coral @ 8%
    val HighlightWash = Color(0x1EF59E0B) // amber @ 12%
    val DangerWash = Color(0x14EF4444)   // danger @ 8%
    val SuccessWash = Color(0x1410B981)  // success @ 8%
    val DarkWash = Color(0x0F121826)     // carbon wash @ 6%
    val Scrim = Color(0x66121826)        // carbon @ 40% — sheet backdrop
    val OnPrimary = Color(0xFFFFFFFF)

    // -----------------------------------------------------------------------
    // Spacing — 4pt base, 9 tokens. Between-group gaps run 2-3x within-group.
    // -----------------------------------------------------------------------
    object Space {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val base = 16.dp
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 48.dp
        val xxxl = 64.dp
        val huge = 96.dp

        val screenPad = base      // standard screen gutter
        val cardPad = base
        val gutter = md

        val floatingInset = md
    }

    // -----------------------------------------------------------------------
    // Radius — generous modern rounded curves (18dp cards, 14dp buttons, 24dp sheets)
    // -----------------------------------------------------------------------
    val CardRadius = 18.dp
    val ButtonRadius = 14.dp
    val SheetRadius = 24.dp
    val ChipRadius = 999.dp     // tags/pills only
    val PinRadius = 999.dp

    /** Minimum touch target. 44 floor, 48 for primary actions (SPEC §5). */
    val TouchTarget = 44.dp
    val TouchTargetPrimary = 48.dp

    // -----------------------------------------------------------------------
    // Type — scale 12/14/16/18/24/32/40 (frozen). Chrome = Inter;
    // stories = Lora at >=17sp. Headings jump >=1 step from body.
    // -----------------------------------------------------------------------
    private val smooth = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None,
    )

    val Display = TextStyle(
        fontFamily = Inter, fontSize = 32.sp, lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
    val Heading = TextStyle(
        fontFamily = Inter, fontSize = 24.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
    val Title = TextStyle(
        fontFamily = Inter, fontSize = 18.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
    val Body = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Normal, color = Text, lineHeightStyle = smooth,
    )
    val Chrome = TextStyle(
        fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Medium, color = Text2, lineHeightStyle = smooth,
    )
    val Caption = TextStyle(
        fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, color = Text2, lineHeightStyle = smooth,
    )

    /** Place stories — serif, 17sp floor, generous leading (reference: 1.65). */
    val Story = TextStyle(
        fontFamily = Lora, fontSize = 17.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Normal, color = Text, lineHeightStyle = smooth,
    )
    val StoryTitle = TextStyle(
        fontFamily = Lora, fontSize = 24.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.Medium, color = Text, lineHeightStyle = smooth,
    )
    val Label = TextStyle(
        fontFamily = Inter, fontSize = 14.sp, lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
}

// ---------------------------------------------------------------------------
// Motion — one system for the whole app. Every animation reads from here;
// a hardcoded duration or curve in a screen is a bug.
//
// Durations chosen so neighbours never differ by more than one step.
// Exits are FASTER than entrances (150 vs 250) — the standard craft detail.
// ---------------------------------------------------------------------------
object Motion {
    /** Immediate feedback (press, tap highlight). */
    const val Instant = 100

    /** Small state change: color, alpha, icon swap. */
    const val Fast = 150

    /** Sheet/screen entrance, layout shift — the workhorse. */
    const val Medium = 250

    /** Large surface: full-screen transition, camera settle. */
    const val Slow = 400

    /** Long, sweeping: route draw, onboarding. */
    const val Slower = 600

    /** Exits use Fast..Medium only — leaving is quicker than arriving. */
    const val Exit = 150

    // Easing as cubic-bezier control points.
    /** Decelerate — entering content. */
    val easeOut = androidx.compose.animation.core.CubicBezierEasing(0f, 0f, 0.2f, 1f)

    /** Accelerate — leaving content. */
    val easeIn = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 1f, 1f)

    /** Standard in-out — movement within the screen. */
    val easeInOut = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    /** Emphasized — M3's signature curve for larger surfaces. */
    val emphasized = androidx.compose.animation.core.CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Spring for press/toggle — small overshoot, never bouncy. */
    fun <T> spring() = androidx.compose.animation.core.spring<T>(
        dampingRatio = 0.8f, stiffness = 500f,
    )

    /** Softer spring for position/drag settle. */
    fun <T> springGentle() = androidx.compose.animation.core.spring<T>(
        dampingRatio = 0.85f, stiffness = 300f,
    )

    /** Press scale — 0.96, never below 0.95. */
    const val PressScale = 0.96f

    /** Stagger for list entrances. */
    const val Stagger = 50

    /** Cap on staggered items (beyond this they appear together). */
    const val StaggerMax = 5

    /**
     * Entrance offset for the list/item "fade up" pattern — the one entrance
     * gesture in the app (reference: "entrances split + staggered, opacity +
     * translateY, ease-out, capped at 5").
     */
    val EnterOffset = 16.dp
}

/**
 * Overlap policy.
 *
 * Every variable-length string in the app declares BOTH a line cap and an
 * overflow behaviour, so a long POI name, a long summary or a long event title
 * can never push a sibling off-screen or clip mid-glyph. Screens pass these
 * instead of inventing their own numbers (docs/UI-AUDIT-2026-09-26.md §4).
 *
 * `Lines.Unbounded` means "show it in full" — legal ONLY inside a scrolling
 * container, because a scroll container is what makes unbounded text safe.
 */
object Lines {
    /** Chrome labels, chips, status words — must never wrap. */
    const val Single = 1

    /** Supporting lines under a row title. */
    const val Supporting = 2

    /** A row/allow-list title. */
    const val Title = 2

    /** Card summaries. */
    const val Summary = 4

    /** Landscape prose in a scrolling list (story, note, history). */
    const val Unbounded = Int.MAX_VALUE
}

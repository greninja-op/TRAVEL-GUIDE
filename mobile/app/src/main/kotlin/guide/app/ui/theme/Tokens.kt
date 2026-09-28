package guide.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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
 * Full theme support: Crisp Luxury Light Mode + Obsidian Midnight Carbon Dark Mode.
 * All color tokens are accessed dynamically via [LocalGuideColors].
 */

// ---------------------------------------------------------------------------
// Fonts — bundled as real TTFs (res/font).
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

/**
 * Complete color palette definition for Travel Guide.
 */
data class GuideColorsPalette(
    val bg: Color,
    val surface: Color,
    val border: Color,
    val primary: Color,
    val primaryDark: Color,
    val dark: Color,
    val highlight: Color,
    val success: Color,
    val text: Color,
    val text2: Color,
    val textMuted: Color,
    val danger: Color,
    val surface0: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val stateHover: Color,
    val statePress: Color,
    val primaryWash: Color,
    val highlightWash: Color,
    val dangerWash: Color,
    val successWash: Color,
    val darkWash: Color,
    val scrim: Color,
    val onPrimary: Color,
    val isDark: Boolean,
)

val LightGuideColors = GuideColorsPalette(
    bg = Color(0xFFF8F9FA),
    surface = Color(0xFFFFFFFF),
    border = Color(0xFFE5E7EB),
    primary = Color(0xFFFF5A36),
    primaryDark = Color(0xFFE04320),
    dark = Color(0xFF121826),
    highlight = Color(0xFFF59E0B),
    success = Color(0xFF10B981),
    text = Color(0xFF111827),
    text2 = Color(0xFF6B7280),
    textMuted = Color(0xFF9CA3AF),
    danger = Color(0xFFEF4444),
    surface0 = Color(0xFFF8F9FA),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF1F3F5),
    surface3 = Color(0xFFE9ECEF),
    stateHover = Color(0x0A121826),
    statePress = Color(0x14121826),
    primaryWash = Color(0x14FF5A36),
    highlightWash = Color(0x1EF59E0B),
    dangerWash = Color(0x14EF4444),
    successWash = Color(0x1410B981),
    darkWash = Color(0x0F121826),
    scrim = Color(0x66121826),
    onPrimary = Color(0xFFFFFFFF),
    isDark = false,
)

val DarkGuideColors = GuideColorsPalette(
    bg = Color(0xFF0B0F17),           // Deep luxury obsidian / midnight slate canvas
    surface = Color(0xFF131A26),      // Elevated dark navy/carbon card surface
    border = Color(0xFF232F42),       // Clear, elegant dark slate border
    primary = Color(0xFFFF6D4D),      // Luminous warm coral brand accent
    primaryDark = Color(0xFFE04320),  // Deep coral for pressed state
    dark = Color(0xFF1E293B),         // Elevated slate for chips & buttons
    highlight = Color(0xFFFBBF24),    // Luminous warm amber for ratings & audio
    success = Color(0xFF34D399),      // Mint emerald for active/open status
    text = Color(0xFFF8FAFC),         // Crisp off-white text (WCAG AAA readability)
    text2 = Color(0xFF94A3B8),        // Soft slate grey secondary text
    textMuted = Color(0xFF64748B),    // Slate caption text
    danger = Color(0xFFF87171),       // Soft warning red
    surface0 = Color(0xFF0B0F17),     // Canvas
    surface1 = Color(0xFF131A26),     // Resting card
    surface2 = Color(0xFF182232),     // Raised sheet/active row
    surface3 = Color(0xFF1F2B3D),     // Floating controls
    stateHover = Color(0x14FFFFFF),
    statePress = Color(0x24FFFFFF),
    primaryWash = Color(0x26FF6D4D),  // Coral wash @ 15%
    highlightWash = Color(0x26FBBF24),
    dangerWash = Color(0x26F87171),
    successWash = Color(0x2634D399),
    darkWash = Color(0x1AFFFFFF),
    scrim = Color(0x99000000),
    onPrimary = Color(0xFFFFFFFF),
    isDark = true,
)

val LocalGuideColors = staticCompositionLocalOf { LightGuideColors }

@Composable
fun GuideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkGuideColors else LightGuideColors
    val m3Colors = if (darkTheme) {
        darkColorScheme(
            background = colors.bg,
            surface = colors.surface,
            onBackground = colors.text,
            onSurface = colors.text,
            primary = colors.primary,
        )
    } else {
        lightColorScheme(
            background = colors.bg,
            surface = colors.surface,
            onBackground = colors.text,
            onSurface = colors.text,
            primary = colors.primary,
        )
    }

    MaterialTheme(colorScheme = m3Colors) {
        CompositionLocalProvider(
            LocalGuideColors provides colors,
            LocalContentColor provides colors.text,
            content = content,
        )
    }
}

object GuideTokens {

    // -----------------------------------------------------------------------
    // Color — Warm luxury travel palette (Light) / Obsidian Slate (Dark)
    // -----------------------------------------------------------------------
    val Bg: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.bg
    val Surface: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.surface
    val Border: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.border
    val Primary: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.primary
    val PrimaryDark: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.primaryDark
    val Dark: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.dark
    val Highlight: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.highlight
    val Success: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.success
    val Text: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.text
    val Text2: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.text2
    val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.textMuted
    val Danger: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.danger

    // -----------------------------------------------------------------------
    // Tonal elevation
    // -----------------------------------------------------------------------
    val Surface0: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.surface0
    val Surface1: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.surface1
    val Surface2: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.surface2
    val Surface3: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.surface3

    // Alpha state layers
    val StateHover: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.stateHover
    val StatePress: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.statePress
    val PrimaryWash: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.primaryWash
    val HighlightWash: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.highlightWash
    val DangerWash: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.dangerWash
    val SuccessWash: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.successWash
    val DarkWash: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.darkWash
    val Scrim: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.scrim
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = LocalGuideColors.current.onPrimary
    val IsDark: Boolean @Composable @ReadOnlyComposable get() = LocalGuideColors.current.isDark

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

    val Display: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Inter, fontSize = 32.sp, lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
    val Heading: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Inter, fontSize = 24.sp, lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
    val Title: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Inter, fontSize = 18.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold, color = Text, lineHeightStyle = smooth,
    )
    val Body: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Inter, fontSize = 16.sp, lineHeight = 24.sp,
        fontWeight = FontWeight.Normal, color = Text, lineHeightStyle = smooth,
    )
    val Chrome: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Inter, fontSize = 14.sp, lineHeight = 20.sp,
        fontWeight = FontWeight.Medium, color = Text2, lineHeightStyle = smooth,
    )
    val Caption: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Inter, fontSize = 12.sp, lineHeight = 16.sp,
        fontWeight = FontWeight.Medium, color = Text2, lineHeightStyle = smooth,
    )

    /** Place stories — serif, 17sp floor, generous leading (reference: 1.65). */
    val Story: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Lora, fontSize = 17.sp, lineHeight = 28.sp,
        fontWeight = FontWeight.Normal, color = Text, lineHeightStyle = smooth,
    )
    val StoryTitle: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
        fontFamily = Lora, fontSize = 24.sp, lineHeight = 32.sp,
        fontWeight = FontWeight.Medium, color = Text, lineHeightStyle = smooth,
    )
    val Label: TextStyle @Composable @ReadOnlyComposable get() = TextStyle(
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

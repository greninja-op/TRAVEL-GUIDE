package guide.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Motion

/**
 * Shared primitives — built ONCE, reused by every screen (reference:
 * "one implementation each of the player sheet, POI card, pin and list row").
 *
 * These exist to make the overlap/overflow class of bug impossible by
 * construction: rows use weight() rather than fixed widths, text carries an
 * explicit overflow policy, and nothing relies on manual offsets.
 */

/**
 * The one card in the app. Tonal surface (not shadow-stacked), 1px border,
 * 10dp radius. `raised = true` steps the tone lighter instead of adding a
 * second shadow.
 */
/**
 * The one card in the app. Clean white surface, 1px border, 18dp radius,
 * and subtle soft elevation.
 */
@Composable
fun GuideCard(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    padding: PaddingValues = PaddingValues(GuideTokens.Space.cardPad),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) Motion.PressScale else 1f,
        animationSpec = Motion.spring(),
        label = "cardScale",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(GuideTokens.CardRadius),
        color = if (raised) GuideTokens.Surface2 else GuideTokens.Surface1,
        border = BorderStroke(1.dp, GuideTokens.Border),
        shadowElevation = if (raised) 6.dp else 2.dp,
        contentColor = GuideTokens.Text,
        onClick = onClick ?: {},
        enabled = onClick != null,
        interactionSource = interaction,
    ) {
        Column(modifier = Modifier.padding(padding)) { content() }
    }
}

/**
 * Category chip — modern pill tag as featured in the reference designs.
 */
@Composable
fun CategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val bg = if (selected) GuideTokens.Primary else GuideTokens.Surface
    val fg = if (selected) Color.White else GuideTokens.Text2
    val border = if (selected) null else BorderStroke(1.dp, GuideTokens.Border)

    Surface(
        shape = RoundedCornerShape(GuideTokens.ChipRadius),
        color = bg,
        contentColor = fg,
        border = border,
        shadowElevation = if (selected) 3.dp else 1.dp,
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else GuideTokens.Text2,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = GuideTokens.Caption,
                color = fg,
            )
        }
    }
}

/**
 * Pressable wrapper: 0.96 scale + alpha, driven by the shared [Motion] spec so
 * every tappable thing in the app feels identical. Enforces the 44dp minimum.
 */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    role: Role = Role.Button,
    withRipple: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) Motion.PressScale else 1f,
        animationSpec = Motion.spring(),
        label = "pressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(Motion.Fast),
        label = "pressAlpha",
    )
    Box(
        modifier = modifier
            .heightIn(min = GuideTokens.TouchTarget)
            .scale(scale)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = if (withRipple) ripple(color = GuideTokens.StatePress, bounded = true) else null,
                        role = role,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        CompositionLocalProvider(
            LocalContentColor provides LocalContentColor.current.copy(alpha = alpha),
        ) { content() }
    }
}

/**
 * The one button. Variants: Primary (sunset coral), Dark (carbon black), Tonal, Quiet.
 */
enum class GuideButtonVariant { Primary, Dark, Tonal, Quiet }

@Composable
fun GuideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GuideButtonVariant = GuideButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val bg: Color
    val fg: Color
    val border: BorderStroke?
    when (variant) {
        GuideButtonVariant.Primary -> {
            bg = GuideTokens.Primary; fg = GuideTokens.OnPrimary; border = null
        }
        GuideButtonVariant.Dark -> {
            bg = if (GuideTokens.IsDark) GuideTokens.Surface2 else GuideTokens.Dark
            fg = Color.White
            border = null
        }
        GuideButtonVariant.Tonal -> {
            bg = GuideTokens.PrimaryWash; fg = GuideTokens.Primary; border = null
        }
        GuideButtonVariant.Quiet -> {
            bg = Color.Transparent; fg = GuideTokens.Text
            border = BorderStroke(1.dp, GuideTokens.Border)
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) Motion.PressScale else 1f,
        animationSpec = Motion.spring(), label = "btnScale",
    )
    Surface(
        modifier = modifier
            .heightIn(min = GuideTokens.TouchTargetPrimary)
            .scale(scale),
        shape = RoundedCornerShape(GuideTokens.ButtonRadius),
        color = bg,
        contentColor = fg,
        border = border,
        shadowElevation = if (variant == GuideButtonVariant.Primary || variant == GuideButtonVariant.Dark) 3.dp else 0.dp,
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = GuideTokens.Space.lg,
                vertical = GuideTokens.Space.md,
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null, // label already says it
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(GuideTokens.Space.sm))
            }
            Text(
                text = text,
                style = GuideTokens.Label,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * List row: icon/leading slot, title + supporting line, trailing slot.
 * The text column takes weight(1f) so long names wrap/ellipsize instead of
 * pushing the trailing element off-screen — the overlap bug is prevented
 * structurally, not by tuning widths.
 */
@Composable
fun GuideRow(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleStyle: TextStyle = GuideTokens.Title,
    onClick: (() -> Unit)? = null,
) {
    val base = modifier
        .fillMaxWidth()
        .heightIn(min = GuideTokens.TouchTargetPrimary)
    val row: @Composable (@Composable () -> Unit) -> Unit = { inner ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = GuideTokens.Space.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.size(GuideTokens.Space.md))
            }
            Column(modifier = Modifier.weight(1f)) { inner() }
            if (trailing != null) {
                Spacer(Modifier.size(GuideTokens.Space.sm))
                trailing()
            }
        }
    }
    if (onClick != null) {
        Pressable(onClick = onClick, modifier = base) {
            row {
                Text(title, style = titleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                supporting?.let {
                    Text(
                        it, style = GuideTokens.Chrome,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    } else {
        Box(modifier = base) {
            row {
                Text(title, style = titleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                supporting?.let {
                    Text(
                        it, style = GuideTokens.Chrome,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Status is NEVER colour alone (SPEC §5 / reference "status taxonomy"):
 * a dot AND a word. Used for open/closed, visited, playing, offline.
 */
@Composable
fun StatusTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        } else {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(GuideTokens.PinRadius),
                color = color,
            ) {}
        }
        Spacer(Modifier.size(GuideTokens.Space.xs + 2.dp))
        Text(text, style = GuideTokens.Caption, color = color)
    }
}

/** Section header — sentence case, one step above body, generous top space. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = GuideTokens.Label,
        color = GuideTokens.Text2,
        modifier = modifier.padding(
            top = GuideTokens.Space.lg,
            bottom = GuideTokens.Space.sm,
        ),
    )
}

/** Horizontal rule matching the border token. */
@Composable
fun GuideDivider(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().heightIn(min = 1.dp),
        color = GuideTokens.Border,
    ) {}
}

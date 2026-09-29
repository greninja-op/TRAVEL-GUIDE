package guide.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Motion

/**
 * Bottom navigation — the app's spine.
 *
 * Icons are hand-authored [ImageVector]s rather than a library dependency:
 * 24dp box, 2dp stroke, round caps and joins, unfilled — the same construction
 * the design reference mandates for Lucide glyphs, without pulling in a
 * runtime icon package. Geometry follows Lucide's originals (map, compass,
 * route, package, clock, sliders) so they sit correctly beside any future
 * bundled SVG.
 *
 * Selection animates: the active icon lifts and its pill background fades in.
 * Only the active tab carries colour; the rest stay secondary — that is the
 * "one focal point" rule applied to navigation.
 */

/** The five primary destinations. Order is deliberate: the walk first. */
enum class NavItem(val route: String, val icon: ImageVector) {
    Map("map", GuideIcons.Map),
    Nearby("nearby", GuideIcons.Compass),
    Packs("packs", GuideIcons.Package),
    History("history", GuideIcons.Clock),
    Settings("settings", GuideIcons.Sliders);

    fun label(lang: guide.app.data.AppLanguage): String = when (this) {
        Map -> guide.app.data.AppStrings.tabMap(lang)
        Nearby -> guide.app.data.AppStrings.tabNearby(lang)
        Packs -> guide.app.data.AppStrings.tabPacks(lang)
        History -> guide.app.data.AppStrings.tabHistory(lang)
        Settings -> guide.app.data.AppStrings.tabSettings(lang)
    }
}

@Composable
fun GuideNavBar(
    current: String,
    onSelect: (String) -> Unit,
    language: guide.app.data.AppLanguage = guide.app.data.AppLanguage.ENGLISH,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GuideTokens.Surface1,
        shadowElevation = 0.dp,
    ) {
        Box {
            // Hairline top border instead of a shadow — tonal separation.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GuideTokens.Border),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = GuideTokens.Space.sm,
                        vertical = GuideTokens.Space.xs,
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavItem.entries.forEach { item ->
                    NavBarItem(
                        item = item,
                        label = item.label(language),
                        selected = current == item.route,
                        onClick = { onSelect(item.route) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: NavItem,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) GuideTokens.Primary else GuideTokens.Text2,
        animationSpec = tween(Motion.Fast),
        label = "navTint",
    )
    val lift by animateDpAsState(
        targetValue = if (selected) 0.dp else 2.dp,
        animationSpec = tween(Motion.Medium),
        label = "navLift",
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(Motion.Medium),
        label = "navPill",
    )
    Pressable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        role = Role.Tab,
        withRipple = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = GuideTokens.Space.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Active pill behind the glyph.
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(GuideTokens.PinRadius))
                        .background(GuideTokens.Primary.copy(alpha = 0.14f * pillAlpha)),
                )
                Icon(
                    imageVector = item.icon,
                    // Label below already announces it; avoid double-reading.
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(bottom = lift),
                )
            }
            Spacer(Modifier.height(GuideTokens.Space.xs))
            Text(
                text = label,
                style = GuideTokens.Caption.copy(
                    fontSize = 10.sp,
                    letterSpacing = (-0.2).sp,
                ),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

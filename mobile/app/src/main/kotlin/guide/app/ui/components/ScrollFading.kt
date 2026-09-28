package guide.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dynamic Edge Fading Mask (DesignSoul Craft).
 *
 * Provides an authentic alpha-masked dissolution at the top and bottom of scrollable containers.
 *
 * Behavior:
 * - When at the top (canScrollBackward == false): Top fade is inactive (0%), bottom fade is active (100%).
 * - While scrolling in the middle: Both top and bottom edges fade out smoothly.
 * - When at the bottom (canScrollForward == false): Bottom fade is inactive (0%), top fade is active (100%).
 * - Pure Alpha Transparency: Uses CompositingStrategy.Offscreen + BlendMode.DstIn.
 *   Dissolves content into natural alpha transparency regardless of background color (Light or Dark mode).
 */

@Composable
fun Modifier.fadingEdges(
    listState: LazyListState,
    topFadeHeight: Dp = 36.dp,
    bottomFadeHeight: Dp = 48.dp,
): Modifier {
    val canScrollUp = listState.canScrollBackward
    val canScrollDown = listState.canScrollForward
    val isScrolling = listState.isScrollInProgress

    val topAlphaTarget = when {
        canScrollUp && isScrolling -> 1.0f
        canScrollUp -> 0.85f
        else -> 0.0f
    }
    val bottomAlphaTarget = when {
        canScrollDown && isScrolling -> 1.0f
        canScrollDown -> 0.85f
        else -> 0.0f
    }

    val topStrength by animateFloatAsState(
        targetValue = topAlphaTarget,
        animationSpec = tween(durationMillis = 200),
        label = "topFadeStrength",
    )
    val bottomStrength by animateFloatAsState(
        targetValue = bottomAlphaTarget,
        animationSpec = tween(durationMillis = 200),
        label = "bottomFadeStrength",
    )

    return this.edgeFadeMask(
        topFadeHeight = topFadeHeight,
        bottomFadeHeight = bottomFadeHeight,
        topStrength = topStrength,
        bottomStrength = bottomStrength,
    )
}

@Composable
fun Modifier.fadingEdges(
    scrollState: ScrollState,
    topFadeHeight: Dp = 36.dp,
    bottomFadeHeight: Dp = 48.dp,
): Modifier {
    val canScrollUp = scrollState.canScrollBackward
    val canScrollDown = scrollState.canScrollForward
    val isScrolling = scrollState.isScrollInProgress

    val topAlphaTarget = when {
        canScrollUp && isScrolling -> 1.0f
        canScrollUp -> 0.85f
        else -> 0.0f
    }
    val bottomAlphaTarget = when {
        canScrollDown && isScrolling -> 1.0f
        canScrollDown -> 0.85f
        else -> 0.0f
    }

    val topStrength by animateFloatAsState(
        targetValue = topAlphaTarget,
        animationSpec = tween(durationMillis = 200),
        label = "topScrollFadeStrength",
    )
    val bottomStrength by animateFloatAsState(
        targetValue = bottomAlphaTarget,
        animationSpec = tween(durationMillis = 200),
        label = "bottomScrollFadeStrength",
    )

    return this.edgeFadeMask(
        topFadeHeight = topFadeHeight,
        bottomFadeHeight = bottomFadeHeight,
        topStrength = topStrength,
        bottomStrength = bottomStrength,
    )
}

/**
 * Core graphics mask applying true alpha dissolution using CompositingStrategy.Offscreen.
 */
fun Modifier.edgeFadeMask(
    topFadeHeight: Dp = 36.dp,
    bottomFadeHeight: Dp = 48.dp,
    topStrength: Float = 1.0f,
    bottomStrength: Float = 1.0f,
): Modifier {
    if (topStrength <= 0.01f && bottomStrength <= 0.01f) {
        return this
    }

    return this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()

            val height = size.height
            if (height <= 0f) return@drawWithContent

            val maxFade = height * 0.35f
            val topPx = (topFadeHeight.toPx() * topStrength.coerceIn(0f, 1f)).coerceAtMost(maxFade)
            val bottomPx = (bottomFadeHeight.toPx() * bottomStrength.coerceIn(0f, 1f)).coerceAtMost(maxFade)

            val topStop = if (topPx > 0f) (topPx / height).coerceIn(0f, 0.45f) else 0f
            val bottomStop = if (bottomPx > 0f) (1f - (bottomPx / height)).coerceIn(0.55f, 1f) else 1f

            val topEdgeColor = Color.Black.copy(alpha = 1f - topStrength.coerceIn(0f, 1f))
            val bottomEdgeColor = Color.Black.copy(alpha = 1f - bottomStrength.coerceIn(0f, 1f))

            val maskBrush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to topEdgeColor,
                    topStop to Color.Black,
                    bottomStop to Color.Black,
                    1.0f to bottomEdgeColor,
                ),
                startY = 0f,
                endY = height,
            )

            drawRect(
                brush = maskBrush,
                size = size,
                blendMode = BlendMode.DstIn,
            )
        }
}

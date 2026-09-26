package guide.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.Pressable
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Motion

/**
 * The one bottom-sheet player (reference: "one bottom-sheet player", never
 * re-implemented per screen).
 *
 * This is the COLLAPSED/PEEK body of the sheet. It is deliberately a plain
 * column — the sheet chrome (drag handle, scrim, anchored states, gesture
 * dismiss) belongs to the hosting `BottomSheetScaffold` in the shell, so this
 * composable stays testable and reusable.
 *
 * Why it matters: before 2026-09-26 this was a static `Card` inside the normal
 * layout — no drag, no peek, no dismissable scrim. On a map-first screen the
 * sheet is the primary surface, so it must behave like one.
 *
 * Layout safety: the transcript can be arbitrarily long (a full place story),
 * so it scrolls inside a bounded height and the whole thing sits in a
 * weight-free column — it can never push the map off-screen or clip.
 */
@Composable
fun NowPlayingSheet(
    poiName: String,
    transcript: String,
    nextUp: List<String>,
    muted: Boolean,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** True while audio is actually playing — drives the live indicator. */
    playing: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ---- Header: state + the place name ----------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NowPlayingIndicator(playing = playing, muted = muted)
            Spacer(Modifier.width(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        muted -> "Paused"
                        playing -> "Now playing"
                        else -> "Up next"
                    },
                    style = GuideTokens.Caption,
                    color = if (muted) GuideTokens.Text2 else GuideTokens.Highlight,
                    maxLines = 1,
                )
                Text(
                    text = poiName,
                    style = GuideTokens.Title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(GuideTokens.Space.sm))
            MuteButton(muted = muted, onToggle = onMuteToggle)
        }

        Spacer(Modifier.height(GuideTokens.Space.md))

        // ---- The story, in the serif face (it is prose) ----------------------
        // Bounded + scrollable: a long story scrolls rather than overflowing.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 152.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = transcript,
                style = GuideTokens.Story,
            )
        }

        // ---- Queue: what the guide will say after this ------------------------
        AnimatedVisibility(
            visible = nextUp.isNotEmpty(),
            enter = fadeIn(androidx.compose.animation.core.tween(Motion.Medium)) +
                expandVertically(animationSpec = androidx.compose.animation.core.tween(Motion.Medium)),
            exit = fadeOut(androidx.compose.animation.core.tween(Motion.Exit)) +
                shrinkVertically(animationSpec = androidx.compose.animation.core.tween(Motion.Exit)),
        ) {
            Column {
                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.md))
                Text("Then", style = GuideTokens.Caption, color = GuideTokens.Text2)
                Spacer(Modifier.height(GuideTokens.Space.xs))
                nextUp.take(3).forEach { name ->
                    Text(
                        text = "· $name",
                        style = GuideTokens.Chrome,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/**
 * Live state dot. Pulses gently while playing — the only continuously
 * animating element in the app, and it earns it: it signals "audio is
 * running right now", which the user must be able to see at a glance.
 * Pairs colour with a WORD in the header (status is never colour alone).
 */
@Composable
private fun NowPlayingIndicator(playing: Boolean, muted: Boolean) {
    val target = if (playing && !muted) 1f else 0.45f
    val alpha by animateFloatAsState(
        targetValue = target,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = if (playing && !muted) Motion.Slow else Motion.Fast,
            easing = Motion.easeInOut,
        ),
        label = "playIndicator",
    )
    val color = when {
        muted -> GuideTokens.Text2
        playing -> GuideTokens.Highlight
        else -> GuideTokens.Primary
    }
    Box(
        modifier = Modifier
            .size(GuideTokens.Space.md)
            .clip(RoundedCornerShape(GuideTokens.PinRadius))
            .background(color.copy(alpha = alpha)),
    )
}

/**
 * One-tap mute (SPEC §1: always visible, never surprising audio). Sized to the
 * 48dp primary target and it is ALWAYS reachable — the reason this lives in
 * the sheet header rather than behind a menu.
 */
@Composable
private fun MuteButton(muted: Boolean, onToggle: () -> Unit) {
    val label = if (muted) "Unmute guide" else "Mute guide"
    Pressable(
        onClick = onToggle,
        modifier = Modifier
            .size(GuideTokens.TouchTargetPrimary)
            .semantics { contentDescription = label },
    ) {
        Surface(
            modifier = Modifier.size(GuideTokens.TouchTargetPrimary),
            shape = RoundedCornerShape(GuideTokens.PinRadius),
            color = if (muted) GuideTokens.PrimaryWash else GuideTokens.Surface3,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Hand-drawn speaker glyph (no icon dependency) — stroke-2 look
                // via two stacked rounded rects, matching the app's icon style.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (muted) 14.dp else 4.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (muted) GuideTokens.Text2 else GuideTokens.Primary),
                    )
                    if (muted) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(GuideTokens.Danger),
                        )
                    }
                }
            }
        }
    }
}

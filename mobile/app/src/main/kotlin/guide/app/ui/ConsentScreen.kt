package guide.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideIcons
import guide.app.ui.theme.GuideTokens

/**
 * First-launch consent (SPEC §1 + docs/PRIVACY.md).
 *
 * This is the moment the product earns the right to the user's location, so it
 * states the trade plainly: what is collected, that background tracking is a
 * SEPARATE opt-in, that GPS costs battery, and that visits and notes stay on
 * the device. Nothing is requested before this screen is acknowledged.
 *
 * Rebuilt 2026-09-26: was a bare non-scrolling `Column` of paragraphs and two
 * `TextButton`s sharing the same visual weight (so "Not now" looked as
 * encouraged as "Continue"). Now every promise is a scannable row, the two
 * actions are ranked as primary/secondary, and the body scrolls so it can never
 * clip on a short screen.
 */
@Composable
fun ConsentScreen(
    onAcknowledgeForeground: () -> Unit,
    onLater: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = GuideTokens.Space.screenPad,
            end = GuideTokens.Space.screenPad,
            top = GuideTokens.Space.xxl,
            bottom = GuideTokens.Space.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
    ) {
        item {
            Text("Before we start", style = GuideTokens.Display)
            Spacer(Modifier.height(GuideTokens.Space.sm))
            Text(
                "Travel Guide tells you what you're walking past. " +
                    "Here's exactly what that needs, and what it doesn't.",
                style = GuideTokens.Body,
            )
        }

        item { Spacer(Modifier.height(GuideTokens.Space.xs)) }

        item {
            GuideCard {
                Promise(
                    icon = GuideIcons.Compass,
                    title = "Your location, only while you ask",
                    body = "Foreground location to start. Background tracking is a " +
                        "separate opt-in inside Settings — the app works fully without it.",
                )
                Spacer(Modifier.height(GuideTokens.Space.base))
                Promise(
                    icon = GuideIcons.Offline,
                    title = "Works with no signal",
                    body = "Download the city once and the whole loop — map, stories, " +
                        "narration — runs offline.",
                )
                Spacer(Modifier.height(GuideTokens.Space.base))
                Promise(
                    icon = GuideIcons.Clock,
                    title = "GPS costs battery, so you choose",
                    body = "Pick a battery profile any time. Every profile says what " +
                        "it costs before you pick it.",
                )
                Spacer(Modifier.height(GuideTokens.Space.base))
                Promise(
                    icon = GuideIcons.Note,
                    title = "It stays on this device",
                    body = "Your visits and notes are stored locally. Nothing about " +
                        "where you walked is sent anywhere.",
                )
            }
        }

        item { Spacer(Modifier.height(GuideTokens.Space.sm)) }

        item {
            GuideButton(
                text = "Continue (foreground only)",
                onClick = onAcknowledgeForeground,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            GuideButton(
                text = "Not now",
                onClick = onLater,
                variant = GuideButtonVariant.Quiet,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = "You can change any of this later in Settings.",
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )
        }
    }
}

/** One promise: icon disc, bold claim, plain-language detail. */
@Composable
private fun Promise(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.size(36.dp),
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
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(GuideTokens.Space.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GuideTokens.Label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = body,
                style = GuideTokens.Chrome,
                color = GuideTokens.Text2,
            )
        }
    }
}

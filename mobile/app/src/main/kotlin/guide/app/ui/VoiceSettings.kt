package guide.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.theme.GuideTokens

/**
 * Voice settings — playback speed and the on-device/cloud voice stance.
 *
 * Rebuilt 2026-09-26:
 *  - was a non-scrolling `Column` with hardcoded `16.dp` padding;
 *  - it carried its OWN "Auto-play nearby stories" switch, duplicating the one
 *    in Settings — two controls for one setting is two things that drift. The
 *    duplicate is removed; auto-play lives in Settings (§ "The voice").
 *
 * What remains is genuinely voice-specific: how fast the guide speaks, and the
 * plain statement that cloud voices are opt-in (SPEC §1 — the user controls the
 * voice, and nothing about their audio leaves the device by default).
 */
@Composable
fun VoiceSettings(onRate: (Float) -> Unit) {
    var rate by remember { mutableFloatStateOf(1.0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = GuideTokens.Space.screenPad,
                vertical = GuideTokens.Space.base,
            ),
        verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
    ) {
        SectionHeader("Voice")
        GuideCard {
            // ---- Speak rate ------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Playback speed",
                    style = GuideTokens.Label,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusTag(
                    text = "${"%.1f".format(rate)}×",
                    color = GuideTokens.Primary,
                )
            }
            Slider(
                value = rate,
                onValueChange = { rate = it; onRate(it) },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = GuideTokens.Primary,
                    activeTrackColor = GuideTokens.Primary,
                    inactiveTrackColor = GuideTokens.Border,
                ),
            )
            Text(
                text = when {
                    rate < 0.9f -> "Slower than normal — easier to follow in a crowd."
                    rate > 1.2f -> "Faster than normal — covers more ground between stops."
                    else -> "Normal pace, the default for a walking loop."
                },
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )

            Spacer(Modifier.height(GuideTokens.Space.base))
            GuideDivider()
            Spacer(Modifier.height(GuideTokens.Space.md))

            // ---- Where the voice comes from --------------------------------
            Text("Voice source", style = GuideTokens.Label)
            Spacer(Modifier.height(GuideTokens.Space.xs))
            StatusTag(text = "On this device", color = GuideTokens.Primary)
            Spacer(Modifier.height(GuideTokens.Space.xs))
            Text(
                text = "Narration is synthesized on your phone, so it keeps working " +
                    "with no signal. Cloud voices can sound better, but they send what " +
                    "you're listening to off the device — so they stay off unless you " +
                    "turn them on yourself.",
                style = GuideTokens.Chrome,
                color = GuideTokens.Text2,
            )
        }
    }
}

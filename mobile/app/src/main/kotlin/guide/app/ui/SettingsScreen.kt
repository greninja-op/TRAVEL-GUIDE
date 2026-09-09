package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.power.BatteryProfile
import guide.app.ui.theme.GuideTokens

/**
 * Settings: battery profile (cost disclosed), background-location opt-in
 * (SPEC §1.1 — separate consent, never at launch), quiet hours, speed gate,
 * auto-play. Plus first-launch consent entry (ConsentScreen on first run).
 */
@Composable
fun SettingsScreen(
    profile: BatteryProfile,
    onProfile: (BatteryProfile) -> Unit,
    backgroundOptIn: Boolean,
    onBackgroundOptIn: () -> Unit,
    quietEnabled: Boolean,
    onQuiet: (Boolean) -> Unit,
    autoPlay: Boolean,
    onAutoPlay: (Boolean) -> Unit,
) {
    var quiet by remember { mutableStateOf(quietEnabled) }
    var auto by remember { mutableStateOf(autoPlay) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", style = GuideTokens.Heading)
        Text("Battery profile (GPS cost shown)", style = GuideTokens.Chrome)
        BatteryProfile.entries.forEach { p ->
            TextButton(onClick = { onProfile(p) }) {
                Text((if (p == profile) "✓ " else "") + p.label)
            }
        }
        Text("Background location", style = GuideTokens.Chrome)
        Text(
            if (backgroundOptIn) "On — narration continues with screen off."
            else "Off — foreground only. Needed for screen-off narration.",
            style = GuideTokens.Body,
        )
        if (!backgroundOptIn) {
            TextButton(onClick = onBackgroundOptIn) { Text("Enable background location") }
        }
        Text("Quiet hours (22:00–07:00, cards only, no voice)", style = GuideTokens.Body)
        Switch(checked = quiet, onCheckedChange = { quiet = it; onQuiet(it) })
        Text("Auto-play nearby stories", style = GuideTokens.Body)
        Switch(checked = auto, onCheckedChange = { auto = it; onAutoPlay(it) })
    }
}

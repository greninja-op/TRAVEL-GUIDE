package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

/** Phase 2 voice settings: language/voice pick, speed, auto-play toggles. */
@Composable
fun VoiceSettings(onRate: (Float) -> Unit, onAutoPlay: (Boolean) -> Unit) {
    var rate by remember { mutableFloatStateOf(1.0f) }
    var auto by remember { mutableStateOf(true) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Voice", style = GuideTokens.Heading)
        Text("Playback speed: ${"%.1f".format(rate)}×", style = GuideTokens.Body)
        Slider(value = rate, onValueChange = { rate = it; onRate(it) }, valueRange = 0.5f..2.0f)
        Text("Auto-play nearby stories", style = GuideTokens.Body)
        Switch(checked = auto, onCheckedChange = { auto = it; onAutoPlay(it) })
        Text("Cloud voices are opt-in only (SPEC §1).", style = GuideTokens.Chrome)
    }
}

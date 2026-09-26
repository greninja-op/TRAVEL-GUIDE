package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

/**
 * Phase 1 now-playing bottom sheet: what is speaking now, transcript card,
 * queue ("next up"), one-tap mute. ContentDescriptions for TalkBack (P3 a11y).
 */
@Composable
fun NowPlayingSheet(
    poiName: String,
    transcript: String,
    nextUp: List<String>,
    muted: Boolean,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(poiName, style = GuideTokens.Heading)
            Text(transcript, style = GuideTokens.Body)
            if (nextUp.isNotEmpty()) {
                Text("Next up: ${nextUp.joinToString(", ")}", style = GuideTokens.Chrome)
            }
            TextButton(onClick = onMuteToggle) {
                Text(if (muted) "Unmute" else "Mute")
            }
        }
    }
}

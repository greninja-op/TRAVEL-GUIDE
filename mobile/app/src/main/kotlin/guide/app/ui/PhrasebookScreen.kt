package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.extras.Phrasebook
import guide.app.ui.theme.GuideTokens

/** Phrasebook: 10 survival phrases with TTS playback (SPEC §3.2 add-on). */
@Composable
fun PhrasebookScreen(onSpeak: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Phrasebook", style = GuideTokens.Heading)
        Phrasebook.phrases.forEach { (en, local) ->
            TextButton(onClick = { onSpeak(local) }) { Text("$en — $local") }
        }
    }
}

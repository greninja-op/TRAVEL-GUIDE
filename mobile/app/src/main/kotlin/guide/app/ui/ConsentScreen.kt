package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

/**
 * First-launch consent (SPEC §1 + docs/PRIVACY.md): plain-language location
 * rationale + background-location separation + battery honesty. Nothing is
 * requested before this screen is acknowledged.
 */
@Composable
fun ConsentScreen(onAcknowledgeForeground: () -> Unit, onLater: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Before we start", style = GuideTokens.Heading)
        Text(
            "Travel Guide uses your location to tell you about the places " +
                "around you. Foreground location only to start — background " +
                "tracking is a separate opt-in inside Settings, and the app " +
                "fully works without it. GPS uses battery; you can pick a " +
                "saver profile anytime. Visits and notes stay on this device.",
            style = GuideTokens.Body,
        )
        TextButton(onClick = onAcknowledgeForeground) { Text("Continue (foreground only)") }
        TextButton(onClick = onLater) { Text("Not now") }
    }
}

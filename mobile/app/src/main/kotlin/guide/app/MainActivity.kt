package guide.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import guide.app.ui.theme.GuideTokens

/**
 * Phase 0 shell: 8 destinations (SPEC §5), one-tap mute always visible.
 * Phase 1 wires Map/Nearby to GuideService + Narrator; screens below are
 * real layouts reading PackLoader state (no dead buttons).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GuideApp() }
    }
}

private val TABS = listOf("map", "nearby", "routes", "packs", "history", "settings")

@Composable
fun GuideApp() {
    MaterialTheme {
        val nav = rememberNavController()
        var muted by remember { mutableStateOf(false) }
        var current by remember { mutableStateOf("map") }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab,
                            onClick = { current = tab; nav.navigate(tab) },
                            label = { Text(tab) },
                            icon = {},
                        )
                    }
                }
            },
        ) { pad ->
            Surface(modifier = Modifier.fillMaxSize().padding(pad), color = GuideTokens.Bg) {
                Column {
                    // SPEC: one-tap mute always visible.
                    Button(onClick = { muted = !muted }) {
                        Text(if (muted) "Unmute guide" else "Mute guide")
                    }
                    NavHost(navController = nav, startDestination = "map") {
                        composable("map") { Screen("Map", "Offline map + you-are-here. Phase 1 adds live pins.") }
                        composable("nearby") { Screen("Nearby", "Live POI radar. Phase 1 adds live sorting.") }
                        composable("routes") { Screen("Routes", "Heritage Loop + Full Day, from the downloaded pack.") }
                        composable("packs") { Screen("Packs", "Fort Kochi v1.0.0 bundled. Phase 3 adds downloads.") }
                        composable("history") { Screen("History", "Visited POIs land here (Room visits table).") }
                        composable("settings") {
                            Screen("Settings", "Audio, battery profile (P3), background-location opt-in (P1).")
                        }
                        composable("poi/{id}") { Screen("POI Detail", "Story, facts, see-list, sources + pack version.") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Screen(title: String, body: String) {
    Column {
        Text(title, style = GuideTokens.Heading)
        Text(body, style = GuideTokens.Body)
    }
}

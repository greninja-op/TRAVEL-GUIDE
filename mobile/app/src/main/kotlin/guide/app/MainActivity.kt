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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import guide.app.power.BatteryProfile
import guide.app.ui.ConsentScreen
import guide.app.ui.HistoryScreen
import guide.app.ui.MapScreen
import guide.app.ui.NearbyScreen
import guide.app.ui.PacksScreen
import guide.app.ui.PhrasebookScreen
import guide.app.ui.RoutesScreen
import guide.app.ui.SettingsScreen
import guide.app.ui.Trio
import guide.app.ui.VoiceSettings
import guide.app.ui.theme.GuideTokens
import org.maplibre.android.maps.MapView

/**
 * Wired shell: consent gate → 6 tabs + detail/phrasebook routes.
 * LocationTracker + Narrator attach in GuideService; screens here own layout
 * and callbacks only. MapView lifecycle is forwarded below (all callbacks).
 */
class MainActivity : ComponentActivity() {
    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GuideApp(onMapView = { mapView = it }) }
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
}

private val TABS = listOf("map", "nearby", "routes", "packs", "history", "settings")

@Composable
fun GuideApp(onMapView: (MapView) -> Unit = {}) {
    MaterialTheme {
        var consented by rememberSaveable { mutableStateOf(false) }
        if (!consented) {
            Surface(color = GuideTokens.Bg) {
                ConsentScreen(
                    onAcknowledgeForeground = { consented = true },
                    onLater = { consented = true }, // explore UI only; no location until granted
                )
            }
            return@MaterialTheme
        }
        val nav = rememberNavController()
        var muted by remember { mutableStateOf(false) }
        var current by remember { mutableStateOf("map") }
        var profile by remember { mutableStateOf(BatteryProfile.BALANCED) }
        val context = LocalContext.current
        val mapView = remember {
            MapView(context).also { onMapView(it) }
        }
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
                        composable("map") {
                            MapScreen(
                                mapView = mapView,
                                offRoute = false,
                                seeingAnswer = null,
                                onSeeingTap = { nav.navigate("nearby") },
                            )
                        }
                        composable("nearby") {
                            NearbyScreen(
                                rows = emptyList(), // fed by LocationTracker + VisitRank
                                events = emptyList(),
                                seeingAnswer = null,
                                onSeeingTap = {},
                                onRowTap = { id -> nav.navigate("poi/$id") },
                            )
                        }
                        composable("routes") {
                            RoutesScreen(
                                routes = listOf(
                                    Trio("Fort Kochi Heritage Loop", "16 stops · ~150 min"),
                                    Trio("Kochi Full Day", "10 stops · ~240 min"),
                                ),
                                onPickPlan = {},
                            )
                        }
                        composable("packs") {
                            PacksScreen(
                                packs = emptyList(), // fed by PackManager inventory
                                downloadProgress = null,
                                onDownloadCity = {},
                                onDelete = {},
                            )
                        }
                        composable("history") {
                            HistoryScreen(visits = emptyList(), onSaveNote = { _, _ -> }, onExport = {})
                        }
                        composable("settings") {
                            Column {
                                SettingsScreen(
                                    profile = profile,
                                    onProfile = { profile = it },
                                    backgroundOptIn = false,
                                    onBackgroundOptIn = {},
                                    quietEnabled = true,
                                    onQuiet = {},
                                    autoPlay = true,
                                    onAutoPlay = {},
                                )
                                VoiceSettings(onRate = {}, onAutoPlay = {})
                            }
                        }
                        composable("phrasebook") {
                            PhrasebookScreen(onSpeak = {})
                        }
                        composable("poi/{id}") {
                            Text("POI detail — fed by PackLoader card.", style = GuideTokens.Body)
                        }
                    }
                }
            }
        }
    }
}

package guide.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import guide.app.location.GuideService
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
import guide.app.ui.components.GuideNavBar
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

// Navigation destinations live in ui/components/NavBar.kt (NavItem) so the bar
// and the routes can never drift apart.

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
        // Mute state lives with the player sheet (ui/NowPlayingSheet.kt) — it is
        // the audio's own control, and it is routed to the live Narrator via
        // GuideService.setMuted() so it takes effect in <500ms (F-08).
        var current by remember { mutableStateOf("map") }
        var profile by remember { mutableStateOf(BatteryProfile.BALANCED) }
        val context = LocalContext.current
        val mapView = remember {
            MapView(context).also { onMapView(it) }
        }

        // SPEC §1 + §1.1: consent is what starts tracking — not app launch.
        // Foreground-only until background is separately opted into in Settings.
        // Runtime permission is requested first; the service starts only on grant.
        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                GuideService.start(context, profile)
            }
        }
        LaunchedEffect(Unit) {
            val fine = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (fine) {
                GuideService.start(context, profile)
            } else {
                permLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }
        // Android 13+: the persistent notification needs POST_NOTIFICATIONS.
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        Scaffold(
            containerColor = GuideTokens.Bg,
            bottomBar = {
                // Real icons + labels, tonal surface, animated selection.
                // The mute control lives in the player sheet (where the audio
                // is), not here — it used to sit above the NavHost in a Column,
                // stealing vertical space from every screen and pushing the
                // map/content down (a direct cause of the overflow defects).
                GuideNavBar(
                    current = current,
                    onSelect = { tab ->
                        current = tab
                        nav.navigate(tab) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { pad ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(pad),
                color = GuideTokens.Bg,
            ) {
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
                            // Auto-play moved into SettingsScreen — it was
                            // duplicated in both places, and two controls for
                            // one setting drift apart.
                            VoiceSettings(onRate = {})
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

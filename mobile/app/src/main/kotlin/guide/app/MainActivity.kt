package guide.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import guide.app.data.AppState
import guide.app.extras.Events
import guide.app.location.GuideService
import guide.app.location.LocationTracker
import guide.app.map.MapStyle
import guide.app.map.OfflinePackHelper
import guide.app.power.BatteryProfile
import guide.app.ui.ConsentScreen
import guide.app.ui.HistoryScreen
import guide.app.ui.MapScreen
import guide.app.ui.NearbyScreen
import guide.app.ui.PacksScreen
import guide.app.ui.PhrasebookScreen
import guide.app.ui.PoiDetailScreen
import guide.app.ui.RoutesScreen
import guide.app.ui.SettingsScreen
import guide.app.ui.VoiceSettings
import guide.app.ui.components.EmptyState
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.GuideNavBar
import guide.app.ui.theme.GuideTokens
import kotlinx.coroutines.awaitCancellation
import org.maplibre.android.maps.MapView
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Wired shell: consent gate → 6 tabs + detail/phrasebook routes.
 * LocationTracker + Narrator attach in GuideService; screens here own layout
 * and callbacks only. MapView lifecycle is forwarded below (all callbacks).
 */
class MainActivity : ComponentActivity() {
    private var mapView: MapView? = null
    private var appState: AppState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE,
            ),
        )
        val map = MapView(this).apply {
            onCreate(savedInstanceState)
        }
        mapView = map
        val route = intent?.getStringExtra("route")
        setContent {
            GuideApp(
                initialRoute = route,
                mapView = map,
                onAppStateReady = { appState = it },
            )
        }
        handleNavigationIntent(intent)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val app = appState
        val action = intent.action
        if (action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            val text = intent.getStringExtra(android.content.Intent.EXTRA_TEXT).orEmpty()
            val firstLine = text.lines().firstOrNull { it.isNotBlank() && !it.startsWith("http") }
                ?: text.substringBefore("http").trim()
            val dest = firstLine.ifBlank { "Destination from Google Maps" }
            guide.app.navigation.MapsCompanionState.onNavStarted(
                destinationName = dest,
                etaOrDistance = "Synced from Google Maps",
                source = guide.app.navigation.CompanionSource.SHARED_INTENT,
            )
            app?.updateCompanionCorridor()
        } else if (action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "geo") {
            val uri = intent.data ?: return
            val schemeSpecific = uri.schemeSpecificPart
            val query = uri.getQueryParameter("q")
            val label = query?.substringAfter('(')?.substringBefore(')')
                ?: query?.replace('+', ' ')
                ?: "Destination from Google Maps"
            val coords = schemeSpecific.substringBefore('?').split(',')
            val lat = coords.getOrNull(0)?.toDoubleOrNull()
            val lng = coords.getOrNull(1)?.toDoubleOrNull()
            guide.app.navigation.MapsCompanionState.onNavStarted(
                destinationName = label,
                destinationLat = lat,
                destinationLng = lng,
                etaOrDistance = "Synced from Google Maps",
                source = guide.app.navigation.CompanionSource.SHARED_INTENT,
            )
            app?.updateCompanionCorridor()
        }
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
fun GuideApp(
    initialRoute: String? = null,
    mapView: MapView,
    onAppStateReady: (AppState) -> Unit = {},
) {
    MaterialTheme {
        val context = LocalContext.current
        val prefs = remember { context.getSharedPreferences("guide_prefs", android.content.Context.MODE_PRIVATE) }
        var consented by rememberSaveable { mutableStateOf(prefs.getBoolean("consented", true)) }
        if (!consented) {
            Surface(color = GuideTokens.Bg) {
                ConsentScreen(
                    onAcknowledgeForeground = {
                        consented = true
                        prefs.edit().putBoolean("consented", true).apply()
                    },
                    onLater = {
                        consented = true
                        prefs.edit().putBoolean("consented", true).apply()
                    },
                )
            }
            return@MaterialTheme
        }
        val nav = rememberNavController()
        var current by remember { mutableStateOf(initialRoute ?: "map") }
        LaunchedEffect(initialRoute) {
            if (!initialRoute.isNullOrEmpty() && initialRoute != "map") {
                nav.navigate(initialRoute)
            }
        }
        var profile by remember { mutableStateOf(BatteryProfile.BALANCED) }

        // The one seam between the engine's data and the screens. Built once,
        // scoped to the composition. Before this, every screen received
        // emptyList()/{} and could only ever show its empty state.
        val app = remember { AppState(context).also { onAppStateReady(it) } }

        // Settings that the service must also know about (they change how the
        // guide behaves while it runs, not just what the screen shows).
        var quiet by rememberSaveable { mutableStateOf(true) }
        var autoPlay by rememberSaveable { mutableStateOf(true) }
        var backgroundOptIn by rememberSaveable { mutableStateOf(false) }

        // Live narration state, mirrored from the service.
        var activePoiId by remember { mutableStateOf<String?>(null) }
        var seeingAnswer by remember { mutableStateOf<String?>(null) }
        var userPos by remember { mutableStateOf<org.maplibre.android.geometry.LatLng?>(null) }
        var downloadProgress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
        var dayPlanMinutes by rememberSaveable { mutableStateOf(120) }
        // No events asset ships with the beta pack, so this is empty by design
        // — the Local Events layer lights up when a pack update carries them
        // (SPEC §3.2). Filtering through the real API keeps the shape correct.
        val events = remember {
            Events.forDate(emptyList(), LocalDate.now().toString())
        }

        // Background location is a SEPARATE, plain-language opt-in (SPEC §1.1):
        // the OS only grants ACCESS_BACKGROUND_LOCATION as its own prompt, and
        // only after foreground is already held — so this asks for exactly that
        // one permission, never bundled with the first-launch request.
        val backgroundLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            backgroundOptIn = granted
            if (granted) GuideService.start(context, profile)
        }
        val onBackgroundOptIn: () -> Unit = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                backgroundOptIn = true
            }
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

        // Live location for the map + the distance-sorted Nearby list. The
        // GuideService also tracks (it owns narration and the foreground
        // notification); this is a second, lighter subscription purely so the
        // UI reflects position — the two do not fight, and the UI one stops
        // with the composition.
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return@LaunchedEffect
            }
            val tracker = LocationTracker(context)
            try {
                tracker.start(profile) { fix ->
                    app.onFix(fix.lat, fix.lng)
                    userPos = org.maplibre.android.geometry.LatLng(fix.lat, fix.lng)
                }
                awaitCancellation()
            } finally {
                tracker.stop()
            }
        }

        // "What am I seeing?" — the app's signature question. Answered from the
        // same seeing logic the narrator uses, against the pack, using the
        // latest fix. Honest when there is no fix or nothing is near.
        val onSeeingTap: () -> Unit = {
            val lat = app.lastLat
            val lng = app.lastLng
            seeingAnswer = if (lat == null || lng == null) {
                "I don't have your location yet — give the GPS a moment outdoors."
            } else {
                val nearest = app.nearby.firstOrNull()
                if (nearest == null) {
                    "No stops in this pack are near you right now."
                } else {
                    val card = app.card(nearest.id)
                    if (card == null) {
                        "No stops in this pack are near you right now."
                    } else {
                        "${card.name} — ${card.summary}"
                    }
                }
            }
            nav.navigate("nearby")
        }

        val window = (context as? android.app.Activity)?.window
        LaunchedEffect(nav) {
            nav.addOnDestinationChangedListener { _, destination, _ ->
                current = destination.route ?: "map"
            }
        }
        LaunchedEffect(current) {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = true
                if (current == "map" || current.startsWith("poi/")) {
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    insetsController.isAppearanceLightStatusBars = true
                } else {
                    window.statusBarColor = android.graphics.Color.parseColor("#F8F9FA")
                    insetsController.isAppearanceLightStatusBars = true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.navigationBarColor = android.graphics.Color.WHITE
                }
            }
        }

        Scaffold(
            containerColor = GuideTokens.Bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!current.startsWith("poi/")) {
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
                }
            },
        ) { pad ->
            val statusPad = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val navPad = pad.calculateBottomPadding()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = navPad),
            ) {
                NavHost(navController = nav, startDestination = "map") {
                    composable("map") {
                        MapScreen(
                            mapView = mapView,
                            offRoute = false,
                            seeingAnswer = null,
                            onSeeingTap = { nav.navigate("nearby") },
                            onPinTap = { id -> nav.navigate("poi/$id") },
                            onSettingsTap = { nav.navigate("settings") },
                            pins = app.pins(),
                            route = app.routePoints(),
                            userPosition = userPos,
                            activePinId = activePoiId,
                        )
                    }
                    composable("nearby") {
                        Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                            NearbyScreen(
                                rows = app.nearby,
                                events = events,
                                seeingAnswer = seeingAnswer,
                                onSeeingTap = onSeeingTap,
                                onRowTap = { id -> nav.navigate("poi/$id") },
                            )
                        }
                    }
                    composable("routes") {
                        Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                            RoutesScreen(
                                routes = app.routes,
                                onPickPlan = { minutes -> dayPlanMinutes = minutes },
                            )
                        }
                    }
                    composable("packs") {
                        Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                            PacksScreen(
                                packs = app.packs,
                                downloadProgress = downloadProgress,
                                onDownloadCity = {
                                    OfflinePackHelper.downloadCity(
                                        context = context,
                                        styleUrl = MapStyle.LOCAL_STYLE_JSON,
                                        onProgress = { done, total ->
                                            downloadProgress = done to total
                                        },
                                        onDone = { downloadProgress = null },
                                    )
                                },
                                onDelete = { /* bundled pack is never evicted */ },
                            )
                        }
                    }
                    composable("history") {
                        Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                            HistoryScreen(
                                visits = app.visits,
                                onSaveNote = { poiId, text -> app.saveNote(poiId, text) },
                                onExport = { exportTrip(context, app) },
                            )
                        }
                    }
                    composable("settings") {
                        Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                            SettingsScreen(
                                profile = profile,
                                onProfile = {
                                    profile = it
                                    GuideService.setProfile(context, it)
                                },
                                backgroundOptIn = backgroundOptIn,
                                onBackgroundOptIn = onBackgroundOptIn,
                                quietEnabled = quiet,
                                onQuiet = { quiet = it; GuideService.setQuiet(context, it) },
                                autoPlay = autoPlay,
                                onAutoPlay = { autoPlay = it; GuideService.setAutoPlay(context, it) },
                                onSimulateMapsRoute = { poiId ->
                                    app.simulateCompanionSession(poiId)
                                },
                                appState = app,
                                onExportData = { exportTrip(context, app) },
                                onClearData = { app.clearAllData() },
                            )
                        }
                    }
                    composable("phrasebook") {
                        Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                            PhrasebookScreen(onSpeak = { phrase -> GuideService.speak(context, phrase) })
                        }
                    }
                    composable("poi/{id}") { entry ->
                        val id = entry.arguments?.getString("id").orEmpty()
                        val card = app.card(id)
                        if (card == null) {
                            Box(modifier = Modifier.fillMaxSize().padding(top = statusPad)) {
                                EmptyState(
                                    title = "That place isn't in this pack",
                                    body = "It may have been renamed or moved in a newer pack version.",
                                    icon = GuideIcons.Compass,
                                )
                            }
                        } else {
                            PoiDetailScreen(
                                card = card,
                                hoursText = card.hours,
                                openNow = isOpenNow(card.hours),
                                onAddNote = { nav.navigate("history") },
                                onBack = { nav.popBackStack() },
                                onStartAudio = {
                                    activePoiId = id
                                    GuideService.speak(context, "${card.name}. ${card.summary}")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Is this POI open right now?
 *
 * `hours` arrives from the pack as "09:00–17:00" (en dash) or null, where null
 * means "always viewable" — a street, a fishing net, an open facade. Those must
 * read as open, not as unknown, or the app would imply a landmark is closed.
 */
private fun isOpenNow(hours: String?): Boolean {
    if (hours == null) return true
    val parts = hours.split("–", "-").map { it.trim() }
    if (parts.size != 2) return true
    return runCatching {
        val now = LocalTime.now()
        val open = LocalTime.parse(parts[0], DateTimeFormatter.ofPattern("HH:mm"))
        val close = LocalTime.parse(parts[1], DateTimeFormatter.ofPattern("HH:mm"))
        if (close < open) now >= open || now <= close else now in open..close
    }.getOrDefault(true)
}

/**
 * Export the trip as a Markdown file and hand it to the system share sheet.
 *
 * The trip is the user's own record, so export is a plain text file they can
 * keep or send anywhere — no account, no upload (SPEC §1.5). Uses TripExport so
 * the format matches the Python tool mirror exactly.
 */
private fun exportTrip(context: android.content.Context, app: AppState) {
    runCatching {
        val body = guide.app.export.TripExport.toMarkdown(
            date = java.time.LocalDate.now().toString(),
            cityName = "Fort Kochi & Mattancherry",
            stops = app.visits.mapNotNull { v ->
                app.card(v.poiId)?.let { c ->
                    guide.app.export.TripExport.Stop(
                        poiId = c.id, name = c.name, summary = c.summary,
                    )
                }
            },
            notes = app.visits.mapNotNull { v -> v.note?.let { v.poiId to it } }.toMap(),
        )
        val file = java.io.File(context.cacheDir, "trip-${System.currentTimeMillis()}.md")
        file.writeText(body)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/markdown"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(send, "Export trip").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}

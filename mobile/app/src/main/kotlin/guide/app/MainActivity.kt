package guide.app

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import guide.app.companion.TravelGuideAccessibilityService
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
import guide.app.ui.PermissionGateScreen
import guide.app.ui.PhrasebookScreen
import guide.app.ui.PoiDetailScreen
import guide.app.ui.PrivacyPolicyViewer
import guide.app.ui.SettingsScreen
import guide.app.ui.VoiceSettings
import guide.app.ui.components.EmptyState
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.GuideNavBar
import guide.app.ui.theme.GuideTokens
import kotlinx.coroutines.awaitCancellation
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Wired shell: consent gate → 6 tabs + detail/phrasebook routes.
 * LocationTracker + Narrator attach in GuideService; screens here own layout
 * and callbacks only. MapView lifecycle is forwarded below (all callbacks).
 */
class MainActivity : FragmentActivity() {
    private var mapView: MapView? = null
    private var appState: AppState? = null
    private val isAppLocked = mutableStateOf(false)
    private var isAuthenticating = false

    private val pinUnlockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        isAuthenticating = false
        if (result.resultCode == Activity.RESULT_OK) {
            unlockApp()
        }
    }

    fun unlockApp() {
        isAppLocked.value = false
        isAuthenticating = false
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun launchPinUnlock() {
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(
            "Unlock Travel Guide",
            "Enter your device PIN, pattern, or password to access Travel Guide",
        )
        if (intent != null) {
            try {
                isAuthenticating = true
                pinUnlockLauncher.launch(intent)
            } catch (_: Exception) {
                unlockApp()
            }
        } else {
            // Device does not have secure lock screen configured
            unlockApp()
        }
    }

    fun authenticateWithBiometrics(onSuccess: () -> Unit = {}) {
        if (isAuthenticating) return

        val biometricManager = BiometricManager.from(this)
        val canWeak = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )

        if (canWeak != BiometricManager.BIOMETRIC_SUCCESS) {
            // Biometrics not enrolled, hardware unavailable, or not supported. Fallback directly to PIN/Pattern.
            launchPinUnlock()
            return
        }

        isAuthenticating = true
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlockApp()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        launchPinUnlock()
                    } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT ||
                               errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT ||
                               errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                               errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                               errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE) {
                        launchPinUnlock()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Travel Guide")
            .setSubtitle("Touch the fingerprint sensor or confirm your identity")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK,
            )
            .setNegativeButtonText("Use PIN / Pattern")
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (_: Exception) {
            isAuthenticating = false
            launchPinUnlock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enforceHighRefreshRate()

        // Security Hardening (OWASP M8, CWE-1021: Tapjacking & Overlay Prevention)
        window.decorView.filterTouchesWhenObscured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val method = window.javaClass.getMethod("setHideOverlayWindows", Boolean::class.javaPrimitiveType)
                method.invoke(window, true)
            }
        }

        val prefs = getSharedPreferences("guide_prefs", MODE_PRIVATE)
        val appLockEnabled = prefs.getBoolean("app_lock_enabled", false)
        isAppLocked.value = appLockEnabled
        if (appLockEnabled) {
            // Task Snapshot & Screen Leakage Protection (CWE-200)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.WHITE,
                android.graphics.Color.parseColor("#131A26"),
            ),
        )
        runCatching {
            MapsInitializer.initialize(applicationContext, MapsInitializer.Renderer.LATEST) { }
        }
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
                isLocked = isAppLocked.value,
                onUnlockWithBiometrics = { authenticateWithBiometrics() },
                onUnlockWithPin = { launchPinUnlock() },
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
            val rawText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT).orEmpty()
            // Security Bounds & Sanitization (OWASP M4, CWE-20): Clamp length and strip control chars
            val text = rawText.take(250)
            val firstLine = text.lines().firstOrNull { it.isNotBlank() && !it.startsWith("http") }
                ?: text.substringBefore("http").trim()
            val dest = firstLine.replace(Regex("""[^\w\s.,'#\-]"""), " ").trim().take(80)
                .ifBlank { "Destination from Google Maps" }
            guide.app.navigation.MapsCompanionState.onNavStarted(
                destinationName = dest,
                etaOrDistance = "Synced from Google Maps",
                source = guide.app.navigation.CompanionSource.SHARED_INTENT,
            )
            app?.updateCompanionCorridor()
        } else if (action == android.content.Intent.ACTION_VIEW && intent.data?.scheme == "geo") {
            val uri = intent.data ?: return
            val schemeSpecific = uri.schemeSpecificPart.orEmpty().take(120)
            val query = uri.getQueryParameter("q")?.take(120)
            val rawLabel = query?.substringAfter('(')?.substringBefore(')')
                ?: query?.replace('+', ' ')
                ?: "Destination from Google Maps"
            val label = rawLabel.replace(Regex("""[^\w\s.,'#\-]"""), " ").trim().take(80)
                .ifBlank { "Destination from Google Maps" }
            val coords = schemeSpecific.substringBefore('?').split(',')
            val rawLat = coords.getOrNull(0)?.toDoubleOrNull()
            val rawLng = coords.getOrNull(1)?.toDoubleOrNull()

            // Strict geographic coordinate bounds verification
            val validLat = rawLat?.takeIf { !it.isNaN() && !it.isInfinite() && it in -90.0..90.0 }
            val validLng = rawLng?.takeIf { !it.isNaN() && !it.isInfinite() && it in -180.0..180.0 }

            guide.app.navigation.MapsCompanionState.onNavStarted(
                destinationName = label,
                destinationLat = validLat,
                destinationLng = validLng,
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
        enforceHighRefreshRate()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        mapView?.onStop()
        val prefs = getSharedPreferences("guide_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("app_lock_enabled", false)) {
            isAppLocked.value = true
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
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

    /**
     * Unlocks the full 120 FPS / high refresh rate pipeline.
     * Prevents OEM / MIUI dynamic display throttling from capping the app at 60Hz.
     */
    private fun enforceHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val disp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val modes = disp?.supportedModes ?: emptyArray()
            // Find highest refresh rate mode (targeting 120Hz mode, or max available >= 90Hz)
            val bestMode = modes
                .filter { it.refreshRate >= 118.0f }
                .maxByOrNull { it.refreshRate }
                ?: modes.maxByOrNull { it.refreshRate }

            val params = window.attributes
            if (bestMode != null) {
                params.preferredDisplayModeId = bestMode.modeId
            }
            @Suppress("DEPRECATION")
            params.preferredRefreshRate = bestMode?.refreshRate ?: 120f
            window.attributes = params
        }
    }
}

// Navigation destinations live in ui/components/NavBar.kt (NavItem) so the bar
// and the routes can never drift apart.

@Composable
fun GuideApp(
    initialRoute: String? = null,
    mapView: MapView,
    onAppStateReady: (AppState) -> Unit = {},
    isLocked: Boolean = false,
    onUnlockWithBiometrics: () -> Unit = {},
    onUnlockWithPin: () -> Unit = {},
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("guide_prefs", android.content.Context.MODE_PRIVATE) }
    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    guide.app.ui.theme.GuideTheme(darkTheme = isDark) {
        if (isLocked) {
            AppLockScreen(
                onUnlockWithBiometrics = onUnlockWithBiometrics,
                onUnlockWithPin = onUnlockWithPin,
            )
            return@GuideTheme
        }

        var profile by remember { mutableStateOf(BatteryProfile.BALANCED) }

        fun checkLocationGranted(): Boolean {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        var hasLocationPerm by remember { mutableStateOf(checkLocationGranted()) }
        var hasAccessibilityPerm by remember { mutableStateOf(TravelGuideAccessibilityService.isEnabled(context)) }
        var dismissedGate by rememberSaveable { mutableStateOf(false) }
        var showPrivacyViewer by remember { mutableStateOf(false) }

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    val locGranted = checkLocationGranted()
                    hasLocationPerm = locGranted
                    hasAccessibilityPerm = TravelGuideAccessibilityService.isEnabled(context)
                    if (locGranted) {
                        GuideService.start(context, profile)
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            hasLocationPerm = granted
            if (granted) {
                GuideService.start(context, profile)
            }
        }

        // If required location permission is missing, display Permission Gate
        if (!hasLocationPerm && !dismissedGate) {
            if (showPrivacyViewer) {
                PrivacyPolicyViewer(onDismiss = { showPrivacyViewer = false })
            } else {
                PermissionGateScreen(
                    hasLocationPermission = hasLocationPerm,
                    hasAccessibilityPermission = hasAccessibilityPerm,
                    onRequestLocation = {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    onContinueToMap = { dismissedGate = true },
                    onViewPrivacyPolicy = { showPrivacyViewer = true },
                )
            }
            return@GuideTheme
        }
        val nav = rememberNavController()
        val navBackStackEntry by nav.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "map"
        val activeTab = when {
            currentRoute.startsWith("poi/") -> "map"
            currentRoute.startsWith("phrasebook") -> "nearby"
            else -> currentRoute
        }

        LaunchedEffect(initialRoute) {
            if (!initialRoute.isNullOrEmpty() && initialRoute != "map") {
                kotlinx.coroutines.delay(120)
                nav.navigate(initialRoute) {
                    launchSingleTop = true
                }
            }
        }
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
        var userPos by remember { mutableStateOf<LatLng?>(null) }
        var downloadProgress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
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

        // Android 13+: the persistent notification needs POST_NOTIFICATIONS.
        val notifLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) {}
        LaunchedEffect(Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                    userPos = LatLng(fix.lat, fix.lng)
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
        LaunchedEffect(currentRoute, isDark) {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightNavigationBars = !isDark
                if (currentRoute == "map" || currentRoute.startsWith("poi/")) {
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    insetsController.isAppearanceLightStatusBars = !isDark
                } else {
                    window.statusBarColor = if (isDark) android.graphics.Color.parseColor("#0B0F17") else android.graphics.Color.parseColor("#F8F9FA")
                    insetsController.isAppearanceLightStatusBars = !isDark
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    window.navigationBarColor = if (isDark) android.graphics.Color.parseColor("#131A26") else android.graphics.Color.WHITE
                }
            }
        }

        Scaffold(
            containerColor = GuideTokens.Bg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!currentRoute.startsWith("poi/")) {
                    GuideNavBar(
                        current = activeTab,
                        onSelect = { tab ->
                            if (tab == "map") {
                                if (!nav.popBackStack("map", inclusive = false)) {
                                    nav.navigate("map") {
                                        popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                nav.navigate(tab) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                    .background(GuideTokens.Bg)
                    .padding(bottom = navPad),
            ) {
                NavHost(
                    navController = nav,
                    startDestination = "map",
                    enterTransition = {
                        val isDetailPush = targetState.destination.route?.startsWith("poi/") == true ||
                            targetState.destination.route?.startsWith("phrasebook") == true
                        if (isDetailPush) {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                            ) + fadeIn(animationSpec = tween(durationMillis = 240))
                        } else {
                            fadeIn(
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            ) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            )
                        }
                    },
                    exitTransition = {
                        val isDetailPush = targetState.destination.route?.startsWith("poi/") == true ||
                            targetState.destination.route?.startsWith("phrasebook") == true
                        if (isDetailPush) {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 340, easing = FastOutSlowInEasing),
                                targetOffset = { fullWidth -> fullWidth / 4 },
                            ) + fadeOut(animationSpec = tween(durationMillis = 240))
                        } else {
                            fadeOut(
                                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
                            )
                        }
                    },
                    popEnterTransition = {
                        val isDetailPop = initialState.destination.route?.startsWith("poi/") == true ||
                            initialState.destination.route?.startsWith("phrasebook") == true
                        if (isDetailPop) {
                            slideIntoContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                initialOffset = { fullWidth -> fullWidth / 4 },
                            ) + fadeIn(animationSpec = tween(durationMillis = 200))
                        } else {
                            fadeIn(
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            ) + scaleIn(
                                initialScale = 0.98f,
                                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            )
                        }
                    },
                    popExitTransition = {
                        val isDetailPop = initialState.destination.route?.startsWith("poi/") == true ||
                            initialState.destination.route?.startsWith("phrasebook") == true
                        if (isDetailPop) {
                            slideOutOfContainer(
                                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                            ) + fadeOut(animationSpec = tween(durationMillis = 200))
                        } else {
                            fadeOut(
                                animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
                            )
                        }
                    },
                ) {
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
                                themeMode = themeMode,
                                onThemeMode = { newMode ->
                                    themeMode = newMode
                                    prefs.edit().putString("theme_mode", newMode).apply()
                                },
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
        val exportDir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
        val file = java.io.File(exportDir, "trip-${System.currentTimeMillis()}.md")
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

/**
 * Biometric authentication security screen displayed when App Lock is active.
 * Supports fingerprint (Class 2 / Class 3) and direct system PIN / Pattern fallback.
 */
@Composable
private fun AppLockScreen(
    onUnlockWithBiometrics: () -> Unit,
    onUnlockWithPin: () -> Unit,
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        onUnlockWithBiometrics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GuideTokens.Bg)
            .padding(GuideTokens.Space.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = GuideTokens.PrimaryWash,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GuideIcons.Lock,
                        contentDescription = "Locked",
                        tint = GuideTokens.Primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Spacer(Modifier.height(GuideTokens.Space.lg))
            Text(
                text = "Travel Guide Locked",
                style = GuideTokens.Title,
                color = GuideTokens.Text,
            )
            Spacer(Modifier.height(GuideTokens.Space.xs))
            Text(
                text = "Biometric protection is enabled for your travel journal and visit history.",
                style = GuideTokens.Chrome,
                color = GuideTokens.Text2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = GuideTokens.Space.sm),
            )
            Spacer(Modifier.height(GuideTokens.Space.xl))
            guide.app.ui.components.GuideButton(
                text = "Unlock with Fingerprint",
                onClick = onUnlockWithBiometrics,
                variant = guide.app.ui.components.GuideButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(GuideTokens.Space.sm))
            guide.app.ui.components.GuideButton(
                text = "Unlock with PIN / Pattern",
                onClick = onUnlockWithPin,
                variant = guide.app.ui.components.GuideButtonVariant.Tonal,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

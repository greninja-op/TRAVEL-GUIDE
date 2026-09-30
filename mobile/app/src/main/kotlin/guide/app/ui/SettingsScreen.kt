package guide.app.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import guide.app.MainActivity
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import guide.app.companion.TravelGuideAccessibilityService
import guide.app.data.AppLanguage
import guide.app.data.AppStrings
import guide.app.data.AppState
import guide.app.location.GuideService
import guide.app.navigation.MapsCompanionState
import guide.app.power.BatteryProfile
import guide.app.security.CryptoVault
import guide.app.ui.components.CategoryChip
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.Pressable
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.components.fadingEdges
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Motion

/**
 * Settings & Privacy Control Center — User Sovereignty, Security & Companion Settings.
 *
 * Implements:
 * 1. Explorer Account / Local Identity
 * 2. External Google Maps Navigation Companion (with Desk-Testing Destination selector)
 * 3. 100% On-Device Privacy & Security Shield
 * 4. App Lock & Data Protection (Wipe & Export)
 * 5. Operating System Permission Transparency Audit
 * 6. Audio & Voice Settings (Speed, Auto-Play, Quiet Hours)
 * 7. Battery Profile Selection (Disclosed GPS Cost)
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
    onSimulateMapsRoute: (String) -> Unit = {},
    appState: AppState? = null,
    onExportData: () -> Unit = {},
    onClearData: () -> Unit = {},
    themeMode: String = "system",
    onThemeMode: (String) -> Unit = {},
    language: AppLanguage = appState?.appLanguage ?: AppLanguage.ENGLISH,
) {
    val currentLanguage = language
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE) }

    var quiet by remember { mutableStateOf(quietEnabled) }
    var auto by remember { mutableStateOf(autoPlay) }
    var appLock by remember { mutableStateOf(prefs.getBoolean("app_lock_enabled", false)) }
    var speechRate by remember { mutableFloatStateOf(prefs.getFloat("speech_rate", 1.0f)) }

    var explorerName by remember { mutableStateOf(prefs.getString("user_name", "Aswin") ?: "Aswin") }
    var showNameDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Spontaneous AI Tour Guide state
    val defaultOpenAiKey = remember {
        runCatching { guide.app.BuildConfig.OPENAI_API_KEY }.getOrDefault("")
    }
    var openAiKeyInput by remember {
        mutableStateOf(prefs.getString("openai_api_key", "")?.ifBlank { null } ?: defaultOpenAiKey)
    }
    var selectedPersonaId by remember {
        mutableStateOf(
            prefs.getString("ai_tour_persona", guide.app.voice.SpontaneousGuideAiEngine.Persona.INSIDER.id)
                ?: guide.app.voice.SpontaneousGuideAiEngine.Persona.INSIDER.id
        )
    }
    var keySaveFeedback by remember { mutableStateOf<String?>(null) }
    var aiSampleStory by remember { mutableStateOf<String?>(null) }
    var isGeneratingStory by remember { mutableStateOf(false) }
    var earbudsChime by remember { mutableStateOf(prefs.getBoolean("earbuds_chime_enabled", true)) }
    var earbudsAutoPause by remember { mutableStateOf(prefs.getBoolean("earbuds_autopause_enabled", true)) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // External Maps testing state
    var selectedTestPoiId by remember { mutableStateOf("chinese-fishing-nets") }
    var customDestInput by remember { mutableStateOf("") }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }

    // Notifications state — real working notification controls
    var notificationsEnabled by remember {
        mutableStateOf(prefs.getBoolean("notifications_enabled", true))
    }
    var arrivalAlertsEnabled by remember {
        mutableStateOf(prefs.getBoolean("arrival_alerts_enabled", true))
    }
    val notifManager = remember { NotificationManagerCompat.from(context) }
    var systemNotifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                notifManager.areNotificationsEnabled()
            }
        )
    }
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        systemNotifGranted = granted
        if (granted) {
            notificationsEnabled = true
            prefs.edit().putBoolean("notifications_enabled", true).apply()
            GuideService.setNotificationsEnabled(context, true)
        }
    }
    val onToggleNotifications: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                notificationsEnabled = true
                prefs.edit().putBoolean("notifications_enabled", true).apply()
                GuideService.setNotificationsEnabled(context, true)
            }
        } else {
            notificationsEnabled = false
            prefs.edit().putBoolean("notifications_enabled", false).apply()
            GuideService.setNotificationsEnabled(context, false)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(9901)
        }
    }
    var notifFeedback by remember { mutableStateOf<String?>(null) }

    val hasAccessibilityAccess = remember { TravelGuideAccessibilityService.isEnabled(context) }
    val hasNotificationAccess = remember { MapsCompanionState.isNotificationAccessGranted(context) }
    val activeSession = MapsCompanionState.currentSession

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .fadingEdges(listState, topFadeHeight = 36.dp, bottomFadeHeight = 52.dp),
        contentPadding = PaddingValues(
            start = GuideTokens.Space.screenPad,
            end = GuideTokens.Space.screenPad,
            top = GuideTokens.Space.base,
            bottom = GuideTokens.Space.xxl + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
    ) {
        // ---- Top Header -----------------------------------------------------
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(AppStrings.settingsTitle(currentLanguage), style = GuideTokens.Heading)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    AppStrings.settingsSubtitle(currentLanguage),
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
            }
        }

        // =====================================================================
        // 1. Explorer Account & Identity Card
        // =====================================================================
        item { SectionHeader(AppStrings.explorerProfileHeader(currentLanguage)) }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar circle with coral glow
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = CircleShape,
                        color = GuideTokens.Primary,
                        shadowElevation = 3.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = explorerName.take(1).uppercase(),
                                style = GuideTokens.Title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = explorerName,
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = GuideTokens.PrimaryWash,
                                modifier = Modifier.clickable { showNameDialog = true },
                            ) {
                                Text(
                                    text = AppStrings.editBtn(currentLanguage),
                                    style = GuideTokens.Caption,
                                    color = GuideTokens.Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = AppStrings.heritageExplorerSubtitle(currentLanguage),
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val visitedCount = appState?.visitedIds?.size ?: 0
                    val totalStops = appState?.cards?.size ?: 24
                    val notesCount = appState?.visits?.count { it.note != null } ?: 0

                    ProfileStatColumn(
                        value = "$visitedCount / $totalStops",
                        label = AppStrings.placesDiscoveredStat(currentLanguage),
                    )
                    ProfileStatColumn(
                        value = "$notesCount",
                        label = AppStrings.tripNotesStat(currentLanguage),
                    )
                    ProfileStatColumn(
                        value = AppStrings.zeroCloudStat(currentLanguage),
                        label = AppStrings.dataSovereigntyStat(currentLanguage),
                    )
                }
            }
        }

        // =====================================================================
        // 2. Appearance & Display (Crisp Light & Obsidian Carbon Dark)
        // =====================================================================
        item { SectionHeader(AppStrings.appearanceSection(currentLanguage)) }
        item {
            GuideCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = AppStrings.themeModeTitle(currentLanguage),
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Text(
                                text = AppStrings.themeModeDesc(currentLanguage),
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                        }
                        StatusTag(
                            text = when (themeMode) {
                                "dark" -> "OBSIDIAN"
                                "light" -> "WARM LIGHT"
                                else -> "SYSTEM"
                            },
                            color = GuideTokens.Primary,
                        )
                    }

                    Spacer(Modifier.height(GuideTokens.Space.base))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ThemeOptionTile(
                            modifier = Modifier.weight(1f),
                            title = AppStrings.themeSystemLabel(currentLanguage),
                            subtitle = AppStrings.themeSystemDesc(currentLanguage),
                            icon = GuideIcons.Sliders,
                            isSelected = themeMode == "system",
                            onClick = { onThemeMode("system") },
                        )
                        ThemeOptionTile(
                            modifier = Modifier.weight(1f),
                            title = AppStrings.themeLightLabel(currentLanguage),
                            subtitle = AppStrings.themeLightDesc(currentLanguage),
                            icon = GuideIcons.Sun,
                            isSelected = themeMode == "light",
                            onClick = { onThemeMode("light") },
                        )
                        ThemeOptionTile(
                            modifier = Modifier.weight(1f),
                            title = AppStrings.themeDarkLabel(currentLanguage),
                            subtitle = AppStrings.themeDarkDesc(currentLanguage),
                            icon = GuideIcons.Moon,
                            isSelected = themeMode == "dark",
                            onClick = { onThemeMode("dark") },
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 3. Google Maps Navigation Companion (Auto-Sync & Desk Testing)
        // =====================================================================
        item { SectionHeader(AppStrings.companionSectionHeader(currentLanguage)) }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.companionSyncTitle(currentLanguage),
                            style = GuideTokens.Title,
                            color = GuideTokens.Text,
                        )
                        Text(
                            text = AppStrings.companionSyncDesc(currentLanguage),
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                    StatusTag(
                        text = if (hasNotificationAccess || hasAccessibilityAccess) "SYNC READY" else "NEEDS PERMISSION",
                        color = if (hasNotificationAccess || hasAccessibilityAccess) GuideTokens.Success else GuideTokens.Highlight,
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.sm))

                // Direct Accessibility Mode (zero notifications, zero google account required)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasAccessibilityAccess) Color(0xFFF0FDF4) else GuideTokens.Surface2,
                    border = BorderStroke(1.dp, if (hasAccessibilityAccess) Color(0xFFBBF7D0) else GuideTokens.Border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (hasAccessibilityAccess) Color(0xFFDCFCE7) else GuideTokens.Border),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = GuideIcons.Navigation,
                                        contentDescription = null,
                                        tint = if (hasAccessibilityAccess) Color(0xFF16A34A) else GuideTokens.Text2,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = AppStrings.accessibilityCompanionTitle(currentLanguage),
                                        style = GuideTokens.Label,
                                        color = GuideTokens.Text,
                                    )
                                    Text(
                                        text = AppStrings.accessibilityCompanionDesc(currentLanguage),
                                        style = GuideTokens.Caption,
                                        color = GuideTokens.Text2,
                                    )
                                }
                            }
                            StatusTag(
                                text = if (hasAccessibilityAccess) "ACTIVE" else "DISABLED",
                                color = if (hasAccessibilityAccess) GuideTokens.Success else GuideTokens.Text2,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Quietly reads destination directly on-screen from Google Maps without requiring notifications, Google accounts, or cloud APIs.",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                        Spacer(Modifier.height(8.dp))
                        GuideButton(
                            text = if (hasAccessibilityAccess) "Accessibility Service Active" else "Enable in Android Accessibility Settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            variant = if (hasAccessibilityAccess) GuideButtonVariant.Tonal else GuideButtonVariant.Primary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.sm))

                if (!hasNotificationAccess) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Notification Listener Access is required for Android to allow Travel Guide to detect Google Maps navigation turn cues.",
                                style = GuideTokens.Caption,
                                color = Color(0xFF9A3412),
                            )
                            Spacer(Modifier.height(8.dp))
                            GuideButton(
                                text = "Grant Notification Access in Android Settings",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                },
                                variant = GuideButtonVariant.Primary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    Spacer(Modifier.height(GuideTokens.Space.sm))
                }

                // Active navigation status
                if (activeSession != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A)),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "ACTIVE ROUTE DETECTED",
                                    style = GuideTokens.Caption,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = activeSession.destinationName,
                                style = GuideTokens.Label,
                                color = Color(0xFF14532D),
                            )
                            Text(
                                text = "${activeSession.etaOrDistance ?: "Active"} • ${MapsCompanionState.corridorPoiIds.size} stops pre-warmed along path",
                                style = GuideTokens.Caption,
                                color = Color(0xFF15803D),
                            )
                            Spacer(Modifier.height(8.dp))
                            GuideButton(
                                text = "Clear Active Navigation",
                                onClick = {
                                    MapsCompanionState.onNavEnded()
                                    testStatusMessage = "Route cleared."
                                },
                                variant = GuideButtonVariant.Tonal,
                            )
                        }
                    }
                    Spacer(Modifier.height(GuideTokens.Space.sm))
                }

                GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.xs))

                // Interactive Testing Section
                Text(
                    text = "Desk-Testing & Location Transfer",
                    style = GuideTokens.Label,
                    color = GuideTokens.Text,
                )
                Text(
                    text = "Sitting at your desk? Pick or enter any destination below to instantly test route syncing without needing to move physically.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.sm))

                // Quick POI Destination chips
                val testDestinations = listOf(
                    "chinese-fishing-nets" to "Chinese Fishing Nets",
                    "st-francis-church" to "St. Francis Church",
                    "santa-cruz-basilica" to "Santa Cruz Basilica",
                    "mattancherry-palace" to "Mattancherry Palace",
                    "jew-town-synagogue" to "Jew Town Synagogue",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    testDestinations.forEach { (id, name) ->
                        CategoryChip(
                            text = name,
                            selected = selectedTestPoiId == id,
                            onClick = {
                                selectedTestPoiId = id
                                customDestInput = ""
                            },
                        )
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.sm))

                // Custom destination text field
                OutlinedTextField(
                    value = customDestInput,
                    onValueChange = { customDestInput = it },
                    placeholder = { Text("Or enter custom location (e.g. Fort Kochi Beach)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GuideTokens.Primary,
                        unfocusedBorderColor = GuideTokens.Border,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(GuideTokens.Space.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideButton(
                        text = "Simulate Route Sync",
                        onClick = {
                            val chosenName = customDestInput.ifBlank {
                                testDestinations.find { it.first == selectedTestPoiId }?.second
                                    ?: "Chinese Fishing Nets"
                            }
                            appState?.simulateCompanionSession(selectedTestPoiId, chosenName)
                            testStatusMessage = "Simulated navigation to $chosenName armed!"
                        },
                        variant = GuideButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = "Send Test Alert",
                        onClick = {
                            val chosenName = customDestInput.ifBlank {
                                testDestinations.find { it.first == selectedTestPoiId }?.second
                                    ?: "Chinese Fishing Nets"
                            }
                            sendTestNotification(context, chosenName)
                            testStatusMessage = "Test notification posted to Android status bar!"
                        },
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                }

                testStatusMessage?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = GuideTokens.Caption,
                        color = GuideTokens.Primary,
                    )
                }
            }
        }

        // =====================================================================
        // 3. Privacy, Security & Data Sovereignty Shield
        // =====================================================================
        item { SectionHeader(AppStrings.securitySectionHeader(currentLanguage)) }
        item {
            GuideCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = GuideIcons.Shield,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Privacy & Data Protection",
                            style = GuideTokens.Title,
                            color = GuideTokens.Text,
                        )
                        Text(
                            text = "Local-first architecture. Zero cloud analytics or telemetry.",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.base))

                // Privacy checklist items
                PrivacyPledgeRow("Zero Remote Servers", "No telemetry, advertising SDKs, or cloud analytics exist in this app.")
                PrivacyPledgeRow("Hardware Keystore Encryption", if (CryptoVault.isHardwareBacked()) "Protected by Android KeyStore hardware StrongBox/TEE with AES-256-GCM." else "Protected by Android KeyStore hardware-backed AES-256-GCM vault.")
                PrivacyPledgeRow("On-Device Voice Synthesis", "Stories are spoken directly by the phone's local Android speech engine.")
                PrivacyPledgeRow("Offline Vector Maps", "Maps and cartography are loaded directly from local storage.")
                PrivacyPledgeRow("RAM-Only Coordinates", "Live GPS fixes are used to calculate proximity in memory and are never uploaded.")

                Spacer(Modifier.height(GuideTokens.Space.sm))
                GuideButton(
                    text = "Read Official Privacy Policy Document",
                    onClick = { showPrivacyPolicyDialog = true },
                    variant = GuideButtonVariant.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // App Lock Toggle
                ToggleRow(
                    title = AppStrings.appLockTitle(currentLanguage),
                    subtitle = AppStrings.appLockDesc(currentLanguage),
                    checked = appLock,
                    onCheckedChange = {
                        appLock = it
                        prefs.edit().putBoolean("app_lock_enabled", it).apply()
                    },
                )

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // Data Sovereignty Controls
                Text("Your Data Controls", style = GuideTokens.Label)
                Spacer(Modifier.height(GuideTokens.Space.xs))
                Text(
                    "You own your travel journal and visit history. You can export everything as Markdown or permanently erase it from this phone.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideButton(
                        text = AppStrings.exportDataBtn(currentLanguage),
                        onClick = onExportData,
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = AppStrings.clearDataBtn(currentLanguage),
                        onClick = { showClearConfirmDialog = true },
                        variant = GuideButtonVariant.Quiet,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // =====================================================================
        // 4. Permissions Transparency & System Settings
        // =====================================================================
        item { SectionHeader(AppStrings.permissionsSectionHeader(currentLanguage)) }
        item {
            GuideCard {
                Text(
                    text = "Operating System Permissions",
                    style = GuideTokens.Title,
                    color = GuideTokens.Text,
                )
                Text(
                    text = "Review what Android grants to Travel Guide and why each permission is used.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )

                Spacer(Modifier.height(GuideTokens.Space.sm))

                PermissionAuditRow(
                    name = "Precise Location (GPS)",
                    reason = "Detects arrival at heritage stops and triggers stories.",
                    status = "ACTIVE",
                    isGranted = true,
                )
                PermissionAuditRow(
                    name = "Background Location",
                    reason = "Allows voice guidance to continue while phone is in your pocket.",
                    status = if (backgroundOptIn) "OPTED IN" else "OFF",
                    isGranted = backgroundOptIn,
                )
                PermissionAuditRow(
                    name = "Notification Listener",
                    reason = "Reads destination from Google Maps notifications for automatic sync.",
                    status = if (hasNotificationAccess) "GRANTED" else "NOT GRANTED",
                    isGranted = hasNotificationAccess,
                )
                PermissionAuditRow(
                    name = "Accessibility Service",
                    reason = "Reads active Google Maps destination directly from screen without notifications.",
                    status = if (hasAccessibilityAccess) "ACTIVE" else "NOT ENABLED",
                    isGranted = hasAccessibilityAccess,
                )
                PermissionAuditRow(
                    name = "Post Notifications",
                    reason = "Displays live trip card and audio playback controls in the status shade.",
                    status = if (systemNotifGranted) "GRANTED" else "BLOCKED",
                    isGranted = systemNotifGranted,
                )

                Spacer(Modifier.height(GuideTokens.Space.sm))
                GuideButton(
                    text = "Open Android App System Settings",
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    variant = GuideButtonVariant.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // =====================================================================
        // 5. Language & Spoken Voice (Sarvam Indic Neural Voice + App UI)
        // =====================================================================
        item { SectionHeader(AppStrings.languageSectionTitle(currentLanguage)) }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.languageSectionTitle(currentLanguage),
                            style = GuideTokens.Title,
                            color = GuideTokens.Text,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = AppStrings.languageSectionDesc(currentLanguage),
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                    StatusTag(
                        text = "SARVAM ${currentLanguage.code.uppercase()}",
                        color = GuideTokens.Primary,
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.base))

                // 4 Interactive Language Tiles (English, Malayalam, Hindi, Tamil)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = currentLanguage == lang
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) GuideTokens.PrimaryWash else GuideTokens.Surface2,
                            border = BorderStroke(
                                1.5.dp,
                                if (isSelected) GuideTokens.Primary else GuideTokens.Border,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appState?.setLanguage(lang)
                                },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    // Custom Radio Indicator (zero emojis)
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(
                                                2.dp,
                                                if (isSelected) GuideTokens.Primary else GuideTokens.Text2.copy(alpha = 0.5f),
                                                CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(GuideTokens.Primary),
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = lang.nativeName,
                                            style = GuideTokens.Label,
                                            color = if (isSelected) GuideTokens.Primary else GuideTokens.Text,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        )
                                        Text(
                                            text = "${lang.title} • Sarvam bulbul:v3 (${lang.sarvamCode})",
                                            style = GuideTokens.Caption,
                                            color = GuideTokens.Text2,
                                        )
                                    }
                                }
                                if (isSelected) {
                                    StatusTag(text = "ACTIVE", color = GuideTokens.Success)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // Voice Preview Button with sample audio text
                GuideButton(
                    text = "${AppStrings.testVoiceBtn(currentLanguage)} (${currentLanguage.nativeName})",
                    onClick = {
                        val sample = AppStrings.sampleVoiceText(currentLanguage)
                        GuideService.speak(context, sample)
                    },
                    variant = GuideButtonVariant.Primary,
                    icon = GuideIcons.Speak,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // =====================================================================
        // 6. Audio & Voice Settings
        // =====================================================================
        item { SectionHeader(AppStrings.audioVoiceSectionHeader(currentLanguage)) }
        item {
            GuideCard {
                ToggleRow(
                    title = AppStrings.quietHoursLabel(currentLanguage),
                    subtitle = AppStrings.quietHoursDesc(currentLanguage),
                    checked = quiet,
                    onCheckedChange = { quiet = it; onQuiet(it) },
                )
                GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.sm))
                ToggleRow(
                    title = AppStrings.autoplayStoriesLabel(currentLanguage),
                    subtitle = AppStrings.autoplayStoriesDesc(currentLanguage),
                    checked = auto,
                    onCheckedChange = { auto = it; onAutoPlay(it) },
                )

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // Speech speed slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppStrings.speechRateLabel(currentLanguage),
                        style = GuideTokens.Label,
                        modifier = Modifier.weight(1f),
                    )
                    StatusTag(
                        text = "${"%.1f".format(speechRate)}×",
                        color = GuideTokens.Primary,
                    )
                }
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        prefs.edit().putFloat("speech_rate", it).apply()
                        GuideService.setSpeechRate(context, it)
                    },
                    valueRange = 0.7f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = GuideTokens.Primary,
                        activeTrackColor = GuideTokens.Primary,
                        inactiveTrackColor = GuideTokens.Border,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when {
                            speechRate < 0.9f -> "Slower pace — great in noisy streets"
                            speechRate > 1.2f -> "Brisk pace — covers more ground"
                            else -> "Natural conversational pace"
                        },
                        style = GuideTokens.Caption,
                        color = GuideTokens.Text2,
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideButton(
                        text = "Sample English",
                        onClick = {
                            GuideService.setLanguage(context, AppLanguage.ENGLISH.code)
                            GuideService.speak(
                                context,
                                AppStrings.sampleVoiceText(AppLanguage.ENGLISH),
                            )
                        },
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = "Sample മലയാളം",
                        onClick = {
                            GuideService.setLanguage(context, AppLanguage.MALAYALAM.code)
                            GuideService.speak(
                                context,
                                AppStrings.sampleVoiceText(AppLanguage.MALAYALAM),
                            )
                        },
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideButton(
                        text = "Sample हिन्दी",
                        onClick = {
                            GuideService.setLanguage(context, AppLanguage.HINDI.code)
                            GuideService.speak(
                                context,
                                AppStrings.sampleVoiceText(AppLanguage.HINDI),
                            )
                        },
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = "Sample தமிழ்",
                        onClick = {
                            GuideService.setLanguage(context, AppLanguage.TAMIL.code)
                            GuideService.speak(
                                context,
                                AppStrings.sampleVoiceText(AppLanguage.TAMIL),
                            )
                        },
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // =====================================================================
        // 6. Spontaneous AI Tour Guide Intelligence (OpenAI GPT-4o-mini)
        // =====================================================================
        item { SectionHeader(AppStrings.aiGuideSectionHeader(currentLanguage)) }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFDF4FF),
                            border = BorderStroke(1.dp, Color(0xFFF0ABFC)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = GuideIcons.Compass,
                                    contentDescription = null,
                                    tint = Color(0xFFA855F7),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Spontaneous Oral Storytelling",
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Text(
                                text = "Powered by OpenAI GPT-4o-mini & On-Device Generative Engine",
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusTag(
                        text = if (openAiKeyInput.isNotBlank()) "GPT-4o-mini ACTIVE" else "SMART LOCAL AI",
                        color = if (openAiKeyInput.isNotBlank()) GuideTokens.Success else GuideTokens.Highlight,
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.base))

                Text(
                    text = "Storytelling Persona & Vibe",
                    style = GuideTokens.Label,
                    color = GuideTokens.Text,
                )
                Text(
                    text = "Controls how your AI companion perceives and narrates landmarks around you.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.sm))

                // Persona selection chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    guide.app.voice.SpontaneousGuideAiEngine.Persona.entries.forEach { p ->
                        CategoryChip(
                            text = p.title,
                            selected = selectedPersonaId == p.id,
                            onClick = {
                                selectedPersonaId = p.id
                                prefs.edit().putString("ai_tour_persona", p.id).apply()
                            },
                        )
                    }
                }

                val currentPersonaDesc = guide.app.voice.SpontaneousGuideAiEngine.Persona.fromId(selectedPersonaId).description
                Spacer(Modifier.height(GuideTokens.Space.xs))
                Text(
                    text = currentPersonaDesc,
                    style = GuideTokens.Caption,
                    color = GuideTokens.Primary,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // OpenAI API Key input
                Text(
                    text = "OpenAI API Key (GPT-4o-mini)",
                    style = GuideTokens.Label,
                    color = GuideTokens.Text,
                )
                Text(
                    text = "Enables real-time contextual cloud reasoning incorporating time-of-day, sun angle, and walking context. If left empty, the built-in Smart On-Device Generative Engine generates unscripted stories locally.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.sm))

                OutlinedTextField(
                    value = openAiKeyInput,
                    onValueChange = {
                        openAiKeyInput = it
                        keySaveFeedback = null
                    },
                    placeholder = { Text("sk-proj-...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GuideTokens.Primary,
                        unfocusedBorderColor = GuideTokens.Border,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(GuideTokens.Space.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideButton(
                        text = "Save API Key",
                        onClick = {
                            prefs.edit().putString("openai_api_key", openAiKeyInput.trim()).apply()
                            keySaveFeedback = if (openAiKeyInput.isNotBlank()) {
                                "OpenAI GPT-4o-mini key saved! Spontaneous live cloud stories active."
                            } else {
                                "Key cleared. Using Smart On-Device Generative Engine."
                            }
                        },
                        variant = GuideButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = if (isGeneratingStory) "Generating..." else "Test AI Story Now",
                        onClick = {
                            isGeneratingStory = true
                            coroutineScope.launch {
                                val dummyCard = appState?.card("chinese-fishing-nets") ?: guide.app.data.PackLoader.PoiCard(
                                    id = "chinese-fishing-nets",
                                    name = "Chinese Fishing Nets",
                                    summary = "Centuries-old cantilevered fishing nets along Fort Kochi beach.",
                                    history = "Introduced by Chinese explorer Zheng He in the 14th century.",
                                    funFacts = listOf("Operated by teams of 4 to 6 fishermen using counterweights."),
                                    seeList = listOf("Teakwood pivot beams", "Granite counterweight stones"),
                                    sources = listOf("ASI Survey"),
                                    lat = 9.9674, lng = 76.2429, radiusM = 65.0,
                                    hours = "06:00–18:00", layer = "heritage", packVersion = "live",
                                )
                                val persona = guide.app.voice.SpontaneousGuideAiEngine.Persona.fromId(selectedPersonaId)
                                val story = guide.app.voice.SpontaneousGuideAiEngine.generateStory(
                                    context = context,
                                    card = dummyCard,
                                    language = language,
                                )
                                aiSampleStory = story
                                isGeneratingStory = false
                                GuideService.speak(context, story)
                            }
                        },
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                        enabled = !isGeneratingStory,
                    )
                }

                keySaveFeedback?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(text = msg, style = GuideTokens.Caption, color = GuideTokens.Primary)
                }

                // AI Sample Story Result
                aiSampleStory?.let { sample ->
                    Spacer(Modifier.height(GuideTokens.Space.md))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GuideTokens.Surface2,
                        border = BorderStroke(1.dp, GuideTokens.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusTag(text = "LIVE SPONTANEOUS OUTPUT", color = GuideTokens.Highlight)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Sarvam Neural Voice",
                                        style = GuideTokens.Caption,
                                        color = GuideTokens.Text2,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = GuideTokens.PrimaryWash,
                                        modifier = Modifier.size(28.dp),
                                        onClick = {
                                            GuideService.speak(context, sample)
                                        },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = GuideIcons.Speak,
                                                contentDescription = "Replay",
                                                tint = GuideTokens.Primary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = GuideTokens.Surface,
                                        border = BorderStroke(1.dp, GuideTokens.Border),
                                        modifier = Modifier.size(28.dp),
                                        onClick = {
                                            GuideService.setMuted(context, true)
                                            GuideService.setMuted(context, false)
                                        },
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = GuideIcons.X,
                                                contentDescription = "Stop Voice",
                                                tint = GuideTokens.Text2,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "\"$sample\"",
                                style = GuideTokens.Body,
                                color = GuideTokens.Text,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // GPS Route Playback & Field Walk Simulation
        // =====================================================================
        item { SectionHeader(guide.app.data.AppStrings.gpsSimulatorHeader(language)) }
        item {
            val simState by guide.app.location.MockLocationSimulator.state.collectAsState()
            GuideCard {
                Text(
                    text = guide.app.data.AppStrings.gpsSimulatorDesc(language),
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.md))

                if (simState.isRunning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GuideTokens.Surface2,
                        border = BorderStroke(1.dp, GuideTokens.Border),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusTag(text = "WALK ACTIVE", color = GuideTokens.Highlight)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Stop ${simState.currentStepIndex}/${simState.totalSteps}",
                                    style = GuideTokens.Label,
                                    color = GuideTokens.Text,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = simState.progressText,
                                style = GuideTokens.Body,
                                color = GuideTokens.Text,
                            )
                        }
                    }
                    Spacer(Modifier.height(GuideTokens.Space.md))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GuideButton(
                        text = if (simState.isRunning) "Walking Loop..." else guide.app.data.AppStrings.startWalkBtn(language),
                        onClick = {
                            if (!simState.isRunning && appState != null) {
                                guide.app.location.MockLocationSimulator.startRouteWalk(
                                    context = context,
                                    appState = appState,
                                    stepDelayMs = 1200L,
                                    onPoiVisited = { stop ->
                                        guide.app.location.GuideService.speak(context, "${stop.name}. ${stop.summary}")
                                    },
                                )
                            }
                        },
                        variant = GuideButtonVariant.Primary,
                        modifier = Modifier.weight(1f),
                        enabled = !simState.isRunning && appState != null,
                    )

                    if (simState.isRunning) {
                        GuideButton(
                            text = guide.app.data.AppStrings.stopWalkBtn(language),
                            onClick = {
                                guide.app.location.MockLocationSimulator.stopSimulation(context)
                            },
                            variant = GuideButtonVariant.Tonal,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 7. Wearables & Bluetooth Earbuds Intelligence
        // =====================================================================
        item { SectionHeader("Wearables & Bluetooth Earbuds") }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = GuideIcons.Headphones,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Screen-Free Earbud Controls",
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Text(
                                text = "Hardware & touch button gestures mapped via Android MediaSession",
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusTag(
                        text = "BLUETOOTH AVRCP",
                        color = GuideTokens.Success,
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.base))

                Text(
                    text = "Earbud Physical & Touch Button Mapping",
                    style = GuideTokens.Label,
                    color = GuideTokens.Text,
                )
                Text(
                    text = "Controls mapped directly to AirPods, Galaxy Buds, Pixel Buds, Sony, Nothing, and smartwatches:",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.sm))

                // Gesture mapping list
                EarbudGestureRow(
                    gesture = "Single Tap / Click",
                    action = "Play / Pause",
                    description = "Pauses current narration or speaks the current landmark story.",
                )
                EarbudGestureRow(
                    gesture = "Double Tap / Click",
                    action = "What am I seeing?",
                    description = "Triggers on-demand spontaneous AI observation of the nearest stop without pulling your phone out.",
                )
                EarbudGestureRow(
                    gesture = "Triple Tap / Click",
                    action = "Replay Story",
                    description = "Repeats the last spoken landmark story from the beginning.",
                )
                EarbudGestureRow(
                    gesture = "Long Press",
                    action = "Mute / Silence",
                    description = "Instantly silences active audio playback in under 500ms.",
                )

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // Toggles
                ToggleRow(
                    title = "Gentle Arrival Audio Chime",
                    subtitle = "Plays a subtle 280ms dual-tone acoustic chime right before the AI voice begins speaking so you never miss the first words.",
                    checked = earbudsChime,
                    onCheckedChange = {
                        earbudsChime = it
                        prefs.edit().putBoolean("earbuds_chime_enabled", it).apply()
                    },
                )

                Spacer(Modifier.height(GuideTokens.Space.sm))

                ToggleRow(
                    title = "Auto-Pause on Earbud Removal",
                    subtitle = "Immediately silences speech if you take an earbud out or if Bluetooth disconnects, preventing loud audio in quiet public places.",
                    checked = earbudsAutoPause,
                    onCheckedChange = {
                        earbudsAutoPause = it
                        prefs.edit().putBoolean("earbuds_autopause_enabled", it).apply()
                    },
                )

                Spacer(Modifier.height(GuideTokens.Space.sm))

                GuideButton(
                    text = "Preview Acoustic Arrival Chime",
                    onClick = {
                        guide.app.voice.NavigationChime.play()
                    },
                    variant = GuideButtonVariant.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // =====================================================================
        // 8. Trip & Drawer Notifications (Real Working On/Off Controls)
        // =====================================================================
        item { SectionHeader("Trip & Drawer Notifications") }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (notificationsEnabled && systemNotifGranted) Color(0xFFEFF6FF) else GuideTokens.Surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (notificationsEnabled) GuideIcons.Bell else GuideIcons.BellOff,
                                contentDescription = null,
                                tint = if (notificationsEnabled && systemNotifGranted) Color(0xFF2563EB) else GuideTokens.Text2,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Trip & Status Notifications",
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Text(
                                text = "Status bar card, story updates & audio playback controls",
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    StatusTag(
                        text = when {
                            !notificationsEnabled -> "MUTED / OFF"
                            !systemNotifGranted -> "PERM NEEDED"
                            else -> "DRAWER ACTIVE"
                        },
                        color = when {
                            !notificationsEnabled -> GuideTokens.Text2
                            !systemNotifGranted -> GuideTokens.Highlight
                            else -> GuideTokens.Success
                        },
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.base))

                // Master Toggle Switch
                ToggleRow(
                    title = "Allow Trip Notifications",
                    subtitle = "Show live interactive travel guide in Android notification drawer with instant Mute & Explore controls.",
                    checked = notificationsEnabled,
                    onCheckedChange = { isChecked ->
                        onToggleNotifications(isChecked)
                        notifFeedback = if (isChecked) "Notifications turned ON. Live cards enabled." else "Notifications turned OFF. Drawer silenced."
                    },
                )

                if (notificationsEnabled && !systemNotifGranted) {
                    Spacer(Modifier.height(GuideTokens.Space.sm))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Android system notifications are blocked or permission is required.",
                                style = GuideTokens.Caption,
                                color = Color(0xFF9A3412),
                            )
                            Spacer(Modifier.height(8.dp))
                            GuideButton(
                                text = "Grant Notification Permission",
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                variant = GuideButtonVariant.Primary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.sm))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.sm))

                // Arrival Cues Toggle Switch
                ToggleRow(
                    title = "Monument Arrival Alerts",
                    subtitle = "Trigger heads-up notification and gentle vibration when arriving near historical landmarks.",
                    checked = arrivalAlertsEnabled,
                    onCheckedChange = {
                        arrivalAlertsEnabled = it
                        prefs.edit().putBoolean("arrival_alerts_enabled", it).apply()
                    },
                )

                Spacer(Modifier.height(GuideTokens.Space.base))

                // Interactive Testing Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GuideButton(
                        text = if (notificationsEnabled) "Send Test Notification" else "Notifications Turned Off",
                        onClick = {
                            if (notificationsEnabled) {
                                sendTestNotification(context, "Chinese Fishing Nets")
                                notifFeedback = "Test notification posted to Android status bar!"
                            } else {
                                notifFeedback = "Notifications are currently disabled. Turn ON toggle above to receive alerts."
                            }
                        },
                        variant = if (notificationsEnabled) GuideButtonVariant.Tonal else GuideButtonVariant.Quiet,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = "System Settings",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        variant = GuideButtonVariant.Quiet,
                    )
                }

                notifFeedback?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = msg,
                        style = GuideTokens.Caption,
                        color = if (notificationsEnabled) GuideTokens.Primary else GuideTokens.Text2,
                    )
                }
            }
        }

        // =====================================================================
        // 7. Battery & Tracking Optimization
        // =====================================================================
        item { SectionHeader(AppStrings.batterySectionHeader(currentLanguage)) }
        item {
            GuideCard {
                Text(
                    "GPS Hardware Profile",
                    style = GuideTokens.Label,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.sm))
                BatteryProfile.entries.forEach { p ->
                    BatteryOption(
                        profile = p,
                        selected = p == profile,
                        onSelect = { onProfile(p) },
                        language = currentLanguage,
                    )
                }
            }
        }

        // Background location disclosure
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(AppStrings.screenOffTrackingTitle(currentLanguage), style = GuideTokens.Label)
                        Text(
                            text = AppStrings.screenOffTrackingDesc(currentLanguage, backgroundOptIn),
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                    StatusTag(
                        text = if (backgroundOptIn) "ACTIVE" else "OFF",
                        color = if (backgroundOptIn) GuideTokens.Primary else GuideTokens.Text2,
                    )
                }
                if (!backgroundOptIn) {
                    Spacer(Modifier.height(GuideTokens.Space.base))
                    GuideButton(
                        text = AppStrings.screenOffTrackingBtn(currentLanguage),
                        onClick = onBackgroundOptIn,
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Text(
                "Travel Guide • Fort Kochi Heritage Edition • v1.0.0",
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )
        }
    }

    // Edit Name Dialog
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(explorerName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Edit Explorer Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    explorerName = tempName.ifBlank { "Aswin" }
                    prefs.edit().putString("user_name", explorerName).apply()
                    showNameDialog = false
                }) {
                    Text("Save", color = GuideTokens.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Wipe Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Erase All Travel History?") },
            text = {
                Text("This permanently deletes all your visited places, timestamps, and personal notes stored on this phone. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onClearData()
                    appState?.clearAllData()
                    showClearConfirmDialog = false
                }) {
                    Text("Erase Everything", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Official In-App Privacy Policy Documentation Viewer
    if (showPrivacyPolicyDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            PrivacyPolicyViewer(onDismiss = { showPrivacyPolicyDialog = false })
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers & Subcomponents
// ---------------------------------------------------------------------------

@Composable
private fun ProfileStatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = GuideTokens.Label,
            fontWeight = FontWeight.Bold,
            color = GuideTokens.Text,
        )
        Text(
            text = label,
            style = GuideTokens.Caption,
            color = GuideTokens.Text2,
        )
    }
}

@Composable
private fun PrivacyPledgeRow(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = GuideIcons.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GuideTokens.Label,
                color = GuideTokens.Text,
            )
            Text(
                text = description,
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )
        }
    }
}

@Composable
private fun PermissionAuditRow(
    name: String,
    reason: String,
    status: String,
    isGranted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = GuideTokens.Label, color = GuideTokens.Text)
            Text(text = reason, style = GuideTokens.Caption, color = GuideTokens.Text2)
        }
        Spacer(Modifier.width(8.dp))
        StatusTag(
            text = status,
            color = if (isGranted) GuideTokens.Success else GuideTokens.Text2,
        )
    }
}

@Composable
private fun BatteryOption(
    profile: BatteryProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    language: AppLanguage = AppLanguage.ENGLISH,
) {
    val cost = when (profile) {
        BatteryProfile.SAVER -> AppStrings.batterySaverDesc(language)
        BatteryProfile.BALANCED -> AppStrings.batteryBalancedDesc(language)
        BatteryProfile.PRECISE -> AppStrings.batteryPreciseDesc(language)
    }
    val label = when (profile) {
        BatteryProfile.SAVER -> AppStrings.batterySaverLabel(language)
        BatteryProfile.BALANCED -> AppStrings.batteryBalancedLabel(language)
        BatteryProfile.PRECISE -> AppStrings.batteryPreciseLabel(language)
    }
    Pressable(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        role = Role.RadioButton,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = GuideTokens.Space.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            width = 2.dp,
                            color = if (selected) GuideTokens.Primary else GuideTokens.Border,
                            shape = CircleShape,
                        ),
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = GuideTokens.Primary,
                                shape = CircleShape,
                            ),
                    )
                }
            }
            Spacer(Modifier.width(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                val activeSuffix = if (selected) {
                    if (language == AppLanguage.MALAYALAM) " — സജീവം" else " — Active"
                } else ""
                Text(
                    text = label + activeSuffix,
                    style = GuideTokens.Label,
                    color = if (selected) GuideTokens.Primary else GuideTokens.Text,
                )
                Text(
                    text = cost,
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = GuideTokens.Label,
                color = GuideTokens.Text,
            )
            Text(
                text = subtitle,
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )
        }
        Spacer(Modifier.width(GuideTokens.Space.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = GuideTokens.Primary,
                uncheckedThumbColor = GuideTokens.Surface1,
                uncheckedTrackColor = GuideTokens.Border,
            ),
        )
    }
}

/**
 * Posts a real test navigation notification from Travel Guide to Android's status shade
 * so the user can test the notification listener directly from their desk!
 */
private fun sendTestNotification(context: Context, destination: String) {
    val prefs = context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE)
    if (!prefs.getBoolean("notifications_enabled", true)) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(9901)
        return
    }

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "maps_companion_test_channel"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Companion Sync Test",
            NotificationManager.IMPORTANCE_HIGH,
        )
        nm.createNotificationChannel(channel)
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("route", "settings")
    }
    val openPendingIntent = PendingIntent.getActivity(
        context, 9901, openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val heroBitmap = runCatching {
        android.graphics.BitmapFactory.decodeResource(context.resources, guide.app.R.drawable.kochi_hero)
    }.getOrNull()

    val messageText = "En route to $destination • 24 heritage stops along your path. Audio stories ready to play as you arrive."

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(guide.app.R.drawable.ic_stat_notification)
        .setColor(0xFFFF5A36.toInt()) // Brand Sunset Coral
        .setContentTitle("Navigating to $destination")
        .setContentText(messageText)
        .setSubText("Travel Guide • Live Sync Active")
        .setContentIntent(openPendingIntent)
        .addAction(android.R.drawable.ic_menu_compass, "Explore Place", openPendingIntent)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    if (heroBitmap != null) {
        builder.setLargeIcon(heroBitmap)
        builder.setStyle(
            NotificationCompat.BigPictureStyle()
                .bigPicture(heroBitmap)
                .bigLargeIcon(null as android.graphics.Bitmap?)
                .setBigContentTitle("Navigating to $destination")
                .setSummaryText(messageText),
        )
    } else {
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .setBigContentTitle("Navigating to $destination")
                .setSummaryText("Route Sync Active")
                .bigText(messageText),
        )
    }

    nm.notify(9901, builder.build())
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) GuideTokens.PrimaryWash else GuideTokens.Surface2,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) GuideTokens.Primary else GuideTokens.Border,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) GuideTokens.Primary else GuideTokens.Surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else GuideTokens.Text,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = GuideTokens.Label,
                color = if (isSelected) GuideTokens.Primary else GuideTokens.Text,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun EarbudGestureRow(
    gesture: String,
    action: String,
    description: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = GuideTokens.Surface2,
            border = BorderStroke(1.dp, GuideTokens.Border),
            modifier = Modifier.width(135.dp),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = gesture,
                    style = GuideTokens.Caption,
                    color = GuideTokens.Primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action,
                style = GuideTokens.Label,
                color = GuideTokens.Text,
            )
            Text(
                text = description,
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )
        }
    }
}


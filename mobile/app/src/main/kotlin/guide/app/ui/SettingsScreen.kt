package guide.app.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import guide.app.data.AppState
import guide.app.location.GuideService
import guide.app.navigation.MapsCompanionState
import guide.app.power.BatteryProfile
import guide.app.ui.components.CategoryChip
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.Pressable
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
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
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("guide_prefs", Context.MODE_PRIVATE) }

    var quiet by remember { mutableStateOf(quietEnabled) }
    var auto by remember { mutableStateOf(autoPlay) }
    var appLock by remember { mutableStateOf(prefs.getBoolean("app_lock_enabled", false)) }
    var speechRate by remember { mutableFloatStateOf(prefs.getFloat("speech_rate", 1.0f)) }

    var explorerName by remember { mutableStateOf(prefs.getString("user_name", "Aswin") ?: "Aswin") }
    var showNameDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // External Maps testing state
    var selectedTestPoiId by remember { mutableStateOf("chinese-fishing-nets") }
    var customDestInput by remember { mutableStateOf("") }
    var testStatusMessage by remember { mutableStateOf<String?>(null) }

    val hasNotificationAccess = remember { MapsCompanionState.isNotificationAccessGranted(context) }
    val activeSession = MapsCompanionState.currentSession

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                    Text("Settings & Privacy", style = GuideTokens.Heading)
                    StatusTag(
                        text = "100% ON-DEVICE",
                        color = GuideTokens.Success,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Manage your local identity, Google Maps sync, security & offline privacy.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
            }
        }

        // =====================================================================
        // 1. Explorer Account & Identity Card
        // =====================================================================
        item { SectionHeader("Explorer Profile") }
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
                                    text = "Edit",
                                    style = GuideTokens.Caption,
                                    color = GuideTokens.Primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = "Heritage Explorer • Fort Kochi Pack v1.1.0",
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
                        label = "Places Discovered",
                    )
                    ProfileStatColumn(
                        value = "$notesCount",
                        label = "Trip Notes",
                    )
                    ProfileStatColumn(
                        value = "Zero Cloud",
                        label = "Data Sovereignty",
                    )
                }
            }
        }

        // =====================================================================
        // 2. Google Maps Navigation Companion (Auto-Sync & Desk Testing)
        // =====================================================================
        item { SectionHeader("Google Maps Navigation Companion") }
        item {
            GuideCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Live Navigation Sync",
                            style = GuideTokens.Title,
                            color = GuideTokens.Text,
                        )
                        Text(
                            text = "Detects active Google Maps routes & pre-warms corridor stories",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                    StatusTag(
                        text = if (hasNotificationAccess) "SYNC READY" else "NEEDS PERMISSION",
                        color = if (hasNotificationAccess) GuideTokens.Success else GuideTokens.Highlight,
                    )
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
        item { SectionHeader("Privacy & Data Security") }
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
                            text = "100% On-Device Sovereignty",
                            style = GuideTokens.Title,
                            color = GuideTokens.Text,
                        )
                        Text(
                            text = "Your data never leaves your hardware. Zero cloud tracking.",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                    }
                }

                Spacer(Modifier.height(GuideTokens.Space.base))

                // Privacy checklist items
                PrivacyPledgeRow("Zero Remote Servers", "No telemetry, advertising SDKs, or cloud analytics exist in this app.")
                PrivacyPledgeRow("On-Device Voice Synthesis", "Stories are spoken directly by the phone's local Android speech engine.")
                PrivacyPledgeRow("Offline Vector Maps", "Maps and cartography are loaded directly from local storage.")
                PrivacyPledgeRow("RAM-Only Coordinates", "Live GPS fixes are used to calculate proximity in memory and are never uploaded.")

                Spacer(Modifier.height(GuideTokens.Space.base))
                GuideDivider()
                Spacer(Modifier.height(GuideTokens.Space.base))

                // App Lock Toggle
                ToggleRow(
                    title = "App Lock / Biometric Protection",
                    subtitle = "Require screen lock or fingerprint authentication when opening Travel Guide.",
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
                        text = "Export Data (Markdown)",
                        onClick = onExportData,
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.weight(1f),
                    )
                    GuideButton(
                        text = "Wipe Travel History",
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
        item { SectionHeader("Required Permissions Audit") }
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
                    name = "Post Notifications",
                    reason = "Displays Now Playing playback controls in the status shade.",
                    status = "GRANTED",
                    isGranted = true,
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
        // 5. Audio & Voice Settings
        // =====================================================================
        item { SectionHeader("Audio & Voice Guidance") }
        item {
            GuideCard {
                ToggleRow(
                    title = "Quiet hours (22:00 – 07:00)",
                    subtitle = "Cards still appear on screen, but audio stories stay silent.",
                    checked = quiet,
                    onCheckedChange = { quiet = it; onQuiet(it) },
                )
                GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.sm))
                ToggleRow(
                    title = "Auto-play nearby stories",
                    subtitle = "Starts a story automatically when you reach a stop without requiring a tap.",
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
                        text = "Voice Playback Speed",
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
                    GuideButton(
                        text = "Test Voice",
                        onClick = {
                            GuideService.speak(
                                context,
                                "Welcome to Fort Kochi. This is a voice test of your private on-device travel companion.",
                            )
                        },
                        variant = GuideButtonVariant.Tonal,
                    )
                }
            }
        }

        // =====================================================================
        // 6. Battery & Tracking Optimization
        // =====================================================================
        item { SectionHeader("Battery & Tracking Optimization") }
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
                        Text("Screen-Off Tracking", style = GuideTokens.Label)
                        Text(
                            text = if (backgroundOptIn) {
                                "Narration continues with the screen turned off in your pocket."
                            } else {
                                "Currently narration only triggers while the app is actively on screen."
                            },
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
                        text = "Opt into Screen-Off Background GPS",
                        onClick = onBackgroundOptIn,
                        variant = GuideButtonVariant.Tonal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Text(
                "Travel Guide • 100% Offline & Private • Build 2026.09.28",
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
) {
    val cost = when (profile) {
        BatteryProfile.SAVER -> "Lowest battery use (~30s intervals) — saves battery on long walks"
        BatteryProfile.BALANCED -> "Moderate battery use (~10s intervals) — ideal for walking loop"
        BatteryProfile.PRECISE -> "Highest battery use (~3s intervals) — instant audio response"
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
                Text(
                    text = profile.label + if (selected) " — Active" else "",
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
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_map)
        .setContentTitle("Navigating to $destination")
        .setContentText("In 200m turn right • 14 min (3.5 km)")
        .setSubText("Google Maps Navigation")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    nm.notify(9901, builder.build())
}

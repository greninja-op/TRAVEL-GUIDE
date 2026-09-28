package guide.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import guide.app.companion.TravelGuideAccessibilityService
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.StatusTag
import guide.app.ui.theme.GuideTokens

/**
 * First-launch and permission recovery screen.
 *
 * Provides:
 * 1. Native Android runtime permission trigger for Location.
 * 2. Direct Settings fallback if native pop-up was permanently dismissed or cleared.
 * 3. Direct Accessibility Settings button for Google Maps Companion setup.
 * 4. Automatic return-to-map transition when permissions are satisfied.
 */
@Composable
fun PermissionGateScreen(
    hasLocationPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestLocation: () -> Unit,
    onContinueToMap: () -> Unit,
    onViewPrivacyPolicy: () -> Unit,
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = GuideTokens.Bg,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = GuideTokens.Space.screenPad,
                end = GuideTokens.Space.screenPad,
                top = GuideTokens.Space.xl,
                bottom = GuideTokens.Space.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
        ) {
            // ---- Header ----
            item {
                Text(
                    text = "Welcome to Travel Guide",
                    style = GuideTokens.Display,
                    color = GuideTokens.Text,
                )
                Spacer(Modifier.height(GuideTokens.Space.xs))
                Text(
                    text = "Set up permissions to enable live navigation sync, route geometry, and localized audio tours.",
                    style = GuideTokens.Body,
                    color = GuideTokens.Text2,
                )
            }

            item { Spacer(Modifier.height(GuideTokens.Space.xs)) }

            // ---- Card 1: Location Access ----
            item {
                GuideCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasLocationPermission) Color(0xFFECFDF5) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, if (hasLocationPermission) Color(0xFFA7F3D0) else Color(0xFFBFDBFE)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = GuideIcons.Compass,
                                    contentDescription = null,
                                    tint = if (hasLocationPermission) Color(0xFF059669) else GuideTokens.Primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "1. Location Permission",
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Text(
                                text = if (hasLocationPermission) "Granted — Real-time GPS active" else "Required for nearby spots and map",
                                style = GuideTokens.Caption,
                                color = if (hasLocationPermission) Color(0xFF059669) else GuideTokens.Text2,
                            )
                        }

                        StatusTag(
                            text = if (hasLocationPermission) "Active" else "Needed",
                            color = if (hasLocationPermission) Color(0xFF059669) else Color(0xFFD97706),
                        )
                    }

                    if (!hasLocationPermission) {
                        Spacer(Modifier.height(GuideTokens.Space.base))
                        Text(
                            text = "Used locally to show your position on the map and calculate distance to historic sites. Never uploaded to external servers.",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Text2,
                        )
                        Spacer(Modifier.height(GuideTokens.Space.base))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GuideButton(
                                text = "Grant Permission",
                                onClick = onRequestLocation,
                                modifier = Modifier.weight(1.2f),
                            )
                            GuideButton(
                                text = "App Settings",
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                },
                                variant = GuideButtonVariant.Tonal,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ---- Card 2: Google Maps Companion (Accessibility) ----
            item {
                GuideCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (hasAccessibilityPermission) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (hasAccessibilityPermission) Color(0xFFA7F3D0) else Color(0xFFFDE68A)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = GuideIcons.Shield,
                                    contentDescription = null,
                                    tint = if (hasAccessibilityPermission) Color(0xFF059669) else Color(0xFFD97706),
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. Google Maps Companion",
                                style = GuideTokens.Title,
                                color = GuideTokens.Text,
                            )
                            Text(
                                text = if (hasAccessibilityPermission) "Enabled — Live route sync active" else "Optional — Detects destination from Maps",
                                style = GuideTokens.Caption,
                                color = if (hasAccessibilityPermission) Color(0xFF059669) else GuideTokens.Text2,
                            )
                        }

                        StatusTag(
                            text = if (hasAccessibilityPermission) "Active" else "Optional",
                            color = if (hasAccessibilityPermission) Color(0xFF059669) else Color(0xFFD97706),
                        )
                    }

                    Spacer(Modifier.height(GuideTokens.Space.base))
                    Text(
                        text = "Allows Travel Guide to automatically mirror your navigation route and highlight corridor spots when you start directions in Google Maps.",
                        style = GuideTokens.Caption,
                        color = GuideTokens.Text2,
                    )

                    if (!hasAccessibilityPermission) {
                        Spacer(Modifier.height(GuideTokens.Space.base))
                        GuideButton(
                            text = "Open Accessibility Settings",
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            variant = GuideButtonVariant.Tonal,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(GuideTokens.Space.sm)) }

            // ---- Continue Button ----
            item {
                GuideButton(
                    text = if (hasLocationPermission) "Continue to Map" else "Continue Anyway (Offline Map)",
                    onClick = onContinueToMap,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---- Privacy Policy link ----
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextButton(onClick = onViewPrivacyPolicy) {
                        Text(
                            text = "Read Official Privacy Policy Document",
                            style = GuideTokens.Caption,
                            color = GuideTokens.Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

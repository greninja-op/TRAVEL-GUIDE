package guide.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.power.BatteryProfile
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.Pressable
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.theme.GuideTokens

/**
 * Settings — the consent and honesty surface.
 *
 * Three product non-negotiables live here and are load-bearing, not decoration:
 *  - **Battery honesty:** the GPS cost of each profile is disclosed BEFORE the
 *    user picks it, not after (SPEC §1).
 *  - **Background location is a separate opt-in** (SPEC §1.1) — never bundled
 *    with the first-launch consent, and refused until plainly explained.
 *  - **The user controls the voice:** quiet hours and auto-play are explicit
 *    switches with the real consequence written next to them.
 *
 * Rebuilt 2026-09-26: it was a non-scrolling `Column` with hardcoded `16.dp`
 * padding and bare `TextButton`s — it could overflow, and the battery cost was
 * a parenthetical rather than a disclosed figure.
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
    onSimulateMapsRoute: () -> Unit = {},
) {
    var quiet by remember { mutableStateOf(quietEnabled) }
    var auto by remember { mutableStateOf(autoPlay) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = GuideTokens.Space.screenPad,
            end = GuideTokens.Space.screenPad,
            top = GuideTokens.Space.base,
            // Clears the nav bar so the last switch is never half-hidden.
            bottom = GuideTokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
    ) {
        item {
            Text("Settings", style = GuideTokens.Heading)
        }

        // ---- Battery profile -------------------------------------------------
        item { SectionHeader("Battery & tracking") }
        item {
            GuideCard {
                Text(
                    "How hard should the guide work?",
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

        // ---- Background location (separate opt-in, SPEC §1.1) ----------------
        item { SectionHeader("Background location") }
        item {
            GuideCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusTag(
                        text = if (backgroundOptIn) "On" else "Off",
                        color = if (backgroundOptIn) GuideTokens.Primary else GuideTokens.Text2,
                    )
                }
                Spacer(Modifier.height(GuideTokens.Space.sm))
                Text(
                    text = if (backgroundOptIn) {
                        "Narration keeps going with the screen off. " +
                            "This is the setting that costs the most battery."
                    } else {
                        "Right now the guide only runs while the app is open. " +
                            "Turn this on to keep narrating with the screen off — " +
                            "it uses noticeably more battery, so it stays off until you say so."
                    },
                    style = GuideTokens.Body,
                )
                if (!backgroundOptIn) {
                    Spacer(Modifier.height(GuideTokens.Space.base))
                    GuideButton(
                        text = "Enable background location",
                        onClick = onBackgroundOptIn,
                        variant = GuideButtonVariant.Quiet,
                    )
                }
            }
        }

        // ---- Voice ------------------------------------------------------------
        item { SectionHeader("The voice") }
        item {
            GuideCard {
                ToggleRow(
                    title = "Quiet hours",
                    subtitle = "22:00–07:00 — cards still appear, the voice stays off.",
                    checked = quiet,
                    onCheckedChange = { quiet = it; onQuiet(it) },
                )
                GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.sm))
                ToggleRow(
                    title = "Auto-play nearby stories",
                    subtitle = "Starts a story when you reach a stop, without asking.",
                    checked = auto,
                    onCheckedChange = { auto = it; onAutoPlay(it) },
                )
            }
        }

        // ---- External Maps Companion ----------------------------------------
        item { SectionHeader("External Maps Companion") }
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val hasAccess = remember { guide.app.navigation.MapsCompanionState.isNotificationAccessGranted(context) }
            val activeSession = guide.app.navigation.MapsCompanionState.currentSession

            GuideCard {
                Text(
                    text = "Google Maps Auto-Sync",
                    style = GuideTokens.Title,
                    color = GuideTokens.Text,
                )
                Spacer(Modifier.height(GuideTokens.Space.xs))
                Text(
                    text = "When you start turn-by-turn navigation in Google Maps, Travel Guide automatically extracts your destination, calculates the route corridor, and pre-arms audio stories for historical spots along your path.",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text2,
                )
                Spacer(Modifier.height(GuideTokens.Space.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusTag(
                        text = if (hasAccess) "SYNC READY" else "ACCESS NEEDED",
                        color = if (hasAccess) GuideTokens.Success else GuideTokens.Highlight,
                    )
                    GuideButton(
                        text = if (hasAccess) "Check Settings" else "Grant Access",
                        onClick = {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        variant = if (hasAccess) GuideButtonVariant.Tonal else GuideButtonVariant.Primary,
                    )
                }

                if (activeSession != null) {
                    Spacer(Modifier.height(GuideTokens.Space.sm))
                    GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.xs))
                    Text(
                        text = "Active: Navigating to ${activeSession.destinationName}",
                        style = GuideTokens.Label,
                        color = GuideTokens.Primary,
                    )
                    Text(
                        text = activeSession.etaOrDistance ?: "Corridor active",
                        style = GuideTokens.Caption,
                        color = GuideTokens.Text2,
                    )
                }

                Spacer(Modifier.height(GuideTokens.Space.sm))
                GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.xs))

                GuideButton(
                    text = "Simulate Google Maps Route (Test)",
                    onClick = onSimulateMapsRoute,
                    variant = GuideButtonVariant.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Text(
                "Location, visits and notes stay on this device.",
                style = GuideTokens.Caption,
                color = GuideTokens.Text2,
            )
        }
    }
}

/**
 * One battery profile. Selection is shown by a dot + the word "Selected" as
 * well as the ring, so it never depends on colour alone (SPEC §5) — and the
 * cost is stated as a word before you tap, which is the whole point of the
 * battery-honesty rule.
 */
@Composable
private fun BatteryOption(
    profile: BatteryProfile,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val cost = when (profile) {
        BatteryProfile.SAVER -> "Lowest battery use — updates less often"
        BatteryProfile.BALANCED -> "Moderate battery use — good for a walking loop"
        BatteryProfile.PRECISE -> "Highest battery use — most responsive narration"
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
            // Radio ring: outer ring + filled centre when chosen.
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
                            shape = RoundedCornerShape(GuideTokens.PinRadius),
                        ),
                )
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = GuideTokens.Primary,
                                shape = RoundedCornerShape(GuideTokens.PinRadius),
                            ),
                    )
                }
            }
            Spacer(Modifier.width(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.label + if (selected) " — Selected" else "",
                    style = GuideTokens.Label,
                    color = if (selected) GuideTokens.Primary else GuideTokens.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = cost,
                    style = GuideTokens.Caption,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Switch row: title + the real consequence, with the whole row tappable. */
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = GuideTokens.Caption,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(GuideTokens.Space.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GuideTokens.OnPrimary,
                checkedTrackColor = GuideTokens.Primary,
                uncheckedThumbColor = GuideTokens.Surface1,
                uncheckedTrackColor = GuideTokens.Border,
            ),
        )
    }
}

package guide.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.components.EmptyState
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideRow
import guide.app.ui.components.Pressable
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.components.GuideIcons
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines
import guide.app.ui.theme.Motion

/** A walkable route as the pack describes it. [detail] is the stops/duration line. */
data class Trio(val name: String, val detail: String)

/** One day-plan option. [minutes] is what the caller passes to `onPickPlan`. */
private data class DayPlan(val minutes: Int, val label: String, val detail: String)

private val DAY_PLANS = listOf(
    DayPlan(minutes = 120, label = "2 hours", detail = "The waterfront essentials, on foot"),
    DayPlan(minutes = 240, label = "Half day", detail = "Adds the Mattancherry side"),
    DayPlan(minutes = 480, label = "Full day", detail = "Both routes, with time to sit down"),
)

/**
 * Routes + day plans.
 *
 * Rebuilt 2026-09-26 (audit §3.4/§4): was a non-scrolling `Column` of bullet
 * `Text` and `TextButton`s with no selection state and no scroll. Now a
 * `LazyColumn`; route cards carry name + stops/duration; the day-plan picker is
 * a real single-select with an animated selection; and "Start this plan" is the
 * one primary action on the view.
 */
@Composable
fun RoutesScreen(
    routes: List<Trio>,
    onPickPlan: (minutes: Int) -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf(DAY_PLANS[0].minutes) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = GuideTokens.Space.screenPad,
            end = GuideTokens.Space.screenPad,
            top = GuideTokens.Space.lg,
            bottom = GuideTokens.Space.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
    ) {
        item(key = "header") {
            Column {
                Text("Routes & day plans", style = GuideTokens.Heading, maxLines = Lines.Single)
                Text(
                    text = "Both walks start and end at Vasco Square, so you can leave whenever " +
                        "you like and pick the rest up tomorrow.",
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Supporting,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.xs),
                )
            }
        }

        if (routes.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "No routes in this pack",
                    body = "The pack on this device shipped without route data. Re-installing " +
                        "the Fort Kochi pack restores the Heritage Loop and the Full Day walk.",
                    icon = GuideIcons.Route,
                )
            }
            return@LazyColumn
        }

        item(key = "routes-header") { SectionHeader("Walks in this pack") }
        items(routes, key = { it.name }) { route ->
            GuideCard {
                GuideRow(
                    title = route.name,
                    supporting = route.detail,
                    leading = {
                        Box(
                            modifier = Modifier.size(GuideTokens.TouchTarget),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = GuideIcons.Route,
                                contentDescription = null,
                                tint = GuideTokens.Primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    trailing = { StatusTag(text = "Auto-guided", color = GuideTokens.Text2) },
                )
            }
        }

        // ---- Day plan: pick a length ---------------------------------------
        item(key = "plans-header") { SectionHeader("Plan my day") }
        items(DAY_PLANS, key = { "plan-${it.minutes}" }) { plan ->
            DayPlanOption(
                plan = plan,
                selected = picked == plan.minutes,
                onSelect = { picked = plan.minutes },
            )
        }

        item(key = "primary") {
            Column(modifier = Modifier.padding(top = GuideTokens.Space.lg)) {
                GuideDivider(modifier = Modifier.padding(bottom = GuideTokens.Space.base))
                GuideButton(
                    text = "Start ${planLabel(picked)} plan",
                    onClick = { onPickPlan(picked) },
                    icon = GuideIcons.Play,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "The guide picks the next stop for you and skips anything closed. " +
                        "Change your mind any time — nothing is locked in.",
                    style = GuideTokens.Caption,
                    maxLines = Lines.Supporting,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

private fun planLabel(minutes: Int): String =
    DAY_PLANS.firstOrNull { it.minutes == minutes }?.label ?: "custom"

/**
 * Single-select plan option. Selection is a filled radio ring PLUS the
 * surface step PLUS the word "Selected" — three signals, so it never depends
 * on colour (SPEC §5). Selection swaps with a short tone/scale tween.
 */
@Composable
private fun DayPlanOption(plan: DayPlan, selected: Boolean, onSelect: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) GuideTokens.PrimaryWash else GuideTokens.Surface1,
        animationSpec = tween(Motion.Fast, easing = Motion.easeOut),
        label = "planBg",
    )
    val border by animateColorAsState(
        targetValue = if (selected) GuideTokens.Primary else GuideTokens.Border,
        animationSpec = tween(Motion.Fast, easing = Motion.easeOut),
        label = "planBorder",
    )
    Pressable(
        onClick = onSelect,
        role = Role.RadioButton,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GuideTokens.TouchTargetPrimary)
            .background(color = bg, shape = RoundedCornerShape(GuideTokens.CardRadius))
            .padding(GuideTokens.Space.cardPad),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (selected) GuideIcons.RadioOn else GuideIcons.RadioOff,
                contentDescription = null,
                tint = if (selected) GuideTokens.Primary else border,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.label, style = GuideTokens.Title, maxLines = Lines.Single)
                Text(
                    text = plan.detail,
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Supporting,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Spacer(Modifier.size(GuideTokens.Space.sm))
                Text(
                    text = "Selected",
                    style = GuideTokens.Caption,
                    color = GuideTokens.Primary,
                    maxLines = Lines.Single,
                )
            }
        }
    }
}

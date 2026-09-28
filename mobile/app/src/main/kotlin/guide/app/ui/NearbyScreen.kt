package guide.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.extras.LocalEvent
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

data class NearbyRow(val id: String, val name: String, val detail: String, val layer: String)

/**
 * Nearby radar: distance-sorted rows (VisitRank order from caller), layer
 * filter chips (All/heritage/food/stay), today's events, and the
 * "What am I seeing?" entry point.
 *
 * Rebuilt 2026-09-26 (audit §3.4/§4): was a non-scrolling `Column` of
 * `TextButton`s — 24 POIs rendered unbounded with no states. Now a
 * `LazyColumn` of `GuideRow`s so list length can never overflow, every string
 * carries a line cap, and the empty/answer states are written rather than
 * implied.
 */
@Composable
fun NearbyScreen(
    rows: List<NearbyRow>,
    events: List<LocalEvent>,
    seeingAnswer: String?,
    onSeeingTap: () -> Unit,
    onRowTap: (String) -> Unit,
) {
    var layer by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = remember(rows, layer) { if (layer == null) rows else rows.filter { it.layer == layer } }
    val filters = remember { FILTERS }

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
        // ---- Header: what this screen is, then the one action -------------
        item(key = "header") {
            Column {
                Text("Nearby", style = GuideTokens.Heading, maxLines = Lines.Single)
                Text(
                    text = "Sorted by how close you are. The guide narrates the top one as you walk.",
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Supporting,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.xs),
                )
            }
        }

        item(key = "seeing") {
            SeeingEntry(answer = seeingAnswer, onClick = onSeeingTap)
        }

        // ---- Layer filter: pills, scrolling not wrapping -------------------
        item(key = "filters") {
            Column {
                SectionHeader("Filter by layer")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
                ) {
                    filters.forEach { option ->
                        FilterPill(
                            label = option.label,
                            selected = layer == option.value,
                            onClick = { layer = option.value },
                        )
                    }
                }
            }
        }

        // ---- Today's events ------------------------------------------------
        if (events.isNotEmpty()) {
            item(key = "events-header") {
                SectionHeader("Happening today")
            }
            items(events, key = { "event-${it.id}" }) { event ->
                GuideCard {
                    GuideRow(
                        title = event.title,
                        supporting = "Follows the ${event.poiId.replace('-', ' ')} stop on the loop",
                        leading = {
                            Box(
                                modifier = Modifier.size(GuideTokens.TouchTarget),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = GuideIcons.Calendar,
                                    contentDescription = null,
                                    tint = GuideTokens.Highlight,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        },
                    )
                }
            }
        }

        // ---- Result count (system status, not decoration) ------------------
        if (shown.isNotEmpty()) {
            item(key = "count") {
                Text(
                    text = countLabel(shown.size),
                    style = GuideTokens.Caption,
                    maxLines = Lines.Single,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                )
            }
        }

        // ---- The list -------------------------------------------------------
        items(shown, key = { it.id }) { row ->
            NearbyPoiCard(row = row, onClick = { onRowTap(row.id) })
        }

        if (shown.isEmpty()) {
            item(key = "empty") {
                if (rows.isEmpty()) {
                    EmptyState(
                        title = "No stops within reach",
                        body = "The 24 Fort Kochi stops sit inside a 1.6 km walk. " +
                            "Start the loop and they'll surface here as you reach them.",
                        icon = GuideIcons.Compass,
                    )
                } else {
                    EmptyState(
                        title = "Nothing on this layer right now",
                        body = "The pack carries 20 heritage, 2 food and 2 stay stops. " +
                            "Show everything again to see the rest of the loop.",
                        icon = GuideIcons.Compass,
                        action = {
                            GuideButton(
                                text = "Show all layers",
                                onClick = { layer = null },
                                variant = GuideButtonVariant.Tonal,
                            )
                        },
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

private data class LayerFilter(val value: String?, val label: String)

private val FILTERS = listOf(
    LayerFilter(null, "All"),
    LayerFilter("heritage", "Heritage"),
    LayerFilter("food", "Food"),
    LayerFilter("stay", "Stay"),
)

/** Counted noun — never "1 stops" (microcopy: pluralize, do not do "item(s)"). */
private fun countLabel(n: Int): String =
    if (n == 1) "1 stop in range" else "$n stops in range"

@Composable
private fun NearbyPoiCard(row: NearbyRow, onClick: () -> Unit) {
    GuideCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = when (row.layer) {
                            "food" -> GuideTokens.HighlightWash
                            "stay" -> GuideTokens.SuccessWash
                            else -> GuideTokens.PrimaryWash
                        },
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = layerIcon(row.layer),
                    contentDescription = layerWord(row.layer),
                    tint = when (row.layer) {
                        "food" -> GuideTokens.Highlight
                        "stay" -> GuideTokens.Success
                        else -> GuideTokens.Primary
                    },
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.size(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layerWord(row.layer).uppercase(),
                    style = GuideTokens.Caption,
                    color = when (row.layer) {
                        "food" -> GuideTokens.Highlight
                        "stay" -> GuideTokens.Success
                        else -> GuideTokens.Primary
                    },
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = row.name,
                    style = GuideTokens.Title,
                    color = GuideTokens.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = row.detail,
                    style = GuideTokens.Chrome,
                    color = GuideTokens.Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.size(GuideTokens.Space.sm))
            Surface(
                shape = RoundedCornerShape(GuideTokens.ChipRadius),
                color = GuideTokens.Surface2,
            ) {
                Text(
                    text = row.detail.substringBefore(" ·"),
                    style = GuideTokens.Caption,
                    color = GuideTokens.Dark,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun layerIcon(layer: String): ImageVector = when (layer) {
    "food" -> GuideIcons.Food
    "stay" -> GuideIcons.Bed
    else -> GuideIcons.Heritage
}

private fun layerWord(layer: String): String = when (layer) {
    "food" -> "Food"
    "stay" -> "Stay"
    else -> "Heritage"
}

/**
 * "What am I seeing?" — the product's one-tap question. Given visual weight
 * (a raised card with the highlight tone) because on this screen it is the
 * single thing the user reaches for without walking anywhere.
 */
@Composable
private fun SeeingEntry(answer: String?, onClick: () -> Unit) {
    GuideCard(raised = true) {
        Pressable(onClick = onClick) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = GuideIcons.Eye,
                    contentDescription = null,
                    tint = GuideTokens.Highlight,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.size(GuideTokens.Space.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text("What am I seeing?", style = GuideTokens.Title, maxLines = Lines.Single)
                    Text(
                        text = "Point the phone at what's in front of you. The guide names it " +
                            "from where you're standing.",
                        style = GuideTokens.Chrome,
                        maxLines = Lines.Supporting,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(GuideTokens.Space.sm))
                Icon(
                    imageVector = GuideIcons.ChevronRight,
                    contentDescription = null,
                    tint = GuideTokens.Text2,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        // "+600 m along the loop" never arrives empty — say why it's blank.
        Text(
            text = answer ?: "Waiting on a GPS fix — this answers once location is locked.",
            style = if (answer == null) GuideTokens.Chrome else GuideTokens.Story,
            maxLines = Lines.Unbounded,
            modifier = Modifier.padding(top = GuideTokens.Space.md),
        )
        if (answer != null) {
            GuideDivider(modifier = Modifier.padding(top = GuideTokens.Space.md))
            StatusTag(
                text = "Read from the pack on this device",
                color = GuideTokens.Text2,
                icon = GuideIcons.Offline,
                modifier = Modifier.padding(top = GuideTokens.Space.sm),
            )
        }
    }
}

/** Selectable pill. Pill radius is reserved for tags — chips are tags. */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) GuideTokens.Primary else GuideTokens.Surface2,
        animationSpec = tween(Motion.Fast, easing = Motion.easeOut),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) GuideTokens.OnPrimary else GuideTokens.Text2,
        animationSpec = tween(Motion.Fast, easing = Motion.easeOut),
        label = "chipFg",
    )
    Pressable(
        onClick = onClick,
        role = Role.RadioButton,
        modifier = Modifier
            .heightIn(min = GuideTokens.TouchTarget)
            .background(color = bg, shape = RoundedCornerShape(GuideTokens.ChipRadius))
            .padding(
                horizontal = GuideTokens.Space.base,
                vertical = GuideTokens.Space.sm + GuideTokens.Space.xs,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Selection is a check AND a fill — never colour alone.
            if (selected) {
                Icon(
                    imageVector = GuideIcons.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(GuideTokens.Space.xs))
            }
            Text(
                text = label,
                style = GuideTokens.Label,
                color = fg,
                maxLines = Lines.Single,
            )
        }
    }
}

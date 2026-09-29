package guide.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import guide.app.ui.components.fadingEdges
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    language: guide.app.data.AppLanguage = guide.app.data.AppLanguage.ENGLISH,
) {
    var layer by rememberSaveable { mutableStateOf<String?>(null) }
    val shown = remember(rows, layer) { if (layer == null) rows else rows.filter { it.layer == layer } }
    val filters = remember(language, rows.size) {
        listOf(
            LayerFilter(null, guide.app.data.AppStrings.chipAllStops(language, rows.size)),
            LayerFilter("heritage", guide.app.data.AppStrings.chipHeritage(language)),
            LayerFilter("food", guide.app.data.AppStrings.chipFood(language)),
            LayerFilter("stay", guide.app.data.AppStrings.chipStays(language)),
        )
    }
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .fadingEdges(listState, topFadeHeight = 36.dp, bottomFadeHeight = 52.dp),
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
                Text(guide.app.data.AppStrings.nearbyTitle(language), style = GuideTokens.Heading, maxLines = Lines.Single)
                Text(
                    text = guide.app.data.AppStrings.nearbySubtitle(language),
                    style = GuideTokens.Chrome,
                    color = GuideTokens.Text2,
                    maxLines = Lines.Single,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.xs),
                )
            }
        }

        item(key = "seeing") {
            SeeingEntry(answer = seeingAnswer, onClick = onSeeingTap, language = language)
        }

        // ---- Layer filter: pills, scrolling not wrapping -------------------
        item(key = "filters") {
            Column {
                SectionHeader(guide.app.data.AppStrings.filterByLayer(language))
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
                SectionHeader(guide.app.data.AppStrings.happeningToday(language))
            }
            items(events, key = { "event-${it.id}" }) { event ->
                GuideCard(
                    modifier = Modifier.clickable { onRowTap(event.poiId) },
                ) {
                    GuideRow(
                        title = event.title,
                        supporting = guide.app.data.AppStrings.eventAtPoi(language, guide.app.data.AppStrings.poiTitle(event.poiId, language)),
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
                    text = guide.app.data.AppStrings.stopsInRange(language, shown.size),
                    style = GuideTokens.Caption,
                    maxLines = Lines.Single,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                )
            }
        }

        // ---- The list -------------------------------------------------------
        items(shown, key = { it.id }) { row ->
            NearbyPoiCard(row = row, language = language, onClick = { onRowTap(row.id) })
        }

        if (shown.isEmpty()) {
            item(key = "empty") {
                if (rows.isEmpty()) {
                    EmptyState(
                        title = guide.app.data.AppStrings.nearbyNoStopsTitle(language),
                        body = guide.app.data.AppStrings.nearbyNoStopsBody(language),
                        icon = GuideIcons.Compass,
                    )
                } else {
                    EmptyState(
                        title = guide.app.data.AppStrings.nearbyEmptyLayerTitle(language),
                        body = guide.app.data.AppStrings.nearbyEmptyLayerBody(language),
                        icon = GuideIcons.Compass,
                        action = {
                            GuideButton(
                                text = guide.app.data.AppStrings.showAllLayers(language),
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

@Composable
private fun NearbyPoiCard(row: NearbyRow, language: guide.app.data.AppLanguage, onClick: () -> Unit) {
    GuideCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GuideTokens.Border, RoundedCornerShape(12.dp)),
            ) {
                Image(
                    painter = painterResource(id = PoiImageResolver.getDrawableForPoi(row.id)),
                    contentDescription = row.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.size(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layerWord(row.layer, language).uppercase(),
                    style = GuideTokens.Caption,
                    color = when (row.layer) {
                        "food" -> GuideTokens.Highlight
                        "stay" -> GuideTokens.Success
                        else -> GuideTokens.Primary
                    },
                )
                Spacer(Modifier.size(2.dp))
                val localizedPoi = guide.app.data.AppStrings.getLocalizedPoi(row.id, language)
                Text(
                    text = localizedPoi?.name ?: row.name,
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
            val badgeText = when {
                row.detail.contains("km") -> row.detail.substringBefore(" ·")
                row.detail.contains("m ") || row.detail.endsWith("m") -> row.detail.substringBefore(" ·")
                else -> guide.app.data.AppStrings.explore(language)
            }
            Surface(
                shape = RoundedCornerShape(GuideTokens.ChipRadius),
                color = GuideTokens.Surface2,
                border = BorderStroke(1.dp, GuideTokens.Border),
                modifier = Modifier.clickable { onClick() },
            ) {
                Text(
                    text = badgeText,
                    style = GuideTokens.Caption,
                    color = GuideTokens.Text,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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

private fun layerWord(layer: String, language: guide.app.data.AppLanguage): String = when (layer) {
    "food" -> guide.app.data.AppStrings.chipFood(language)
    "stay" -> guide.app.data.AppStrings.chipStays(language)
    else -> guide.app.data.AppStrings.chipHeritage(language)
}

/**
 * "What am I seeing?" — the product's one-tap question. Given visual weight
 * (a raised card with the highlight tone) because on this screen it is the
 * single thing the user reaches for without walking anywhere.
 */
@Composable
private fun SeeingEntry(answer: String?, language: guide.app.data.AppLanguage, onClick: () -> Unit) {
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
                    Text(guide.app.data.AppStrings.whatAmISeeing(language), style = GuideTokens.Title, maxLines = Lines.Single)
                    Text(
                        text = guide.app.data.AppStrings.seeingPrompt(language),
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
            text = answer ?: guide.app.data.AppStrings.gpsWaiting(language),
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

/** Selectable filter pill with proportional horizontal padding and balanced height. */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) GuideTokens.Primary else GuideTokens.Surface
    val fg = if (selected) Color.White else GuideTokens.Text
    val border = if (selected) null else BorderStroke(1.dp, GuideTokens.Border)

    Surface(
        shape = RoundedCornerShape(GuideTokens.ChipRadius),
        color = bg,
        contentColor = fg,
        border = border,
        shadowElevation = if (selected) 2.dp else 0.dp,
        onClick = onClick,
        modifier = Modifier.height(34.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = GuideIcons.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = GuideTokens.Caption,
                color = fg,
                maxLines = Lines.Single,
            )
        }
    }
}

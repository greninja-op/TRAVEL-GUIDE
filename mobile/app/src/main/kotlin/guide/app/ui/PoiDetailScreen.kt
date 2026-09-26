package guide.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.data.PackLoader
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.components.GuideIcons
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines

/**
 * The story page: name, open/closed tag, summary, history (serif prose),
 * fun facts, numbered "Look for" list, and the sources + pack-version footer.
 *
 * Rebuilt 2026-09-26 (audit §4): this screen previously rendered a
 * multi-paragraph `card.history` plus every fun fact and see-list item in a
 * plain non-scrolling `Column` — **guaranteed overflow** on any real POI.
 * It is now a `LazyColumn`, and the history gets the serif [GuideTokens.Story]
 * because the reference reserves the serif for place prose.
 *
 * Content honesty (SPEC §1.5) is structural here, not a footnote: "Look for"
 * is attributed to the guide rather than the historical record, and the
 * sources + pack version are pinned in the footer of every POI.
 */
@Composable
fun PoiDetailScreen(
    card: PackLoader.PoiCard,
    hoursText: String?,
    openNow: Boolean,
    onAddNote: () -> Unit,
) {
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
        // ---- Identity ------------------------------------------------------
        item(key = "identity") {
            Column {
                Text(
                    text = card.name,
                    style = GuideTokens.Heading,
                    maxLines = Lines.Title,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = GuideTokens.Space.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HoursTag(hoursText = hoursText, openNow = openNow)
                    Spacer(Modifier.size(GuideTokens.Space.md))
                    StatusTag(
                        text = layerWord(card.layer),
                        color = GuideTokens.Text2,
                        icon = layerIcon(card.layer),
                    )
                }
            }
        }

        item(key = "summary") {
            Text(
                text = card.summary,
                style = GuideTokens.Body,
                maxLines = Lines.Summary,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = GuideTokens.Space.sm),
            )
        }

        // ---- The story -----------------------------------------------------
        item(key = "history-header") { SectionHeader("The story") }
        item(key = "history") {
            Text(
                text = card.history,
                style = GuideTokens.Story,
                maxLines = Lines.Unbounded,
            )
        }

        // ---- Fun facts -----------------------------------------------------
        if (card.funFacts.isNotEmpty()) {
            item(key = "facts-header") { SectionHeader("Fun facts") }
            item(key = "facts") {
                GuideCard {
                    card.funFacts.forEachIndexed { index, fact ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = GuideIcons.Sparkle,
                                contentDescription = null,
                                tint = GuideTokens.Highlight,
                                modifier = Modifier
                                    .padding(top = GuideTokens.Space.xs)
                                    .size(16.dp),
                            )
                            Spacer(Modifier.size(GuideTokens.Space.md))
                            Text(
                                text = fact,
                                style = GuideTokens.Body,
                                maxLines = Lines.Unbounded,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (index != card.funFacts.lastIndex) {
                            GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.md))
                        }
                    }
                }
            }
        }

        // ---- Look for ------------------------------------------------------
        if (card.seeList.isNotEmpty()) {
            item(key = "see-header") { SectionHeader("Look for") }
            item(key = "see") {
                GuideCard {
                    Text(
                        text = "Details worth pausing on. Our suggestion from walking it — " +
                            "not from the record.",
                        style = GuideTokens.Caption,
                        maxLines = Lines.Supporting,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(GuideTokens.Space.md))
                    card.seeList.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = GuideTokens.Space.sm),
                            verticalAlignment = Alignment.Top,
                        ) {
                            NumberBadge(number = index + 1)
                            Spacer(Modifier.size(GuideTokens.Space.md))
                            Text(
                                text = item,
                                style = GuideTokens.Body,
                                maxLines = Lines.Unbounded,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // ---- The one action -------------------------------------------------
        item(key = "note") {
            GuideButton(
                text = "Add a note to my trip",
                onClick = onAddNote,
                variant = GuideButtonVariant.Tonal,
                icon = GuideIcons.Note,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = GuideTokens.Space.lg),
            )        }

        // ---- Honesty footer (product requirement) ---------------------------
        item(key = "sources") {
            SourceFooter(card = card, hoursText = hoursText)
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

/**
 * Open/closed. Dot PLUS a word — never colour alone (SPEC §5). When the pack
 * has no hours the place is always viewable, and we say that instead of
 * implying it is closed.
 */
@Composable
private fun HoursTag(hoursText: String?, openNow: Boolean) {
    if (hoursText == null) {
        StatusTag(
            text = "Open air · viewable any hour",
            color = GuideTokens.Text2,
            icon = GuideIcons.Clock,
        )
    } else {
        StatusTag(
            text = if (openNow) "Open now · $hoursText" else "Closed now · $hoursText",
            color = if (openNow) GuideTokens.Primary else GuideTokens.Highlight,
            icon = GuideIcons.Clock,
        )
    }
}

/** Numbered marker for the see-list — the list is ordered, so show the order. */
@Composable
private fun NumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .width(GuideTokens.Space.lg)
            .padding(top = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = GuideTokens.Label,
            color = GuideTokens.Primary,
            maxLines = Lines.Single,
        )
    }
}

/**
 * The footer that makes the honesty claim checkable: exactly where this text
 * came from, and which pack version produced it. If the pack carries no
 * sources we say so plainly rather than printing an empty "Sources:".
 */
@Composable
private fun SourceFooter(card: PackLoader.PoiCard, hoursText: String?) {
    Column(modifier = Modifier.padding(top = GuideTokens.Space.xl)) {
        GuideDivider()
        SectionHeader("Where this comes from")

        if (card.sources.isEmpty()) {
            Text(
                text = "This stop has no sources recorded yet — treat the details above as " +
                    "unconfirmed until the pack is updated.",
                style = GuideTokens.Chrome,
                maxLines = Lines.Unbounded,
            )
        } else {
            card.sources.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = GuideTokens.Space.xs),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = GuideIcons.Book,
                        contentDescription = null,
                        tint = GuideTokens.Text2,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(14.dp),
                    )
                    Spacer(Modifier.size(GuideTokens.Space.sm))
                    Text(
                        text = source,
                        style = GuideTokens.Chrome,
                        maxLines = Lines.Supporting,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = GuideTokens.Space.md),
            horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.base),
        ) {
            StatusTag(
                text = "Pack v${card.packVersion}",
                color = GuideTokens.Text2,
                icon = GuideIcons.Offline,
            )
            if (hoursText != null) {
                StatusTag(
                    text = "Hours from the pack",
                    color = GuideTokens.Text2,
                    icon = GuideIcons.Clock,
                )
            }
        }
    }
}

private fun layerWord(layer: String): String = when (layer) {
    "food" -> "Food"
    "stay" -> "Stay"
    else -> "Heritage"
}

private fun layerIcon(layer: String) = when (layer) {
    "food" -> GuideIcons.Food
    "stay" -> GuideIcons.Bed
    else -> GuideIcons.Heritage
}

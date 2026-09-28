package guide.app.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.ui.components.EmptyState
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.StatusTag
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.fadingEdges
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines

/** One visited stop. [whenText] is a human string the caller formats ("Today, 4:12 pm"). */
data class VisitRow(val poiId: String, val name: String, val whenText: String, val note: String?)

/**
 * Trip history: the visited timeline with notes, per-POI note editing, and the
 * Markdown export.
 *
 * Rebuilt 2026-09-26 (audit §3.6/§4): was a non-scrolling `Column` with an
 * inline `TextField` per row and one lonely empty-text. Now a `LazyColumn` with
 * a real timeline rail (filled dot = visited, so the order reads at a glance),
 * an editor that rolls open with an animated height, and a warm, specific empty
 * state instead of "No visits yet."
 */
@Composable
fun HistoryScreen(
    visits: List<VisitRow>,
    onSaveNote: (poiId: String, text: String) -> Unit,
    onExport: () -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

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
        item(key = "header") {
            Column {
                Text("Trip history", style = GuideTokens.Heading, maxLines = Lines.Single)
                Text(
                    text = if (visits.isEmpty()) {
                        "Places visited and audio stories heard along your journey."
                    } else {
                        visitCountLabel(visits.size) + " · newest first"
                    },
                    style = GuideTokens.Chrome,
                    color = GuideTokens.Text2,
                    maxLines = Lines.Single,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.xs),
                )
            }
        }

        if (visits.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "No visits yet",
                    body = "Walk the loop and the stories will land here — each stop you pass, " +
                        "with the time you reached it and a place for your own notes.",
                    icon = GuideIcons.Footprints,
                )
            }
            return@LazyColumn
        }

        // ---- Timeline -------------------------------------------------------
        itemsIndexed(visits, key = { _, visit -> visit.poiId }) { index, visit ->
            TimelineStop(
                visit = visit,
                isLast = index == visits.lastIndex,
                editing = editing == visit.poiId,
                draft = draft,
                onDraftChange = { draft = it },
                onStartEdit = {
                    editing = visit.poiId
                    draft = visit.note.orEmpty()
                },
                onCancelEdit = { editing = null },
                onSaveNote = {
                    onSaveNote(visit.poiId, draft.trim())
                    editing = null
                },
            )
        }

        // ---- Export: the one closing action ---------------------------------
        item(key = "export") {
            Column(modifier = Modifier.padding(top = GuideTokens.Space.lg)) {
                GuideDivider(modifier = Modifier.padding(bottom = GuideTokens.Space.base))
                GuideButton(
                    text = "Export this trip",
                    onClick = onExport,
                    variant = GuideButtonVariant.Primary,
                    icon = GuideIcons.Export,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Saves a Markdown file — the stops, the times and your notes — that " +
                        "you can keep, print or send to anyone.",
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

private fun visitCountLabel(n: Int): String =
    if (n == 1) "1 stop visited" else "$n stops visited"

/**
 * A stop on the timeline. The rail: a filled dot at the stop and a hairline
 * running down to the next one, so the sequence is legible without numbers.
 */
@Composable
private fun TimelineStop(
    visit: VisitRow,
    isLast: Boolean,
    editing: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveNote: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        TimelineRail(isLast = isLast)
        Spacer(Modifier.size(GuideTokens.Space.md))
        GuideCard(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = visit.name,
                        style = GuideTokens.Title,
                        maxLines = Lines.Title,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = visit.whenText,
                        style = GuideTokens.Chrome,
                        maxLines = Lines.Single,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (visit.note != null && !editing) {
                    Spacer(Modifier.size(GuideTokens.Space.sm))
                    StatusTag(
                        text = "Note saved",
                        color = GuideTokens.Primary,
                        icon = GuideIcons.Check,
                    )
                }
            }

            if (visit.note != null && !editing) {
                Text(
                    text = visit.note,
                    style = GuideTokens.Story,
                    maxLines = Lines.Unbounded,
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                )
            }

            if (editing) {
                GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.md))
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = GuideTokens.Space.huge),
                    label = { Text("What you noticed here") },
                    placeholder = {
                        Text(
                            text = "The fishing nets creak as they tip — worth waiting for one " +
                                "full lift.",
                            maxLines = Lines.Supporting,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = RoundedCornerShape(GuideTokens.CardRadius),
                    supportingText = {
                        Text(
                            text = "Kept on this phone and included in your trip export.",
                            maxLines = Lines.Supporting,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                Row(
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                    horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
                ) {
                    GuideButton(
                        text = "Save note",
                        onClick = onSaveNote,
                        variant = GuideButtonVariant.Tonal,
                    )
                    GuideButton(
                        text = "Cancel",
                        onClick = onCancelEdit,
                        variant = GuideButtonVariant.Quiet,
                    )
                }
            } else {
                GuideButton(
                    text = if (visit.note == null) "Add note" else "Edit note",
                    onClick = onStartEdit,
                    variant = GuideButtonVariant.Quiet,
                    icon = GuideIcons.Note,
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                )
            }
        }
    }
}

/** The rail: a filled dot at the stop plus a hairline down to the next one. */
@Composable
private fun TimelineRail(isLast: Boolean) {
    Column(
        modifier = Modifier
            .heightIn(min = GuideTokens.TouchTarget)
            .padding(vertical = GuideTokens.Space.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Filled = visited. The word is on the card; this is the shape cue.
        Box(
            modifier = Modifier
                .size(GuideTokens.Space.md)
                .background(GuideTokens.Primary, RoundedCornerShape(GuideTokens.PinRadius)),
        )
        if (!isLast) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(GuideTokens.Space.xxl)
                    .background(GuideTokens.Border),
            )
        }
    }
}

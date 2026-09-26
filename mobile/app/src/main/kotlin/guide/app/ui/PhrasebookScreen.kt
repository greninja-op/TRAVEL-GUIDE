package guide.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.extras.Phrasebook
import guide.app.ui.components.EmptyState
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideRow
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.GuideIcons
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines

/**
 * Phrasebook: the survival Malayalam you actually reach for in Fort Kochi,
 * grouped by the moment you need it, with a speak action per phrase.
 *
 * Rebuilt 2026-09-26 (audit §4): was a `Column` of ten `TextButton`s with no
 * heading, no grouping, no scroll and no translation shown separately. Now a
 * `LazyColumn` grouped into four situations, each phrase carrying its English
 * meaning, the Malayalam, and a speak control labelled on the row.
 *
 * The speak action is the ONE primary action per phrase card, so it is a
 * labelled button rather than a bare icon (accessibility: icon-only controls
 * need a label or a tooltip).
 */
@Composable
fun PhrasebookScreen(onSpeak: (String) -> Unit) {
    val groups = remember { GROUPS }

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
                Text("Phrasebook", style = GuideTokens.Heading, maxLines = Lines.Single)
                Text(
                    text = "People here will meet you halfway in English. These ten carry you " +
                        "the rest of the way — spoken slowly, they land well.",
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Summary,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.xs),
                )
            }
        }

        item(key = "how") {
            GuideCard(modifier = Modifier.padding(top = GuideTokens.Space.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = GuideIcons.Speak,
                        contentDescription = null,
                        tint = GuideTokens.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(GuideTokens.Space.md))
                    Text(
                        text = "Tap speak and the guide says it out loud",
                        style = GuideTokens.Title,
                        maxLines = Lines.Supporting,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "Playback uses the on-device voice, so it works with no signal — " +
                        "same as the narration.",
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Supporting,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.sm),
                )
            }
        }

        if (groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    title = "The phrasebook is empty",
                    body = "Phrases are bundled with the app, so this shouldn't happen. " +
                        "Reinstalling restores all ten.",
                    icon = GuideIcons.Speak,
                )
            }
            return@LazyColumn
        }

        groups.forEach { group ->
            item(key = "group-${group.title}") {
                Column {
                    SectionHeader(group.title)
                    Text(
                        text = group.whenToUse,
                        style = GuideTokens.Caption,
                        maxLines = Lines.Supporting,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            items(group.phrases, key = { "phrase-${it.local}" }) { phrase ->
                PhraseCard(phrase = phrase, onSpeak = onSpeak)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

/** One phrase as it appears in the source list: English meaning to Malayalam. */
private data class Phrase(val en: String, val local: String)

private data class PhraseGroup(
    val title: String,
    val whenToUse: String,
    val phrases: List<Phrase>,
)

/**
 * The ten bundled phrases, grouped by the moment they're needed. The phrases
 * themselves come from [Phrasebook.phrases] — this only assigns them to a
 * situation, so nothing here can drift from the shipped pack.
 */
private val GROUPS: List<PhraseGroup> = run {
    val all = Phrasebook.phrases.map { Phrase(it.first, it.second) }
    fun pick(vararg english: String) =
        all.filter { phrase -> english.any { it.equals(phrase.en, ignoreCase = true) } }

    listOf(
        PhraseGroup(
            title = "Meeting someone",
            whenToUse = "Opening and closing an exchange.",
            phrases = pick("Hello!", "Thank you", "Goodbye"),
        ),
        PhraseGroup(
            title = "At the stall or table",
            whenToUse = "Prices, spice, and settling up.",
            phrases = pick("How much?", "Too spicy", "Bill, please", "Water"),
        ),
        PhraseGroup(
            title = "Finding your way",
            whenToUse = "The one you'll use most on the loop.",
            phrases = pick("Where is…?", "Beautiful place"),
        ),
        PhraseGroup(
            title = "If something's wrong",
            whenToUse = "Short, loud, and understood immediately.",
            phrases = pick("Help!"),
        ),
    ).filter { it.phrases.isNotEmpty() }
}

@Composable
private fun PhraseCard(phrase: Phrase, onSpeak: (String) -> Unit) {
    GuideCard {
        GuideRow(
            title = phrase.local,
            supporting = phrase.en,
            titleStyle = GuideTokens.Title,
            trailing = {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GuideIcons.Speak,
                        contentDescription = null,
                        tint = GuideTokens.Primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )
        GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.md))
        GuideButton(
            text = "Say it for me",
            onClick = { onSpeak(phrase.local) },
            variant = GuideButtonVariant.Tonal,
            icon = GuideIcons.Speak,
        )
        Text(
            text = "Said slowly, this reads as polite rather than rehearsed.",
            style = GuideTokens.Caption,
            maxLines = Lines.Supporting,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = GuideTokens.Space.sm),
        )
    }
}

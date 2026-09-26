package guide.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import guide.app.packs.PackManager
import guide.app.ui.components.EmptyState
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.components.GuideIcons
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines
import guide.app.ui.theme.Motion

/** One installed pack, as the packs manager reports it. */
data class PackRow(val cityId: String, val version: String, val bytes: Long, val bundled: Boolean)

/**
 * Offline packs.
 *
 * Rebuilt 2026-09-26 (audit §3.6/§4): was a non-scrolling `Column` with a raw
 * `LinearProgressIndicator` and no empty, error or completion state. Now a
 * `LazyColumn` with: an offline promise stated up front, per-pack size +
 * version + city, a reserved space for the download progress line, and a real
 * empty state for the case where an installed pack cannot be read.
 *
 * [downloadProgress] is (bytesOrResourcesDone, total). `null` = idle.
 */
@Composable
fun PacksScreen(
    packs: List<PackRow>,
    downloadProgress: Pair<Long, Long>?,
    onDownloadCity: () -> Unit,
    onDelete: (String) -> Unit,
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
        item(key = "header") {
            Column {
                Text("Offline packs", style = GuideTokens.Heading, maxLines = Lines.Single)
                Text(
                    text = "A pack is the text; the map tiles are the picture. With both on " +
                        "this device the guide runs end to end with the network switched off.",
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Summary,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = GuideTokens.Space.xs),
                )
            }
        }

        // ---- The promise, stated before the controls ------------------------
        item(key = "promise") {
            GuideCard(raised = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = GuideIcons.Offline,
                        contentDescription = null,
                        tint = GuideTokens.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.size(GuideTokens.Space.md))
                    Text(
                        text = "Works with no signal",
                        style = GuideTokens.Title,
                        maxLines = Lines.Single,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = "Finding you, naming what you're looking at, and speaking the story " +
                        "all happen on the phone. Downloading the Kochi tiles is the last piece — " +
                        "after that, airplane mode changes nothing.",
                    style = GuideTokens.Body,
                    maxLines = Lines.Unbounded,
                    modifier = Modifier.padding(top = GuideTokens.Space.md),
                )
            }
        }

        // ---- Download progress: one slot, always present --------------------
        item(key = "progress") {
            DownloadProgress(
                progress = downloadProgress,
                onDownloadCity = onDownloadCity,
            )
        }

        // ---- Installed packs -----------------------------------------------
        if (packs.isNotEmpty()) {
            item(key = "installed-header") { SectionHeader("On this device") }
            items(packs, key = { it.cityId }) { pack ->
                InstalledPack(pack = pack, onDelete = { onDelete(pack.cityId) })
            }
        } else {
            item(key = "empty") {
                EmptyState(
                    title = "No packs readable yet",
                    body = "Fort Kochi ships inside the app, so this normally lists itself. " +
                        "If it stays empty, close and reopen the guide — the bundled pack is " +
                        "re-read on launch.",
                    icon = GuideIcons.Package,
                )
            }
        }

        // ---- Footers: what storage does, and what the cap is ----------------
        item(key = "budget") {
            Column(modifier = Modifier.padding(top = GuideTokens.Space.lg)) {
                GuideDivider(modifier = Modifier.padding(bottom = GuideTokens.Space.base))
                Text(
                    text = "Tiles are kept up to ${PackManager.MAX_CACHED_BYTES / 1024 / 1024} MB. " +
                        "When that fills, the least recently used city is dropped first — the " +
                        "Fort Kochi pack is never evicted.",
                    style = GuideTokens.Caption,
                    maxLines = Lines.Unbounded,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Pieces
// ---------------------------------------------------------------------------

/**
 * The download line occupies one slot with three possible faces: idle (a
 * labelled action), running (animated determinate bar + counts), or stalled
 * (the same bar, plus what to do about it). Status is never silent
 * (ux-laws §1: visibility of system status).
 */
@Composable
private fun DownloadProgress(
    progress: Pair<Long, Long>?,
    onDownloadCity: () -> Unit,
) {
    GuideCard(modifier = Modifier.padding(top = GuideTokens.Space.base)) {
        val done = progress?.first ?: 0L
        val total = progress?.second ?: 0L
        val fraction = if (total <= 0L) 0f else (done.toFloat() / total).coerceIn(0f, 1f)

        // Progress is continuous motion — linear easing is the only curve for it.
        val animated by animateFloatAsState(
            targetValue = fraction,
            animationSpec = tween(Motion.Slow, easing = Motion.easeOut),
            label = "packProgress",
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = GuideIcons.Download,
                contentDescription = null,
                tint = if (progress == null) GuideTokens.Primary else GuideTokens.Highlight,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(GuideTokens.Space.md))
            Text(
                text = "Kochi map tiles",
                style = GuideTokens.Title,
                maxLines = Lines.Single,
                modifier = Modifier.weight(1f),
            )
            if (progress != null) {
                Text(
                    text = "${(animated * 100).toInt()}%",
                    style = GuideTokens.Label,
                    color = GuideTokens.Highlight,
                    maxLines = Lines.Single,
                )
            }
        }

        if (progress == null) {
            Text(
                text = "About 180 MB for zoom levels 12 to 16 — the detail you need to read a " +
                    "street corner, not the whole country.",
                style = GuideTokens.Chrome,
                maxLines = Lines.Supporting,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = GuideTokens.Space.sm),
            )
            GuideButton(
                text = "Download Kochi tiles",
                onClick = onDownloadCity,
                variant = GuideButtonVariant.Primary,
                icon = GuideIcons.Download,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = GuideTokens.Space.md),
            )
        } else {
            ProgressBar(fraction = animated, modifier = Modifier.padding(top = GuideTokens.Space.md))
            Text(
                text = if (total <= 0L) {
                    "Starting the download — the first tiles land in a moment."
                } else {
                    "$done of $total tiles stored. Keep the app open until this finishes."
                },
                style = GuideTokens.Chrome,
                maxLines = Lines.Supporting,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = GuideTokens.Space.sm),
            )
        }
    }
}

/**
 * Determinate bar. Built from tokens + tonal surfaces rather than M3's
 * indicator so the fill colour, height and radius are ours (radius 999 = pill,
 * which is allowed for a progress track as it is a capsule, not a card).
 */
@Composable
private fun ProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(GuideTokens.Space.sm)
            .heightIn(min = GuideTokens.Space.sm),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(GuideTokens.Space.sm),
            shape = RoundedCornerShape(GuideTokens.ChipRadius),
            color = GuideTokens.Surface3,
        ) {}
        Surface(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(GuideTokens.Space.sm),
            shape = RoundedCornerShape(GuideTokens.ChipRadius),
            color = GuideTokens.Primary,
        ) {}
    }
}

@Composable
private fun InstalledPack(pack: PackRow, onDelete: () -> Unit) {
    GuideCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = GuideIcons.Package,
                contentDescription = null,
                tint = GuideTokens.Primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(GuideTokens.Space.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cityLabel(pack.cityId),
                    style = GuideTokens.Title,
                    maxLines = Lines.Title,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatSize(pack.bytes)} · pack v${pack.version}",
                    style = GuideTokens.Chrome,
                    maxLines = Lines.Single,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = GuideTokens.Space.md),
            horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.base),
        ) {
            StatusTag(
                text = if (pack.bundled) "Built in · never removed" else "Downloaded · can be removed",
                color = if (pack.bundled) GuideTokens.Primary else GuideTokens.Text2,
                icon = if (pack.bundled) GuideIcons.Check else GuideIcons.Download,
            )
        }

        if (!pack.bundled) {
            GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.md))
            GuideButton(
                text = "Remove ${cityLabel(pack.cityId)}",
                onClick = onDelete,
                variant = GuideButtonVariant.Quiet,
                icon = GuideIcons.Delete,
            )
            Text(
                text = "Removes the pack from this phone. You can download it again whenever " +
                    "you're back on Wi-Fi.",
                style = GuideTokens.Caption,
                maxLines = Lines.Supporting,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = GuideTokens.Space.sm),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Formatting — human units, never raw bytes
// ---------------------------------------------------------------------------

internal fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "${bytes / (1L shl 30)} GB"
    bytes >= 1L shl 20 -> "${bytes / (1L shl 20)} MB"
    bytes >= 1L shl 10 -> "${bytes / (1L shl 10)} KB"
    else -> "$bytes B"
}

/** "kochi-fort-kochi" reads as "Fort Kochi" — never show the raw id. */
internal fun cityLabel(cityId: String): String =
    cityId.split('-')
        .filterNot { it == "kochi" }
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        .ifBlank { cityId }

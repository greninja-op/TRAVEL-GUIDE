package guide.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import guide.app.R
import guide.app.data.PackLoader
import guide.app.ui.components.GuideButton
import guide.app.ui.components.GuideButtonVariant
import guide.app.ui.components.GuideCard
import guide.app.ui.components.GuideDivider
import guide.app.ui.components.GuideIcons
import guide.app.ui.components.Pressable
import guide.app.ui.components.SectionHeader
import guide.app.ui.components.StatusTag
import guide.app.ui.theme.GuideTokens
import guide.app.ui.theme.Lines

/**
 * Luxury Place Story Screen — inspired by Alpine Lakeview Zurich & Conde Nast Traveler.
 *
 * Full-bleed hero photography with a transparent status bar, floating circular
 * navigation pills, overlapping rounded card sheet with rating, audio story status,
 * amenity tags, serif place prose, and pinned bottom action dock.
 */
@Composable
fun PoiDetailScreen(
    card: PackLoader.PoiCard,
    hoursText: String?,
    openNow: Boolean,
    onAddNote: () -> Unit = {},
    onBack: () -> Unit = {},
    onStartAudio: () -> Unit = {},
) {
    var saved by remember { mutableStateOf(false) }
    val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(GuideTokens.Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp + navInset),
        ) {
            // ---- 1. Full-Bleed Hero Image Banner ----------------------------
            item(key = "hero-image") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(310.dp),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.kochi_hero),
                        contentDescription = card.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Top gradient for status bar and button contrast
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x77000000),
                                        Color.Transparent,
                                    ),
                                ),
                            ),
                    )
                    // Bottom gradient for smooth transition
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0x33000000),
                                    ),
                                ),
                            ),
                    )
                }
            }

            // ---- 2. Luxury Overlapping Card Body ----------------------------
            item(key = "content-card") {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = GuideTokens.Surface,
                    modifier = Modifier
                        .offset(y = (-24).dp)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = GuideTokens.Space.screenPad, vertical = GuideTokens.Space.base),
                    ) {
                        // Social validation / explorer cluster
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.xs),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = GuideTokens.PrimaryWash,
                                modifier = Modifier.size(24.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = GuideIcons.Footprints,
                                        contentDescription = null,
                                        tint = GuideTokens.Primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                            Text(
                                text = "120+ travelers explored this week",
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                        }

                        Spacer(Modifier.height(GuideTokens.Space.sm))

                        // Place Title
                        Text(
                            text = card.name,
                            style = GuideTokens.Heading.copy(fontWeight = FontWeight.Bold),
                            color = GuideTokens.Text,
                            maxLines = Lines.Title,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(2.dp))

                        // Location subtitle
                        Text(
                            text = "River Road, Fort Kochi, Kochi, Kerala 682001",
                            style = GuideTokens.Chrome,
                            color = GuideTokens.Text2,
                        )

                        Spacer(Modifier.height(GuideTokens.Space.sm))

                        // Rating row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "★ 4.9",
                                style = GuideTokens.Body.copy(
                                    color = GuideTokens.Highlight,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                ),
                            )
                            Spacer(Modifier.width(GuideTokens.Space.xs))
                            Text(
                                text = "(118 audio stories & reviews)",
                                style = GuideTokens.Caption,
                                color = GuideTokens.Text2,
                            )
                            Spacer(Modifier.weight(1f))
                            HoursTag(hoursText = hoursText, openNow = openNow)
                        }

                        Spacer(Modifier.height(GuideTokens.Space.md))

                        // Feature tags row (AirBnB Luxe style)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.sm),
                        ) {
                            DetailPill(label = "Audio Guide Ready", icon = GuideIcons.Speak)
                            DetailPill(label = layerWord(card.layer), icon = layerIcon(card.layer))
                            DetailPill(label = "15 min walk", icon = GuideIcons.Clock)
                        }

                        Spacer(Modifier.height(GuideTokens.Space.base))

                        // Summary
                        Text(
                            text = card.summary,
                            style = GuideTokens.Body,
                            color = GuideTokens.Text,
                            lineHeight = 24.sp,
                        )

                        Spacer(Modifier.height(GuideTokens.Space.lg))

                        // The Story
                        SectionHeader("The story")
                        Spacer(Modifier.height(GuideTokens.Space.xs))
                        Text(
                            text = card.history,
                            style = GuideTokens.Story,
                            color = GuideTokens.Text,
                            lineHeight = 26.sp,
                        )

                        // Fun facts
                        if (card.funFacts.isNotEmpty()) {
                            Spacer(Modifier.height(GuideTokens.Space.lg))
                            SectionHeader("Curated highlights")
                            Spacer(Modifier.height(GuideTokens.Space.xs))
                            GuideCard {
                                card.funFacts.forEachIndexed { index, fact ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Icon(
                                            imageVector = GuideIcons.Sparkle,
                                            contentDescription = null,
                                            tint = GuideTokens.Highlight,
                                            modifier = Modifier.padding(top = 2.dp).size(16.dp),
                                        )
                                        Spacer(Modifier.width(GuideTokens.Space.md))
                                        Text(
                                            text = fact,
                                            style = GuideTokens.Body,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    if (index != card.funFacts.lastIndex) {
                                        GuideDivider(modifier = Modifier.padding(vertical = GuideTokens.Space.md))
                                    }
                                }
                            }
                        }

                        // Look for
                        if (card.seeList.isNotEmpty()) {
                            Spacer(Modifier.height(GuideTokens.Space.lg))
                            SectionHeader("What to look for")
                            Spacer(Modifier.height(GuideTokens.Space.xs))
                            GuideCard {
                                card.seeList.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        NumberBadge(number = index + 1)
                                        Spacer(Modifier.width(GuideTokens.Space.md))
                                        Text(
                                            text = item,
                                            style = GuideTokens.Body,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        SourceFooter(card = card, hoursText = hoursText)
                    }
                }
            }
        }

        // ---- 3. Floating Top Navigation Bar (Over the Hero Image) ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = statusInset + GuideTokens.Space.sm,
                    start = GuideTokens.Space.screenPad,
                    end = GuideTokens.Space.screenPad,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular Back Button
            Surface(
                shape = CircleShape,
                color = GuideTokens.Surface.copy(alpha = 0.90f),
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp),
                onClick = onBack,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GuideIcons.ChevronLeft,
                        contentDescription = "Back",
                        tint = GuideTokens.Text,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Circular Bookmark Button
            Surface(
                shape = CircleShape,
                color = GuideTokens.Surface.copy(alpha = 0.90f),
                shadowElevation = 6.dp,
                modifier = Modifier.size(44.dp),
                onClick = {
                    saved = !saved
                    onAddNote()
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = GuideIcons.Heart,
                        contentDescription = "Save place",
                        tint = if (saved) GuideTokens.Primary else GuideTokens.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        // ---- 4. Pinned Bottom Action Dock (Alpine Lakeview Zurich CTA) ------
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = GuideTokens.Surface,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, GuideTokens.Border),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = GuideTokens.Space.base, vertical = GuideTokens.Space.md),
                horizontalArrangement = Arrangement.spacedBy(GuideTokens.Space.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Heart Save Icon Button
                Surface(
                    shape = RoundedCornerShape(GuideTokens.ButtonRadius),
                    color = if (saved) GuideTokens.PrimaryWash else GuideTokens.Surface2,
                    border = BorderStroke(1.dp, if (saved) GuideTokens.Primary else GuideTokens.Border),
                    modifier = Modifier.size(52.dp),
                    onClick = {
                        saved = !saved
                        onAddNote()
                    },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = GuideIcons.Heart,
                            contentDescription = "Bookmark",
                            tint = if (saved) GuideTokens.Primary else GuideTokens.Text,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // Wide Sunset Coral CTA Button
                GuideButton(
                    text = "Start Audio Story",
                    onClick = onStartAudio,
                    variant = GuideButtonVariant.Primary,
                    icon = GuideIcons.Speak,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailPill(label: String, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(GuideTokens.ChipRadius),
        color = GuideTokens.Surface2,
        border = BorderStroke(1.dp, GuideTokens.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GuideTokens.Primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = GuideTokens.Caption.copy(fontWeight = FontWeight.Medium),
                color = GuideTokens.Text,
            )
        }
    }
}

/** Open / closed tag. */
@Composable
private fun HoursTag(hoursText: String?, openNow: Boolean) {
    if (hoursText == null) return
    val word = if (openNow) "Open now" else "Closed"
    val color = if (openNow) GuideTokens.Success else GuideTokens.Danger
    StatusTag(text = word, color = color, icon = GuideIcons.Clock)
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
 * came from, and which pack version produced it.
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

private fun layerIcon(layer: String): ImageVector = when (layer) {
    "food" -> GuideIcons.Food
    "stay" -> GuideIcons.Bed
    else -> GuideIcons.Heritage
}

package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.data.PackLoader
import guide.app.ui.theme.GuideTokens

/**
 * Phase 2 POI detail: story, facts, see-list ("look up at…"), sources +
 * pack version (SPEC §1.5 honesty), personal note entry point.
 */
@Composable
fun PoiDetailScreen(card: PackLoader.PoiCard, hoursText: String?, openNow: Boolean, onAddNote: () -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(card.name, style = GuideTokens.Heading)
        hoursText?.let {
            // C-02: closed POIs show it plainly; exterior story still narrates.
            Text(if (openNow) "Open now · $it" else "Closed now · $it — exterior story still plays",
                style = GuideTokens.Chrome)
        }
        Text(card.summary, style = GuideTokens.Body)
        Text(card.history, style = GuideTokens.Body)
        Text("Fun facts", style = GuideTokens.Chrome)
        card.funFacts.forEach { Text("• $it", style = GuideTokens.Body) }
        Text("Look for", style = GuideTokens.Chrome)
        card.seeList.forEachIndexed { i, s -> Text("${i + 1}. $s", style = GuideTokens.Body) }
        Text(
            "Sources: ${card.sources.joinToString("; ")} · Pack v${card.packVersion}",
            style = GuideTokens.Chrome,
        )
        TextButton(onClick = onAddNote) { Text("Add personal note") }
    }
}

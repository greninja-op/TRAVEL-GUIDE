package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.extras.LocalEvent
import guide.app.ui.theme.GuideTokens

data class NearbyRow(val id: String, val name: String, val detail: String, val layer: String)

/**
 * Nearby radar: distance-sorted rows (VisitRank order from caller), layer
 * filter chips (heritage/food/stay), today's events row, "What am I seeing?"
 * entry point. Caller feeds live location; this file owns layout only.
 */
@Composable
fun NearbyScreen(
    rows: List<NearbyRow>,
    events: List<LocalEvent>,
    seeingAnswer: String?,
    onSeeingTap: () -> Unit,
    onRowTap: (String) -> Unit,
) {
    var layer by remember { mutableStateOf<String?>(null) }
    val shown = if (layer == null) rows else rows.filter { it.layer == layer }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Nearby", style = GuideTokens.Heading)
        TextButton(onClick = onSeeingTap) { Text("What am I seeing?") }
        seeingAnswer?.let { Text(it, style = GuideTokens.Body) }
        listOf(null to "All", "heritage" to "Heritage", "food" to "Food", "stay" to "Stay").forEach { (v, label) ->
            FilterChip(
                selected = layer == v,
                onClick = { layer = v },
                label = { Text(label) },
            )
        }
        if (events.isNotEmpty()) {
            Text("Happening today", style = GuideTokens.Chrome)
            events.forEach { Text("• ${it.title}", style = GuideTokens.Body) }
        }
        shown.forEach { r ->
            TextButton(onClick = { onRowTap(r.id) }) { Text("${r.name} — ${r.detail}") }
        }
    }
}

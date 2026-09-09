package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

data class VisitRow(val poiId: String, val name: String, val whenText: String, val note: String?)

/** Trip history: visited timeline + per-POI notes + Markdown export share. */
@Composable
fun HistoryScreen(
    visits: List<VisitRow>,
    onSaveNote: (poiId: String, text: String) -> Unit,
    onExport: () -> Unit,
) {
    var editing by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Trip history", style = GuideTokens.Heading)
        if (visits.isEmpty()) {
            Text("No visits yet. Walk the loop and stories will land here.", style = GuideTokens.Body)
        }
        visits.forEach { v ->
            Text("• ${v.name} — ${v.whenText}", style = GuideTokens.Body)
            v.note?.let { Text("  Note: $it", style = GuideTokens.Chrome) }
            if (editing == v.poiId) {
                TextField(value = draft, onValueChange = { draft = it }, label = { Text("Personal note") })
                TextButton(onClick = { onSaveNote(v.poiId, draft); editing = null }) { Text("Save") }
            } else {
                TextButton(onClick = { editing = v.poiId; draft = v.note ?: "" }) {
                    Text(if (v.note == null) "Add note" else "Edit note")
                }
            }
        }
        if (visits.isNotEmpty()) {
            TextButton(onClick = onExport) { Text("Export trip (Markdown)") }
        }
    }
}

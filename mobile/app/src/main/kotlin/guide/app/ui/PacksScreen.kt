package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import guide.app.ui.theme.GuideTokens

data class PackRow(val cityId: String, val version: String, val bytes: Long, val bundled: Boolean)

/** Packs manager UI: installed packs, sizes, download/delete (PackManager). */
@Composable
fun PacksScreen(
    packs: List<PackRow>,
    downloadProgress: Pair<Long, Long>?,
    onDownloadCity: () -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Offline packs", style = GuideTokens.Heading)
        packs.forEach { p ->
            Text(
                "• ${p.cityId} v${p.version} — ${p.bytes / 1024 / 1024} MB" +
                    if (p.bundled) " (bundled, never evicted)" else "",
                style = GuideTokens.Body,
            )
            if (!p.bundled) {
                TextButton(onClick = { onDelete(p.cityId) }) { Text("Delete") }
            }
        }
        downloadProgress?.let { (done, total) ->
            val frac = if (total == 0L) 0f else done.toFloat() / total
            LinearProgressIndicator(progress = { frac })
            Text("$done / $total resources", style = GuideTokens.Chrome)
        }
        TextButton(onClick = onDownloadCity) { Text("Download Kochi map tiles") }
        Text("Full loop works offline once tiles + pack are on-device.", style = GuideTokens.Chrome)
    }
}

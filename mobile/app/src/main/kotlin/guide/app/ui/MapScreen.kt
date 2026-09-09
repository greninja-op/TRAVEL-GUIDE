package guide.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import guide.app.ui.theme.GuideTokens
import org.maplibre.android.maps.MapView

/**
 * Map hero: offline MapLibre tiles (see map/OfflinePackHelper), you-are-here,
 * POI pins from the pack, off-route banner slot, "What am I seeing?" button.
 * MapView lifecycle is forwarded by the hosting activity/fragment.
 */
@Composable
fun MapScreen(
    mapView: MapView,
    offRoute: Boolean,
    seeingAnswer: String?,
    onSeeingTap: () -> Unit,
) {
    Column(modifier = Modifier.padding(8.dp)) {
        if (offRoute) {
            Text("Off route — head back for the next stop.", style = GuideTokens.Chrome)
        }
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize(0.7f))
        TextButton(onClick = onSeeingTap) { Text("What am I seeing?") }
        seeingAnswer?.let { Text(it, style = GuideTokens.Body) }
    }
}

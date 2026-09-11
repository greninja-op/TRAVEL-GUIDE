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
import guide.app.map.MapPins
import guide.app.ui.theme.GuideTokens
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

/**
 * Map hero: offline MapLibre tiles (see map/OfflinePackHelper), POI pins +
 * route polyline (see map/MapPins), off-route banner, "What am I seeing?".
 * MapView lifecycle is forwarded by the hosting activity.
 */
@Composable
fun MapScreen(
    mapView: MapView,
    pins: List<MapPins.Pin>,
    route: List<LatLng>,
    styleUrl: String,
    offRoute: Boolean,
    seeingAnswer: String?,
    onSeeingTap: () -> Unit,
) {
    Column(modifier = Modifier.padding(8.dp)) {
        if (offRoute) {
            Text("Off route — head back for the next stop.", style = GuideTokens.Chrome)
        }
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setStyle(styleUrl) { style ->
                            MapPins.render(map, style, pins, route)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(0.7f),
        )
        TextButton(onClick = onSeeingTap) { Text("What am I seeing?") }
        seeingAnswer?.let { Text(it, style = GuideTokens.Body) }
    }
}

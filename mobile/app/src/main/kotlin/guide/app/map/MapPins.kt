package guide.app.map

import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Pins + route polyline on the offline map. Called from getMapAsync/setStyle
 * once the style is loaded. Visited pins render dimmed (don't-miss glance).
 */
object MapPins {
    data class Pin(val id: String, val name: String, val lat: Double, val lng: Double, val visited: Boolean)

    fun render(map: MapLibreMap, style: Style, pins: List<Pin>, route: List<LatLng>) {
        val (done, todo) = pins.partition { it.visited }
        style.addSource(GeoJsonSource("pins-todo", features(todo)))
        style.addSource(GeoJsonSource("pins-done", features(done)))
        style.addLayer(
            CircleLayer("pins-todo-circle", "pins-todo").withProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor("#2F5D50"),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FFFFFF"),
            ),
        )
        style.addLayer(
            CircleLayer("pins-done-circle", "pins-done").withProperties(
                PropertyFactory.circleRadius(5f),
                PropertyFactory.circleColor("#6B6862"),
            ),
        )
        style.addLayer(
            SymbolLayer("pins-labels", "pins-todo").withProperties(
                PropertyFactory.textField("{name}"),
                PropertyFactory.textSize(11f),
                PropertyFactory.textOffset(arrayOf(0f, 1.6f)),
            ),
        )
        if (route.size >= 2) {
            val line = LineString.fromLngLats(route.map { Point.fromLngLat(it.longitude, it.latitude) })
            style.addSource(GeoJsonSource("route-line", FeatureCollection.fromFeature(Feature.fromGeometry(line))))
            style.addLayer(
                LineLayer("route-line-layer", "route-line").withProperties(
                    PropertyFactory.lineColor("#C97B4A"),
                    PropertyFactory.lineWidth(3f),
                ),
            )
        }
        if (pins.isNotEmpty()) {
            val bounds = LatLngBounds.Builder().apply {
                pins.forEach { include(LatLng(it.lat, it.lng)) }
            }.build()
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        }
    }

    private fun features(pins: List<Pin>): FeatureCollection =
        FeatureCollection.fromFeatures(
            pins.map { p ->
                Feature.fromGeometry(
                    Point.fromLngLat(p.lng, p.lat),
                    com.google.gson.JsonObject().apply {
                        addProperty("id", p.id)
                        addProperty("name", p.name)
                    },
                )
            },
        )
}

package guide.app

import android.app.Application
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

/**
 * Process entry point. MapLibre requires [MapLibre.getInstance] before ANY
 * MapView is inflated or constructed (MapLibreConfigurationException otherwise).
 * The map tab builds its MapView inside composition, so initialization must
 * happen here, at Application creation — never lazily from a screen.
 *
 * OSM raster tiles need no API key; the beta runs on-device with downloaded
 * packs and zero network (SPEC §1.4).
 */
class GuideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)
    }
}

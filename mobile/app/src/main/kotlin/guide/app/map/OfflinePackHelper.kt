package guide.app.map

import android.content.Context
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

/**
 * Offline city tiles via MapLibre Native (android-sdk:11.11.0).
 * Style: any offline-capable OSM style URL baked per pack release.
 * Kochi bbox covers Fort Kochi + Mattancherry at zoom 12..16.
 */
object OfflinePackHelper {
    val KOCHI_BOUNDS: LatLngBounds = LatLngBounds.Builder()
        .include(LatLng(9.950, 76.230))
        .include(LatLng(9.980, 76.265))
        .build()

    fun downloadCity(
        context: Context,
        styleUrl: String,
        onProgress: (done: Long, total: Long) -> Unit,
        onDone: () -> Unit,
    ) {
        val manager = OfflineManager.getInstance(context)
        val def = OfflineTilePyramidRegionDefinition(
            styleUrl, KOCHI_BOUNDS, 12.0, 16.0,
            context.resources.displayMetrics.density,
        )
        manager.createOfflineRegion(
            def, "{}".toByteArray(),
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(region: OfflineRegion) {
                    region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: org.maplibre.android.offline.OfflineRegionStatus) {
                            onProgress(status.completedResourceCount, status.requiredResourceCount)
                            if (status.isComplete) {
                                region.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                onDone()
                            }
                        }

                        override fun onError(error: org.maplibre.android.offline.OfflineRegionError) = Unit
                        override fun mapboxTileCountLimitExceeded(limit: Long) = Unit
                    })
                    region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) = Unit
            },
        )
    }
}

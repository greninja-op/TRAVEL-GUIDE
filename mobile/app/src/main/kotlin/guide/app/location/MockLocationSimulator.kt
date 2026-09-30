package guide.app.location

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.LocationServices
import guide.app.data.AppState
import guide.app.data.PackLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object MockLocationSimulator {

    private const val TAG = "MockLocationSimulator"

    data class SimulationState(
        val isRunning: Boolean = false,
        val currentPoiName: String? = null,
        val currentStepIndex: Int = 0,
        val totalSteps: Int = 0,
        val progressText: String = "",
    )

    private val _state = MutableStateFlow(SimulationState())
    val state = _state.asStateFlow()

    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @SuppressLint("MissingPermission")
    fun injectFix(context: Context, lat: Double, lng: Double, speedMps: Double = 1.4, appState: AppState? = null) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.setMockMode(true)
            val loc = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = lat
                longitude = lng
                altitude = 3.0
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                accuracy = 2.5f
                speed = speedMps.toFloat()
            }
            client.setMockLocation(loc)
        } catch (e: Exception) {
            Log.w(TAG, "MockLocation injection: ${e.message}")
        }
        appState?.onFix(lat, lng)
    }

    fun startRouteWalk(
        context: Context,
        appState: AppState,
        stepDelayMs: Long = 1200L,
        onPoiVisited: (PackLoader.PoiCard) -> Unit = {},
    ) {
        stopSimulation(context)

        val stops: List<PackLoader.PoiCard> = appState.cards
        if (stops.isEmpty()) return

        _state.value = SimulationState(
            isRunning = true,
            currentPoiName = stops[0].name,
            currentStepIndex = 1,
            totalSteps = stops.size,
            progressText = "Starting walk at ${stops[0].name}",
        )

        simulationJob = scope.launch {
            try {
                for (index in stops.indices) {
                    if (!_state.value.isRunning) break
                    val stop = stops[index]

                    _state.value = _state.value.copy(
                        currentPoiName = stop.name,
                        currentStepIndex = index + 1,
                        progressText = "Walking to ${stop.name} (${index + 1}/${stops.size})",
                    )

                    // 1. Approach stop from 50 meters out
                    val approachLat = stop.lat - 0.0004
                    val approachLng = stop.lng - 0.0004
                    injectFix(context, approachLat, approachLng, speedMps = 1.4, appState = appState)
                    delay(stepDelayMs)

                    // 2. Cross geofence boundary into POI
                    injectFix(context, stop.lat, stop.lng, speedMps = 0.8, appState = appState)
                    _state.value = _state.value.copy(
                        progressText = "Arrived inside ${stop.name} (Dwelling inside geofence)",
                    )

                    // 3. Dwell inside the stop (simulate dwell requirement)
                    for (dwellStep in 1..3) {
                        if (!_state.value.isRunning) break
                        injectFix(context, stop.lat, stop.lng, speedMps = 0.3, appState = appState)
                        delay(stepDelayMs)
                    }

                    onPoiVisited(stop)
                    delay(stepDelayMs)
                }

                _state.value = _state.value.copy(
                    isRunning = false,
                    progressText = "Route simulation completed! Visited ${stops.size} stops.",
                )
            } catch (e: Exception) {
                Log.w(TAG, "Simulation interrupted: ${e.message}")
                _state.value = SimulationState(isRunning = false, progressText = "Simulation cancelled.")
            }
        }
    }

    fun jumpToPoi(context: Context, appState: AppState, poiId: String) {
        val stop = appState.card(poiId) ?: return
        injectFix(context, stop.lat, stop.lng, speedMps = 0.5, appState = appState)
        _state.value = SimulationState(
            isRunning = false,
            currentPoiName = stop.name,
            currentStepIndex = 1,
            totalSteps = 1,
            progressText = "Jumped directly to ${stop.name}",
        )
    }

    fun stopSimulation(context: Context? = null) {
        simulationJob?.cancel()
        simulationJob = null
        _state.value = SimulationState(isRunning = false, progressText = "Simulation idle.")
        if (context != null) {
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.setMockMode(false)
            } catch (_: Exception) {}
        }
    }
}

class MockLocationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appState = guide.app.MainActivity.currentAppState

        when (intent.action) {
            "guide.app.location.MOCK_FIX" -> {
                val lat = intent.getDoubleExtra("lat", intent.getFloatExtra("lat", 9.9667f).toDouble())
                val lng = intent.getDoubleExtra("lng", intent.getFloatExtra("lng", 76.2425f).toDouble())
                val speed = intent.getDoubleExtra("speed", 1.4)
                MockLocationSimulator.injectFix(context, lat, lng, speed, appState)
                Log.i("MockLocationReceiver", "Injected GPS fix: $lat, $lng (speed: $speed m/s)")
            }
            "guide.app.location.SIMULATE_STOP" -> {
                val poiId = intent.getStringExtra("poi_id") ?: "chinese-fishing-nets"
                if (appState != null) {
                    MockLocationSimulator.jumpToPoi(context, appState, poiId)
                } else {
                    val (lat, lng) = when (poiId) {
                        "chinese-fishing-nets" -> 9.9667 to 76.2425
                        "fort-kochi-beach" -> 9.9658 to 76.2438
                        "vasco-square" -> 9.9663 to 76.2421
                        "st-francis-church" -> 9.9659 to 76.2415
                        "santa-cruz-cathedral" -> 9.9642 to 76.2429
                        else -> 9.9667 to 76.2425
                    }
                    MockLocationSimulator.injectFix(context, lat, lng, 0.5, null)
                }
                Log.i("MockLocationReceiver", "Simulated stop jump to: $poiId")
            }
            "guide.app.location.STOP_WALK" -> {
                MockLocationSimulator.stopSimulation(context)
                Log.i("MockLocationReceiver", "Stopped GPS walk simulation")
            }
        }
    }
}

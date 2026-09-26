package guide.app.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/** Compass azimuth 0..360 for "What am I seeing?" (shared-core Seeing.pick). */
class HeadingSensor(context: Context) {
    private val mgr = context.getSystemService(SensorManager::class.java)
    private var listener: SensorEventListener? = null
    var azimuthDeg: Double? = null
        private set

    fun start(onHeading: (Double) -> Unit = {}) {
        val sensor = mgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) ?: return
        stop()
        listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val r = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(r, e.values)
                val o = FloatArray(3)
                SensorManager.getOrientation(r, o)
                azimuthDeg = ((Math.toDegrees(o[0].toDouble()) + 360) % 360)
                onHeading(azimuthDeg!!)
            }

            override fun onAccuracyChanged(s: Sensor?, acc: Int) = Unit
        }
        mgr.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        listener?.let { mgr.unregisterListener(it) }
        listener = null
    }
}

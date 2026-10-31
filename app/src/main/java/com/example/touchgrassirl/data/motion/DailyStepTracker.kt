package com.example.touchgrassirl.data.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.LocalDate

/**
 * Always-on daily step counter. TYPE_STEP_COUNTER is cumulative since boot;
 * [step_baseline] is the counter value observed at day start. Runs while the
 * foreground service process is alive, no manual session needed.
 */
class DailyStepTracker(
    context: Context,
    private val onDailySteps: (steps: Int) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val prefs = context.getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)
    private var lastNotified = -1

    fun register() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister() {
        stepSensor?.let { sensorManager.unregisterListener(this, it) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values[0]
        val today = LocalDate.now().toEpochDay()

        val steps: Int
        if (prefs.getLong(KEY_STEP_DAY, -1L) != today) {
            prefs.edit()
                .putLong(KEY_STEP_DAY, today)
                .putFloat(KEY_STEP_BASELINE, total)
                .putInt(KEY_STEP_COUNT, 0)
                .apply()
            steps = 0
        } else {
            var baseline = prefs.getFloat(KEY_STEP_BASELINE, total)
            if (total < baseline) {
                // shortcut: reboot mid-day resets step counter, pre-reboot steps lost until next day
                baseline = total
                prefs.edit().putFloat(KEY_STEP_BASELINE, baseline).apply()
            }
            steps = (total - baseline).toInt().coerceAtLeast(0)
            prefs.edit().putInt(KEY_STEP_COUNT, steps).apply()
        }

        if (steps != lastNotified) {
            lastNotified = steps
            onDailySteps(steps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val KEY_STEP_DAY = "step_day"
        const val KEY_STEP_BASELINE = "step_baseline"
        const val KEY_STEP_COUNT = "step_count"
    }
}

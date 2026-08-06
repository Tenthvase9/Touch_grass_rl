package com.example.touchgrassirl.data.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.touchgrassirl.data.location.OutdoorLocationTracker
import com.example.touchgrassirl.data.weather.WeatherClient
import com.example.touchgrassirl.domain.NatureSpotGenerator
import com.example.touchgrassirl.domain.SessionMotionSnapshot
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionMotionTracker(
    context: Context,
    private val locationTracker: OutdoorLocationTracker,
    private val weatherClient: WeatherClient,
    private val onNearSpot: suspend (lat: Double, lng: Double) -> Unit,
    private val onStatsUpdate: ((minutes: Int, steps: Int, xp: Int) -> Unit)? = null,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _stats = MutableStateFlow(SessionMotionSnapshot())
    val stats: StateFlow<SessionMotionSnapshot> = _stats.asStateFlow()

    private var stepBaseline: Float? = null
    private var sessionSteps = 0
    private var distanceMeters = 0.0
    private var lastPosition: GeoPoint? = null
    private var isRaining = false

    private var locationJob: Job? = null
    private var running = false
    private var startTimeMillis: Long = 0L

    fun start(scope: CoroutineScope) {
        if (running) return
        running = true
        startTimeMillis = System.currentTimeMillis()
        stepBaseline = null
        sessionSteps = 0
        distanceMeters = 0.0
        lastPosition = null
        isRaining = false
        _stats.value = SessionMotionSnapshot()

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        locationJob = scope.launch {
            locationTracker.locationUpdates().collect { geoPoint ->
                lastPosition?.let { prev ->
                    distanceMeters += NatureSpotGenerator.distanceMeters(
                        prev.latitude,
                        prev.longitude,
                        geoPoint.latitude,
                        geoPoint.longitude,
                    )
                }
                lastPosition = geoPoint
                publishStats()
                onNearSpot(geoPoint.latitude, geoPoint.longitude)
            }
        }

        scope.launch {
            val location = locationTracker.getCurrentLocation()
            if (location != null) {
                isRaining = weatherClient.isRaining(location.latitude, location.longitude)
                publishStats()
            }
        }
    }

    fun stop(): SessionMotionSnapshot {
        running = false
        stepSensor?.let { sensorManager.unregisterListener(this, it) }
        locationJob?.cancel()
        locationJob = null
        return _stats.value
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        val total = event.values[0]
        if (stepBaseline == null) {
            stepBaseline = total
        }
        sessionSteps = (total - (stepBaseline ?: total)).toInt().coerceAtLeast(0)
        publishStats()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun publishStats() {
        _stats.update {
            SessionMotionSnapshot(
                steps = sessionSteps,
                distanceMeters = distanceMeters.toInt(),
                isRaining = isRaining,
                newlyVisitedSpotIds = it.newlyVisitedSpotIds,
                newlyCollectedIds = it.newlyCollectedIds,
            )
        }
        val elapsedMin = ((System.currentTimeMillis() - startTimeMillis) / 60_000L).toInt()
        val xp = elapsedMin
        onStatsUpdate?.invoke(elapsedMin, sessionSteps, xp)
    }

    fun appendExplorationResults(visited: List<String>, collected: List<String>) {
        _stats.update {
            it.copy(
                newlyVisitedSpotIds = it.newlyVisitedSpotIds + visited,
                newlyCollectedIds = it.newlyCollectedIds + collected,
            )
        }
    }
}

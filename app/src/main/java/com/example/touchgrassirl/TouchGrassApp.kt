package com.example.touchgrassirl

import android.app.Application
import com.example.touchgrassirl.data.local.TouchGrassDatabase
import com.example.touchgrassirl.data.location.OutdoorLocationTracker
import com.example.touchgrassirl.data.motion.SessionMotionTracker
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.data.weather.WeatherClient
import org.osmdroid.config.Configuration

class TouchGrassApp : Application() {

    val database: TouchGrassDatabase by lazy {
        TouchGrassDatabase.getInstance(this)
    }

    val locationTracker: OutdoorLocationTracker by lazy {
        OutdoorLocationTracker(this)
    }

    val weatherClient: WeatherClient by lazy {
        WeatherClient()
    }

    val repository: TouchGrassRepository by lazy {
        TouchGrassRepository(
            database = database,
            locationTracker = locationTracker,
            weatherClient = weatherClient,
        )
    }

    lateinit var sessionMotionTracker: SessionMotionTracker
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize osmdroid configuration
        Configuration.getInstance().userAgentValue = packageName

        sessionMotionTracker = SessionMotionTracker(
            context = this,
            locationTracker = locationTracker,
            weatherClient = weatherClient,
            onNearSpot = { lat, lng ->
                val result = repository.processLocationUpdate(lat, lng)
                sessionMotionTracker.appendExplorationResults(
                    visited = result.newlyVisitedSpots.map { it.id },
                    collected = result.newlyCollectedIds,
                )
            },
        )
    }
}

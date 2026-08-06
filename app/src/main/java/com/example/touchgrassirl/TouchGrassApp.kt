package com.example.touchgrassirl

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.touchgrassirl.data.local.TouchGrassDatabase
import com.example.touchgrassirl.data.location.OutdoorLocationTracker
import com.example.touchgrassirl.data.motion.SessionMotionTracker
import com.example.touchgrassirl.data.repository.FirebaseSocialRepository
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.data.repository.TouchGrassRepository
import com.example.touchgrassirl.data.service.OutdoorDetectionService
import com.example.touchgrassirl.data.service.TrackingService
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

    val socialRepository: SocialRepository by lazy {
        FirebaseSocialRepository()
    }

    lateinit var sessionMotionTracker: SessionMotionTracker
        private set

    override fun onCreate() {
        super.onCreate()

        Configuration.getInstance().apply {
            userAgentValue = "TouchGrassIRL/1.0 (Android; +https://github.com/matthewmichihara/touchgrassirl)"
            osmdroidBasePath = filesDir
            osmdroidTileCache = cacheDir
        }

        createNotificationChannels()

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
            onStatsUpdate = { minutes, steps, xp ->
                TrackingService.updateNotification(this, minutes, steps, xp)
            },
        )
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            TrackingService.CHANNEL_ID,
            "Outdoor Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows your active outdoor session status"
            setShowBadge(false)
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            "reminder_channel",
            "Grass Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminds you to go outside"
            manager.createNotificationChannel(this)
        }

        NotificationChannel(
            OutdoorDetectionService.CHANNEL_ID,
            "Outdoor Detection",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when outdoor time is being tracked"
            setShowBadge(false)
            manager.createNotificationChannel(this)
        }
    }
}

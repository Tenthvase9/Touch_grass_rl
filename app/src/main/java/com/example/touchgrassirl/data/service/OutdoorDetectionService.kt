package com.example.touchgrassirl.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.touchgrassirl.MainActivity
import com.example.touchgrassirl.R
import com.example.touchgrassirl.TouchGrassApp
import com.example.touchgrassirl.data.local.TouchGrassDatabase
import com.example.touchgrassirl.data.local.entity.DailyLogEntity
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.domain.ProgressCalculator
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class OutdoorDetectionService : Service() {

    private lateinit var database: TouchGrassDatabase

    private var outdoorStartMillis: Long = 0L
    private var isOutdoors: Boolean = false
    private var lastActivityType: Int = DetectedActivity.STILL
    private var todayEpochDay: Long = 0L

    private val geofencingClient by lazy { LocationServices.getGeofencingClient(this) }
    private val activityClient by lazy { ActivityRecognition.getClient(this) }
    private val scope = CoroutineScope(Dispatchers.IO)

    private val socialRepository: SocialRepository? by lazy {
        (application as? TouchGrassApp)?.socialRepository
    }

    override fun onCreate() {
        super.onCreate()
        database = TouchGrassDatabase.getInstance(this)
        todayEpochDay = LocalDate.now().toEpochDay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "ACTION_EXITED_HOME" -> onExitedHome()
                "ACTION_ENTERED_HOME" -> onEnteredHome()
                "ACTION_ACTIVITY_TRANSITION" -> {
                    val activityType = intent.getIntExtra("activity_type", DetectedActivity.STILL)
                    val transitionType = intent.getIntExtra("transition_type", -1)
                    if (transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                        handleActivityTransition(activityType)
                    }
                }
                else -> ensureStarted()
            }
        } else {
            ensureStarted()
        }
        return START_STICKY
    }

    private fun ensureStarted() {
        startForeground(NOTIFICATION_ID, buildNotification(0))
        registerGeofence()
        registerActivityTransitions()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerGeofence() {
        val prefs = getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)
        val homeLat = prefs.getFloat("home_lat", 0f).toDouble()
        val homeLng = prefs.getFloat("home_lng", 0f).toDouble()
        if (homeLat == 0.0 && homeLng == 0.0) return

        val geofence = Geofence.Builder()
            .setRequestId("home_geofence")
            .setCircularRegion(homeLat, homeLng, GEOFENCE_RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER
            )
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent())
                .addOnSuccessListener { }
                .addOnFailureListener { }
        } catch (_: SecurityException) {
        }
    }

    private fun registerActivityTransitions() {
        val activities = listOf(
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE,
            DetectedActivity.ON_FOOT,
            DetectedActivity.RUNNING,
            DetectedActivity.WALKING,
            DetectedActivity.STILL,
        )
        val transitions = activities.map { activityType ->
            ActivityTransition.Builder()
                .setActivityType(activityType)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build()
        }

        val request = ActivityTransitionRequest(transitions)

        try {
            activityClient.requestActivityTransitionUpdates(request, activityPendingIntent())
                .addOnSuccessListener { }
                .addOnFailureListener { }
        } catch (_: SecurityException) {
        }
    }

    private fun geofencePendingIntent(): PendingIntent {
        val intent = Intent(this, GeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun activityPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this,
            ACTIVITY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun handleActivityTransition(activityType: Int) {
        lastActivityType = activityType
        if (!isOutdoors) return

        if (activityType == DetectedActivity.STILL) {
            val elapsedMin = ((SystemClock.elapsedRealtime() - outdoorStartMillis) / 60_000L).toInt()
            if (elapsedMin > 0) {
                awardOutdoorTime(elapsedMin)
            }
            isOutdoors = false
            outdoorStartMillis = 0L
        } else {
            outdoorStartMillis = SystemClock.elapsedRealtime()
        }
        updateNotification()
    }

    private fun onExitedHome() {
        if (!isOutdoors) {
            isOutdoors = true
            outdoorStartMillis = SystemClock.elapsedRealtime()
            updateNotification()
        }
    }

    private fun onEnteredHome() {
        if (isOutdoors) {
            val elapsedMin = ((SystemClock.elapsedRealtime() - outdoorStartMillis) / 60_000L).toInt()
            if (elapsedMin > 0) {
                awardOutdoorTime(elapsedMin)
            }
            isOutdoors = false
            updateNotification()
        }
    }

    private fun awardOutdoorTime(minutes: Int) {
        val today = LocalDate.now().toEpochDay()
        if (today != todayEpochDay) {
            todayEpochDay = today
        }

        scope.launch {
            val existing = database.dailyLogDao().getForDay(today)
            val xp = minutes * GameConstants.XP_PER_OUTDOOR_MINUTE
            if (existing != null) {
                database.dailyLogDao().upsert(
                    existing.copy(
                        outdoorMinutes = existing.outdoorMinutes + minutes,
                        xpEarned = existing.xpEarned + xp,
                        touchedGrass = existing.touchedGrass || minutes >= GameConstants.MIN_OUTDOOR_MINUTES,
                    )
                )
            } else {
                database.dailyLogDao().upsert(
                    DailyLogEntity(
                        dateEpochDay = today,
                        outdoorMinutes = minutes,
                        xpEarned = xp,
                        touchedGrass = minutes >= GameConstants.MIN_OUTDOOR_MINUTES,
                    )
                )
            }

            val progress = database.userProgressDao().getProgress()
            if (progress != null) {
                val newTotalMinutes = progress.totalOutdoorMinutes + minutes
                database.userProgressDao().upsert(
                    progress.copy(
                        totalOutdoorMinutes = newTotalMinutes,
                        totalXp = progress.totalXp + xp,
                    )
                )
                val level = ProgressCalculator.levelFromTotalXp(progress.totalXp + xp)
                socialRepository?.syncMyStats(
                    outdoorMinutes = newTotalMinutes,
                    streak = progress.currentStreak,
                    level = level,
                )
            }
            updateNotification()
        }
    }

    private fun buildNotification(minutes: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = if (isOutdoors) {
            "\uD83C\uDF31 Outside now — $minutes min today"
        } else {
            "\uD83C\uDF3F Ready to track your outdoor time"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Touch Grass IRL")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_track)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        scope.launch {
            val today = LocalDate.now().toEpochDay()
            val log = database.dailyLogDao().getForDay(today)
            val minutes = log?.outdoorMinutes ?: 0
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(minutes))
        }
    }

    override fun onDestroy() {
        geofencingClient.removeGeofences(geofencePendingIntent())
        activityClient.removeActivityTransitionUpdates(activityPendingIntent())
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "outdoor_detection"
        const val NOTIFICATION_ID = 2001
        const val GEOFENCE_RADIUS_METERS = 150f
        const val GEOFENCE_REQUEST_CODE = 3001
        const val ACTIVITY_REQUEST_CODE = 3002

        fun start(context: Context) {
            val intent = Intent(context, OutdoorDetectionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OutdoorDetectionService::class.java)
            context.stopService(intent)
        }
    }
}

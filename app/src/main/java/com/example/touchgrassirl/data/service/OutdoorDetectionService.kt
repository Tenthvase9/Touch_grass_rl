package com.example.touchgrassirl.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.touchgrassirl.MainActivity
import com.example.touchgrassirl.R
import com.example.touchgrassirl.TouchGrassApp
import com.example.touchgrassirl.data.local.TouchGrassDatabase
import com.example.touchgrassirl.data.local.entity.DailyLogEntity
import com.example.touchgrassirl.data.repository.SocialRepository
import com.example.touchgrassirl.domain.GameConstants
import com.example.touchgrassirl.domain.ProgressCalculator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class OutdoorDetectionService : Service() {

    private lateinit var database: TouchGrassDatabase
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val scope = CoroutineScope(Dispatchers.IO)
    private var samplerJob: Job? = null

    private var lastSampleMillis: Long = 0L
    private var isAway: Boolean = false
    private var todayEpochDay: Long = 0L

    private val socialRepository: SocialRepository? by lazy {
        (application as? TouchGrassApp)?.socialRepository
    }

    override fun onCreate() {
        super.onCreate()
        database = TouchGrassDatabase.getInstance(this)
        todayEpochDay = LocalDate.now().toEpochDay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureStarted()
        return START_STICKY
    }

    private fun ensureStarted() {
        startForeground(NOTIFICATION_ID, buildNotification(0))
        samplerJob?.cancel()
        samplerJob = scope.launch { samplerLoop() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun samplerLoop() {
        while (scope.isActive) {
            try {
                trySample()
            } catch (_: Exception) {
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    private suspend fun trySample() {
        val today = LocalDate.now().toEpochDay()
        if (today != todayEpochDay) {
            todayEpochDay = today
            lastSampleMillis = 0L
            isAway = false
        }

        val location = try {
            withTimeout(30_000) {
                Tasks.await(
                    fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null),
                )
            }
        } catch (_: Exception) {
            null
        } ?: return

        if (location.accuracy > ACCURACY_THRESHOLD_METERS) return

        val prefs = getSharedPreferences("touch_grass_prefs", Context.MODE_PRIVATE)
        var homeLat = prefs.getFloat("home_lat", 0f).toDouble()
        var homeLng = prefs.getFloat("home_lng", 0f).toDouble()
        if (homeLat == 0.0 && homeLng == 0.0) {
            // Auto-set home from the first good fix so tracking works without manual setup.
            prefs.edit()
                .putFloat("home_lat", location.latitude.toFloat())
                .putFloat("home_lng", location.longitude.toFloat())
                .apply()
            return
        }

        val dist = distanceMeters(homeLat, homeLng, location.latitude, location.longitude)
        val nowMs = System.currentTimeMillis()

        if (dist > AWAY_THRESHOLD_METERS) {
            if (lastSampleMillis != 0L) {
                val delta = ((nowMs - lastSampleMillis) / 60_000L).toInt()
                if (delta > 0) awardOutdoorTime(delta)
            }
            lastSampleMillis = nowMs
            isAway = true
        } else {
            if (isAway && lastSampleMillis != 0L) {
                val delta = ((nowMs - lastSampleMillis) / 60_000L).toInt()
                if (delta > 0) awardOutdoorTime(delta)
            }
            isAway = false
            lastSampleMillis = 0L
        }
        updateNotification()
    }

    private fun distanceMeters(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
    ): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
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
                        touchedGrass = existing.touchedGrass ||
                            minutes >= GameConstants.MIN_OUTDOOR_MINUTES,
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
                socialRepository?.addActivity(
                    type = "outdoor_time",
                    message = "Spent $minutes minutes outside",
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

        val contentText = if (isAway) {
            "\uD83C\uDF31 Outside now — $minutes min today"
        } else {
            "\uD83C\uDF3F Tracking your outdoor time"
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
        if (isAway && lastSampleMillis != 0L) {
            val delta = ((System.currentTimeMillis() - lastSampleMillis) / 60_000L).toInt()
            if (delta > 0) awardOutdoorTime(delta)
        }
        samplerJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "outdoor_detection"
        const val NOTIFICATION_ID = 2001
        const val SAMPLE_INTERVAL_MS = 10 * 60_000L
        const val AWAY_THRESHOLD_METERS = 500.0
        const val ACCURACY_THRESHOLD_METERS = 100.0

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

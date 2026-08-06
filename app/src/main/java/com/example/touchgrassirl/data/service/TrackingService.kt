package com.example.touchgrassirl.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.touchgrassirl.MainActivity
import com.example.touchgrassirl.R

class TrackingService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(0, 0, 0)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(minutes: Int, steps: Int, xp: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentText = if (minutes > 0) {
            "$minutes min · $steps steps · +$xp XP"
        } else {
            "Touching grass..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("\uD83C\uDF3F Outdoor Session Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_track)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Outdoor Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows your active outdoor session status"
            setShowBadge(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "tracking_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE = "com.example.touchgrassirl.UPDATE_TRACKING"

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            context.stopService(intent)
        }

        fun updateNotification(context: Context, minutes: Int, steps: Int, xp: Int) {
            val contentText = if (minutes > 0) {
                "$minutes min · $steps steps · +$xp XP"
            } else {
                "Touching grass..."
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("\uD83C\uDF3F Outdoor Session Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_track)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }
}

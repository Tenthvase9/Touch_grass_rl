package com.example.touchgrassirl.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.touchgrassirl.MainActivity
import com.example.touchgrassirl.R
import kotlin.random.Random

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val messages = listOf(
            "\uD83C\uDF3F Time to touch grass! Even 5 minutes counts.",
            "\u2600\uFE0F The sun is calling your name. Go outside!",
            "\uD83C\uDF31 Your garden misses you. Take a walk!",
            "\uD83D\uDC4A Step away from the screen. Nature awaits!",
            "\uD83C\uDF43 Fresh air is free. Go grab some!",
            "\uD83D\uDE0E You know what\'s better than sitting inside? Literally anything outside.",
            "\uD83C\uDF1E Vitamin D delivery is available. Pick it up!",
        )

        val message = messages.random()
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "reminder_channel"
        val channel = NotificationChannel(
            channelId,
            "Grass Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminds you to go outside"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("\uD83C\uDF3F Touch Grass Reminder")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_track)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(Random.nextInt(), notification)
    }
}

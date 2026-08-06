package com.example.touchgrassirl.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransitionResult

class ActivityTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return

        for (event in result.transitionEvents) {
            val serviceIntent = Intent(context, OutdoorDetectionService::class.java).apply {
                action = "ACTION_ACTIVITY_TRANSITION"
                putExtra("activity_type", event.activityType)
                putExtra("transition_type", event.transitionType)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}

package com.example.touchgrassirl.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        val serviceIntent = Intent(context, OutdoorDetectionService::class.java).apply {
            action = when (transition) {
                Geofence.GEOFENCE_TRANSITION_EXIT -> "ACTION_EXITED_HOME"
                Geofence.GEOFENCE_TRANSITION_ENTER -> "ACTION_ENTERED_HOME"
                else -> return
            }
        }
        context.startForegroundService(serviceIntent)
    }
}

package com.example.touchgrassirl.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OutdoorLocationTracker(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        5_000L,
    )
        .setMinUpdateIntervalMillis(3_000L)
        .setMinUpdateDistanceMeters(4f)
        .build()

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): GeoPoint? = suspendCancellableCoroutine { cont ->
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                cont.resume(
                    location?.let { GeoPoint(it.latitude, it.longitude) },
                )
            }
            .addOnFailureListener { cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<GeoPoint> = callbackFlow {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    trySend(GeoPoint(it.latitude, it.longitude))
                }
            }
        }
        fusedClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper(),
        )
        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }
}

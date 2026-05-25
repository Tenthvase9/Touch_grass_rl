package com.example.touchgrassirl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outdoor_sessions")
data class OutdoorSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null,
    val durationMinutes: Int = 0,
    val sessionSteps: Int = 0,
    val sessionDistanceMeters: Int = 0,
    val xpAwarded: Int = 0,
    val countedForDaily: Boolean = false,
    val isActive: Boolean = true,
)

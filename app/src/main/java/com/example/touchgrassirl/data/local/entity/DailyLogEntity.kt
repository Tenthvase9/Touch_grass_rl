package com.example.touchgrassirl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val dateEpochDay: Long,
    val touchedGrass: Boolean = false,
    val outdoorMinutes: Int = 0,
    val xpEarned: Int = 0,
    val steps: Int = 0,
    val distanceMeters: Int = 0,
    val challengeId: String? = null,
    val challengeCompleted: Boolean = false,
    val stepsXpClaimed: Boolean = false,
    val parkVisitXpClaimed: Boolean = false,
)

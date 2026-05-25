package com.example.touchgrassirl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.touchgrassirl.domain.GameConstants

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val totalXp: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastTouchGrassEpochDay: Long? = null,
    val totalSessionsCompleted: Int = 0,
    val gardenPlotCount: Int = 1,
    val dailyGoalMinutes: Int = GameConstants.DEFAULT_DAILY_GOAL_MINUTES,
    val totalOutdoorMinutes: Int = 0,
)

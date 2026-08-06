package com.example.touchgrassirl.data.repository

import java.io.Serializable

data class SessionResult(
    val durationMinutes: Int,
    val sessionSteps: Int,
    val sessionDistanceMeters: Int,
    val xpEarned: Int,
    val outdoorXp: Int,
    val streakXp: Int,
    val challengeXp: Int,
    val stepsXp: Int,
    val touchedGrassToday: Boolean,
    val countedThisSession: Boolean,
    val newStreak: Int,
    val leveledUp: Boolean,
    val newLevel: Int,
    val levelTitleRes: Int,
    val gardenPlots: Int,
    val challengeCompleted: Boolean,
    val newlyUnlockedAchievementIds: List<String>,
    val newlyCollectedIds: List<String>,
    val newlyVisitedSpotNames: List<String>,
) : Serializable

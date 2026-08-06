package com.example.touchgrassirl.data.repository

data class WeeklyStats(
    val totalMinutes: Int,
    val totalSessions: Int,
    val totalSteps: Int,
    val streakDays: Int,
    val dailyBreakdown: List<Int>,
)

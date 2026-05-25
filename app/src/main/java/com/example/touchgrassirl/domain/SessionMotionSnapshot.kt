package com.example.touchgrassirl.domain

data class SessionMotionSnapshot(
    val steps: Int = 0,
    val distanceMeters: Int = 0,
    val isRaining: Boolean = false,
    val newlyVisitedSpotIds: List<String> = emptyList(),
    val newlyCollectedIds: List<String> = emptyList(),
)

package com.example.touchgrassirl.data.repository

import com.example.touchgrassirl.domain.NatureSpot

data class LocationExplorationResult(
    val newlyVisitedSpots: List<NatureSpot> = emptyList(),
    val newlyCollectedIds: List<String> = emptyList(),
    val parkXpAwarded: Int = 0,
)

package com.example.touchgrassirl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_spots")
data class VisitedSpotEntity(
    @PrimaryKey val spotId: String,
    val visitedAtMillis: Long,
    val spotName: String,
    val spotType: String,
)

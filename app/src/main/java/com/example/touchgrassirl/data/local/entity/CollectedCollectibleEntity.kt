package com.example.touchgrassirl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collected_collectibles")
data class CollectedCollectibleEntity(
    @PrimaryKey val id: String,
    val collectedAtMillis: Long,
)

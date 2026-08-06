package com.example.touchgrassirl.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey val profileId: String,
    val displayName: String,
    val totalOutdoorMinutes: Int = 0,
    val currentStreak: Int = 0,
    val level: Int = 1,
    val lastActiveEpochDay: Long = 0,
    val status: String = "accepted",
    val addedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "pending_requests")
data class PendingRequestEntity(
    @PrimaryKey val profileId: String,
    val displayName: String,
    val direction: String,
    val sentAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "gifts")
data class GiftEntity(
    @PrimaryKey val id: String,
    val fromProfileId: String,
    val fromDisplayName: String,
    val toProfileId: String,
    val giftType: String,
    val message: String = "",
    val sentAtMillis: Long = System.currentTimeMillis(),
    val claimed: Boolean = false,
)

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val displayName: String,
    val type: String,
    val message: String,
    val timestampMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "my_profile")
data class MyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val profileId: String,
    val displayName: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

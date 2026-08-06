package com.example.touchgrassirl.data.repository

import com.example.touchgrassirl.data.local.entity.ActivityEntity
import com.example.touchgrassirl.data.local.entity.FriendEntity
import com.example.touchgrassirl.data.local.entity.GiftEntity
import com.example.touchgrassirl.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SocialRepository {
    fun observeFriends(): Flow<List<FriendEntity>>
    fun observePendingRequests(): Flow<List<PendingRequestInfo>>
    fun observeGifts(): Flow<List<GiftEntity>>

    suspend fun ensureProfileCreated(): String
    suspend fun getMyProfileId(): String

    suspend fun sendFriendRequest(targetProfileId: String)
    suspend fun acceptFriendRequest(friendUid: String)
    suspend fun declineFriendRequest(friendUid: String)

    suspend fun sendGift(toProfileId: String, giftType: String, message: String)
    suspend fun claimGift(giftId: String)

    suspend fun updateDisplayName(name: String) {}
    suspend fun getMyDisplayName(): String = "Nature Explorer"

    suspend fun syncMyStats(outdoorMinutes: Int, streak: Int, level: Int) {}

    suspend fun addActivity(type: String, message: String) {}
    fun observeActivities(): Flow<List<ActivityEntity>> = flowOf(emptyList())

    suspend fun updateProfile(bio: String, avatar: String) {}
    suspend fun getMyProfile(): Map<String, Any?> = emptyMap()

    suspend fun saveLocation(latitude: Double, longitude: Double, timestamp: Long) {}
    fun observeLocations(): Flow<List<LocationEntity>> = flowOf(emptyList())

    suspend fun updateWeatherBadges(badges: Map<String, Int>) {}
    suspend fun getWeatherBadges(): Map<String, Int> = emptyMap()

    companion object {
        val GIFT_TYPES = listOf(
            "sunflower" to "\uD83C\uDF3B Sunflower",
            "sapling" to "\uD83C\uDF3F Sapling",
            "mushroom" to "\uD83C\uDF44 Mushroom",
            "clover" to "\u2740 Lucky Clover",
            "pine" to "\uD83C\uDF32 Pine Tree",
        )
    }
}

data class PendingRequestInfo(
    val uid: String = "",
    val profileId: String,
    val displayName: String,
)
